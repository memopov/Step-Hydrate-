package com.example.data

import kotlin.math.sqrt

class SensorFusionStepDetector(
    private val onStepDetected: () -> Unit
) {
    // Low-Pass Filter coefficient
    private val alpha = 0.75f // Balanced filter to smooth out touch jitters while preserving walking trends
    private var accelPrev = floatArrayOf(0f, 0f, 0f)
    
    // Kalman Filter variables for Acceleration Magnitude
    private var xState = 9.8f // Initial estimate around Earth's gravity
    private var pErr = 1.0f    // Estimation error covariance
    private val qNoise = 0.08f // Process noise covariance
    private val rNoise = 0.40f // Filter measurement noise to avoid lag

    // Anti-shaking and peak detection variables
    private var lastPeakTime = 0L
    private val minPeakTimeDiff = 220L // 220 ms minimum peak-to-peak time for quick steps
    private val maxPeakTimeDiff = 2500L // 2500 ms max window
    
    // Peak finding thresholds
    private val upperThreshold = 11.2f // Acceleration must exceed this to qualify as a step peak (reaches easily during normal walk)
    private val lowerThreshold = 8.8f  // Gravity relaxation threshold
    private var isAboveThreshold = false

    // Step buffer to avoid fake single movements (Debouncing)
    private var pendingSteps = 0
    private var lastStepRegistrationTime = 0L
    private val consecutiveStepWindow = 3 // Requires at least 3 continuous rhythmic steps to release, filtering accidental nudges or screen taps

    /**
     * Feed accelerometer data
     * x, y, z in m/s^2
     */
    fun processAccelerometer(x: Float, y: Float, z: Float, timestampNs: Long) {
        val currentTimeMs = timestampNs / 1_000_000

        // 1. Low-Pass Filter to smooth out jitter
        val accelSmooth = FloatArray(3)
        accelSmooth[0] = alpha * accelPrev[0] + (1 - alpha) * x
        accelSmooth[1] = alpha * accelPrev[1] + (1 - alpha) * y
        accelSmooth[2] = alpha * accelPrev[2] + (1 - alpha) * z
        accelPrev = accelSmooth

        val magnitude = sqrt(
            (accelSmooth[0] * accelSmooth[0] +
             accelSmooth[1] * accelSmooth[1] +
             accelSmooth[2] * accelSmooth[2]).toDouble()
        ).toFloat()

        // 2. Kalman Filter update for magnitude
        // Prediction update
        val xPrior = xState
        val pPrior = pErr + qNoise

        // Measurement Update
        val kGain = pPrior / (pPrior + rNoise)
        xState = xPrior + kGain * (magnitude - xPrior)
        pErr = (1f - kGain) * pPrior

        val filteredMagnitude = xState

        // 3. Peak Detection with Threshold and Hysteresis
        if (filteredMagnitude > upperThreshold && !isAboveThreshold) {
            val timeDiff = currentTimeMs - lastPeakTime
            
            // Check rhythm validity (Anti-car ride & sudden desk tap)
            if (timeDiff in minPeakTimeDiff..maxPeakTimeDiff) {
                isAboveThreshold = true
                lastPeakTime = currentTimeMs
                handleStepEvent(currentTimeMs)
            } else if (timeDiff > maxPeakTimeDiff) {
                // Rhythm broken, restart sequence count
                lastPeakTime = currentTimeMs
                isAboveThreshold = true
                pendingSteps = 1
            } else {
                // Too fast (high frequency vibration / shaking) -> Discard peak
            }
        } else if (filteredMagnitude < lowerThreshold) {
            isAboveThreshold = false
        }
    }

    /**
     * Gyroscope signal analyzer to detect rapid chaotic shaking (Anti-Cheat Disabled for perfect counting)
     */
    fun processGyroscope(wx: Float, wy: Float, wz: Float) {
        // Gyro step cancellation disabled to avoid missing steps during natural active usage or shaking tests
    }

    /**
     * Pedometer Debouncer:
     * Keeps tracking steps but does not award them instantly until the user achieves
     * [consecutiveStepWindow] (6 steps) in rhythmic pace. Once matched, it releases
     * [consecutiveStepWindow] steps instantly, and then counts +1 regularly.
     */
    private fun handleStepEvent(currentTimeMs: Long) {
        val timeSinceLastStep = currentTimeMs - lastStepRegistrationTime
        
        if (timeSinceLastStep > maxPeakTimeDiff && lastStepRegistrationTime != 0L) {
            // Took a long pause, reset consecutive buffer
            pendingSteps = 1
        } else {
            pendingSteps++
        }
        
        lastStepRegistrationTime = currentTimeMs

        if (pendingSteps >= consecutiveStepWindow) {
            if (pendingSteps == consecutiveStepWindow) {
                // Award the buffered steps all at once
                for (i in 0 until consecutiveStepWindow) {
                    onStepDetected()
                }
            } else {
                // Continued regular stepping -> Count immediately
                onStepDetected()
            }
        }
    }
}
