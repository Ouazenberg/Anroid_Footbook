package com.example.ferhat.myapplication

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.db.asMapSequence
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.db.select
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity(), DetailFragment.Listener, ListeFragment.Listener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fragmentManager.beginTransaction().replace(R.id.frag_liste_ou_map, ListeFragment()).commit()

        btn_send.setOnClickListener {
            startActivityForResult<AjouterActivity>(1)
        }
    }

    lateinit var menuMap: MenuItem
    lateinit var menuListe: MenuItem

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        menuMap = menu.findItem(R.id.action_map)
        menuListe = menu.findItem(R.id.action_list)

        return true
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete ->  {
                // On passe le message au fragement (cas tablette)
                val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
                fragDet.supprimerItemActif()
                return true
            }
            R.id.action_map -> {
                // On masque le bouton "map"
                menuMap.isVisible = false
                // On affiche le bouton "liste"
                menuListe.isVisible = true
                // On remplace le fragment "liste" par un fragment dynamique "MapFragment"
                val fragMap = MapFragment.newInstance()
                fragmentManager.beginTransaction().replace(R.id.frag_liste_ou_map, fragMap).commit()
                // On modifie la carte...
                fragMap.getMapAsync {
                    // On ajoute un marqueur par Message dans la base de données (même code que dans ListeFragment)

                    // On select sur la base de données
                    dbMessages.use {
                        select(DBMessages.TABLE_PARTIES, // Table
                            DBMessages.COLUMN_PARTIES_ID, // Les colones utiles undiquement : id pour ouvrir le détail, lat et lng pour afficher le marqeur, image pour en faire le marqueur
                            DBMessages.COLUMN_PARTIES_IMAGE,
                            DBMessages.COLUMN_PARTIES_LAT,
                            DBMessages.COLUMN_PARTIES_LNG).exec {
                            // Pas de Where, on veut tous les messages
                            for (row in asMapSequence()) {
                                it.addMarker(MarkerOptions()
                                    .position(LatLng(row[DBMessages.COLUMN_PARTIES_LAT] as Double, row[DBMessages.COLUMN_PARTIES_LNG] as Double))
                                    // Je mets l'ID dans le title, pas d'autre choix apparement
                                    .title(""+row[DBMessages.COLUMN_PARTIES_ID] as Long)
                                    .icon(
                                        if(row[DBMessages.COLUMN_PARTIES_IMAGE] == null)
                                        // Icone par défaut
                                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                        else
                                        // Icone = l'image du Message
                                            BitmapDescriptorFactory.fromPath(row[DBMessages.COLUMN_PARTIES_IMAGE] as String)
                                    ))
                            }
                        }
                    }

                    // Quand on clique sur un marqueur
                    it.setOnMarkerClickListener {
                        // On récupère l'ID dans le title (on n'affichera pas la popup par défaut de toute façon)
                        val id = it.title.toLong()
                        // On avait déjà écrit une fonction onMessageSelection pour le cas liste !
                        onMessageSelection(id)
                        // dernière ligne, true = on n'affiche pas le snippet
                        true
                    }
                }
                return true
            }
            R.id.action_list -> {
                // On affiche le bouton "map"
                menuMap.isVisible = true
                // On masque le bouton "liste"
                menuListe.isVisible = false
                // On remet un fragment "liste" dans frag_liste_ou_map
                fragmentManager.beginTransaction().replace(R.id.frag_liste_ou_map, ListeFragment()).commit()
                return true
            }
            R.id.options ->  {
                //TODO: faire une activité "options" et la lancer
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            when (requestCode) {
                1 -> {
                    if (resultCode == RESULT_OK) {
                        val r = data?.getBooleanExtra(AjouterActivity.EXTRA_LIST_CHANGED, false) ?: false
                        if (r) {
                            val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
                            fragListe.chargerListe()
                        }
                    }
                }
                2 -> {
                    if (resultCode == RESULT_OK) {
                        val r = data?.getBooleanExtra(DetailActivity.EXTRA_LIST_CHANGED, false) ?: false
                        if (r) {
                            val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
                            fragListe.chargerListe()
                        }
                    }
                }
            }
        }catch (e:Exception){

        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onMessageSelection(id: Long) {
        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment?
        if(fragDet != null){
            fragDet.afficherDetail(id)
        }else{
            startActivityForResult<DetailActivity>(2, DetailActivity.EXTRA_MESSAGE_ID to id)
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onMessageDelete() {
        val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment
        fragListe.chargerListe()

        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
        fragDet.afficherDetail(-1)
        /*dbMessages.use {
            // TODO: penser à aussi supprimer son image si il en a une
            delete(DBMessages.TABLE_PARTIES,
                "${DBMessages.COLUMN_PARTIES_ID} = {id}",
                "id" to intent.getLongExtra("id",0))
            val intent=Intent(this@MainActivity,MainActivity::class.java)
            toast("Deleted")
            startActivity(intent)
        }*/
    }
}
