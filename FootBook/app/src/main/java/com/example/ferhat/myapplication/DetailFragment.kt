package com.example.ferhat.myapplication

import android.app.Fragment
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_detail_fragment.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.select
import java.util.*


class DetailFragment : Fragment(){
    interface Listener {
        // Quand on supprime un message, il faut prévenir MainActivity de mettre à jour la liste
        fun onMessageDelete()
    }

    var mListener: Listener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement DetailFragment.Listener")
        }
    }

    // L'id du message actuellement affiché
    var idMessage : Long = -1

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun onActivityCreated(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Il faut utiliser childFragmentManager au lieu de fragmentManager quand on est nous même dans un fragment
        val carte = childFragmentManager.findFragmentById(R.id.carte) as MapFragment
        // On désactive tout, carte statique
        carte.getMapAsync {
            it.uiSettings.isZoomControlsEnabled = false
            it.uiSettings.isMyLocationButtonEnabled = false
            it.uiSettings.setAllGesturesEnabled(false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.activity_detail_fragment, container, false)
    }

    fun supprimerItemActif(){
        // L'ancien code du botuon delete est maintenant une fonction pour pouvoir être appelé soit par DetailActivity (cas téléphone) soit par MaintActivity (cas tablette) quand on appuie sur le bouton de action bar corbeille

        // On supprime le message actuellement affiché de la base de données
        activity.dbMessages.use {
            // TODO: penser à aussi supprimer son image si il en a une
            delete(DBMessages.TABLE_PARTIES,
                "${DBMessages.COLUMN_PARTIES_ID} = {id}",
                "id" to idMessage)
        }
        // On prévient notre activité :
        // si on est sur tablette, ce fragment est directement dans MainActivity qui va mettre à la jour le RecyclerView
        // si on est sur téléphone, ce fragment est dans DetailActivity, c'est lui qui va prévenir MainActivity pour nous
        mListener!!.onMessageDelete()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun afficherDetail(id: Long) {
        // On sauvegarde l'id affiché
        idMessage = id

        // On charge le message de la base de données
        activity.dbMessages.use {
            select(DBMessages.TABLE_PARTIES, // Table
                DBMessages.COLUMN_PARTIES_ID, // Toutes les colones
                DBMessages.COLUMN_PARTIES_STADE,
                DBMessages.COLUMN_PARTIES_DATE,
                DBMessages.COLUMN_PARTIES_LAT,
                DBMessages.COLUMN_PARTIES_LNG,
                DBMessages.COLUMN_PARTIES_ADDRESS)
                .whereArgs("${DBMessages.COLUMN_PARTIES_ID} = {id}", // On va charger un message à la fois avec comme identifiant id
                    "id" to idMessage)
                .exec {
                    if(count == 0){
                        // Au cas où il n'y est aucun message
                        val am = activity.getString(R.string.aucun_message)
                        tv_pk.text = am
                        tv_stade.text = am
                        tv_date.text = am
                        tv_adress.text = am
                    }else {
                        // 1 message maximum retourné, parce que les id sont uniques (PK)
                        for (row in asMapSequence()) {
                            // On ne passe qu'une seule fois dans le for du coup
                            tv_pk.text = (row[DBMessages.COLUMN_PARTIES_ID] as Long).toString()
                            tv_stade.text = row[DBMessages.COLUMN_PARTIES_STADE] as String
                            val date = Date()
                            date.time = row[DBMessages.COLUMN_PARTIES_DATE] as Long
                            tv_date.text = date.toLocaleString()
                            tv_adress.text = row[DBMessages.COLUMN_PARTIES_ADDRESS] as String

                            // Il faut utiliser childFragmentManager au lieu de fragmentManager quand on est nous même dans un fragment
                            val carte= childFragmentManager.findFragmentById(R.id.carte) as MapFragment
                            carte.getMapAsync {
                                // On centre la carte sur la position et on ajoute un marqueur
                                it.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    LatLng(row[DBMessages.COLUMN_PARTIES_LAT] as Double, row[DBMessages.COLUMN_PARTIES_LNG] as Double),
                                    15f /// Zoom
                                ))
                                it.addMarker(MarkerOptions()
                                    .position(LatLng(row[DBMessages.COLUMN_PARTIES_LAT] as Double, row[DBMessages.COLUMN_PARTIES_LNG] as Double))
                                    .title(row[DBMessages.COLUMN_PARTIES_STADE] as String)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                            }
                        }
                    }
                }
        }
    }
}
