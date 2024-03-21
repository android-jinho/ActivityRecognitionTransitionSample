package com.cashwalk.activity

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.cashwalk.activity.ActivityTransitionData.STAIR_TRANSITIONS_RECEIVER_ACTION
import com.cashwalk.activity.ActivityTransitionData.TRANSITIONS_EXTRA
import com.cashwalk.activity.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.cashwalk.activity.databinding.ActivityMainBinding
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransitionRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var adapter: ActivityTransitionAdapter

    private val activityTransitionReceiver by lazy { ActivityTransitionsReceiver() }
    private val stairTransitionServiceIntent by lazy { Intent(this, StairTransitionService::class.java) }

    private lateinit var request: ActivityTransitionRequest
    private lateinit var pendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Check Permissions
        if (checkRecognitionPermissionIfLaterVersionQ().not()) {
            requestRecognitionPermission()
        }

        initViews()

        // Register Request
        initPendingIntent()
        if (checkRecognitionPermissionIfLaterVersionQ()) {
            registerActivityTransitionUpdates()
        }
    }

    // OS Q 이상인 경우, RecognitionPermission 확인
    private fun checkRecognitionPermissionIfLaterVersionQ(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }
        else true
    }

    private fun requestRecognitionPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            0,
        )
    }

    private fun initViews() {
        val linearLayoutManager = LinearLayoutManager(this@MainActivity)
        linearLayoutManager.reverseLayout = true
        linearLayoutManager.stackFromEnd = true

        adapter = ActivityTransitionAdapter()
        binding.rvActivityTransition.adapter = adapter
        binding.rvActivityTransition.layoutManager = linearLayoutManager

        binding.btRegisterRequest.setOnClickListener {
            registerActivityTransitionUpdates()
        }
    }

    private fun initPendingIntent() {
        val intent = Intent(TRANSITIONS_RECEIVER_ACTION)
        intent.setPackage(this.packageName)

        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun registerActivityTransitionUpdates() {
        request = ActivityTransitionRequest(ActivityTransitionData.getActivityTransitionList())
        ActivityRecognition
            .getClient(this)
            .requestActivityTransitionUpdates(request, pendingIntent)
            .addOnSuccessListener {
                adapter.addItem("활동 감지가 시작되었습니다. " + getCurrentTime())
            }
            .addOnFailureListener { exception ->
                adapter.addItem(exception.localizedMessage ?: "예외가 발생하였습니다.")
            }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = IntentFilter(TRANSITIONS_RECEIVER_ACTION)
        intentFilter.addAction(STAIR_TRANSITIONS_RECEIVER_ACTION)

        registerReceiver(
            activityTransitionReceiver,
            intentFilter,
            RECEIVER_NOT_EXPORTED
        )
        startService(stairTransitionServiceIntent)
    }

    // From ActivityTransitionReceiver
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("Activity Recognition", "onNewIntent")
        if (intent != null) {
            checkIntentData(intent)
        }
    }

    private fun checkIntentData(intent: Intent) {
        val activityTransition = intent.getStringExtra(TRANSITIONS_EXTRA)

        if (activityTransition != null) {
            adapter.addItem(activityTransition)
        }
    }

    override fun onStop() {
        if (checkRecognitionPermissionIfLaterVersionQ()) {
            unregisterActivityTransitionUpdates()
        }

        unregisterReceiver(activityTransitionReceiver)
        stopService(stairTransitionServiceIntent)
        super.onStop()
    }

    private fun unregisterActivityTransitionUpdates() {
        ActivityRecognition
            .getClient(this)
            .removeActivityTransitionUpdates(pendingIntent)
            .addOnSuccessListener {
                adapter.addItem("활동 감지가 종료되었습니다. " + getCurrentTime())
            }
            .addOnFailureListener { exception ->
                adapter.addItem(exception.localizedMessage ?: "예외가 발생하였습니다.")
            }
    }

    private fun getCurrentTime(): String {
        val timeZoneId = ZoneId.of("Asia/Seoul")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("a KK:mm:ss")
        return LocalDateTime.now(timeZoneId).format(dateTimeFormatter)
    }
}