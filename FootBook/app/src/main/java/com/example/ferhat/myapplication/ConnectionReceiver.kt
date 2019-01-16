package com.example.ferhat.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.support.v4.app.JobIntentService
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select


class ConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(connMgr.activeNetworkInfo?.type == null)
            return; // on quitte la fonction

        context.dbMessages.use {
            select(DBMessages.TABLE_PARTIES, // Table
                DBMessages.COLUMN_PARTIES_ID) // id seulement
                .whereArgs("${DBMessages.COLUMN_PARTIES_IMAGE} IS NULL")
                .exec {
                    for (row in asMapSequence()) {
                        val id = row[DBMessages.COLUMN_PARTIES_ID] as Long
                        val intSrv = Intent()
                        intSrv.putExtra(MessageService.EXTRA_MESSAGE_DOWNLOAD_ID, id)
                        JobIntentService.enqueueWork(context, MessageService::class.java, 0, intSrv)
                    }
                }
        }
    }
}