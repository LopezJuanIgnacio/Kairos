package com.juanignaciolopez.kairos.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.juanignaciolopez.kairos.data.remote.FirebaseAuthService
import com.juanignaciolopez.kairos.data.remote.FirebaseTaskService
import com.juanignaciolopez.kairos.data.repository.AuthRepositoryImpl
import com.juanignaciolopez.kairos.data.repository.TaskRepositoryImpl
import com.juanignaciolopez.kairos.domain.repository.AuthRepository
import com.juanignaciolopez.kairos.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        servicioFirebaseAutenticacion: FirebaseAuthService
    ): AuthRepository = AuthRepositoryImpl(servicioFirebaseAutenticacion)

    @Provides
    @Singleton
    fun provideTaskRepository(
        servicioFirebaseTareas: FirebaseTaskService,
        servicioFirebaseAutenticacion: FirebaseAuthService
    ): TaskRepository = TaskRepositoryImpl(servicioFirebaseTareas, servicioFirebaseAutenticacion)
}
