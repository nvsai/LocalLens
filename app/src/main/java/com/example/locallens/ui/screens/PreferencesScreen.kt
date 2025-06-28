package com.example.locallens.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.locallens.data.model.UserPreferences
import com.example.locallens.data.util.Resource
import com.example.locallens.ui.navigation.Screen
import com.example.locallens.ui.theme.LocalLensTheme
import com.example.locallens.ui.theme.PrimaryGreen
import com.example.locallens.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class) // Removed ExperimentalMaterialApi
@Composable
fun PreferencesScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferencesResource by homeViewModel.userPreferences.collectAsState()

    var travelStyle by remember { mutableStateOf(emptyList<String>()) }
    var interests by remember { mutableStateOf(emptyList<String>()) }
    var foodPreferences by remember { mutableStateOf(emptyList<String>()) }
    var budget by remember { mutableStateOf("") }
    var pacing by remember { mutableStateOf("") }
    var preferredTransportMode by remember { mutableStateOf("driving") }

    LaunchedEffect(userPreferencesResource) {
        if (userPreferencesResource is Resource.Success) {
            val prefs = (userPreferencesResource as Resource.Success).data
            prefs?.let {
                travelStyle = it.travelStyle
                interests = it.interests
                foodPreferences = it.foodPreferences
                budget = it.budget
                pacing = it.pacing
                preferredTransportMode = it.preferredTransportMode
            }
        }
    }

    val isLoading = userPreferencesResource is Resource.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Travel Preferences", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading your preferences...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "Help us personalize your trip!",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                PreferenceSection(title = "Travel Style") {
                    listOf("Adventure", "Relaxation", "Culture", "Party", "Family-Friendly").forEach { option ->
                        FilterChip(
                            selected = travelStyle.contains(option),
                            onClick = {
                                travelStyle = if (travelStyle.contains(option)) {
                                    travelStyle - option
                                } else {
                                    travelStyle + option
                                }
                            },
                            label = { Text(option) },
                            leadingIcon = {
                                AnimatedVisibility(visible = travelStyle.contains(option)) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                PreferenceSection(title = "Interests") {
                    listOf("History", "Nature", "Food", "Art", "Shopping", "Nightlife", "Beaches", "Temples").forEach { option ->
                        FilterChip(
                            selected = interests.contains(option),
                            onClick = {
                                interests = if (interests.contains(option)) {
                                    interests - option
                                } else {
                                    interests + option
                                }
                            },
                            label = { Text(option) },
                            leadingIcon = {
                                AnimatedVisibility(visible = interests.contains(option)) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                PreferenceSection(title = "Food Preferences") {
                    listOf("Vegetarian", "Vegan", "Non-Vegetarian", "Spicy", "Mild", "Seafood", "Local Cuisine").forEach { option ->
                        FilterChip(
                            selected = foodPreferences.contains(option),
                            onClick = {
                                foodPreferences = if (foodPreferences.contains(option)) {
                                    foodPreferences - option
                                } else {
                                    foodPreferences + option
                                }
                            },
                            label = { Text(option) },
                            leadingIcon = {
                                AnimatedVisibility(visible = foodPreferences.contains(option)) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                PreferenceSection(title = "Budget") {
                    listOf("Low", "Medium", "High").forEach { option ->
                        FilterChip(
                            selected = budget == option,
                            onClick = { budget = option },
                            label = { Text(option) },
                            leadingIcon = {
                                AnimatedVisibility(visible = budget == option) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                PreferenceSection(title = "Pacing") {
                    listOf("Relaxed", "Moderate", "Packed").forEach { option ->
                        FilterChip(
                            selected = pacing == option,
                            onClick = { pacing = option },
                            label = { Text(option) },
                            leadingIcon = {
                                AnimatedVisibility(visible = pacing == option) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                PreferenceSection(title = "Preferred Transport Mode") {
                    listOf("driving", "transit", "walking", "auto_rickshaw").forEach { option ->
                        FilterChip(
                            selected = preferredTransportMode == option,
                            onClick = { preferredTransportMode = option },
                            label = { Text(option.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                            leadingIcon = {
                                AnimatedVisibility(visible = preferredTransportMode == option) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (travelStyle.isEmpty() || interests.isEmpty() || budget.isBlank() || pacing.isBlank() || preferredTransportMode.isBlank()) {
                            Toast.makeText(context, "Please fill all preferences", Toast.LENGTH_SHORT).show()
                        } else {
                            val newPreferences = UserPreferences(
                                userId = homeViewModel.currentUser?.uid ?: "",
                                travelStyle = travelStyle,
                                interests = interests,
                                foodPreferences = foodPreferences,
                                budget = budget,
                                pacing = pacing,
                                preferredTransportMode = preferredTransportMode
                            )
                            homeViewModel.saveUserPreferences(newPreferences)
                            Toast.makeText(context, "Preferences saved!", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Preferences.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save Preferences & Plan Trip", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferenceSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    LocalLensTheme {
        PreferencesScreen(navController = rememberNavController())
    }
}