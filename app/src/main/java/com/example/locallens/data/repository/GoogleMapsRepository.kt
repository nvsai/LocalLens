package com.example.locallens.data.repository

import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import com.example.locallens.data.model.DirectionsApiResponse
import com.example.locallens.data.model.RouteInfo
import com.example.locallens.data.model.SimpleGooglePlace
import com.example.locallens.data.model.TransitStep
import com.example.locallens.data.model.UserPreferences
import com.example.locallens.data.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.Response
import com.google.gson.JsonElement // Import for JsonElement
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

private val String.name: Unit?
    get() {return name}

interface DirectionsApiService {
    @retrofit2.http.GET("maps/api/directions/json")
    suspend fun getDirections(
        @retrofit2.http.Query("origin") origin: String,
        @retrofit2.http.Query("destination") destination: String,
        @retrofit2.http.Query("mode") mode: String,
        @retrofit2.http.Query("alternatives") alternatives: Boolean,
        @retrofit2.http.Query("key") apiKey: String
    ): DirectionsApiResponse
}

// New interface for Gemini API calls
interface GeminiApiService {
    @retrofit2.http.POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Query("key") apiKey: String,
        @retrofit2.http.Body request: Map<String, Any>
    ): Response<JsonElement> // Using JsonElement to handle dynamic structure
}

