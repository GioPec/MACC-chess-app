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

    private var isAlreadySentToAdv = false

    private var myLastMove = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online)

        isAlreadySentToAdv = false

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

        chessView = findViewById(R.id.chess_view)
        challengeButton = findViewById(R.id.challenge_button)
        challengeUsername = findViewById(R.id.challenge_username)

        if (ChessGame.myOnlineColor == "") {
            challengeButton.visibility = View.VISIBLE
            challengeUsername.visibility = View.VISIBLE
        }
        else {
            challengeButton.visibility = View.INVISIBLE
            challengeUsername.visibility = View.INVISIBLE
        }

        chessView.chessDelegate = this

        val b:Bundle = intent.extras!!
        val maybeColor = b.getString("color")
        if (maybeColor!="") {
            ChessGame.myOnlineColor=maybeColor!!
            startOnlineGame(ChessGame.myOnlineColor)
        }

        /*
        resetButton.setOnClickListener {
            ChessGame.reset()
            chessView.invalidate()
            serverSocket?.close()
            listenButton.isEnabled = true
        }
         */
    }

    override fun onDestroy () {
        super.onDestroy()

        //ChessGame.challengeAlreadyNotified = false
        ChessGame.waitingForAdversary = true
        ChessGame.gameInProgress = ""
        val localAdversary = ChessGame.adversary
        val localmyOnlineColor = ChessGame.myOnlineColor
        ChessGame.myOnlineColor = ""
        ChessGame.adversary = ""

        if (localmyOnlineColor=="BLACK") {

            val date = Calendar.getInstance().timeInMillis.toString()
            var match : String

            myRef.child("Users").child(ChessGame.myUsername).child(localAdversary).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val td = snapshot.value as HashMap<*, *>
                    for (key in td.keys) {
                        if (key.toString() == "currentMatch") {

                            match = td[key] as String
                            myRef.child("Users").child(ChessGame.myUsername).child(localAdversary).child("savedMatches").child(date).setValue(match)
                            myRef.child("Users").child(localAdversary).child(ChessGame.myUsername).child("savedMatches").child(date).setValue(match)
                            myRef.child("Users").child(ChessGame.myUsername).child(localAdversary).child("currentMatch").setValue(null)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)
    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun updateTurn(player: Player, move: String) {
        myLastMove = move
        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
        dbRef.child("currentMatch").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val match = snapshot.value as String
                val newMatch = if (match == "accepted") move else "$match|$move"
                dbRef.child("currentMatch").setValue(newMatch)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        ChessGame.waitingForAdversary = true
    }

    fun requestChallenge(view: View) {
        ChessGame.adversary = challengeUsername.text.toString()
        if (ChessGame.adversary==ChessGame.myUsername) {
            Toast.makeText(applicationContext,"You can't play against yourself! *Facepalm*", Toast.LENGTH_LONG).show()
            return
        }
        myRef.child("Users").addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val td = snapshot.value as HashMap<*, *>
                for (key in td.keys) {
                    if (key.toString()==ChessGame.adversary) {
                        val au = key as String
                        //Log.i("I", "Key is: $au, value is: ${td[key]}")
                        //Log.i("I", "isAlreadySentToAdv = $isAlreadySentToAdv")

                        if (!isAlreadySentToAdv) {
                            isAlreadySentToAdv = true
                            myRef.child("Users").child(au).child(ChessGame.myUsername).child("currentMatch").setValue("")
                                .addOnSuccessListener {
                                    Toast.makeText(applicationContext, "Challenge sent to $au!", Toast.LENGTH_LONG).show()
                                    listenForChallengeAccepted()
                                }.addOnFailureListener {
                                    Toast.makeText(applicationContext, "Challenge error! :(\nTry again", Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }

    private fun listenForChallengeAccepted() {

        myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername).child("currentMatch").addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val isAccepted = try {
                    snapshot.value as String
                } catch (e: Exception) {val isAccepted = ""}
                if (!(isAccepted=="accepted" || isAccepted=="refused")) return
                Log.i("I", "Challenge has been $isAccepted")
                Toast.makeText(applicationContext,"Challenge has been $isAccepted by ${ChessGame.adversary}", Toast.LENGTH_LONG).show()

                if (isAccepted=="accepted") {
                    ChessGame.myOnlineColor = "WHITE"
                    challengeButton.visibility = View.INVISIBLE
                    challengeUsername.visibility = View.INVISIBLE
                    startOnlineGame("WHITE")
                }
                else if (isAccepted=="refused") {
                    challengeUsername.setText("")
                    ChessGame.adversary = ""
                    myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername).child("currentMatch").setValue(null)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }

    private fun startOnlineGame(color:String) {
        Toast.makeText(applicationContext,"Game is started!", Toast.LENGTH_LONG).show()
        if (color=="WHITE") ChessGame.waitingForAdversary = false
        ChessGame.gameInProgress = "ONLINE"

        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
        dbRef.child("currentMatch").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val match = snapshot.value as String
                    val lastMatchMove = match.split("|").last()
                    Log.i("I", "onDataChange: $lastMatchMove")
                    Log.i("I", "onDataChange: $myLastMove")
                    if (lastMatchMove!=myLastMove && lastMatchMove!="accepted") {
                        playAdversaryMove(lastMatchMove)
                        ChessGame.waitingForAdversary = false
                    }
                } catch (e: Exception) {}
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun playAdversaryMove(move: String) {
        val squares = ChessGame.convertMoveStringToSquares(move)
        val fromRow = squares[0].row
        val fromCol = squares[0].col
        val row = squares[1].row
        val col = squares[1].col
        val movingPiece = ChessGame.pieceAt(fromCol, fromRow)

        val promotionCheck = ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)

        ChessGame.removeEnpassantPawn(movingPiece, fromRow, fromCol, row, col)

        val castleCheck = ChessGame.castle(movingPiece, fromRow, fromCol, row, col)
        when (castleCheck) {
            "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
            "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
            "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
            "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
        }

        ChessGame.piecesBox.remove(movingPiece)
        if (promotionCheck == "") {
            movingPiece?.let {
                ChessGame.addPiece(
                    it.copy(
                            col = col,
                            row = row
                    )
                )
            }
        }
        if (movingPiece != null) {
            ChessGame.pieceAt(col, row)?.let {
                if (it.player != movingPiece?.player) {
                    ChessGame.piecesBox.remove(it)
                }
            }
        }
        chessView.invalidate()
    }
}