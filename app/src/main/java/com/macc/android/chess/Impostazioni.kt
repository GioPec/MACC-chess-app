package com.macc.android.chess

import android.app.AlertDialog
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


private lateinit var mAuth: FirebaseAuth
private lateinit var database: FirebaseDatabase
private lateinit var myRef: DatabaseReference
private lateinit var username: TextView

class Impostazioni : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)
        username = findViewById(R.id.username)
        username.text = ChessGame.myUsername

        //
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.package.ACTION_LOGOUT")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("onReceive", "Logout in progress")
                //At this point you should start the login activity and finish this one
                finish()
            }
        }, intentFilter)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

    }

    fun logout(view: View) {

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    FirebaseAuth.getInstance().signOut()
                    mAuth.signOut();
                    /*mGoogleSignInClient.signOut();
                    LoginManager.getInstance().logOut();*/
                    startActivity(Intent(this, Login::class.java))
                    val broadcastIntent = Intent()
                    broadcastIntent.action = "com.package.ACTION_LOGOUT"
                    sendBroadcast(broadcastIntent)
                }
                DialogInterface.BUTTON_NEGATIVE -> {

                }
            }
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@Impostazioni)
        builder.setMessage("Are you sure you want to logout from your account?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()



    }

    fun delete(View: View){
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    FirebaseDatabase.getInstance().getReference()
                        .child("Users").child(ChessGame.myUsername).removeValue();
                    mAuth.currentUser?.delete()
                    mAuth.signOut();
                    FirebaseAuth.getInstance().signOut();
                    startActivity(Intent(this, Login::class.java))
                    val broadcastIntent = Intent()
                    broadcastIntent.action = "com.package.ACTION_LOGOUT"
                    sendBroadcast(broadcastIntent)
                }
                DialogInterface.BUTTON_NEGATIVE -> {

                }
            }
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this@Impostazioni)
        builder.setMessage("Are you sure you want to delete your account?")
            .setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.back, menu)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_back -> {
            this.finish();
            true
        }
        else -> false
    }
}