@Singleton
class GoogleMapsRepository @Inject constructor(
    private val placesClient: PlacesClient,
    retrofit: Retrofit,
    private val googleApiKey: String,
    private val geminiApiService: GeminiApiService // Inject GeminiApiService
) {
    private val directionsApiService: DirectionsApiService = retrofit.create(DirectionsApiService::class.java)

    fun findCurrentPlace(): Flow<Resource<List<SimpleGooglePlace>>> = flow {
        emit(Resource.Loading())
        try {
            val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS, Place.Field.RATING, Place.Field.USER_RATINGS_TOTAL, Place.Field.TYPES)
            val request = FindCurrentPlaceRequest.newInstance(placeFields)

            val response = placesClient.findCurrentPlace(request).await()
            val simplePlaces = response.placeLikelihoods.map { placeLikelihood ->
                val place = placeLikelihood.place
                SimpleGooglePlace(
                    id = place.id ?: "",
                    name = place.name ?: "",
                    latLng = place.latLng ?: LatLng(0.0, 0.0),
                    address = place.address,
                    rating = place.rating,
                    userRatingsTotal = place.userRatingsTotal,
                    types = place.placeTypes?.mapNotNull { it.name } // Corrected to use mapNotNull
                )
            }
            emit(Resource.Success(simplePlaces))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to find current place."))
        }
    }

    private fun SimpleGooglePlace(
        id: String,
        name: String,
        latLng: LatLng,
        address: String?,
        rating: Double?,
        userRatingsTotal: Int?,
        types: List<Unit>?
    ): SimpleGooglePlace {
            TODO("Not yet implemented")
    }

    fun fetchPlaceDetails(placeId: String): Flow<Resource<SimpleGooglePlace>> = flow {
        emit(Resource.Loading())
        try {
            val placeFields = listOf(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS,
                Place.Field.PHONE_NUMBER, Place.Field.WEBSITE_URI, Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL, Place.Field.PRICE_LEVEL, Place.Field.TYPES,
                Place.Field.OPENING_HOURS, Place.Field.PHOTO_METADATAS
            )
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)
            val response = placesClient.fetchPlace(request).await()
            val place = response.place

            emit(Resource.Success(
                SimpleGooglePlace(
                    id = place.id ?: "",
                    name = place.name ?: "",
                    latLng = place.latLng ?: LatLng(0.0, 0.0),
                    address = place.address,
                    rating = place.rating,
                    userRatingsTotal = place.userRatingsTotal,
                    types = place.placeTypes?.mapNotNull { it.name } // Corrected to use mapNotNull
                )
            ))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch place details."))
        }
    }

    fun getDirections(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double,
        mode: String = "driving"
    ): Flow<Resource<RouteInfo>> = flow {
        emit(Resource.Loading())
        try {
            val originStr = "$originLat,$originLng"
            val destStr = "$destLat,$destLng"
            val response = directionsApiService.getDirections(originStr, destStr, mode, true, googleApiKey)

            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                val firstLeg = route.legs.firstOrNull()

                if (firstLeg != null) {
                    val distance = firstLeg.distance.text
                    val duration = firstLeg.duration.text
                    val polyline = route.overview_polyline.points

                    val transitSteps = if (mode == "transit") {
                        firstLeg.steps.mapNotNull { step ->
                            if (step.travel_mode == "TRANSIT" && step.transit_details != null) {
                                TransitStep(
                                    instruction = step.html_instructions,
                                    travelMode = step.travel_mode,
                                    lineName = step.transit_details.line.name,
                                    departureStop = step.transit_details.departure_stop.name,
                                    arrivalStop = step.transit_details.arrival_stop.name,
                                    departureTime = step.transit_details.departure_time.text,
                                    arrivalTime = step.transit_details.arrival_time.text,
                                    numStops = step.transit_details.num_stops,
                                    durationMinutes = (step.duration.value / 60.0).roundToInt()
                                )
                            } else {
                                TransitStep(
                                    instruction = step.html_instructions,
                                    travelMode = step.travel_mode,
                                    durationMinutes = (step.duration.value / 60.0).roundToInt()
                                )
                            }
                        }
                    } else null

                    emit(Resource.Success(
                        RouteInfo(
                            distance = distance,
                            duration = duration,
                            polyline = polyline,
                            transitSteps = transitSteps
                        )
                    ))
                } else {
                    emit(Resource.Error("No legs found in route."))
                }
            } else {
                emit(Resource.Error("Directions API call failed with status: ${response.status}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred getting directions."))
        }
    }

    /**
     * Fetches a list of places from the Gemini API based on the planning location and user preferences.
     * The Gemini API is prompted to return structured JSON data for these places.
     */
    fun getPlacesFromGemini(
        planningLocation: String,
        userPreferences: UserPreferences
    ): Flow<Resource<List<SimpleGooglePlace>>> = flow {
        emit(Resource.Loading())
        try {
            // Constructing the prompt for the Gemini API, including user preferences
            val prompt = """
                Generate a list of 8-10 popular tourist attractions and good restaurants/cafes in ${planningLocation}.
                The user has the following preferences:
                Travel Style: ${userPreferences.travelStyle.joinToString()}
                Interests: ${userPreferences.interests.joinToString()}
                Food Preferences: ${userPreferences.foodPreferences.joinToString()}
                Budget: ${userPreferences.budget}
                Pacing: ${userPreferences.pacing}
                
                For each place, provide its:
                - id (a unique short string like 'p1', 'p2', etc.)
                - name
                - latitude
                - longitude
                - address (e.g., "Visakhapatnam, Andhra Pradesh, India")
                - rating (e.g., 4.5)
                - userRatingsTotal (e.g., 10000)
                - types (a list of relevant types, e.g., ["tourist_attraction", "beach", "restaurant"])

                Return the data as a JSON array of objects, strictly following this schema:
                [
                  {
                    "id": "string",
                    "name": "string",
                    "latLng": {
                      "latitude": double,
                      "longitude": double
                    },
                    "address": "string",
                    "rating": double,
                    "userRatingsTotal": integer,
                    "types": ["string"]
                  }
                ]
            """.trimIndent()

            // Constructing the payload for the Gemini API request
            val payload = mapOf(
                "contents" to listOf(
                    mapOf("role" to "user", "parts" to listOf(mapOf("text" to prompt)))
                ),
                "generationConfig" to mapOf(
                    "responseMimeType" to "application/json",
                    "responseSchema" to mapOf(
                        "type" to "ARRAY",
                        "items" to mapOf(
                            "type" to "OBJECT",
                            "properties" to mapOf(
                                "id" to mapOf("type" to "STRING"),
                                "name" to mapOf("type" to "STRING"),
                                "latLng" to mapOf(
                                    "type" to "OBJECT",
                                    "properties" to mapOf(
                                        "latitude" to mapOf("type" to "NUMBER"),
                                        "longitude" to mapOf("type" to "NUMBER")
                                    )
                                ),
                                "address" to mapOf("type" to "STRING"),
                                "rating" to mapOf("type" to "NUMBER"),
                                "userRatingsTotal" to mapOf("type" to "INTEGER"),
                                "types" to mapOf("type" to "ARRAY", "items" to mapOf("type" to "STRING"))
                            ),
                            "required" to listOf("id", "name", "latLng", "address", "rating", "userRatingsTotal", "types")
                        )
                    )
                )
            )

            // Make the Gemini API call
            val response = geminiApiService.generateContent(googleApiKey, payload)

            if (response.isSuccessful) {
                val jsonBody = response.body()?.asJsonArray
                if (jsonBody != null) {
                    val places = mutableListOf<SimpleGooglePlace>()
                    for (jsonElement in jsonBody) {
                        try {
                            val jsonObject = jsonElement.asJsonObject
                            val id = jsonObject.get("id")?.asString ?: ""
                            val name = jsonObject.get("name")?.asString ?: ""
                            val latLngObject = jsonObject.getAsJsonObject("latLng")
                            val latitude = latLngObject.get("latitude")?.asDouble ?: 0.0
                            val longitude = latLngObject.get("longitude")?.asDouble ?: 0.0
                            val address = jsonObject.get("address")?.asString
                            val rating = jsonObject.get("rating")?.asDouble
                            val userRatingsTotal = jsonObject.get("userRatingsTotal")?.asInt
                            val types = jsonObject.getAsJsonArray("types")?.map { it.asString }

                            places.add(SimpleGooglePlace(id, name, LatLng(latitude, longitude), address, rating, userRatingsTotal, types))
                        } catch (e: Exception) {
                            println("Error parsing a place from Gemini API: ${e.message}")
                            // Log the error but continue parsing other elements if one fails
                        }
                    }
                    emit(Resource.Success(places))
                } else {
                    emit(Resource.Error("Gemini API returned empty or malformed JSON."))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                emit(Resource.Error("Gemini API call failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred during Gemini API call."))
        }
    }
}