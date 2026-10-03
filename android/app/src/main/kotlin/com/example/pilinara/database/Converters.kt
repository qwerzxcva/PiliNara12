package com.example.pilinara.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters - 支持复杂类型的序列化
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun listToString(list: List<String>?): String? {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        if (value == null) return null
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun intListToString(list: List<Int>?): String? {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromStringSet(value: String?): Set<String>? {
        if (value == null) return null
        val type = object : TypeToken<Set<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun stringSetToString(set: Set<String>?): String? {
        return gson.toJson(set)
    }
    
    @TypeConverter
    fun fromIntSet(value: String?): Set<Int>? {
        if (value == null) return null
        val type = object : TypeToken<Set<Int>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun intSetToString(set: Set<Int>?): String? {
        return gson.toJson(set)
    }
    
    @TypeConverter
    fun fromMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    @TypeConverter
    fun mapToString(map: Map<String, String>?): String? {
        return gson.toJson(map)
    }
}
