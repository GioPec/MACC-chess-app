package com.macc.android.chess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Registration : AppCompatActivity()  {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database:FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var register: TextView
    private lateinit var loginButton: Button
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private var matchMoves = mutableListOf<Any>()
    private  var valore: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)


        /**snip  */
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.package.ACTION_LOGOUT")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("onReceive", "Logout in progress")
                //At this point you should start the login activity and finish this one
                finish()
            }
        }, intentFilter)

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
        presenceUsername(splitString(emailView.text.toString()))
        Handler(Looper.getMainLooper()).postDelayed({
            println("finalmente"+valore)
            if (emailView.text.toString()!="" && splitMail(emailView.text.toString()) && passwordView.text.toString()!="" && !valore)
                registerToFireBase(emailView.text.toString(), passwordView.text.toString())
            else toast("Error: empty fields or email already exist")

        }, 1000)


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
                    println("proviamo che 2 "+ valore)
                    if (currentUser != null && !valore) {

                        myRef.child("Users").child(splitString(currentUser.email.toString())).setValue("")
                            .addOnSuccessListener {
                                Log.i("Firebase DB write", "Success")
                            }.addOnFailureListener {
                                Log.e("Firebase DB write", "$it")
                            }
                        //Init with 100 Chess Points
                        myRef.child("Users").child(splitString(currentUser.email.toString()))
                            .child("chessPoints").setValue(100)

                        startActivity(Intent(this, Login::class.java))
                    }


                } else {
                    Log.e("ERROR", task.exception.toString())
                    Toast.makeText(applicationContext, task.exception?.message, Toast.LENGTH_LONG).show()
                    //emailView.setText("")
                    //passwordView.setText("")
                }
            }
    }


    private fun presenceUsername(name: String) {
        myRef.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val tuttiutenti = snapshot.value as HashMap<*, *>

                for (adv in tuttiutenti.keys) {
                    println("### adv = $adv")
                    if (name == adv) {
                        valore = true
                        println("proviamo che "+ valore)
                        //return
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

    }


    private fun splitString(str:String):String {
        val split = str.split("@")
        var stringa=split[0].replace(".", "^");
        return stringa
    }

    private fun splitMail(str:String):Boolean {
        val split = str.split("@")
        if(split[1]=="gmail.com"){
            return true
        }else{
            return true
        }
    }

    private fun toast(msg:String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }
}