package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "hydration_logs")
data class HydrationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // "guest" or Google account ID
    val liquidType: String, // "Water", "Coffee", "Tea", "Soda", "Sports"
    val amountMl: Int,
    val hydrationCoefficient: Float,
    val netHydrationMl: Int, // amountMl * hydrationCoefficient (adjusted)
    val timestamp: Long = System.currentTimeMillis(),
    val loggedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
)

@Entity(tableName = "daily_steps_logs")
data class DailyStepsLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // "guest" or Google account ID
    val steps: Int,
    val distanceMeters: Int,
    val caloriesBurnt: Int,
    val stepTarget: Int = 8000,
    val dateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val isGpsTracked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val userId: String, // "guest" or Google account ID
    val name: String,
    val email: String,
    // Encrypted health metrics
    val encryptedHeight: String, // encrypted meters or cm
    val encryptedWeight: String, // encrypted kg
    val stepTarget: Int = 8000,
    val waterTargetMl: Int = 2000,
    val preferredLanguage: String = "tr", // "tr" or "en"
    val lastSyncTime: Long = System.currentTimeMillis()
) {
    // Unencrypted Helper properties
    val decWeight: Float
        get() = try {
            val decrypted = EncryptionManager.decrypt(encryptedWeight)
            decrypted.toFloatOrNull() ?: 70.0f
        } catch (e: Exception) {
            70.0f
        }

    val decHeight: Float
        get() = try {
            val decrypted = EncryptionManager.decrypt(encryptedHeight)
            decrypted.toFloatOrNull() ?: 170.0f
        } catch (e: Exception) {
            170.0f
        }
}
