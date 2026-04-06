package com.juanignaciolopez.kairos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanignaciolopez.kairos.data.models.Task
import com.juanignaciolopez.kairos.data.models.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con tareas en la base de datos local
 */
@Dao
interface TaskDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(tarea: Task)
    
    @Update
    suspend fun updateTask(tarea: Task)
    
    @Delete
    suspend fun deleteTask(tarea: Task)
    
    @Query("DELETE FROM tasks WHERE id = :idTarea")
    suspend fun deleteTaskById(idTarea: String)
    
    @Query("SELECT * FROM tasks WHERE id = :idTarea")
    suspend fun getTaskById(idTarea: String): Task?
    
    @Query("SELECT * FROM tasks WHERE userId = :idUsuario ORDER BY createdAt DESC")
    fun getAllTasks(idUsuario: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE userId = :idUsuario AND status = :estado ORDER BY createdAt DESC")
    fun getTasksByStatus(idUsuario: String, estado: TaskStatus): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE userId = :idUsuario AND isNextAction = 1 ORDER BY createdAt DESC")
    fun getNextActions(idUsuario: String): Flow<List<Task>>
    
    @Query("""
        SELECT * FROM tasks 
        WHERE userId = :idUsuario 
        AND (title LIKE '%' || :consulta || '%' OR description LIKE '%' || :consulta || '%')
        ORDER BY createdAt DESC
    """)
    fun searchTasks(idUsuario: String, consulta: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE userId = :idUsuario AND isSyncPending = 1")
    suspend fun getPendingSyncTasks(idUsuario: String): List<Task>
    
    @Query("DELETE FROM tasks WHERE userId = :idUsuario")
    suspend fun deleteAllUserTasks(idUsuario: String)
}
