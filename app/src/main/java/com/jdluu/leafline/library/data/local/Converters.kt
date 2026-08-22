package com.jdluu.leafline.library.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAuthors(authors: List<String>): String {
        return gson.toJson(authors)
    }

    @TypeConverter
    fun toAuthors(authorsJson: String): List<String> {
        if (authorsJson.isEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(authorsJson, type)
    }
}