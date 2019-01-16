package com.example.ferhat.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.support.v4.app.JobIntentService
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.update
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*


class MessageService : JobIntentService() {
    companion object {
        const val NOTIF_TYPE_MSG_SENT = "MessageSent"
        const val NOTIF_TYPE_DL_IMG = "DownloadImage"

        const val EXTRA_MESSAGE_DOWNLOAD_ID = "MessageService.DownloadId"
    }

    override fun onCreate() {
        super.onCreate()
        // On déclare le NotificationChannel
        val mgr = getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel1 = NotificationChannel(NOTIF_TYPE_MSG_SENT,getString(R.string.notif_type_msg_sent), NotificationManager.IMPORTANCE_DEFAULT)
            mgr.createNotificationChannel(channel1)

            val channel2 = NotificationChannel(NOTIF_TYPE_DL_IMG,getString(R.string.notif_type_dl_img), NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(channel2)
        }
    }

    override fun onHandleWork(intent: Intent) {
        if(intent.hasExtra(DBMessages.COLUMN_PARTIES_ADDRESS)){
            // C'est un intent d'envoie de message
            traiterEnvoiMessage(intent)
        }else if(intent.hasExtra(EXTRA_MESSAGE_DOWNLOAD_ID)){
            // C'est un intent de téléchargement
            traiterTelechargementImage(intent.getLongExtra(EXTRA_MESSAGE_DOWNLOAD_ID, 0))
        }
    }

    private fun traiterEnvoiMessage(intent: Intent) {
        // On "envoie" le message
        var primaryKey: Long = 0
        dbMessages.use {
            val date = intent.getSerializableExtra(DBMessages.COLUMN_PARTIES_DATE) as Date
            primaryKey = insert(DBMessages.TABLE_PARTIES,
                DBMessages.COLUMN_PARTIES_ADDRESS to intent.getStringExtra(DBMessages.COLUMN_PARTIES_ADDRESS),
                DBMessages.COLUMN_PARTIES_STADE to intent.getStringExtra(DBMessages.COLUMN_PARTIES_STADE),
                DBMessages.COLUMN_PARTIES_DATE to date.time)
        }

        // On affiche une notification
        val notifIntent = Intent(this, DetailActivity::class.java)
        notifIntent.putExtra(DetailActivity.EXTRA_MESSAGE_ID, primaryKey)
        val notifPendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, NOTIF_TYPE_MSG_SENT)
            .setSmallIcon(R.drawable.ic_message_white_24dp) // icone Google
            .setContentTitle(getString(R.string.message_programme_ok))
            .setContentText(intent.getStringExtra(DBMessages.COLUMN_PARTIES_STADE))
            .setContentIntent(notifPendingIntent)
            .setAutoCancel(true)

        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(primaryKey.toInt(), notif.build())
    }

    private fun traiterTelechargementImage(id: Long) {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notif = NotificationCompat.Builder(this, EXTRA_MESSAGE_DOWNLOAD_ID)
            .setSmallIcon(R.drawable.ic_cloud_download_white_24dp) // icone Google
            .setContentTitle(getString(R.string.di_img_encours))
            .setOngoing(true)
            .setProgress(0, 0, true)

         mgr.notify(-1, notif.build())

        val url = URL("https://source.unsplash.com/random/200x200") // 200x200 : petites images carrées

        val conn = url.openConnection() as HttpURLConnection
        conn.connect()
        if (conn.responseCode != HttpURLConnection.HTTP_OK) {
           mgr.cancel(-1)
            return
        }else {


            val file = File(this.filesDir, "$id")
           val output = FileOutputStream(file)
            try {
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read = conn.inputStream.read(buffer)

                while (read != -1) {
                    output.write(buffer, 0, read)
                    read = conn.inputStream.read(buffer)
                }

                output.flush()
            } finally {
                output.close()
            }

            dbMessages.use {
                update(DBMessages.TABLE_PARTIES, DBMessages.COLUMN_PARTIES_IMAGE to file.path)
                    .whereArgs("${DBMessages.COLUMN_PARTIES_ID} = {id}", "id" to id).exec()
            }
        }

        mgr.cancel(-1)
    }
}