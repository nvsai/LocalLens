package com.example.locallens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.locallens.data.repository.AuthRepository
import com.example.locallens.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _signInState = MutableStateFlow<Resource<FirebaseUser>>(Resource.Idle())
    val signInState: StateFlow<Resource<FirebaseUser>> = _signInState

    private val _signUpState = MutableStateFlow<Resource<FirebaseUser>>(Resource.Idle())
    val signUpState: StateFlow<Resource<FirebaseUser>> = _signUpState

    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signIn(email, password).collect { resource ->
                _signInState.value = resource
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            authRepository.signUp(email, password).collect { resource ->
                _signUpState.value = resource
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun resetStates() {
        _signInState.value = Resource.Idle()
        _signUpState.value = Resource.Idle()
    }
}