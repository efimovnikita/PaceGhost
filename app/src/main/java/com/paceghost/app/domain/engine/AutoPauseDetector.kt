package com.paceghost.app.domain.engine

class AutoPauseDetector(
    val speedThresholdKmh: Float = 0.5f,
    val durationThresholdSeconds: Long = 7L
) {
    private var lowSpeedStartElapsedSeconds: Long? = null
    private var isCurrentlyAutoPaused: Boolean = false

    /**
     * Updates detector with current speed and walk elapsed seconds.
     * Triggers auto-pause if speed stays below threshold for longer than durationThresholdSeconds.
     * Resumes tracking when speed rises back above threshold.
     *
     * @return true if currently auto-paused, false otherwise.
     */
    fun update(currentSpeedKmh: Float, elapsedSeconds: Long): Boolean {
        if (currentSpeedKmh < speedThresholdKmh) {
            val start = lowSpeedStartElapsedSeconds
            if (start == null) {
                lowSpeedStartElapsedSeconds = elapsedSeconds
            } else if (elapsedSeconds - start > durationThresholdSeconds) {
                isCurrentlyAutoPaused = true
            }
        } else {
            lowSpeedStartElapsedSeconds = null
            isCurrentlyAutoPaused = false
        }
        return isCurrentlyAutoPaused
    }

    fun isAutoPaused(): Boolean = isCurrentlyAutoPaused

    fun reset() {
        lowSpeedStartElapsedSeconds = null
        isCurrentlyAutoPaused = false
    }
}
