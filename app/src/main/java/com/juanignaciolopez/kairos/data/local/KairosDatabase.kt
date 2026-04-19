package com.juanignaciolopez.kairos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.juanignaciolopez.kairos.data.models.TaskCategory
import com.juanignaciolopez.kairos.data.models.TaskPriority
import com.juanignaciolopez.kairos.data.models.TaskStatus
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
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(raw: String): TaskStatus {
        return when (raw.trim().uppercase()) {
            "INBOX" -> TaskStatus.INBOX
            "PROCESSING" -> TaskStatus.PROCESSING
            "TODO" -> TaskStatus.TODO
            "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
            "COMPLETED" -> TaskStatus.COMPLETED
            "ARCHIVED" -> TaskStatus.ARCHIVED
            "DELETED" -> TaskStatus.DELETED
            else -> TaskStatus.INBOX
        }
    }

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(raw: String): TaskPriority {
        return when (raw.trim().uppercase()) {
            "VERY_HIGH", "CRITICA", "CRÍTICA" -> TaskPriority.VERY_HIGH
            "HIGH", "ALTA" -> TaskPriority.HIGH
            "NORMAL" -> TaskPriority.NORMAL
            "LOW", "BAJA" -> TaskPriority.LOW
            "VERY_LOW", "MUY_BAJA", "MUY BAJA" -> TaskPriority.VERY_LOW
            else -> TaskPriority.NORMAL
        }
    }

    @TypeConverter
    fun fromTaskCategory(category: TaskCategory): String = category.name

    @TypeConverter
    fun toTaskCategory(raw: String): TaskCategory {
        return when (raw.trim().uppercase()) {
            "RECURRENT", "RECURRENTE" -> TaskCategory.RECURRENT
            "ACTIONABLE", "ACCIONABLE" -> TaskCategory.ACTIONABLE
            "SHORT_TERM", "CORTO_PLAZO", "CORTO PLAZO" -> TaskCategory.SHORT_TERM
            "LONG_TERM", "LARGO_PLAZO", "LARGO PLAZO" -> TaskCategory.LONG_TERM
            "INCUBATOR", "INCUBADORA" -> TaskCategory.INCUBATOR

            // Compatibilidad con categorías legacy guardadas anteriormente.
            "WORK", "PERSONAL", "HEALTH", "FINANCE", "LEARNING", "FAMILY", "HOME", "SOMEDAY", "OTHER" -> TaskCategory.ACTIONABLE

            else -> TaskCategory.ACTIONABLE
        }
    }

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
    version = 3,
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
