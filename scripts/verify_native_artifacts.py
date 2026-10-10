#!/usr/bin/env python3
"""Verify ARM64 shared-library ELF headers and 16 KB load-segment alignment.

This checks native files only. It does not verify APK ZIP alignment, Android
API compatibility, JNI exports, dependency resolution, or GPU-next rendering.
"""
import argparse
import json
from pathlib import Path
import struct
import sys


PAGE_SIZE = 16384
ELF_HEADER = struct.Struct('<16sHHIQQQIHHHHHH')
PROGRAM_HEADER = struct.Struct('<IIQQQQQQ')


class VerificationError(Exception):
    pass


def verify_library(path):
    if path.is_symlink() or not path.is_file():
        raise VerificationError(f'{path}: expected a regular, non-symlink library')
    size = path.stat().st_size
    with path.open('rb') as stream:
        raw = stream.read(ELF_HEADER.size)
        if len(raw) != ELF_HEADER.size:
            raise VerificationError(f'{path}: truncated ELF header')
        header = ELF_HEADER.unpack(raw)
        ident, elf_type, machine, version = header[:4]
        if ident[:4] != b'\x7fELF':
            raise VerificationError(f'{path}: not an ELF file')
        if ident[4:7] != bytes((2, 1, 1)):
            raise VerificationError(f'{path}: requires ELF64, little-endian, version 1')
        if elf_type != 3 or machine != 183 or version != 1:
            raise VerificationError(f'{path}: requires an AArch64 ET_DYN shared object')
        ph_offset = header[5]
        eh_size, ph_size, ph_count = header[8:11]
        if eh_size != ELF_HEADER.size or ph_size != PROGRAM_HEADER.size:
            raise VerificationError(f'{path}: invalid ELF or program-header size')
        if ph_count == 0 or ph_count == 65535:
            raise VerificationError(f'{path}: missing or unsupported extended program-header count')
        if ph_offset < eh_size or ph_offset + ph_count * ph_size > size:
            raise VerificationError(f'{path}: program-header table exceeds file bounds')
        loads = 0
        minimum_alignment = None
        for index in range(ph_count):
            stream.seek(ph_offset + index * ph_size)
            entry = stream.read(ph_size)
            if len(entry) != ph_size:
                raise VerificationError(f'{path}: truncated program header {index}')
            kind, flags, offset, address, physical, file_size, memory_size, alignment = PROGRAM_HEADER.unpack(entry)
            if kind == 0:
                continue
            if file_size and offset + file_size > size:
                raise VerificationError(f'{path}: segment {index} exceeds file bounds')
            if kind != 1:
                continue
            loads += 1
            if file_size > memory_size:
                raise VerificationError(f'{path}: load segment {index} has filesz greater than memsz')
            if alignment < PAGE_SIZE or alignment & (alignment - 1):
                raise VerificationError(f'{path}: load segment {index} alignment {alignment} is not a power of two >= {PAGE_SIZE}')
            if offset % alignment != address % alignment:
                raise VerificationError(f'{path}: load segment {index} offset/address alignment mismatch')
            minimum_alignment = alignment if minimum_alignment is None else min(minimum_alignment, alignment)
        if loads == 0:
            raise VerificationError(f'{path}: no PT_LOAD segments')
    return {'name': path.name, 'machine': 'AArch64', 'load_segments': loads,
            'minimum_load_alignment': minimum_alignment}


def verify_directory(root, required):
    if root.is_symlink() or not root.is_dir():
        raise VerificationError(f'{root}: expected a non-symlink artifact directory')
    resolved = root.resolve()
    libraries = []
    names = set()
    verified_paths = set()
    for path in sorted(root.rglob('*')):
        is_library = path.name.endswith('.so') or '.so.' in path.name
        if path.is_symlink():
            try:
                target = path.resolve(strict=True)
            except (OSError, RuntimeError) as error:
                raise VerificationError(f'{path}: broken or cyclic artifact symlink') from error
            if not target.is_relative_to(resolved):
                raise VerificationError(f'{path}: artifact symlink escapes the supplied directory')
            if not target.is_file():
                raise VerificationError(f'{path}: artifact symlink must reference a regular file')
        else:
            target = path.resolve()
        if not is_library:
            continue
        if not target.is_relative_to(resolved) or not target.is_file():
            raise VerificationError(f'{path}: expected an in-tree regular shared library')
        names.add(path.name)
        if target not in verified_paths:
            libraries.append(target)
            verified_paths.add(target)
    if not libraries:
        raise VerificationError(f'{root}: no shared libraries found')
    missing = sorted(set(required) - names)
    if missing:
        raise VerificationError('Missing required libraries: ' + ', '.join(missing))
    return [verify_library(path) for path in libraries]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('directory', type=Path)
    parser.add_argument('--require-library', action='append', default=[])
    args = parser.parse_args(argv)
    try:
        for name in args.require_library:
            if not name or Path(name).name != name or '/' in name or '\\' in name:
                raise VerificationError(f'Invalid required library basename: {name!r}')
        libraries = verify_directory(args.directory, args.require_library)
    except (VerificationError, OSError, struct.error) as error:
        print(f'Native artifact verification failed: {error}', file=sys.stderr)
        return 1
    print(json.dumps({
        'status': 'verified',
        'abi': 'arm64-v8a',
        'elf_page_alignment': PAGE_SIZE,
        'libraries': libraries,
        'apk_zip_alignment': 'not_verified',
        'android_api_compatibility': 'not_verified',
        'gpu_next_rendering': 'not_verified',
        'device_playback': 'not_verified'
    }, indent=2, sort_keys=True))
    return 0


if __name__ == '__main__':
    sys.exit(main())
