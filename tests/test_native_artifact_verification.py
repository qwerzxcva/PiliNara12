import importlib.util
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'scripts' / 'verify_native_artifacts.py'
spec = importlib.util.spec_from_file_location('native_artifact_verifier', SCRIPT)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)


def elf_fixture(machine=183, alignment=16384, offset=0, address=0,
                file_size=120, memory_size=120, kind=1):
    ident = b'\x7fELF' + bytes((2, 1, 1)) + bytes(9)
    header = struct.pack('<16sHHIQQQIHHHHHH', ident, 3, machine, 1,
                         0, 64, 0, 0, 64, 56, 1, 0, 0, 0)
    segment = struct.pack('<IIQQQQQQ', kind, 5, offset, address, 0,
                          file_size, memory_size, alignment)
    return header + segment


class NativeArtifactVerificationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.library = self.root / 'libfixture.so'
        self.library.write_bytes(elf_fixture())

    def reject(self, data):
        self.library.write_bytes(data)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_library(self.library)

    def test_valid_synthetic_arm64_library(self):
        result = verifier.verify_library(self.library)
        self.assertEqual(result['machine'], 'AArch64')
        self.assertEqual(result['load_segments'], 1)
        self.assertEqual(result['minimum_load_alignment'], 16384)

    def test_wrong_architecture_and_elf_identity_are_rejected(self):
        self.reject(elf_fixture(machine=62))
        for position, value in ((0, 0), (4, 1), (5, 2), (6, 0)):
            data = bytearray(elf_fixture())
            data[position] = value
            self.reject(data)
        data = bytearray(elf_fixture())
        struct.pack_into('<H', data, 16, 2)
        self.reject(data)

    def test_every_load_segment_requires_valid_alignment(self):
        for alignment in (0, 1, 4096, 24576):
            with self.subTest(alignment=alignment):
                self.reject(elf_fixture(alignment=alignment))
        self.reject(elf_fixture(address=1))
        self.library.write_bytes(elf_fixture(alignment=65536))
        result = verifier.verify_library(self.library)
        self.assertEqual(result['minimum_load_alignment'], 65536)

    def test_truncated_headers_and_segment_bounds_are_rejected(self):
        for length in (0, 20, 63, 100, 119):
            with self.subTest(length=length):
                self.reject(elf_fixture()[:length])
        self.reject(elf_fixture(file_size=121, memory_size=121))
        self.reject(elf_fixture(file_size=120, memory_size=119))
        self.reject(elf_fixture(kind=0))
        for count in (0, 2, 65535):
            data = bytearray(elf_fixture())
            struct.pack_into('<H', data, 56, count)
            self.reject(data)

    def test_zero_file_size_load_segment_still_requires_alignment(self):
        self.library.write_bytes(elf_fixture(file_size=0, memory_size=16384))
        result = verifier.verify_library(self.library)
        self.assertEqual(result['load_segments'], 1)
        self.assertEqual(result['minimum_load_alignment'], 16384)
        self.reject(elf_fixture(file_size=0, memory_size=16384, address=1))
        self.reject(elf_fixture(file_size=0, memory_size=16384, alignment=4096))

    def test_unused_program_header_ignores_undefined_fields(self):
        data = bytearray(elf_fixture(file_size=176, memory_size=176))
        struct.pack_into('<H', data, 56, 2)
        data.extend(struct.pack('<IIQQQQQQ', 0, 0, 2**64 - 1,
                                2**64 - 1, 0, 2**64 - 1, 0, 3))
        self.library.write_bytes(data)
        result = verifier.verify_library(self.library)
        self.assertEqual(result['load_segments'], 1)

    def test_mixed_valid_load_alignments_report_actual_minimum(self):
        data = bytearray(elf_fixture(alignment=65536, file_size=176, memory_size=176))
        struct.pack_into('<H', data, 56, 2)
        data.extend(struct.pack('<IIQQQQQQ', 1, 4, 0, 0, 0, 176, 176, 16384))
        self.library.write_bytes(data)
        result = verifier.verify_library(self.library)
        self.assertEqual(result['load_segments'], 2)
        self.assertEqual(result['minimum_load_alignment'], 16384)

    def test_invalid_second_load_segment_is_not_ignored(self):
        data = bytearray(elf_fixture(file_size=176, memory_size=176))
        struct.pack_into('<H', data, 56, 2)
        data.extend(struct.pack('<IIQQQQQQ', 1, 4, 0, 0, 0, 176, 176, 4096))
        self.reject(data)

    def test_missing_required_library_and_empty_tree_are_rejected(self):
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_directory(self.root, ['libmpv.so'])
        empty = self.root / 'empty'
        empty.mkdir()
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_directory(empty, [])

    def test_versioned_libraries_are_checked(self):
        versioned = self.root / 'libfixture.so.1'
        versioned.write_bytes(elf_fixture(machine=62))
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_directory(self.root, [])

    def test_in_tree_library_aliases_are_verified_once(self):
        versioned = self.root / 'libmpv.so.2.0'
        versioned.write_bytes(elf_fixture(alignment=65536))
        soname = self.root / 'libmpv.so.2'
        soname.symlink_to(versioned.name)
        alias = self.root / 'libmpv.so'
        alias.symlink_to(soname.name)
        results = verifier.verify_directory(self.root, ['libmpv.so', 'libmpv.so.2'])
        self.assertEqual(len(results), 2)
        result = next(item for item in results if item['name'] == versioned.name)
        self.assertEqual(result['minimum_load_alignment'], 65536)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_library(alias)

    def test_library_alias_cannot_hide_invalid_elf(self):
        target = self.root / 'invalid-target'
        target.write_bytes(elf_fixture(machine=62))
        (self.root / 'libalias.so').symlink_to(target.name)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_directory(self.root, ['libalias.so'])

    def test_outside_artifact_symlinks_are_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            outside = Path(directory) / 'liboutside.so'
            outside.write_bytes(elf_fixture())
            (self.root / 'libalias.so').symlink_to(outside)
            with self.assertRaises(verifier.VerificationError):
                verifier.verify_directory(self.root, ['libalias.so'])

    def test_broken_and_cyclic_symlinks_are_rejected(self):
        for cyclic in (False, True):
            with self.subTest(cyclic=cyclic), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                (root / 'libvalid.so').write_bytes(elf_fixture())
                alias = root / 'libalias.so'
                alias.symlink_to(alias.name if cyclic else 'missing.so')
                with self.assertRaises(verifier.VerificationError):
                    verifier.verify_directory(root, [])

    def test_directory_symlinks_are_rejected(self):
        directory = self.root / 'nested'
        directory.mkdir()
        (self.root / 'alias-directory').symlink_to(directory.name, target_is_directory=True)
        with self.assertRaises(verifier.VerificationError):
            verifier.verify_directory(self.root, [])

    def test_cli_success_preserves_verification_limits(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT), str(self.root),
             '--require-library', self.library.name],
            capture_output=True, text=True, timeout=10, check=False)
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        self.assertEqual(payload['abi'], 'arm64-v8a')
        for field in ('apk_zip_alignment', 'android_api_compatibility',
                      'gpu_next_rendering', 'device_playback'):
            self.assertEqual(payload[field], 'not_verified')

    def test_cli_failure_returns_nonzero_without_success_payload(self):
        result = subprocess.run(
            [sys.executable, str(SCRIPT), str(self.root),
             '--require-library', '../libmpv.so'],
            capture_output=True, text=True, timeout=10, check=False)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(result.stdout, '')
        self.assertIn('Invalid required library basename', result.stderr)


if __name__ == '__main__':
    unittest.main()
