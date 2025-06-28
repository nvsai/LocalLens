package com.example.locallens.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.locallens.R
import com.example.locallens.data.util.Resource
import com.example.locallens.ui.navigation.Screen
import com.example.locallens.ui.theme.LocalLensTheme
import com.example.locallens.ui.theme.PrimaryGreen
import com.example.locallens.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val signInState by authViewModel.signInState.collectAsState()
    val signUpState by authViewModel.signUpState.collectAsState()

    val isLoading = signInState is Resource.Loading || signUpState is Resource.Loading

    LaunchedEffect(signInState) {
        signInState.let { resource ->
            when (resource) {
                is Resource.Loading -> { }
                is Resource.Success -> {
                    Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Preferences.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                    authViewModel.resetStates()
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Login Failed: ${resource.message}", Toast.LENGTH_LONG).show()
                    authViewModel.resetStates()
                }
                is Resource.Idle -> {}
            }
        }
    }

    LaunchedEffect(signUpState) {
        signUpState.let { resource ->
            when (resource) {
                is Resource.Loading -> { }
                is Resource.Success -> {
                    Toast.makeText(context, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    navController.navigate(Screen.Preferences.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                    authViewModel.resetStates()
                }
                is Resource.Error -> {
                    Toast.makeText(context, "Registration Failed: ${resource.message}", Toast.LENGTH_LONG).show()
                    authViewModel.resetStates()
                }
                is Resource.Idle -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Lens", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isRegisterMode) "Create Your Account" else "Welcome Back!",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isRegisterMode) "Join Local Lens today!" else "Sign in to plan your next adventure.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                if (isRegisterMode) {
                                    authViewModel.signUp(email, password)
                                } else {
                                    authViewModel.signIn(email, password)
                                }
                            } else {
                                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        )
                    ) {
                        AnimatedVisibility(visible = isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                        AnimatedVisibility(visible = !isLoading) {
                            Text(if (isRegisterMode) "Register" else "Login", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                        Text(
                            text = if (isRegisterMode) "Already have an account? Login" else "Don't have an account? Register",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    LocalLensTheme {
        AuthScreen(navController = rememberNavController())
    }
}