package com.example.ferhat.myapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.android.synthetic.main.activity_detail.*
import org.jetbrains.anko.db.delete
import org.jetbrains.anko.toast
import java.util.*

class DetailActivity : AppCompatActivity(), DetailFragment.Listener {
    companion object {
        const val EXTRA_MESSAGE_ID = "DetailActivity.EXTRA_MESSAGE_ID"
        const val EXTRA_LIST_CHANGED = "DetailActivity.EXTRA_LIST_CHANGED"
    }

    var people: MutableList<String> = ArrayList()
    lateinit var dialog: AlertDialog
    lateinit var adapter: ArrayAdapter<String>
    lateinit var sp: SharedPreferences
    var num = 0
    lateinit var set:HashSet<String>

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete ->  {
                // On passe le message au fragement
                try{
                val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
                fragDet.supprimerItemActif()
                }catch (e:Exception){

                }
                val intent=Intent(this@DetailActivity,MainActivity::class.java)
                toast("Partie Supprimée")
                startActivity(intent)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val fragDet = fragmentManager.findFragmentById(R.id.frag_detail) as DetailFragment
        fragDet.afficherDetail(intent.getLongExtra(EXTRA_MESSAGE_ID, 0))
        sp = getSharedPreferences("People"+intent.getLongExtra(EXTRA_MESSAGE_ID, 0),0)
        getpeople()
        list_players.setOnClickListener { add_people() }
    }

    private fun getpeople() {
        people.removeAll{true}
        set= sp.getStringSet("people", java.util.HashSet()) as HashSet<String>
        set.forEach {
            people.add(it)
        }
    }

    override fun onMessageDelete() {
        val ret = Intent()
        ret.putExtra(EXTRA_LIST_CHANGED, true)
        setResult(Activity.RESULT_OK, ret)
        finish()
    }

    fun add_people() {
        val view = layoutInflater.inflate(R.layout.people, null)
        adapter = Adapter(this, R.layout.people, people)
        val list = view.findViewById<ListView>(R.id.list_)
        val et = view.findViewById<EditText>(R.id.et)
        list.adapter = adapter
        list.onItemLongClickListener = AdapterView.OnItemLongClickListener { parent, view, position, id ->
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Retirer ce joueur?")
            builder.setPositiveButton("Oui") { dialog, which ->
                set.remove(people.get(position))
                val editor=sp.edit()
                editor.putStringSet("people",set)
                editor.commit()
                num = num - 1
                toast("Joueur Retiré")
                this@DetailActivity.dialog.dismiss()
                getpeople()
                add_people()
            }
            builder.setNegativeButton("Non") { dialog, which -> }
            builder.setNeutralButton("Annuler", null)
            builder.setCancelable(true)
            builder.create().show()
            return@OnItemLongClickListener false
        }
        val btn = view.findViewById<Button>(R.id.btn)
        if(num>10) {
            btn.isEnabled = false
        }
        btn.setOnClickListener {
            if(et.text.toString().equals("")){
                toast("Entrez un nom")
                return@setOnClickListener
            }
            val editor = sp.edit()
            set.add(et.text.toString())
            editor.putStringSet("people",set)
            editor.commit()
            num = num + 1
            et.setText("")
            getpeople()
            adapter = Adapter(this, R.layout.people, people)
            list.adapter = adapter
            if(num>=10) {
                btn.isEnabled = false
            }
        }
        val builder = AlertDialog.Builder(this)
            .setView(view)
        dialog = builder.create()
        dialog.show()
    }

    class Adapter(internal var context: Context, resource: Int, var array: MutableList<String>) :
        ArrayAdapter<String>(context, resource, R.id.textView, array) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = TextView(context)
            tv.text = array.get(position)
            tv.setTextSize(18f)
            tv.setTypeface(null, Typeface.BOLD)
            return tv
        }
    }
}
