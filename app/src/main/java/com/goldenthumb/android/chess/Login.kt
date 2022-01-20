package com.goldenthumb.android.chess

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Login : AppCompatActivity()  {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database:FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var register: TextView
    private lateinit var loginButton: Button
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        register = findViewById<TextView>(R.id.textView6)
        loginButton = findViewById<Button>(R.id.button4)
        emailView = findViewById<EditText>(R.id.editTextTextEmailAddress)
        passwordView = findViewById<EditText>(R.id.editTextTextPassword)

        //mAuth = Firebase.auth
        mAuth = FirebaseAuth.getInstance()
    }

    fun goToRegistration(view: View) {
        startActivity(Intent(this, Registration::class.java))
    }

    fun login(view: View) {
        loginToFireBase(emailView.text.toString(), passwordView.text.toString())
    }

    private fun loginToFireBase(email:String, password:String) {

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()
                    ChessGame.myUsername = splitString(email)
                    loadMain()
                } else {
                    Log.e("ERROR", task.exception.toString())
                    Toast.makeText(applicationContext, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        /* !!!
        Uncomment following lines to skip login */
        ChessGame.myUsername = splitString(FirebaseAuth.getInstance().currentUser?.email.toString())
        loadMain()
    }

    private fun loadMain(){
        val currentUser = mAuth.currentUser

        if (currentUser!=null) {

            var intent = Intent(this, MainMenu::class.java)
            intent.putExtra("email", currentUser.email)
            intent.putExtra("uid", currentUser.uid)
            startActivity(intent)
            finish()
        }
    }

    private fun splitString(str:String):String {
        val split = str.split("@")
        return split[0]
    }
}