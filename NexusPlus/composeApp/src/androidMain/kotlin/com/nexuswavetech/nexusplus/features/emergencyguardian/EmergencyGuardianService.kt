package com.nexuswavetech.nexusplus.features.emergencyguardian

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.sqrt

class EmergencyGuardianService : Service(), SensorEventListener {

    companion object {
        val isActive = AtomicBoolean(false)

        private const val CHANNEL_ID      = "emergency_guardian_ch"
        private const val NOTIF_ID        = 9_001
        private const val ACTION_CANCEL   = "com.nexuswavetech.nexusplus.GUARDIAN_CANCEL"

        private const val SHAKE_THRESHOLD  = 22f
        private const val SHAKE_WINDOW_MS  = 2_500L
        private const val SHAKE_MIN_COUNT  = 5
        private const val DEBOUNCE_MS      = 350L
        private const val COUNTDOWN_SEC    = 10
        private const val LOC_TIMEOUT_MS   = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, EmergencyGuardianService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, EmergencyGuardianService::class.java))
        }
    }

    private lateinit var sensorManager:      SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var wakeLock:            PowerManager.WakeLock

    private var accelerometer: Sensor? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var countdownJob: Job? = null

    @Volatile private var emergencyTriggered = false
    private var shakeCount     = 0
    private var shakeWindowStart = 0L
    private var lastShakeTs    = 0L

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isActive.set(true)

        notificationManager = getSystemService(NotificationManager::class.java)
        createChannel()
        startForeground(NOTIF_ID, buildIdleNotification())

        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "nexus:guardian")
        wakeLock.acquire(24 * 3_600 * 1_000L)

        sensorManager  = getSystemService(SensorManager::class.java)
        accelerometer  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        registerAccelerometer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) cancelCountdown()
        return START_STICKY
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        countdownJob?.cancel()
        serviceScope.cancel()
        if (wakeLock.isHeld) wakeLock.release()
        isActive.set(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Sensor ────────────────────────────────────────────────────────────────

    private fun registerAccelerometer() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (emergencyTriggered) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        if (magnitude < SHAKE_THRESHOLD) return

        val now = System.currentTimeMillis()
        if (now - lastShakeTs < DEBOUNCE_MS) return
        lastShakeTs = now

        if (shakeWindowStart == 0L || now - shakeWindowStart > SHAKE_WINDOW_MS) {
            shakeCount = 1
            shakeWindowStart = now
        } else {
            shakeCount++
            if (shakeCount >= SHAKE_MIN_COUNT && countdownJob?.isActive != true) {
                emergencyTriggered = true
                sensorManager.unregisterListener(this)
                startCountdown()
            }
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private fun startCountdown() {
        countdownJob = serviceScope.launch {
            for (remaining in COUNTDOWN_SEC downTo 1) {
                notificationManager.notify(NOTIF_ID, buildCountdownNotification(remaining))
                delay(1_000L)
            }
            dispatchEmergency()
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob    = null
        emergencyTriggered = false
        shakeCount      = 0
        shakeWindowStart = 0L
        registerAccelerometer()
        notificationManager.notify(NOTIF_ID, buildIdleNotification())
    }

    // ── Emergency dispatch ────────────────────────────────────────────────────

    private suspend fun dispatchEmergency() {
        val locationText = withTimeoutOrNull(LOC_TIMEOUT_MS) { acquireLocation() }
            ?.let { "Lat: ${it.latitude}, Lon: ${it.longitude}\nhttps://maps.google.com/?q=${it.latitude},${it.longitude}" }
            ?: "Location unavailable"

        val message  = "EMERGENCY ALERT from Nexus Guardian!\nI need immediate help!\n$locationText"
        val contacts = runCatching {
            EmergencyGuardianRepository(this).contactsFlow.first()
        }.getOrElse { emptyList() }

        sendSmsToAll(message, contacts)
        callFirst(contacts)

        notificationManager.notify(NOTIF_ID, buildDispatchedNotification(contacts.size))
    }

    private fun sendSmsToAll(message: String, contacts: List<EmergencyContact>) {
        if (!hasPermission(Manifest.permission.SEND_SMS)) return
        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        for (contact in contacts) {
            runCatching {
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
            }
        }
    }

    private fun callFirst(contacts: List<EmergencyContact>) {
        if (!hasPermission(Manifest.permission.CALL_PHONE)) return
        val first = contacts.firstOrNull() ?: return
        val intent = Intent(Intent.ACTION_CALL).apply {
            data  = Uri.parse("tel:${first.phone}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private suspend fun acquireLocation(): Location? = suspendCancellableCoroutine { cont ->
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        val lm = getSystemService(LocationManager::class.java)
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER)     -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { cont.resume(null); return@suspendCancellableCoroutine }
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                lm.removeUpdates(this)
                if (cont.isActive) cont.resume(loc)
            }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onProviderDisabled(p: String) {
                if (cont.isActive) cont.resume(null)
            }
        }
        try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener)
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Emergency Guardian",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Emergency Guardian active service notification"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildIdleNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Emergency Guardian Active")
            .setContentText("Monitoring for distress signals. Stay safe.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun buildCountdownNotification(remaining: Int): Notification {
        val cancelPi = PendingIntent.getService(
            this, 0,
            Intent(this, EmergencyGuardianService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Emergency Alert in ${remaining}s")
            .setContentText("Shake detected — tap Cancel to abort")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPi)
            .build()
    }

    private fun buildDispatchedNotification(contactCount: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Emergency Alert Dispatched")
            .setContentText("SMS sent to $contactCount contact(s). Calling now.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
}
