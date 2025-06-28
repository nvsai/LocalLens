package com.example.locallens.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser // Import FirebaseUser
import com.example.locallens.data.model.Activity
import com.example.locallens.data.model.DailyPlan
import com.example.locallens.data.model.Itinerary
import com.example.locallens.data.model.LocalRecommendation
import com.example.locallens.data.model.LocalStory
import com.example.locallens.data.model.SimpleGooglePlace
import com.example.locallens.data.model.TransportDetails
import com.example.locallens.data.model.UserPreferences
import com.example.locallens.data.repository.AuthRepository
import com.example.locallens.data.model.RouteInfo
import com.example.locallens.data.model.TransitStep
import com.example.locallens.data.repository.FirestoreRepository
import com.example.locallens.data.repository.GoogleMapsRepository
import com.example.locallens.data.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first // Added import for .first()
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
    private val googleMapsRepository: GoogleMapsRepository
) : ViewModel() {

    private val _userPreferences = MutableStateFlow<Resource<UserPreferences>>(Resource.Loading())
    val userPreferences: StateFlow<Resource<UserPreferences>> = _userPreferences

    private val _currentItinerary = MutableStateFlow<Resource<Itinerary>>(Resource.Idle())
    val currentItinerary: StateFlow<Resource<Itinerary>> = _currentItinerary

    private val _loadingItinerary = MutableStateFlow(false)
    val loadingItinerary: StateFlow<Boolean> = _loadingItinerary

    private val _localStories = MutableStateFlow<Resource<List<LocalStory>>>(Resource.Loading())
    val localStories: StateFlow<Resource<List<LocalStory>>> = _localStories

    private val _localRecommendations = MutableStateFlow<Resource<List<LocalRecommendation>>>(Resource.Loading())
    val localRecommendations: StateFlow<Resource<List<LocalRecommendation>>> = _localRecommendations

    // Expose currentUser from AuthRepository
    val currentUser: FirebaseUser?
        get() = authRepository.currentUser

    init {
        fetchUserPreferences()
        fetchLocalContent("Visakhapatnam")
    }

    fun saveUserPreferences(preferences: UserPreferences) {
        authRepository.currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                // Set loading state immediately for user preferences
                _userPreferences.value = Resource.Loading()
                firestoreRepository.saveUserPreferences(userId, preferences).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            // On successful save, re-fetch user preferences to update the state with the actual data
                            fetchUserPreferences()
                        }
                        is Resource.Error -> {
                            // On error, update _userPreferences state with the error
                            _userPreferences.value = Resource.Error(resource.message ?: "Failed to save preferences.")
                        }
                        is Resource.Loading -> {
                            // Do nothing, already set to loading at the start of the coroutine
                        }
                        is Resource.Idle -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun fetchUserPreferences() {
        authRepository.currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                firestoreRepository.getUserPreferences(userId).collect {
                    _userPreferences.value = it
                }
            }
        } ?: run {
            _userPreferences.value = Resource.Error("User not logged in.")
        }
    }

    private fun fetchLocalContent(location: String) {
        viewModelScope.launch {
            firestoreRepository.getLocalStories(location).collect {
                _localStories.value = it
            }
        }
        viewModelScope.launch {
            firestoreRepository.getLocalRecommendations(location).collect {
                _localRecommendations.value = it
            }
        }
    }

    fun generateItinerary(
        currentLocation: LatLng,
        days: Int = 1,
        planningLocation: String
    ) {
        _loadingItinerary.value = true
        viewModelScope.launch {
            val prefs = (_userPreferences.value as? Resource.Success)?.data
            val stories = (_localStories.value as? Resource.Success)?.data ?: emptyList()
            val recommendations = (_localRecommendations.value as? Resource.Success)?.data ?: emptyList()

            if (prefs == null) {
                _currentItinerary.value = Resource.Error("User preferences not loaded. Cannot generate itinerary.")
                _loadingItinerary.value = false
                return@launch
            }

            val allPossiblePlaces = mutableListOf<SimpleGooglePlace>()

            allPossiblePlaces.add(SimpleGooglePlace("p1", "RK Beach", LatLng(17.7121, 83.3323), "Visakhapatnam", 4.5, 10000, listOf("tourist_attraction", "beach")))
            allPossiblePlaces.add(SimpleGooglePlace("p2", "Kailasagiri", LatLng(17.7473, 83.3644), "Visakhapatnam", 4.7, 15000, listOf("tourist_attraction", "park")))
            allPossiblePlaces.add(SimpleGooglePlace("p3", "Submarine Museum", LatLng(17.7203, 83.3283), "Visakhapatnam", 4.6, 8000, listOf("museum", "tourist_attraction")))
            allPossiblePlaces.add(SimpleGooglePlace("p4", "Indira Gandhi Zoological Park", LatLng(17.7712, 83.3512), "Visakhapatnam", 4.4, 6000, listOf("zoo", "park")))
            allPossiblePlaces.add(SimpleGooglePlace("p5", "Tenneti Park", LatLng(17.7460, 83.3520), "Visakhapatnam", 4.5, 7000, listOf("park", "viewpoint")))
            allPossiblePlaces.add(SimpleGooglePlace("p6", "Daspalla Hotel", LatLng(17.7200, 83.3000), "Visakhapatnam", 4.2, 5000, listOf("restaurant", "lodging")))
            allPossiblePlaces.add(SimpleGooglePlace("p7", "Tycoon Restaurant", LatLng(17.7300, 83.3100), "Visakhapatnam", 4.3, 4000, listOf("restaurant")))
            allPossiblePlaces.add(SimpleGooglePlace("p8", "Gangavaram Beach", LatLng(17.6186, 83.2505), "Visakhapatnam", 4.1, 3000, listOf("beach")))


            val filteredPlaces = allPossiblePlaces.filter { place ->
                val matchesInterest = place.types?.any { type -> // 'type' is inferred as String
                    prefs.interests.any { interest -> // 'interest' is inferred as String
                        type.contains(interest.lowercase(Locale.ROOT))
                    }
                } ?: false // Ensures a Boolean result even if place.types is null

                val matchesCommonType = place.types?.any { type -> // 'type' is inferred as String
                    val commonTypes = listOf("tourist_attraction", "restaurant", "park", "museum", "beach")
                    commonTypes.contains(type)
                } ?: false // Ensures a Boolean result even if place.types is null

                matchesInterest || matchesCommonType
            }.sortedByDescending { it.rating }

            val generatedDailyPlans = mutableListOf<DailyPlan>()
            var remainingPlaces = filteredPlaces.toMutableList()
            var currentPlanningLocation = currentLocation

            for (dayNum in 1..days) {
                val dailyActivities = mutableListOf<Activity>()
                var currentDayTimeBudgetMinutes = 8 * 60

                while (remainingPlaces.isNotEmpty() && currentDayTimeBudgetMinutes > 60) {
                    val nextActivityCandidate = remainingPlaces.firstOrNull()
                    if (nextActivityCandidate == null) break

                    val transportDetails = getTransportDetails(
                        currentPlanningLocation,
                        nextActivityCandidate.latLng,
                        prefs.preferredTransportMode
                    )

                    if (currentDayTimeBudgetMinutes - transportDetails.travelTimeMinutes > 30) {
                        val activityDuration = 90
                        if (currentDayTimeBudgetMinutes - transportDetails.travelTimeMinutes - activityDuration >= 0) {
                            val activity = Activity(
                                placeId = nextActivityCandidate.id,
                                name = nextActivityCandidate.name,
                                latitude = nextActivityCandidate.latLng.latitude,
                                longitude = nextActivityCandidate.latLng.longitude,
                                type = nextActivityCandidate.types?.firstOrNull() ?: "point_of_interest",
                                startTime = null,
                                endTime = null,
                                howToReach = transportDetails,
                                localStoryId = stories.find { it.placeId == nextActivityCandidate.id }?.id,
                                audioGuideId = stories.find { it.placeId == nextActivityCandidate.id }?.id
                            )
                            dailyActivities.add(activity)
                            currentDayTimeBudgetMinutes -= (transportDetails.travelTimeMinutes + activityDuration)
                            currentPlanningLocation = nextActivityCandidate.latLng
                            remainingPlaces.remove(nextActivityCandidate)
                        } else {
                            remainingPlaces.remove(nextActivityCandidate)
                        }
                    } else {
                        break
                    }
                }
                if (dailyActivities.isNotEmpty()) {
                    generatedDailyPlans.add(DailyPlan(dayNumber = dayNum, activities = dailyActivities))
                }
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            val newItinerary = Itinerary(
                id = "${authRepository.currentUser?.uid}_${System.currentTimeMillis()}",
                userId = authRepository.currentUser?.uid ?: "unknown",
                date = today,
                days = generatedDailyPlans
            )
            _currentItinerary.value = Resource.Success(newItinerary)
            _loadingItinerary.value = false
            saveItinerary(newItinerary)
        }
    }

    private suspend fun getTransportDetails(
        origin: LatLng,
        destination: LatLng,
        mode: String
    ): TransportDetails {
        val defaultTransport = TransportDetails(mode = mode, travelTimeMinutes = 0, distanceKm = 0.0, fareEstimateINR = 0.0)
        return try {
            val resource: Resource<RouteInfo> = googleMapsRepository.getDirections(
                origin.latitude, origin.longitude,
                destination.latitude, destination.longitude,
                mode
            ).first { it is Resource.Success || it is Resource.Error } // Use .first() to get the relevant result

            when (resource) {
                is Resource.Success -> {
                    val routeInfo: RouteInfo = resource.data!!
                    val travelTimeMinutes = parseDurationToMinutes(routeInfo.duration)
                    val distanceKm = parseDistanceToKm(routeInfo.distance)
                    val fare = estimateFare(mode, distanceKm, routeInfo.transitSteps)

                    TransportDetails(
                        mode = mode,
                        travelTimeMinutes = travelTimeMinutes,
                        distanceKm = distanceKm,
                        fareEstimateINR = fare,
                        transitSteps = routeInfo.transitSteps,
                        polyline = routeInfo.polyline
                    )
                }
                is Resource.Error -> {
                    println("Error getting directions: ${resource.message}")
                    defaultTransport
                }
                is Resource.Loading, is Resource.Idle -> {
                    // This case should ideally not be reached due to .first filter
                    defaultTransport
                }
            }
        } catch (e: Exception) {
            println("Exception during transport details: ${e.message}")
            defaultTransport
        }
    }

    private fun saveItinerary(itinerary: Itinerary) {
        authRepository.currentUser?.uid?.let { userId ->
            viewModelScope.launch {
                firestoreRepository.saveItinerary(userId, itinerary).collect {
                    // No direct UI update needed here, just saving the itinerary
                }
            }
        }
    }

    fun getStoryById(storyId: String): StateFlow<Resource<LocalStory>> {
        val storyState = MutableStateFlow<Resource<LocalStory>>(Resource.Loading())
        viewModelScope.launch {
            firestoreRepository.getLocalStoryById(storyId).collect {
                storyState.value = it
            }
        }
        return storyState
    }

    private fun parseDurationToMinutes(durationText: String): Int {
        val parts = durationText.split(" ")
        var minutes = 0
        var i = 0
        while (i < parts.size) {
            val value = parts[i].toIntOrNull()
            if (value != null) {
                when (parts.getOrNull(i + 1)) {
                    "hour", "hours" -> minutes += value * 60
                    "min", "mins", "minutes" -> minutes += value
                }
                i += 2
            } else {
                i++
            }
        }
        return minutes
    }

    private fun parseDistanceToKm(distanceText: String): Double {
        val parts = distanceText.split(" ")
        val value = parts.firstOrNull()?.toDoubleOrNull() ?: 0.0
        val unit = parts.getOrNull(1)
        return when (unit) {
            "km", "kms" -> value
            "m", "meters" -> value / 1000.0
            else -> value
        }
    }

    private fun estimateFare(
        mode: String,
        distanceKm: Double,
        transitSteps: List<TransitStep>?
    ): Double {
        return when (mode.lowercase(Locale.ROOT)) {
            "driving" -> 0.0
            "walking" -> 0.0
            "transit" -> {
                var totalFare = 0.0
                transitSteps?.forEach { step ->
                    if (step.travelMode == "TRANSIT") {
                        totalFare += when (step.lineName) {
                            "28", "52", "38" -> 15.0
                            "Metro" -> 20.0
                            else -> 10.0
                        }
                        step.numStops?.let {
                            totalFare += (it / 5).coerceAtLeast(0) * 2.0
                        }
                    }
                }
                if (totalFare < 10.0 && totalFare > 0.0) 10.0 else totalFare
            }
            "auto_rickshaw" -> {
                val baseFare = 30.0
                val ratePerKm = 15.0
                val estimated = baseFare + (distanceKm * ratePerKm)
                (estimated / 5.0).roundToInt() * 5.0
            }
            else -> 0.0
        }
    }
}