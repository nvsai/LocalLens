package com.example.locallens.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.example.locallens.data.model.Activity
import com.example.locallens.data.model.DailyPlan
import com.example.locallens.data.model.UserPreferences
import com.example.locallens.data.util.Resource
import com.example.locallens.ui.navigation.Screen
import com.example.locallens.ui.theme.LocalLensTheme
import com.example.locallens.ui.theme.PrimaryGreen
import com.example.locallens.ui.theme.SecondaryOrange
import com.example.locallens.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferencesResource by homeViewModel.userPreferences.collectAsState()
    val currentItineraryResource by homeViewModel.currentItinerary.collectAsState()
    val isLoadingItinerary by homeViewModel.loadingItinerary.collectAsState()

    var currentLocation: LatLng? by remember { mutableStateOf(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchLastLocation(context) { latLng ->
                    currentLocation = latLng
                    Toast.makeText(context, "Location fetched!", Toast.LENGTH_SHORT).show()
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchLastLocation(context) { latLng ->
                    currentLocation = latLng
                    Toast.makeText(context, "Coarse location fetched!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(context, "Location permission denied. Cannot generate itinerary based on current location.", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (checkLocationPermissions(context)) {
            fetchLastLocation(context) { latLng ->
                currentLocation = latLng
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Travel Plan", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        val preferences = (userPreferencesResource as? Resource.Success)?.data
                        if (preferences == null) {
                            Toast.makeText(context, "Please set your preferences first!", Toast.LENGTH_LONG).show()
                            navController.navigate(Screen.Preferences.route)
                            return@FloatingActionButton
                        }

                        currentLocation?.let { loc ->
                            homeViewModel.generateItinerary(loc, days = 1, planningLocation = "Visakhapatnam")
                        } ?: run {
                            Toast.makeText(context, "Cannot get current location. Please grant permission or try again.", Toast.LENGTH_LONG).show()
                            if (!checkLocationPermissions(context)) {
                                requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = PrimaryGreen
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Generate New Plan", tint = MaterialTheme.colorScheme.onPrimary)
                }

                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.Preferences.route)
                    },
                    containerColor = SecondaryOrange
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Edit Preferences", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = isLoadingItinerary, enter = fadeIn(), exit = fadeOut()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (val itineraryResource = currentItineraryResource) {
                is Resource.Loading -> {
                    if (!isLoadingItinerary) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                        Text("Loading your itinerary...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is Resource.Success -> {
                    val itinerary = itineraryResource.data
                    if (itinerary == null || itinerary.days.isEmpty() || itinerary.days.all { it.activities.isEmpty() }) {
                        NoItineraryMessage()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(itinerary.days) { dailyPlan ->
                                DailyPlanCard(dailyPlan = dailyPlan, navController = navController, homeViewModel = homeViewModel)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    ErrorState(itineraryResource.message ?: "Failed to load itinerary.") {
                        navController.navigate(Screen.Preferences.route)
                    }
                }
                is Resource.Idle -> {
                    NoItineraryMessage()
                }
            }
        }
    }
}

@Composable
fun NoItineraryMessage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No plan generated yet!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set your preferences and tap the refresh button to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
fun DailyPlanCard(dailyPlan: DailyPlan, navController: NavController, homeViewModel: HomeViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Day ${dailyPlan.dayNumber}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            dailyPlan.activities.forEachIndexed { index, activity ->
                ActivityItem(activity = activity, navController = navController, homeViewModel = homeViewModel)
                if (index < dailyPlan.activities.lastIndex) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity, navController: NavController, homeViewModel: HomeViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 32.dp)
        )

        activity.howToReach?.let { transport ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = "Transport",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Mode: ${transport.mode.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Travel: ${transport.travelTimeMinutes} mins (${String.format("%.1f", transport.distanceKm)} km)",
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (transport.fareEstimateINR > 0) {
                        Text(
                            text = "Est. Fare: ₹${String.format("%.0f", transport.fareEstimateINR)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    transport.transitSteps?.let { steps ->
                        if (steps.isNotEmpty()) {
                            Text(text = "Details:", style = MaterialTheme.typography.labelSmall)
                            steps.take(2).forEach { step ->
                                Text(
                                    text = "  • ${step.instruction} (${step.durationMinutes} min)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (steps.size > 2) {
                                Text(text = "  ...and more steps", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    navController.navigate(Screen.Map.route + "/${activity.latitude}/${activity.longitude}")
                }) {
                    Icon(Icons.Default.Map, contentDescription = "View on Map", tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        activity.localStoryId?.let { storyId ->
            TextButton(
                onClick = { navController.navigate(Screen.Detail.route + "/${storyId}") },
                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
            ) {
                Text("Read Local Story / Audio Guide")
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops! Something went wrong.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetryClick) {
            Text("Retry")
        }
    }
}

private fun checkLocationPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private fun fetchLastLocation(context: Context, onLocation: (LatLng) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    if (checkLocationPermissions(context)) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                onLocation(LatLng(it.latitude, it.longitude))
            } ?: run {
                Toast.makeText(context, "Last known location not found. Try again.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ItineraryScreenPreview() {
    LocalLensTheme {
        ItineraryScreen(navController = rememberNavController())
    }
}