package com.juanignaciolopez.kairos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Convertidores para tipos complejos en Room
 */
class Converters {
    private val json = Json

    @TypeConverter
    fun fromTags(tags: List<String>): String {
        return json.encodeToString(ListSerializer(String.serializer()), tags)
    }

    @TypeConverter
    fun toTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), raw)
        }.getOrDefault(emptyList())
    }
}

/**
 * Base de datos SQLite local con Room
 * Almacena tareas y usuarios de forma local para soporte offline
 */
@Database(
    entities = [Task::class, User::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KairosDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun userDao(): UserDao
    
    companion object {
        const val DATABASE_NAME = "kairos_db"
    }
}
