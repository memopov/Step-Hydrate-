package com.example.ui

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val repository = AppRepository(application)
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // UI Localized Strings state
    var currentLanguage by mutableStateOf(LanguageManager.Language.TR)
        private set

    // Active connection profile state
    var activeUserId by mutableStateOf("guest")
        private set
    var isGoogleSignedIn by mutableStateOf(false)
        private set
    var selectedGoogleEmail by mutableStateOf("mehmetsaidsulak123@gmail.com")
        private set

    // Mock Multiple google profiles for switching
    val mockGoogleAccounts = listOf(
        "mehmetsaidsulak123@gmail.com",
        "mehmet.ids@gmail.com",
        "athlete.pro.health@gmail.com"
    )

    // Dynamic Sensor fusion stats
    var liveKalmanValue by mutableStateOf(9.8f)
        private set
    var liveStepCounterInSession by mutableStateOf(0)
        private set

    // GPS Long Distance Mode states
    var gpsModeEnabled by mutableStateOf(false)
        private set
    var gpsSignalActive by mutableStateOf(true)
        private set
    var currentSpeedKmh by mutableStateOf(0.0f)
        private set
    var trackerOdometerMeters by mutableStateOf(0)
        private set

    // Profile inputs
    var heightInput by mutableStateOf("175")
    var weightInput by mutableStateOf("72")
    var stepGoalInput by mutableStateOf("8000")
    var waterGoalInput by mutableStateOf("2000")

    private val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Flow streams
    private val _dailyStepLog = MutableStateFlow<DailyStepsLog?>(null)
    val dailyStepLog: StateFlow<DailyStepsLog?> = _dailyStepLog.asStateFlow()

    private val _hydrationLogs = MutableStateFlow<List<HydrationLog>>(emptyList())
    val hydrationLogs: StateFlow<List<HydrationLog>> = _hydrationLogs.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Sensor Fusion helper
    private var pedometerDetector: SensorFusionStepDetector? = null

    // Notifications trackers to avoid duplicate spamming
    private var sentStepGoalAlert = false
    private var sentWater50Alert = false
    private var sentWater100Alert = false
    private var lastStepTimeMs = 0L

    init {
        // Initialize sensor listener
        pedometerDetector = SensorFusionStepDetector {
            registerStepDetected()
        }

        registerSensorListeners()
        loadActiveProfile()

        // Trigger welcome notification on enter & periodic 20-min notifications
        viewModelScope.launch {
            // Short delay so app UI is visible when notification pops up
            kotlinx.coroutines.delay(1200L)
            
            // Welcome notification
            val welcomeTitle = if (currentLanguage == LanguageManager.Language.TR) "Hoş Geldiniz!" else "Welcome!"
            val welcomeMsg = if (currentLanguage == LanguageManager.Language.TR) 
                "Uygulamaya hoş geldiniz! Bugün sağlıklı kalmak için mükemmel bir gün." 
                else "Welcome to the app! Today is a perfect day to stay healthy."
            NotificationHelper.showGoalNotification(getApplication(), welcomeTitle, welcomeMsg, bypassDnd = true)

            // Periodic 20-minute loop
            while (true) {
                kotlinx.coroutines.delay(20 * 60 * 1000L) // 20 minutes block delay
                val periodicTitle = if (currentLanguage == LanguageManager.Language.TR) "Harekete Geçme Zamanı!" else "Time to Move!"
                val periodicMsg = if (currentLanguage == LanguageManager.Language.TR) 
                    "Son 20 dakikadır durumunuzu kontrol ettiniz mi? Hadi biraz adım atalım!" 
                    else "Have you checked your status in the last 20 minutes? Let's take some steps!"
                NotificationHelper.showGoalNotification(getApplication(), periodicTitle, periodicMsg)
            }
        }
    }

    private fun registerSensorListeners() {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (gyro != null) {
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun loadActiveProfile() {
        viewModelScope.launch {
            // Get profile details or insert a default one
            var profile = repository.getProfile(activeUserId)
            if (profile == null) {
                // Generate a default encrypted height & weight profile
                val encHeight = EncryptionManager.encrypt("175")
                val encWeight = EncryptionManager.encrypt("72")
                profile = UserProfile(
                    userId = activeUserId,
                    name = if (activeUserId == "guest") "Misafir Kullanıcı" else activeUserId.split("@")[0].lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() },
                    email = if (activeUserId == "guest") "guest@local" else activeUserId,
                    encryptedHeight = encHeight,
                    encryptedWeight = encWeight,
                    stepTarget = 8000,
                    waterTargetMl = 2000,
                    preferredLanguage = currentLanguage.name.lowercase(Locale.ROOT)
                )
                repository.insertProfile(profile)
            }

            _userProfile.value = profile
            heightInput = profile.decHeight.toInt().toString()
            weightInput = profile.decWeight.toInt().toString()
            stepGoalInput = profile.stepTarget.toString()
            waterGoalInput = profile.waterTargetMl.toString()
            
            // Set App Language preference
            currentLanguage = if (profile.preferredLanguage == "en") LanguageManager.Language.EN else LanguageManager.Language.TR

            // Observe Database flow reactive updates
            launch {
                repository.observeProfile(activeUserId).collect {
                    if (it != null) {
                        _userProfile.value = it
                    }
                }
            }

            launch {
                repository.getHydrationLogsForDay(activeUserId, todayString).collect {
                    _hydrationLogs.value = it
                    checkWaterMilestones(it)
                }
            }

            launch {
                repository.observeStepsLogForDay(activeUserId, todayString).collect {
                    if (it == null) {
                        // Create a blank step record for today
                        val newLog = DailyStepsLog(
                            userId = activeUserId,
                            steps = 0,
                            distanceMeters = 0,
                            caloriesBurnt = 0,
                            stepTarget = profile?.stepTarget ?: 8000,
                            dateString = todayString
                        )
                        repository.insertOrUpdateStepLog(newLog)
                        _dailyStepLog.value = newLog
                    } else {
                        _dailyStepLog.value = it
                        checkStepMilestones(it)
                    }
                }
            }
        }
    }

    /**
     * Language toggler
     */
    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == LanguageManager.Language.TR) {
            LanguageManager.Language.EN
        } else {
            LanguageManager.Language.TR
        }
        updateProfileSettings()
    }

    /**
     * Google and Guest auth toggles
     */
    fun switchUserMode(toGoogle: Boolean, email: String = "mehmetsaidsulak123@gmail.com") {
        viewModelScope.launch {
            if (toGoogle) {
                selectedGoogleEmail = email
                val previousGuestId = activeUserId
                activeUserId = email
                isGoogleSignedIn = true

                // Sync data: Trigger Zero-Bug Merging algorithm!
                repository.mergeGuestDataToUser(activeUserId)
                
                // Show standard merge alert
                val titleString = LanguageManager.getString("app_title", currentLanguage)
                val bodyString = LanguageManager.getString("merge_success", currentLanguage)
                NotificationHelper.showGoalNotification(getApplication(), titleString, bodyString, bypassDnd = true)
            } else {
                activeUserId = "guest"
                isGoogleSignedIn = false
            }
            loadActiveProfile()
        }
    }

    /**
     * Commit securely encrypted profile changes
     */
    fun updateProfileSettings() {
        val h = heightInput.toFloatOrNull() ?: 170f
        val w = weightInput.toFloatOrNull() ?: 70f
        val stepsT = stepGoalInput.toIntOrNull() ?: 8000
        val waterT = waterGoalInput.toIntOrNull() ?: 2000

        val encH = EncryptionManager.encrypt(h.toString())
        val encW = EncryptionManager.encrypt(w.toString())

        val currentProf = _userProfile.value
        val updatedProfile = UserProfile(
            userId = activeUserId,
            name = currentProf?.name ?: (if (activeUserId == "guest") "Misafir" else activeUserId.split("@")[0]),
            email = currentProf?.email ?: activeUserId,
            encryptedHeight = encH,
            encryptedWeight = encW,
            stepTarget = stepsT,
            waterTargetMl = waterT,
            preferredLanguage = currentLanguage.name.lowercase(Locale.ROOT)
        )

        viewModelScope.launch {
            repository.insertProfile(updatedProfile)
            
            // Also align stepsLog target limit with the updated setting
            val stepsLog = _dailyStepLog.value
            if (stepsLog != null) {
                repository.insertOrUpdateStepLog(stepsLog.copy(stepTarget = stepsT))
            }
        }
    }

    /**
     * Local and simulated drink logger with hydration coefficients & volume calculation
     */
    fun logBeverage(beverageType: String, ml: Int) {
        val coef = when (beverageType) {
            "Water" -> 1.0f
            "Coffee" -> -0.2f // caffeine diuretic drops total hydration
            "Tea" -> 0.6f
            "Soda" -> 0.5f
            "Sports" -> 1.1f
            else -> 1.0f
        }
        val netAdjusted = (ml * coef).toInt()

        val log = HydrationLog(
            userId = activeUserId,
            liquidType = beverageType,
            amountMl = ml,
            hydrationCoefficient = coef,
            netHydrationMl = netAdjusted
        )

        viewModelScope.launch {
            repository.insertHydration(log)
        }
    }

    fun deleteHydration(log: HydrationLog) {
        viewModelScope.launch {
            repository.deleteHydration(log)
        }
    }

    /**
     * Dynamic speed intervals helper for GPS energy preservation
     */
    fun updateGpsMode(isEnabled: Boolean) {
        gpsModeEnabled = isEnabled
        if (isEnabled) {
            // Simulated live updates as if GPS listener is triggered
            observeSimulatedSpeedUpdates()
        } else {
            currentSpeedKmh = 0f
        }
    }

    fun toggleGpsSignal() {
        gpsSignalActive = !gpsSignalActive
    }

    private fun observeSimulatedSpeedUpdates() {
        // Simulates realistic speed feedback. Under pure GPS signal loss,
        // we use step rates to calculate paces.
        viewModelScope.launch {
            while (gpsModeEnabled) {
                if (gpsSignalActive) {
                    val timeSinceLastStep = System.currentTimeMillis() - lastStepTimeMs
                    if (timeSinceLastStep < 4500L) {
                        // User is walking/active!
                        // Simulate natural human walk pace: between 4.2 and 5.8 km/h with light variations
                        currentSpeedKmh = (4.2f + (0..16).random() / 10f)
                    } else {
                        // Stationary / not walking. Decay speed to 0 km/h quickly.
                        if (currentSpeedKmh > 0f) {
                            currentSpeedKmh = kotlin.math.max(0.0f, currentSpeedKmh - 1.2f)
                        }
                    }
                } else {
                    currentSpeedKmh = 0f
                }
                kotlinx.coroutines.delay(1500L) // Update every 1.5 seconds for incredible real-time feel!
            }
        }
    }

    /**
     * Steps recorder core algorithm
     */
    fun registerStepDetected() {
        liveStepCounterInSession++
        lastStepTimeMs = System.currentTimeMillis()
        val stepWeight = _userProfile.value?.decWeight ?: 70f
        val stepHeight = _userProfile.value?.decHeight ?: 170f
        
        // Approximate pace size
        val strideLengthMeters = stepHeight * 0.414f / 100f // height in meters * factor
        
        val activeLog = _dailyStepLog.value ?: return
        val currentSteps = activeLog.steps + 1
        
        // Calculations for distance & calorie burn
        val updatedDistance = (currentSteps * strideLengthMeters).toInt()
        
        // Calories: steps * weight(kg) * multiplier (0.0005 kcal/step/kg)
        val updatedCal = (currentSteps * stepWeight * 0.0005f).toInt()

        val updatedLog = activeLog.copy(
            steps = currentSteps,
            distanceMeters = updatedDistance,
            caloriesBurnt = updatedCal,
            lastUpdated = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.insertOrUpdateStepLog(updatedLog)
            _dailyStepLog.value = updatedLog
        }
    }

    /**
     * Explicit direct check triggered on simulation click
     */
    fun handleManualStepSimulation() {
        registerStepDetected()
    }

    /**
     * Milestone validation notifications
     */
    private fun checkStepMilestones(log: DailyStepsLog) {
        val target = log.stepTarget
        if (log.steps >= target && !sentStepGoalAlert) {
            sentStepGoalAlert = true
            val titleStr = LanguageManager.getString("steps", currentLanguage)
            val bodyStr = if (currentLanguage == LanguageManager.Language.TR) {
                "Tebrikler! Günlük $target adım hedefinize ulaştınız. Harika gidiyorsunuz!"
            } else {
                "Congratulations! You reached your daily target of $target steps. Keep it up!"
            }
            NotificationHelper.showGoalNotification(getApplication(), titleStr, bodyStr)
        }
    }

    private fun checkWaterMilestones(logs: List<HydrationLog>) {
        val totalNet = logs.sumOf { it.netHydrationMl }
        val target = _userProfile.value?.waterTargetMl ?: 2000
        
        if (totalNet >= target / 2 && !sentWater50Alert) {
            sentWater50Alert = true
            val titleStr = LanguageManager.getString("hydration", currentLanguage)
            val bodyStr = if (currentLanguage == LanguageManager.Language.TR) {
                "Yarı Yol Hedefi! Bugün su ihtiyacınızın %50'sini karşıladınız."
            } else {
                "Halfway there! You have secured 50% of your hydration target."
            }
            NotificationHelper.showGoalNotification(getApplication(), titleStr, bodyStr)
        }

        if (totalNet >= target && !sentWater100Alert) {
            sentWater100Alert = true
            val titleStr = LanguageManager.getString("hydration", currentLanguage)
            val bodyStr = if (currentLanguage == LanguageManager.Language.TR) {
                "Tebrikler! Günlük su içme hedefinizi tamamladınız! Vücudunuz size teşekkür ediyor."
            } else {
                "Congratulations! Hydration goal completed successfully! Your body thanks you."
            }
            NotificationHelper.showGoalNotification(getApplication(), titleStr, bodyStr)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val timestamp = event.timestamp
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                pedometerDetector?.processAccelerometer(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    timestamp
                )
                // Extract magnitude for dynamic chart/UI feedback
                val mag = kotlin.math.sqrt(
                    event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]
                )
                // Just keep state within limit
                liveKalmanValue = mag
            }
            Sensor.TYPE_GYROSCOPE -> {
                pedometerDetector?.processGyroscope(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
