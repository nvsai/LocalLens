package com.example.locallens.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.locallens.data.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun signUp(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { emit(Resource.Success(it)) } ?: emit(Resource.Error("User not created"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred during signup"))
        }
    }

    fun signIn(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { emit(Resource.Success(it)) } ?: emit(Resource.Error("Login failed"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred during signin"))
        }
    }

    fun signOut() {
        auth.signOut()
    }
}