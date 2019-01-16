package com.example.ferhat.myapplication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class LoginActivity : AppCompatActivity() {

    var _emailText: EditText? = null
    var _passwordText: EditText? = null
    var _loginButton: Button? = null
    var _signupLink: TextView? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        _loginButton = findViewById(R.id.btn_login) as Button
        _signupLink = findViewById(R.id.link_signup) as TextView
        _passwordText = findViewById(R.id.input_password) as EditText
        _emailText = findViewById(R.id.input_email) as EditText
        _loginButton!!.setOnClickListener { login() }

        _signupLink!!.setOnClickListener {
            val intent = Intent(applicationContext, SignupActivity::class.java)
            startActivityForResult(intent, REQUEST_SIGNUP)
            finish()
        }
    }

    fun login() {
        Log.d(TAG, "Connexion")

        if (!validate()) {
            onLoginFailed()
            return
        }

        _loginButton!!.isEnabled = false

        val progressDialog = ProgressDialog(this@LoginActivity,
                R.style.AppTheme_Dark_Dialog)
        progressDialog.isIndeterminate = true
        progressDialog.setMessage("Connexion...")
        progressDialog.show()

        val email = _emailText!!.text.toString()
        val password = _passwordText!!.text.toString()

        android.os.Handler().postDelayed(
                {
                    onLoginSuccess()
                    progressDialog.dismiss()
                }, 3000)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == Activity.RESULT_OK) {
                this.finish()
            }
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    fun onLoginSuccess() {
        _loginButton!!.isEnabled = true
//        finish()
        startActivity(Intent(this, MainActivity::class.java))
    }

    fun onLoginFailed() {
        Toast.makeText(baseContext, "Connexion échouée", Toast.LENGTH_LONG).show()

        _loginButton!!.isEnabled = true
    }

    fun validate(): Boolean {
        var valid = true

        val email = _emailText!!.text.toString()
        val password = _passwordText!!.text.toString()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText!!.error = "Entrez  adresse email valide"
            valid = false
        } else {
            _emailText!!.error = null
        }

        if (password.isEmpty() || password.length < 4 || password.length > 10) {
            _passwordText!!.error = "entre 4 et 10 caractères alphanumeriques"
            valid = false
        } else {
            _passwordText!!.error = null
        }

        return valid
    }

    companion object {
        private val TAG = "LoginActivity"
        private val REQUEST_SIGNUP = 0
    }
}
