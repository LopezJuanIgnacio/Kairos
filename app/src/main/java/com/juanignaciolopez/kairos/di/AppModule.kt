package com.juanignaciolopez.kairos.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.juanignaciolopez.kairos.data.local.KairosDatabase
import com.juanignaciolopez.kairos.data.remote.FirebaseAuthService
import com.juanignaciolopez.kairos.data.remote.FirebaseTaskService
import com.juanignaciolopez.kairos.data.remote.FirebaseUserService
import com.juanignaciolopez.kairos.data.repository.AuthRepositoryImpl
import com.juanignaciolopez.kairos.data.repository.TaskRepositoryImpl
import com.juanignaciolopez.kairos.data.repository.UserRepositoryImpl
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import com.juanignaciolopez.kairos.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para proveer dependencias relacionadas a la base de datos
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideKairosDatabase(
        @ApplicationContext context: Context
    ): KairosDatabase {
        return Room.databaseBuilder(
            context,
            KairosDatabase::class.java,
            KairosDatabase.DATABASE_NAME
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideTaskDao(database: KairosDatabase) = database.taskDao()
    
    @Provides
    @Singleton
    fun provideUserDao(database: KairosDatabase) = database.userDao()
}

/**
 * Módulo de Hilt para proveer dependencias de Firebase
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(
        @ApplicationContext context: Context
    ): FirebaseAuth? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).firstOrNull()?.let { app ->
                FirebaseAuth.getInstance(app)
            }
        } catch (_: Exception) {
            null
        }
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(
        @ApplicationContext context: Context
    ): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseApp.getApps(context).firstOrNull()?.let { app ->
                FirebaseFirestore.getInstance(app)
            }
        } catch (_: Exception) {
            null
        }
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        firebaseAuth: FirebaseAuth?
    ): FirebaseAuthService = FirebaseAuthService(firebaseAuth)
    
    @Provides
    @Singleton
    fun provideFirebaseTaskService(
        baseFirestore: FirebaseFirestore?
    ): FirebaseTaskService = FirebaseTaskService(baseFirestore)
    
    @Provides
    @Singleton
    fun provideFirebaseUserService(
        baseFirestore: FirebaseFirestore?
    ): FirebaseUserService = FirebaseUserService(baseFirestore)
}

/**
 * Módulo de Hilt para proveer los repositorios
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        servicioFirebaseAutenticacion: FirebaseAuthService,
        daoUsuario: com.juanignaciolopez.kairos.data.local.UserDao
    ): AuthRepository = AuthRepositoryImpl(servicioFirebaseAutenticacion, daoUsuario)
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        daoTarea: com.juanignaciolopez.kairos.data.local.TaskDao,
        servicioFirebaseTareas: FirebaseTaskService
    ): TaskRepository = TaskRepositoryImpl(daoTarea, servicioFirebaseTareas)
    
    @Provides
    @Singleton
    fun provideUserRepository(
        daoUsuario: com.juanignaciolopez.kairos.data.local.UserDao,
        servicioFirebaseUsuario: FirebaseUserService
    ): UserRepository = UserRepositoryImpl(daoUsuario, servicioFirebaseUsuario)
}
