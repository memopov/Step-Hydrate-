package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {
    @Query("SELECT * FROM hydration_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHydrationLogs(userId: String): Flow<List<HydrationLog>>

    @Query("SELECT * FROM hydration_logs WHERE userId = :userId AND loggedDate = :dateString")
    fun getHydrationLogsForDay(userId: String, dateString: String): Flow<List<HydrationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHydration(log: HydrationLog)

    @Delete
    suspend fun deleteHydration(log: HydrationLog)

    @Query("UPDATE hydration_logs SET userId = :userId WHERE userId = 'guest'")
    suspend fun mergeGuestHydration(userId: String)
}

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps_logs WHERE userId = :userId ORDER BY dateString DESC")
    fun getAllStepsLogs(userId: String): Flow<List<DailyStepsLog>>

    @Query("SELECT * FROM daily_steps_logs WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    suspend fun getStepsLogForDay(userId: String, dateString: String): DailyStepsLog?

    @Query("SELECT * FROM daily_steps_logs WHERE userId = :userId AND dateString = :dateString LIMIT 1")
    fun observeStepsLogForDay(userId: String, dateString: String): Flow<DailyStepsLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStepLog(log: DailyStepsLog)

    @Query("UPDATE daily_steps_logs SET userId = :userId WHERE userId = 'guest'")
    suspend fun mergeGuestSteps(userId: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}
