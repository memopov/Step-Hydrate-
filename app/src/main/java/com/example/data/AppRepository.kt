package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val hydrationDao = db.hydrationDao()
    private val dailyStepsDao = db.dailyStepsDao()
    private val userProfileDao = db.userProfileDao()

    // Hydration
    fun getHydrationLogs(userId: String): Flow<List<HydrationLog>> =
        hydrationDao.getHydrationLogs(userId)

    fun getHydrationLogsForDay(userId: String, dateString: String): Flow<List<HydrationLog>> =
        hydrationDao.getHydrationLogsForDay(userId, dateString)

    suspend fun insertHydration(log: HydrationLog) = withContext(Dispatchers.IO) {
        hydrationDao.insertHydration(log)
    }

    suspend fun deleteHydration(log: HydrationLog) = withContext(Dispatchers.IO) {
        hydrationDao.deleteHydration(log)
    }

    // Steps
    fun getAllStepsLogs(userId: String): Flow<List<DailyStepsLog>> =
        dailyStepsDao.getAllStepsLogs(userId)

    suspend fun getStepsLogForDay(userId: String, dateString: String): DailyStepsLog? = withContext(Dispatchers.IO) {
        dailyStepsDao.getStepsLogForDay(userId, dateString)
    }

    fun observeStepsLogForDay(userId: String, dateString: String): Flow<DailyStepsLog?> =
        dailyStepsDao.observeStepsLogForDay(userId, dateString)

    suspend fun insertOrUpdateStepLog(log: DailyStepsLog) = withContext(Dispatchers.IO) {
        dailyStepsDao.insertOrUpdateStepLog(log)
    }

    // Profiles
    suspend fun getProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        userProfileDao.getProfile(userId)
    }

    fun observeProfile(userId: String): Flow<UserProfile?> =
        userProfileDao.observeProfile(userId)

    suspend fun insertProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.insertProfile(profile)
    }

    /**
     * Zero-Bug Guest to Google sync merging algorithm
     */
    suspend fun mergeGuestDataToUser(newUserId: String) = withContext(Dispatchers.IO) {
        // Merges all hydration logs & step logs belonging to "guest" into the active Google Account
        hydrationDao.mergeGuestHydration(newUserId)
        dailyStepsDao.mergeGuestSteps(newUserId)

        // Read Guest settings to carry over metrics to avoid making the user re-enter them
        val guestProfile = userProfileDao.getProfile("guest")
        if (guestProfile != null) {
            val userProfile = userProfileDao.getProfile(newUserId)
            if (userProfile == null) {
                // If new profile doesn't exist, build one using guest metrics
                userProfileDao.insertProfile(
                    UserProfile(
                        userId = newUserId,
                        name = guestProfile.name,
                        email = guestProfile.email,
                        encryptedHeight = guestProfile.encryptedHeight,
                        encryptedWeight = guestProfile.encryptedWeight,
                        stepTarget = guestProfile.stepTarget,
                        waterTargetMl = guestProfile.waterTargetMl,
                        preferredLanguage = guestProfile.preferredLanguage,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
