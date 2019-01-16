package com.example.ferhat.myapplication

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_selection_carte.*
import org.jetbrains.anko.longToast

class SelectionCarteActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_LAT = "SelectionCarteActivity.LAT"
        const val EXTRA_LNG = "SelectionCarteActivity.LNG"
    }

    // La position choisie, initialement il n'y en a pas
    var lat: Double? = null
    var lng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection_carte)

        longToast(R.string.map_help_toast)

        val carte = fragmentManager.findFragmentById(R.id.carte) as MapFragment
        carte.getMapAsync {
            val map = it
            it.uiSettings.isMyLocationButtonEnabled = true
            it.uiSettings.isCompassEnabled = true
            it.uiSettings.isZoomControlsEnabled = true
            it.uiSettings.setAllGesturesEnabled(true)

            var marker: Marker? = null
            it.setOnMapClickListener {
                if(marker == null){
                    marker = map.addMarker(MarkerOptions()
                        .position(it)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
                }else {
                    // On met à jour le marqueur existant
                    marker?.position = it
                }

                // On stocke la position pour pouvoir la returner via un intent
                lat = it.latitude
                lng = it.longitude
            }
        }

        btn_ok.setOnClickListener {
            // On fait une copie local en lecture seule ("val") des variables parce que apparement Kotlin aime pas les var globale ("smart cast not possible because var could have changed")
            val lat2 = lat
            val lng2 = lng

            if(lat2 == null || lng2 == null){
                // l'utilisateur a appuyé sur valider avant d'appyer sue la carte... on réaffiche l'aide
                longToast(R.string.map_help_toast)
            }else{
                val retIntent = Intent()
                retIntent.putExtra(EXTRA_LAT, lat2)
                retIntent.putExtra(EXTRA_LNG, lng2)
                setResult(Activity.RESULT_OK, retIntent)
                finish()
            }
        }
    }
}
