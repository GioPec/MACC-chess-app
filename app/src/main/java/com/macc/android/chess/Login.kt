package com.macc.android.chess

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import java.util.HashMap

class Login : AppCompatActivity()  {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database:FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var register: TextView
    private lateinit var loginButton: Button
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText

    private lateinit var googleButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private var RC_SIGN_IN: Int = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        register = findViewById(R.id.textView6)
        loginButton = findViewById(R.id.button4)
        emailView = findViewById(R.id.editTextTextEmailAddress)
        passwordView = findViewById(R.id.editTextTextPassword)
        googleButton = findViewById(R.id.google_login)

        createRequest()

        mAuth = Firebase.auth
        //mAuth = FirebaseAuth.getInstance()

        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

        // Configure Google Sign In
        // set on-click listener
        googleButton.setOnClickListener {
            signIn()
        }
    }

    private fun createRequest() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id_))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                //Log.d("TAG", "firebaseAuthWithGoogle:" + account.id)
                toast("Succesful login!")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.e("TAG", "Google sign in failed", e)
                toast("Google sign in failed!")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    //Log.d("TAG", "signInWithCredential:success")

                    val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
                    val mail = signInAccount?.email.toString()
                    ChessGame.myUsername = splitString(mail)

                    val currentUser = mAuth.currentUser

                    ////////////////////////

                    myRef.child("Users").addListenerForSingleValueEvent(object :
                        ValueEventListener {

                        override fun onDataChange(snapshot: DataSnapshot) {
                            val td = snapshot.value as HashMap<*, *>
                            for (user in td.keys) {
                                //user found in DB, don't need to add it
                                if (user.toString() == ChessGame.myUsername) return
                            }

                            //user NOT found in DB, we add it here
                            if (currentUser != null) {
                                myRef.child("Users").child(ChessGame.myUsername).setValue("")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                    ////////////////////////

                    loadMain()

                } else {
                    // If sign in fails, display a message to the user.
                    Log.e("TAG", "signInWithCredential:failure", task.exception)
                    toast("Google sign in failed!")
                }
            }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    fun goToRegistration(view: View) {
        startActivity(Intent(this, Registration::class.java))
    }

    fun login(view: View) {
        if (emailView.text.toString()!="" && passwordView.text.toString()!="") {
            Log.i("I", "login: *****************************************************")
            loginToFireBase(emailView.text.toString(), passwordView.text.toString())
        }
        else toast("Error: empty fields")
    }

    private fun loginToFireBase(email:String, password:String) {

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful login",Toast.LENGTH_LONG).show()
                    ChessGame.myUsername = splitString(email)
                    loadMain()
                } else {
                    Log.e("ERROR: ", task.exception.toString())
                    Toast.makeText(applicationContext, task.exception?.message, Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        /* Login with email and password... */
        //Log.i("I", "Username with email login = ${splitString(FirebaseAuth.getInstance().currentUser?.email.toString())}")
        ChessGame.myUsername = splitString(FirebaseAuth.getInstance().currentUser?.email.toString())

        /* ...else login with Google */
        if (!loadMain()) {
            val signInAccount = GoogleSignIn.getLastSignedInAccount(this)
            //val user = mAuth.currentUser?.displayName.toString()
            ChessGame.myUsername = splitString(FirebaseAuth.getInstance().currentUser?.email.toString())
            //Log.i("I", "Username with Google = ${ChessGame.myUsername}")
            loadMain()
        }
    }

    private fun loadMain():Boolean{
        val currentUser = mAuth.currentUser

        if (currentUser!=null) {

            val intent = Intent(this, MainMenu::class.java)
            //intent.putExtra("email", currentUser.email)
            //intent.putExtra("uid", currentUser.uid)
            startActivity(intent)
            finish()
            return true
        }
        return false
    }

    private fun splitString(str:String):String {
        val split = str.split("@")
        return split[0]
    }

    private fun toast(msg:String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }
}