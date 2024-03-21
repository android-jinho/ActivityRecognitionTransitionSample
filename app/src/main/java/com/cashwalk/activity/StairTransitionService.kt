package com.cashwalk.activity

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cashwalk.activity.ActivityTransitionData.STAIR_TRANSITIONS_EXTRA
import com.cashwalk.activity.ActivityTransitionData.STAIR_TRANSITIONS_RECEIVER_ACTION
import com.google.common.base.Optional

class StairTransitionService : Service(), SensorEventListener {

    companion object {
        private const val ALPHA_ACCELERATION = 0.5f
        private const val THRESHOLD_EWMA_ACCELERATION = 0.1f
        private const val ALPHA_PRESSURE = 0.5f
        private const val THRESHOLD_EWMA_PRESSURE = 0.2f
    }

    private var mSensorManager: SensorManager? = null
    private var mEwmaAccelleration = 0f
    private var mMoving = false
    private var mEwmaPressure = 0f
    private var mInitialPressure = 0f
    private var mOnStairs = false

    override fun onCreate() {
        super.onCreate()
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensorAccelerometer: Optional<Sensor> = Optional.fromNullable(mSensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION))
        val sensorBarometer: Optional<Sensor> = Optional.fromNullable(mSensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE))

        if (sensorAccelerometer.isPresent()) {
            mSensorManager!!.registerListener(this, sensorAccelerometer.get(), SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(this, "가속도 센서에 접근할 수 없어, 계단 활동을 인식을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
        }

        if (sensorBarometer.isPresent()) {
            mSensorManager!!.registerListener(this, sensorBarometer.get(), SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Toast.makeText(this, "압력 센서에 접근할 수 없어, 계단 활동을 인식을 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            handleAccelerationChanged(event)
        } else if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            handlePressureChanged(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    fun handleAccelerationChanged(event: SensorEvent) {
        val accX = event.values[0]
        val accY = event.values[1]
        val accZ = event.values[2]
        val accVectorLength = Math.round(Math.sqrt((accX * accX + accY * accY + accZ * accZ).toDouble())).toInt()
        mEwmaAccelleration = ALPHA_ACCELERATION * accVectorLength + (1 - ALPHA_ACCELERATION) * mEwmaAccelleration
        if (mEwmaAccelleration > THRESHOLD_EWMA_ACCELERATION) {
            if (!mMoving) {
                mMoving = true
            }
        } else {
            mMoving = false
        }
    }

    private fun handlePressureChanged(event: SensorEvent) {
        val pressure = event.values[0]
        if (mEwmaPressure == 0f) {
            mInitialPressure = pressure
            mEwmaPressure = pressure
        } else {
            mEwmaPressure = ALPHA_PRESSURE * pressure + (1 - ALPHA_PRESSURE) * mEwmaPressure
        }
        if (mMoving) {
            if (Math.abs(mInitialPressure - mEwmaPressure) > THRESHOLD_EWMA_PRESSURE) {
                handleStairsDetected()
                mOnStairs = true
                mInitialPressure = mEwmaPressure
            } else {
                mOnStairs = false
            }
        }
    }

    private fun handleStairsDetected() {
        val intent = Intent(STAIR_TRANSITIONS_RECEIVER_ACTION)
        intent.putExtra(STAIR_TRANSITIONS_EXTRA, true)
        intent.setPackage(this.packageName)
        this.sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager!!.unregisterListener(this)
    }
}