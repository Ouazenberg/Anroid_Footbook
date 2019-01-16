package com.example.ferhat.myapplication

import android.Manifest
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.JobIntentService
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_liste_fragment.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.select
import org.jetbrains.anko.toast
import java.util.*


class ListeFragment : Fragment(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    interface Listener {
        // Quand l'utilisateur clique sur un message, on prévient l'activity
        fun onMessageSelection(id: Long)
    }

    var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement ListeFragment.Listener")
        }
    }

    // Pour résoudre un bug avec les anciennes versions d'android
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            mListener = activity as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement ListeFragment.Listener")
        }
    }

    // Des variables pout utiliser le GPS
    lateinit var apiClient: GoogleApiClient
    var gapiOkay = false // initialement pas okay

    // La position actuelle
    var lat: Double? = null
    var lng: Double? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // On charge initialement la liste des message dans les positions GPS parce que le GPS n'est pas encore prêt
        chargerListe()

        apiClient = GoogleApiClient.Builder(activity)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_liste_fragment, container, false)
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

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // On récupère la dernière position connue, pas besoin de temps réel ici, les distance sont approximative de toute façon (à vol d'oiseau)
            val pos = LocationServices.FusedLocationApi.getLastLocation(apiClient)
            lat = pos?.latitude
            lng = pos?.longitude
            // On recharche la liste maintenant qu'on a la position
            chargerListe()
        } else {
            // Pas de permission, ici on va choisir d'afficher la liste sans les positions plutôt que de redemander la position (elle est demandée quand on ajoute un élement de toute façon)
            // Donc rien à faire de spécial
        }
    }

    override fun onConnectionSuspended(cause: Int) {
        gapiOkay = false
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        toast("Connection to Google API Failed")
    }

    fun chargerListe() {
        // Les messages qui sont affichés dans le RecyclerView
        val listeMessage = arrayListOf<MessageRVD>()

        // On select sur la base de données
        activity.dbMessages.use {
            select(DBMessages.TABLE_PARTIES, // Table
                DBMessages.COLUMN_PARTIES_ID, // Les colones de MessageRVD uniquement
                DBMessages.COLUMN_PARTIES_STADE,
                DBMessages.COLUMN_PARTIES_DATE,
                DBMessages.COLUMN_PARTIES_IMAGE,
                DBMessages.COLUMN_PARTIES_LAT,
                DBMessages.COLUMN_PARTIES_LNG).exec {
                // Pas de Where, on veut tous les messages
                for (row in asMapSequence()) {
                    val date = Date()
                    date.time = row[DBMessages.COLUMN_PARTIES_DATE] as Long
                    listeMessage.add(MessageRVD(
                        row[DBMessages.COLUMN_PARTIES_ID] as Long,
                        row[DBMessages.COLUMN_PARTIES_STADE] as String,
                        date,
                        row[DBMessages.COLUMN_PARTIES_IMAGE] as String?,
                        calculerDistance(row[DBMessages.COLUMN_PARTIES_LAT] as Double, row[DBMessages.COLUMN_PARTIES_LNG] as Double)
                    ))
                    // Si on a une image vide, on télécharge une nouvelle image
                    // - soit on vient d'ajouter cet item
                    // - soit la fonction était désactivée (batterie faible)
                    // - soit pas d'internet avant (ni wifi ni 3G/4G)
                    // - soit anciens messages du TP4, avant qu'on ajoute cette fonction
                    // - autres cas ?
                    if(row[DBMessages.COLUMN_PARTIES_IMAGE] == null)
                        telechargerImage(row[DBMessages.COLUMN_PARTIES_ID] as Long)
                }
            }
        }

        rv_liste.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rv_liste.adapter = MessageRecyclerAdapter(listeMessage){
            // On prévient MainActivity que l'utilisateur a cliqué sur un message
            mListener!!.onMessageSelection(it.id)
        }
    }

    fun chargerListeMotClef(q: String) {
        // Les messages qui sont affichés dans le RecyclerView
        val listeMessage = arrayListOf<MessageRVD>()

        // On select sur la base de données
        activity.dbMessages.use {
            select(DBMessages.TABLE_PARTIES, // Table
                DBMessages.COLUMN_PARTIES_ID, // Les colones de MessageRVD uniquement
                DBMessages.COLUMN_PARTIES_STADE,
                DBMessages.COLUMN_PARTIES_DATE,
                DBMessages.COLUMN_PARTIES_IMAGE,
                DBMessages.COLUMN_PARTIES_LAT,
                DBMessages.COLUMN_PARTIES_LNG)
                // Ici on veut un where ! On va dire qu'on fait une recherche par expéditeur
                .whereArgs("${DBMessages.COLUMN_PARTIES_STADE} LIKE {q}",
                    "q" to "%$q%").exec {

                    for (row in asMapSequence()) {
                        val date = Date()
                        date.time = row[DBMessages.COLUMN_PARTIES_DATE] as Long
                        listeMessage.add(MessageRVD(
                            row[DBMessages.COLUMN_PARTIES_ID] as Long,
                            row[DBMessages.COLUMN_PARTIES_STADE] as String,
                            date,
                            row[DBMessages.COLUMN_PARTIES_IMAGE] as String?,
                            calculerDistance(row[DBMessages.COLUMN_PARTIES_LAT] as Double, row[DBMessages.COLUMN_PARTIES_LNG] as Double)
                        ))
                        // Comme telechargerImage appelle chargetListe à nouveau, on va, dans le cas d'une liste de résultat de recherche, ne pas traiter les images manquantes
                    }
                }
        }

        rv_liste.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        rv_liste.adapter = MessageRecyclerAdapter(listeMessage){
            // On prévient RechercheActivity (pas MainActicity ici) que l'utilisateur a cliqué sur un message
            mListener!!.onMessageSelection(it.id)
        }
    }

    private fun  calculerDistance(lat1: Double, lng1: Double): Double? {
        // On fait une copie local en lecture seule ("val") des variables parce que apparement Kotlin aime pas les var globale ("smart cast not possible because var could have changed")
        val lat2 = lat
        val lng2 = lng
        if(lat2 == null || lng2 == null){
            return null
        }
        // Recherche Google : "distance between two latitude longitude java"
        // 1er résultat : https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
        // ==> Conversion automatique en Kotlin par Android Studio
        val R = 6371 // Radius of the earth

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lng2 - lng1)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        var distance = R.toDouble() * c * 1000.0 // convert to meters
        // La suite ud code de Stackoverflow parle de l'altitude, on ne gère pas ça nous

        // Par contre, on convertit en KILOmètre (au lieu de mètre) et on arroundi à deux chiffres après la virgule
        return Math.round(distance / 10) / 100.0
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                // 1 = les permissions réseaux. Normalement c'est automatique par dérogation, mais on ne sait jamais
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission accordée, il faut maintenant relancer le traitement qui necéssitait cette permission
                    // On recharge la liste pour trouver (dans la boucle de chargerListe) tous les messages sans images
                    chargerListe()
                } else {
                    // Permission refusées par l'utilisateur
                    // Ici on affiche une boite de dialogue avec un bouton Ok. A la place on pourrait afficher juste un toast
                    val msg = AlertDialog.Builder(activity)
                    msg.setMessage(R.string.pas_permission_reseau)
                    msg.setTitle(R.string.app_name)
                    msg.setPositiveButton(R.string.ok, DialogInterface.OnClickListener { _, _ ->  })
                    msg.setCancelable(true)
                    msg.create().show()
                }
            }
        }
    }

    fun telechargerImage(id: Long){
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            //toast("Permission Internet Ok")
            val connMgr = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            when (connMgr.activeNetworkInfo?.type) {
                ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE -> {
                    val intSrv = Intent()
                    intSrv.putExtra(MessageService.EXTRA_MESSAGE_DOWNLOAD_ID, id)
                    JobIntentService.enqueueWork(activity, MessageService::class.java, 0, intSrv)
                }
                null -> {
                    toast("Pas de réseau")
                }
            }
        } else {
            // On demande la permission
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE), 1);
        }
    }
}
