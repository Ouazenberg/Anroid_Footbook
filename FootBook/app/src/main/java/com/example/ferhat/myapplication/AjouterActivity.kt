package com.example.ferhat.myapplication

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_ajouter.*
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.longToast
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class AjouterActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    companion object {
        const val EXTRA_LIST_CHANGED = "AjouterActivity.EXTRA_LIST_CHANGED"
    }

    lateinit var apiClient: GoogleApiClient
    var gapiOkay = false

    var lat: Double? = null
    var lng: Double? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajouter)

        apiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()


        btn_send.setOnClickListener {
            envoyer()
        }

        rb_my_position.setOnClickListener {
            txt_position.isEnabled = rb_text_position.isChecked
            btn_open_map.isEnabled = rb_map_position.isChecked
        }
        rb_text_position.setOnClickListener {
            txt_position.isEnabled = rb_text_position.isChecked
            btn_open_map.isEnabled = rb_map_position.isChecked
        }
        rb_map_position.setOnClickListener {
            txt_position.isEnabled = rb_text_position.isChecked
            btn_open_map.isEnabled = rb_map_position.isChecked
        }

        btn_open_map.setOnClickListener {
            // On lance une activity avec juste une carte et on récupérera le résultat
            startActivityForResult<SelectionCarteActivity>(1)
        }
    }

    override fun onStart() {
        super.onStart()
        apiClient.connect()
    }

    override fun onStop() {
        super.onStop()
        apiClient.disconnect()
    }

    override fun onConnected(bundle: Bundle?) {
        gapiOkay = true
        // On crée une fonction séparée pour pouvoir l'appeller ailleurs
        updatePositionUtilisateur()
    }

    private fun updatePositionUtilisateur() {
        if (gapiOkay) {
            // Si le truc est initilisé comme il faut...
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Et si on a la permission GPS, on lance la détection (requestLocationUpdates)
                val lr = LocationRequest.create()
                lr.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                lr.interval = 6 * 1000 // 1 minute (en millisecondes)

                LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, lr) {
                    // La position est dans "it" (jamais null)
                    if (rb_my_position.isChecked) {
                        // Si l'utilisateur a choisi l'option "ma position" (peut-être que le temps que le GPS s'initialise, il a choisi autre chose et donc on ignore le résultat)
                        lat = it.latitude
                        lng = it.longitude
                        // On affiche l'adresse correspondante dans le champ texte
                        updatePositionTexte()
                    }
                }
            } else {
                // On demande la permission
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1);
            }
        }
    }

    private fun updatePositionTexte() {
        // On fait une copie local en lecture seule ("val") des variables parce que apparement Kotlin aime pas les var globale ("smart cast not possible because var could have changed")
        val lat2 = lat
        val lng2 = lng

        if (lat2 != null && lng2 != null) {
            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocation(lat2, lng2, 1);

            if (addresses != null && addresses.size > 0) {
                txt_position.text.clear()
                for (i in 0..addresses[0].maxAddressLineIndex) {
                    txt_position.text.append(addresses[0].getAddressLine(i) + ", ")
                }
                // On supprime le dernier ", " en trop
                txt_position.text.removeSuffix(", ")
            }
        } else {
            txt_position.text.clear()
        }
    }


    override fun onConnectionSuspended(cause: Int) {
        gapiOkay = false
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        toast("Connection to Google API Failed")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> { // ACCESS_FINE_LOCATION
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée, il faut maintenant relancer le traitement qui necéssitait cette permission
                    updatePositionUtilisateur()
                } else {
                    // Permission refusées par l'utilisateur
                    // Ici on affiche une boite de dialogue avec un bouton Ok. A la place on pourrait afficher juste un toast
                    val msg = AlertDialog.Builder(this)
                    msg.setMessage(R.string.pas_permission_gps)
                    msg.setTitle(R.string.app_name)
                    msg.setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ -> })
                    msg.setCancelable(true)
                    msg.create().show()
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            1 -> {
                if (resultCode == RESULT_OK) {
                    lat = data?.getDoubleExtra(SelectionCarteActivity.EXTRA_LAT, 0.0)
                    lng = data?.getDoubleExtra(SelectionCarteActivity.EXTRA_LNG, 0.0)
                    updatePositionTexte()
                } else if (resultCode == RESULT_CANCELED) {
                    // Le bouton "retour" du téléphone veut dire "ne pas modifier la position"
                }
            }
        }
    }

    fun envoyer() {
        if (rb_text_position.isChecked) {
            // On convertit l'adresse saisie en coordonnées

            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocationName(txt_position.text.toString(), 1);

            if (addresses != null && addresses.size > 0) {
                lat = addresses[0].latitude
                lng = addresses[0].longitude
            }
            // Pour rb_my_position et rb_map_position, c'est déjà sous forme de double dans lat et lng, pas besoin de conversion
        }

        // On fait une copie local en lecture seule ("val") des variables parce que apparement Kotlin aime pas les var globale ("smart cast not possible because var could have changed")
        val lat2 = lat
        val lng2 = lng

        if (lat2 == null || lng2 == null) {
            // Si il n'y a pas de position valide (soit GPS pas encore initialisé, adresse saisie dans le champs inconnue, choix "choisir sur un carte" coché mais l'utilisateur n'a pas ouvert la carte pour faire son choix,...)
            longToast(R.string.pas_de_position)
            return
        }
        try {
            val cal = Calendar.getInstance()
            val format = SimpleDateFormat("dd-MMM-yyyy HH:mm:ss")
            // Comme avant, on "envoie" (= ajoute à SQL) le message immédiatement
            val date = format.parse(txt_date.text.toString())
            dbMessages.use {
                insert(
                    DBMessages.TABLE_PARTIES,
                    DBMessages.COLUMN_PARTIES_ADDRESS to txt_position.text.toString(),
                    DBMessages.COLUMN_PARTIES_STADE to txt_stade.text.toString(),
                    // On n'utilise plus le contenu de txt_date, on met la date/heure de maintenant
                    DBMessages.COLUMN_PARTIES_DATE to date.time,
                    // On ajoute lat et lng
                    DBMessages.COLUMN_PARTIES_LAT to lat2,
                    DBMessages.COLUMN_PARTIES_LNG to lng2
                )
            }
            toast(R.string.message_ajoute)
            // On prévient MainActivity que la liste a changé
            val retIntent = Intent()
            retIntent.putExtra(EXTRA_LIST_CHANGED, true)
            setResult(Activity.RESULT_OK, retIntent)
            // Et on ferme l'écran de saisie et on revient à MainActivity
            // Ca va aussi effacer tous les champs
            finish()
        }catch (e: ParseException) {
            // Erreur sur la date
            toast(R.string.invalid_date)
            txt_date.error = getString(R.string.invalid_date)
            txt_date.requestFocus()
        }

    }
}
