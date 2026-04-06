package com.juanignaciolopez.kairos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.juanignaciolopez.kairos.data.models.User
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con usuarios en la base de datos local
 */
@Dao
interface UserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(usuario: User)
    
    @Update
    suspend fun updateUser(usuario: User)
    
    @Delete
    suspend fun deleteUser(usuario: User)
    
    @Query("DELETE FROM users WHERE id = :idUsuario")
    suspend fun deleteUserById(idUsuario: String)
    
    @Query("SELECT * FROM users WHERE id = :idUsuario")
    suspend fun getUserById(idUsuario: String): User?
    
    @Query("SELECT * FROM users WHERE id = :idUsuario")
    fun getUserFlowById(idUsuario: String): Flow<User?>
    
    @Query("SELECT * FROM users WHERE email = :correo")
    suspend fun getUserByEmail(correo: String): User?
    
    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    fun getCurrentUser(): Flow<User?>
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
