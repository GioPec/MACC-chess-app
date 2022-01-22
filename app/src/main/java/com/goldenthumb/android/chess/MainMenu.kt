package com.goldenthumb.android.chess

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.lang.Exception
import java.util.HashMap

class MainMenu : AppCompatActivity()  {

    private lateinit var stockfishStatus: TextView
    var resumeButton: Button? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        ///
        ChessGame.waitingForAdversary = true
        ChessGame.gameInProgress = ""
        ChessGame.myOnlineColor = ""
        ChessGame.adversary = ""
        ChessGame.isOnlineMate = "false"
        ///

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference
        listenForChallenges()

        resumeButton = findViewById(R.id.resume_button)
        Log.d("Game in progress", ChessGame.gameInProgress)
        Log.d("Resetted game", ChessGame.resettedGame.toString())
        if (ChessGame.gameInProgress=="" || ChessGame.resettedGame) resumeButton?.visibility = View.GONE
        else resumeButton?.visibility = View.VISIBLE  //TODO: fix

        stockfishStatus = findViewById(R.id.stockfishStatus)
        getHelloWorldFromStockfishAPI()
    }

    private fun listenForChallenges() {
        myRef.child("Users").child(ChessGame.myUsername).addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    val td = snapshot.value as HashMap<*, *>
                    for (key in td.keys) {
                        val username = td[key] as HashMap<*, *>
                        for (cm in username.keys) {
                            if (cm.toString() == "currentMatch") {
                                val adversaryUsername = key as String
                                if (!ChessGame.challengeAlreadyNotified || (username[cm] as String)=="") {
                                    ChessGame.challengeAlreadyNotified = true
                                    ////////////////////////////////////////////////////////////////////
                                    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                                        when (which) {
                                            DialogInterface.BUTTON_POSITIVE -> {
                                                myRef.child("Users").child(ChessGame.myUsername).child(adversaryUsername).child("currentMatch")
                                                        .setValue("accepted")
                                                ChessGame.myOnlineColor = "BLACK"
                                                ChessGame.adversary = adversaryUsername
                                                confirmOnline("BLACK")
                                            }
                                            DialogInterface.BUTTON_NEGATIVE -> {
                                                myRef.child("Users").child(ChessGame.myUsername).child(adversaryUsername).child("currentMatch")
                                                        .setValue("refused")
                                            }
                                        }
                                    }
                                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
                                    builder.setMessage("You have been challenged by $adversaryUsername!\nAccept?")
                                        .setPositiveButton("Yes", dialogClickListener)
                                        .setNegativeButton("No", dialogClickListener).show()
                                    ////////////////////////////////////////////////////////////////////
                                }
                            }
                        }
                    }
                } catch (e:Exception) { Log.i ("MainMenu", "No challenges found")}
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }

    private fun getHelloWorldFromStockfishAPI() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://giacomovenneri.pythonanywhere.com/hello/"

        val stringRequest = StringRequest(
                Request.Method.GET, url,

                { response ->
                    response.subSequence(1, 3)

                    if (response.subSequence(1, 3) == "OK") {
                        stockfishStatus.text = "Chess API is online!"
                    } else {
                        stockfishStatus.text = "Chess API is offline! "
                    }
                },
                { error ->
                    stockfishStatus.text = "Chess API" + error
                    stockfishStatus.setTextColor(Color.RED)
                },

                )
        queue.add(stringRequest)
    }

    fun startGameAgainstStockfish(view: View) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmStockfish()
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmStockfish()
    }
    private fun confirmStockfish() {
        ChessGame.reset()
        ChessGame.gameInProgress="STOCKFISH"
        startActivity(Intent(this, StockfishGame::class.java))
        resumeButton?.visibility = View.VISIBLE
    }

    fun startGameLocal(view: View) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmLocal()
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmLocal()
    }
    private fun confirmLocal() {
        ChessGame.reset()
        ChessGame.gameInProgress="LOCAL"
        startActivity(Intent(this, LocalGame::class.java))
        resumeButton?.visibility = View.VISIBLE
    }

    fun startGameOnline(view: View) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmOnline("")
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="") {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmOnline("")
    }
    private fun confirmOnline(c:String) {
        ChessGame.reset()
        ChessGame.gameInProgress="ONLINE"

        val intent = Intent(this, OnlineGame::class.java)
        intent.putExtra("color", c)

        startActivity(intent)
        //resumeButton?.visibility = View.VISIBLE
    }

    fun resumeGame(view: View) {
        when (ChessGame.gameInProgress) {
            "LOCAL" -> startActivity(Intent(this, LocalGame::class.java))
            "STOCKFISH" -> startActivity(Intent(this, StockfishGame::class.java))
            "ONLINE" -> startActivity(Intent(this, OnlineGame::class.java))
            "" -> return
        }
    }
}