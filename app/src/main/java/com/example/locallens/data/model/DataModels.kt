package com.example.locallens.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId

data class UserPreferences(
    val userId: String = "",
    val travelStyle: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val foodPreferences: List<String> = emptyList(),
    val budget: String = "",
    val pacing: String = "",
    val preferredTransportMode: String = "driving"
)

data class Itinerary(
    @DocumentId val id: String = "",
    val userId: String = "",
    val date: String = "",
    val days: List<DailyPlan> = emptyList()
)

data class DailyPlan(
    val dayNumber: Int = 0,
    val activities: List<Activity> = emptyList()
)

data class Activity(
    val placeId: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: Any = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val localStoryId: String? = null,
    val audioGuideId: String? = null,
    val howToReach: TransportDetails? = null
)

data class TransportDetails(
    val mode: String = "",
    val travelTimeMinutes: Int = 0,
    val distanceKm: Double = 0.0,
    val fareEstimateINR: Double = 0.0,
    val transitSteps: List<TransitStep>? = null,
    val polyline: String? = null
)

data class TransitStep(
    val instruction: String = "",
    val travelMode: String = "",
    val lineName: String? = null,
    val departureStop: String? = null,
    val arrivalStop: String? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val numStops: Int? = null,
    val durationMinutes: Int = 0
)


data class LocalStory(
    @DocumentId val id: String = "",
    val placeId: String = "",
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val location: String = "",
    val factChecked: Boolean = false
)

data class LocalRecommendation(
    @DocumentId val id: String = "",
    val placeId: String = "",
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val location: String = "",
    val recommendedBy: String = "",
    val imageUrl: String? = null
)


data class DirectionsApiResponse(
    val routes: List<Route>,
    val status: String
)

data class Route(
    val legs: List<Leg>,
    val overview_polyline: OverviewPolyline
)

data class Leg(
    val distance: ValueText,
    val duration: ValueText,
    val steps: List<Step>
)

data class Step(
    val distance: ValueText,
    val duration: ValueText,
    val html_instructions: String,
    val polyline: OverviewPolyline,
    val travel_mode: String,
    val transit_details: TransitDetails? = null
)

data class ValueText(
    val text: String,
    val value: Int
)

data class OverviewPolyline(
    val points: String
)

data class TransitDetails(
    val arrival_stop: StopLocation,
    val arrival_time: TimeDetails,
    val departure_stop: StopLocation,
    val departure_time: TimeDetails,
    val headsign: String?,
    val line: TransitLine,
    val num_stops: Int?,
    val trip_short_name: String?
)

data class StopLocation(
    val lat_lng: com.google.android.gms.maps.model.LatLng,
    val name: String
)

data class TimeDetails(
    val text: String,
    val time_zone: String?,
    val value: Long
)

data class TransitLine(
    val name: String,
    val short_name: String?,
    val vehicle: TransitVehicle?,
    val agencies: List<Agency>?
)

data class TransitVehicle(
    val icon: String?,
    val name: String?,
    val type: String?
)

data class Agency(
    val name: String,
    val url: String
)

data class SimpleGooglePlace(
    val id: String,
    val name: String,
    val latLng: LatLng,
    val address: String?,
    val rating: Double?,
    val userRatingsTotal: Int?,
    val types: List<String>?
)

data class RouteInfo(
    val distance: String,
    val duration: String,
    val polyline: String,
    val transitSteps: List<TransitStep>? = null
)