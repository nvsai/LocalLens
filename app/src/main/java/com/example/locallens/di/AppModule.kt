package com.example.locallens.di

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.locallens.data.repository.AuthRepository
import com.example.locallens.data.repository.FirestoreRepository
import com.example.locallens.data.repository.GoogleMapsRepository
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(auth: FirebaseAuth): AuthRepository {
        return AuthRepository(auth)
    }

    @Provides
    @Singleton
    fun provideFirestoreRepository(db: FirebaseFirestore): FirestoreRepository {
        return FirestoreRepository(db)
    }

    @Provides
    @Singleton
    fun providePlacesClient(@ApplicationContext context: Context): PlacesClient {
        if (!Places.isInitialized()) {
            Places.initialize(context, "YOUR_GOOGLE_MAPS_API_KEY_HERE")
        }
        return Places.createClient(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGoogleMapsRepository(
        placesClient: PlacesClient,
        retrofit: Retrofit
    ): GoogleMapsRepository {
        val googleApiKey = "AIzaSyAaY1wNs1qnEVybcRY_d1FTgCS2tYn99U8"
        return GoogleMapsRepository(
            placesClient, retrofit, googleApiKey,
            geminiApiService = TODO()
        )
    }
}