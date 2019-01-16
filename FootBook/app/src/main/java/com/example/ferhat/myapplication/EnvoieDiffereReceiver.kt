package com.example.ferhat.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.JobIntentService

class EnvoieDiffereReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        JobIntentService.enqueueWork(context, MessageService::class.java, 0, intent)

    }
}
