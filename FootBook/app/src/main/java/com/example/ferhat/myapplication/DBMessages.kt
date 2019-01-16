package com.example.ferhat.myapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

val Context.dbMessages: DBMessages
    get() = DBMessages.getInstance(applicationContext)

class DBMessages(ctx: Context) : ManagedSQLiteOpenHelper(ctx, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "parties.db"
        const val DATABASE_VERSION = 12

        const val TABLE_PARTIES = "parties"
        const val COLUMN_PARTIES_ID = "id"
        const val COLUMN_PARTIES_STADE = "stade"//
        const val COLUMN_PARTIES_ADDRESS = "address"
        const val COLUMN_PARTIES_DATE = "date"
        const val COLUMN_PARTIES_IMAGE = "image"
        const val COLUMN_PARTIES_LAT = "lat"
        const val COLUMN_PARTIES_LNG = "lng"


        private var instance: DBMessages? = null

        @Synchronized
        fun getInstance(ctx: Context): DBMessages {
            if (instance == null) {
                instance = DBMessages(ctx)
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_PARTIES, true,
            COLUMN_PARTIES_ID to INTEGER + PRIMARY_KEY,
            COLUMN_PARTIES_STADE to TEXT + NOT_NULL,
            COLUMN_PARTIES_ADDRESS to TEXT + NOT_NULL,
            COLUMN_PARTIES_DATE to INTEGER + NOT_NULL,
            COLUMN_PARTIES_IMAGE to TEXT,
            COLUMN_PARTIES_LAT to REAL + NOT_NULL,
            COLUMN_PARTIES_LNG to REAL + NOT_NULL
            )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 11) {
            db.execSQL("ALTER TABLE $TABLE_PARTIES ADD $COLUMN_PARTIES_IMAGE TEXT;")
        }
        if(oldVersion < 12) {
            db.execSQL("ALTER TABLE $TABLE_PARTIES ADD $COLUMN_PARTIES_LAT REAL NOT NULL DEFAULT 48.945705;")
            db.execSQL("ALTER TABLE $TABLE_PARTIES ADD $COLUMN_PARTIES_LNG REAL NOT NULL DEFAULT 2.363056;")
        }
    }
}
