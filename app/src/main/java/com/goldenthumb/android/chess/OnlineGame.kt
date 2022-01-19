package com.goldenthumb.android.chess

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.*

class OnlineGame : AppCompatActivity(), ChessDelegate {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var chessView: ChessView
    private lateinit var challengeButton: Button
    private lateinit var challengeUsername: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online)

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

        chessView = findViewById(R.id.chess_view)
        challengeButton = findViewById(R.id.challenge_button)
        challengeUsername = findViewById(R.id.challenge_username)

        chessView.chessDelegate = this

        /*
        resetButton.setOnClickListener {
            ChessGame.reset()
            chessView.invalidate()
            serverSocket?.close()
            listenButton.isEnabled = true
        }
         */
    }

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)
    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun updateTurn(player: Player) {}

    fun requestChallenge(view: View) {
        val adversary = challengeUsername.text.toString()
        if (adversary==ChessGame.myUsername) {
            Toast.makeText(applicationContext,"You can't play against yourself! *Facepalm*", Toast.LENGTH_LONG).show()
            return
        }
        myRef.child("Users").addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val td = snapshot.value as HashMap<*, *>
                for (key in td.keys) {
                    if (key.toString()==adversary) {
                        val value = td[key] as String
                        //Log.i("I", "Key is: ${key.toString()}, value is: $value")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }
}