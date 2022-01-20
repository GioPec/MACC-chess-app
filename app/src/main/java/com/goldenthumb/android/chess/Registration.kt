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

class Registration : AppCompatActivity()  {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database:FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var register: TextView
    private lateinit var loginButton: Button
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        register = findViewById<TextView>(R.id.textView6)
        loginButton = findViewById<Button>(R.id.button4)
        emailView = findViewById<EditText>(R.id.editTextTextEmailAddress)
        passwordView = findViewById<EditText>(R.id.editTextTextPassword)

        //mAuth = Firebase.auth
        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference
    }

    fun goToLogin(view: View) {
        startActivity(Intent(this, Login::class.java))
    }

    fun register(view: View) {
        registerToFireBase(emailView.text.toString(), passwordView.text.toString())
    }

    private fun registerToFireBase(email:String, password:String) {

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    Toast.makeText(
                        applicationContext,
                        "Successfully registered!",
                        Toast.LENGTH_LONG
                    ).show()

                    val currentUser = mAuth.currentUser
                    //save in database
                    if (currentUser != null) {

                        myRef.child("Users").child(splitString(currentUser.email.toString())).setValue("")
                            .addOnSuccessListener {
                                Log.i("Firebase DB write", "Success")
                            }.addOnFailureListener {
                                Log.e("Firebase DB write", "$it")
                            }
                    }

                    startActivity(Intent(this, Login::class.java))

                } else {
                    Log.e("ERROR", task.exception.toString())
                    Toast.makeText(applicationContext, task.exception?.message, Toast.LENGTH_LONG).show()
                    //emailView.setText("")
                    //passwordView.setText("")
                }
            }
    }

    private fun splitString(str:String):String {
        val split = str.split("@")
        return split[0]
    }
}