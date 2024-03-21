package com.cashwalk.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cashwalk.activity.ActivityTransitionData.STAIR_TRANSITIONS_EXTRA
import com.cashwalk.activity.ActivityTransitionData.STAIR_TRANSITIONS_RECEIVER_ACTION
import com.cashwalk.activity.ActivityTransitionData.TRANSITIONS_EXTRA
import com.cashwalk.activity.ActivityTransitionData.TRANSITIONS_RECEIVER_ACTION
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ActivityTransitionsReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TRANSITIONS_RECEIVER_ACTION, "ON_RECEIVE $intent")

        when (intent.action) {
            TRANSITIONS_RECEIVER_ACTION -> {
                if (ActivityTransitionResult.hasResult(intent)) {
                    val result: ActivityTransitionResult =
                        ActivityTransitionResult.extractResult(intent) ?: return

                    for (event in result.transitionEvents) {
                        val transitionInfo = getTransitionInfo(event)
                        sendTransitionInfo(transitionInfo, context)
                    }
                }
            }
            STAIR_TRANSITIONS_RECEIVER_ACTION -> {
                val isStairTransition = intent.getBooleanExtra(STAIR_TRANSITIONS_EXTRA, false)

                if (isStairTransition) {
                    val transitionInfo = "계단 타기 시작 " + getCurrentTime()
                    sendTransitionInfo(transitionInfo, context)
                }
            }
        }
    }

    private fun getTransitionInfo(event: ActivityTransitionEvent): String {
        val activityType = activityType(event.activityType)
        val transitionType = transitionType(event.transitionType)

        return "$activityType ($transitionType) ${getCurrentTime()}"
    }

    private fun sendTransitionInfo(transitionInfo: String, context: Context) {
        Log.d("Activity Recognition", "Send Transition")
        val intent = Intent(context, MainActivity::class.java)
        intent.apply {
            putExtra(TRANSITIONS_EXTRA, transitionInfo)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(context.packageName)
        }
        context.startActivity(intent)
    }

    private fun transitionType(transition: Int): String? {
        return when (transition) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "시작"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "종료"
            else -> "알 수 없음"
        }
    }

    private fun activityType(activity: Int): String? {
        return when (activity) {
            DetectedActivity.IN_VEHICLE -> "차량 이동"
            DetectedActivity.STILL -> "정지"
            DetectedActivity.WALKING -> "걷기"
            DetectedActivity.ON_BICYCLE -> "자전거 이동"
            DetectedActivity.RUNNING -> "뛰기"
            else -> "알 수 업음"
        }
    }

    private fun getCurrentTime(): String {
        val timeZoneId = ZoneId.of("Asia/Seoul")
        val dateTimeFormatter = DateTimeFormatter.ofPattern("a KK:mm:ss")
        return LocalDateTime.now(timeZoneId).format(dateTimeFormatter)
    }
}