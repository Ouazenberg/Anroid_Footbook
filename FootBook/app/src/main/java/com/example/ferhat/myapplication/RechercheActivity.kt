package com.example.ferhat.myapplication

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class RechercheActivity : AppCompatActivity(), ListeFragment.Listener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recherche)
    }

    override fun onStart() {
        super.onStart()

        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val fragListe = fragmentManager.findFragmentById(R.id.frag_liste) as ListeFragment

            fragListe.chargerListeMotClef(query)
        }

    }

    override fun onMessageSelection(id: Long) {
        finish()
    }
}
