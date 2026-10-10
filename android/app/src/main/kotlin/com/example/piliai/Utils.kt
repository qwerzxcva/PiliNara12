package com.example.piliai

import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? {
    return getParcelable(key, T::class.java)
}
