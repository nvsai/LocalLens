package com.example.locallens.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.example.locallens.data.model.Itinerary
import com.example.locallens.data.model.LocalRecommendation
import com.example.locallens.data.model.LocalStory
import com.example.locallens.data.model.UserPreferences
import com.example.locallens.data.util.Resource
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    fun saveUserPreferences(userId: String, preferences: UserPreferences): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            db.collection("users").document(userId)
                .set(preferences)
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save preferences."))
        }
    }

    fun getUserPreferences(userId: String): Flow<Resource<UserPreferences>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("users").document(userId).get().await()
            val preferences = snapshot.toObject<UserPreferences>()
            if (preferences != null) {
                emit(Resource.Success(preferences))
            } else {
                emit(Resource.Success(UserPreferences(userId = userId)))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch preferences."))
        }
    }

    fun saveItinerary(userId: String, itinerary: Itinerary): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            db.collection("users").document(userId).collection("itineraries").document(itinerary.id)
                .set(itinerary)
                .await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save itinerary."))
        }
    }

    fun getItineraries(userId: String): Flow<Resource<List<Itinerary>>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("users").document(userId).collection("itineraries").get().await()
            val itineraries = snapshot.documents.mapNotNull { it.toObject<Itinerary>() }
            emit(Resource.Success(itineraries))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch itineraries."))
        }
    }

    fun getLocalStories(location: String? = null): Flow<Resource<List<LocalStory>>> = flow {
        emit(Resource.Loading())
        try {
            var query = db.collection("local_stories")
            if (location != null) {
                query = query.whereEqualTo("location", location) as CollectionReference
            }
            val snapshot = query.get().await()
            val stories = snapshot.documents.mapNotNull { it.toObject<LocalStory>() }
            emit(Resource.Success(stories))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch local stories."))
        }
    }

    fun getLocalStoryById(storyId: String): Flow<Resource<LocalStory>> = flow {
        emit(Resource.Loading())
        try {
            val snapshot = db.collection("local_stories").document(storyId).get().await()
            val story = snapshot.toObject<LocalStory>()
            if (story != null) {
                emit(Resource.Success(story))
            } else {
                emit(Resource.Error("Local story not found."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch local story by ID."))
        }
    }


    fun getLocalRecommendations(location: String? = null, type: String? = null): Flow<Resource<List<LocalRecommendation>>> = flow {
        emit(Resource.Loading())
        try {
            var query = db.collection("local_recommendations")
            if (location != null) {
                query = query.whereEqualTo("location", location) as CollectionReference
            }
            if (type != null) {
                query = query.whereEqualTo("type", type) as CollectionReference
            }
            val snapshot = query.get().await()
            val recommendations = snapshot.documents.mapNotNull { it.toObject<LocalRecommendation>() }
            emit(Resource.Success(recommendations))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch local recommendations."))
        }
    }
}