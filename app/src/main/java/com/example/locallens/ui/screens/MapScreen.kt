package com.example.locallens.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.locallens.ui.theme.LocalLensTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    initialLat: Double,
    initialLng: Double
) {
    val initialLatLng = LatLng(initialLat, initialLng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 14f)
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                    if (!cameraPositionState.isMoving) {
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 15f))
                    }
                }
            }
        }
    }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {
            startLocationUpdates(fusedLocationClient, locationCallback, context)
        } else {
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermissions(context)) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            startLocationUpdates(fusedLocationClient, locationCallback, context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (hasLocationPermissions(context)) {
                    startLocationUpdates(fusedLocationClient, locationCallback, context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map View", color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    myLocationButtonEnabled = true
                )
            ) {
                Marker(
                    state = MarkerState(position = initialLatLng),
                    title = "Selected Place",
                    snippet = "Latitude: ${initialLat}, Longitude: ${initialLng}"
                )

                currentLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "Your Current Location",
                        snippet = "You are here!"
                    )
                }

            }

            AnimatedVisibility(
                visible = currentLocation == null && hasLocationPermissions(context),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Text(
                    "Fetching current location...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                        .padding(8.dp)
                        .wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}

private fun hasLocationPermissions(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

private fun startLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    locationCallback: LocationCallback,
    context: Context
) {
    if (hasLocationPermissions(context)) {
        val locationRequest = LocationRequest.Builder(10000L)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    LocalLensTheme {
        MapScreen(navController = rememberNavController(), initialLat = 17.7203, initialLng = 83.3283)
    }
}