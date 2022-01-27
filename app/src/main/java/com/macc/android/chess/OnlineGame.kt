package com.macc.android.chess

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class OnlineGame : AppCompatActivity(), ChessDelegate {
    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var chessView: ChessView
    private lateinit var challengeButton: Button
    private lateinit var challengeUsername: EditText
    private lateinit var drawResignButtons: ConstraintLayout
    private lateinit var drawButton: Button
    private lateinit var resignButton: Button
    private lateinit var progressBar: ProgressBar

    private var myLastMove = ""

    private var ignoreFirstSave = false

    private var isDrawRefused = false
    private var isWaitingForDrawResult = 0

    private var hasAlreadyBeenNotified = false

    private lateinit var listenerForChallengeAccepted : ValueEventListener
    private lateinit var listenerSavedMatches : ValueEventListener
    private lateinit var listenerOnlineGame : ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online)

        ChessGame.gameInProgress=""

        ignoreFirstSave = false
        isDrawRefused = false
        isWaitingForDrawResult = 0
        hasAlreadyBeenNotified = false

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

        chessView = findViewById(R.id.chess_view)
        challengeButton = findViewById(R.id.challenge_button)
        challengeUsername = findViewById(R.id.challenge_username)
        drawResignButtons = findViewById(R.id.drawResignButtons)
        drawButton = findViewById(R.id.draw_button)
        resignButton = findViewById(R.id.resign_button)
        progressBar = findViewById(R.id.progress_bar)
        drawButton.isEnabled = true
        resignButton.isEnabled = true
        progressBar.visibility = View.INVISIBLE

        if (ChessGame.myOnlineColor == "") {
            challengeButton.visibility = View.VISIBLE
            challengeUsername.visibility = View.VISIBLE
            drawResignButtons.visibility = View.INVISIBLE
        }
        else {
            challengeButton.visibility = View.INVISIBLE
            challengeUsername.visibility = View.INVISIBLE
            drawResignButtons.visibility = View.VISIBLE
        }

        chessView.chessDelegate = this

        val b:Bundle = intent.extras!!
        val maybeColor = b.getString("color")
        if (maybeColor!="") {
            ChessGame.myOnlineColor=maybeColor!!
            startOnlineGame(ChessGame.myOnlineColor)
        }
    }

    override fun onStop () {
        super.onStop()

        if (ChessGame.gameInProgress=="ONLINE") {
            if (ChessGame.myOnlineColor=="WHITE") win("BLACK") else win("WHITE")
        }

        removeListeners()
    }

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)
    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}

    override fun updateTurn(player: Player, move: String) {
        myLastMove = move
        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)

        //////////////////////////////// scrive la tua mossa sul db ////////////////////////////////

        dbRef.child("currentMatch").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val match = snapshot.value as String
                val newMatch = if (match == "accepted") move else "$match|$move"

                dbRef.child("currentMatch").setValue(newMatch)

                if (ChessGame.isOnlineMate=="true") {
                    win(ChessGame.myOnlineColor)

                    try {
                        intent.removeExtra("color")
                    } catch (e: Exception) {}
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        ChessGame.waitingForAdversary = true
        drawButton.isEnabled = false
        resignButton.isEnabled = false
    }

    fun requestChallenge(view: View) {

        ChessGame.adversary = challengeUsername.text.toString()
        if (ChessGame.adversary==ChessGame.myUsername) {
            Toast.makeText(applicationContext,"You can't play against yourself! *Facepalm*", Toast.LENGTH_LONG).show()
            return
        }

        myRef.child("Users").addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val td = snapshot.value as HashMap<*, *>
                var found = false
                for (key in td.keys) {
                    if (key.toString()==ChessGame.adversary) {
                        found = true
                        val au = key as String

                        myRef.child("Users").child(au).child(ChessGame.myUsername).child("currentMatch").setValue("")
                            .addOnSuccessListener {
                                Toast.makeText(applicationContext, "Challenge sent to $au!", Toast.LENGTH_LONG).show()
                                listenForChallengeAccepted()
                            }.addOnFailureListener {
                                Toast.makeText(applicationContext, "Challenge error! :(\nTry again", Toast.LENGTH_LONG).show()
                            }

                        progressBar.visibility = View.VISIBLE
                    }
                }
                if (!found) toast("User not found!")
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }

    private fun listenForChallengeAccepted() {

        listenerForChallengeAccepted = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername).child("currentMatch").addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                var isAccepted = ""
                try {
                    isAccepted = snapshot.value as String
                } catch (e: Exception) {
                    Log.e("E", "onDataChange: $isAccepted")
                }
                if (!(isAccepted == "accepted" || isAccepted == "refused")) return

                if (!hasAlreadyBeenNotified) {
                    Toast.makeText(applicationContext, "Challenge has been $isAccepted by ${ChessGame.adversary}", Toast.LENGTH_LONG).show()
                    hasAlreadyBeenNotified = true
                }

                if (isAccepted == "accepted") {
                    ChessGame.myOnlineColor = "WHITE"
                    challengeButton.visibility = View.INVISIBLE
                    challengeUsername.visibility = View.INVISIBLE
                    drawResignButtons.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE
                    startOnlineGame("WHITE")
                } else if (isAccepted == "refused") {
                    challengeUsername.setText("")
                    progressBar.visibility = View.INVISIBLE
                    myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername).child("currentMatch").setValue(null)
                    resetFlags()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }

    private fun startOnlineGame(color:String) {

        //remove listenerForChallengeAccepted
        if (ChessGame.myOnlineColor=="WHITE") {
            myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
                    .child("currentMatch").removeEventListener(listenerForChallengeAccepted) }

        //listen for changes in saved matches in db
        listenSavedMatches()

        Toast.makeText(applicationContext,"Game is started!", Toast.LENGTH_LONG).show()
        drawResignButtons.visibility = View.VISIBLE
        ChessGame.gameInProgress = "ONLINE"

        if (color=="WHITE") {
            drawButton.isEnabled = true
            resignButton.isEnabled = true
            ChessGame.waitingForAdversary = false
        }
        else {
            drawButton.isEnabled = false
            resignButton.isEnabled = false
        }

        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)

        ////////////////////// listen for adversary moves //////////////////////////////////////////

        listenerOnlineGame = dbRef.child("currentMatch").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val match = snapshot.value as String
                    val lastMatchMove = match.split("|").last()

                    //I have just *refused* a draw, and I need to ignore this event
                    if (isDrawRefused) {
                        isDrawRefused = false
                        return
                    }
                    //I have just *proposed* a draw, and I need to ignore this event
                    else if (isWaitingForDrawResult==1 && lastMatchMove=="½-½") {
                        isWaitingForDrawResult = 2
                        return
                    }
                    else if (isWaitingForDrawResult==2 && lastMatchMove!="½-½") {
                        isWaitingForDrawResult = 0
                        toast("Draw proposal has been refused! Keep playing")
                        drawButton.isEnabled = true
                        resignButton.isEnabled = true
                        return
                    }
                    //draw check
                    else if (lastMatchMove=="½-½") {
                        alertDraw()
                    }

                    else if (lastMatchMove != myLastMove && lastMatchMove != "accepted" && lastMatchMove != "refused" && lastMatchMove != "") {
                        playAdversaryMove(lastMatchMove)
                        ChessGame.waitingForAdversary = false
                        drawButton.isEnabled = true
                        resignButton.isEnabled = true
                    }
                } catch (e: Exception) {Log.e("E", e.toString())}
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        ////////////////////////////////////////////////////////////////////////////////////////////
    }

    private fun alertDraw() {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> win("draw")
                DialogInterface.BUTTON_NEGATIVE -> refuseDraw()
            }
        }
        //ask user
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@OnlineGame)
        builder.setMessage("Your adversary proposed a draw. Accept?").setPositiveButton("Yes", dialogClickListener)
            .setNegativeButton("No", dialogClickListener).show()
    }
    private fun refuseDraw() {
        isDrawRefused = true
        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
        dbRef.child("currentMatch").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val match = snapshot.value as String
                val newMatch = match.substring(0, match.length - 4)
                Log.i("I", "onDataChange: $newMatch")
                dbRef.child("currentMatch").setValue(newMatch)
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

        //HIGHLIGHT
        ChessGame.fromSquareHighlight = Square(fromCol, 7-fromRow)
        ChessGame.toSquareHighlight = Square(col, 7-row)

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

    fun proposeDraw(view: View) {
        if (!ChessGame.waitingForAdversary) {

            isWaitingForDrawResult = 1
            drawButton.isEnabled = false
            resignButton.isEnabled = false

            var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
            if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
            dbRef.child("currentMatch").addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val match = snapshot.value as String
                    val newMatch = if (match == "accepted") "|½-½" else "$match|½-½"
                    dbRef.child("currentMatch").setValue(newMatch)

                    toast("Draw proposal sent")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    fun resign(view: View) {
        if (!ChessGame.waitingForAdversary) {
            if (ChessGame.gameInProgress == "ONLINE") {
                if (ChessGame.myOnlineColor == "WHITE") win("BLACK") else win("WHITE")
            }
        }
    }

    private fun toast(msg:String) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private fun listenSavedMatches() {

        listenerSavedMatches = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary).child("savedMatches")
                .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!ignoreFirstSave) {
                    ignoreFirstSave = true
                    return
                }
                val td = snapshot.value as HashMap<Int, String>
                var match = ""
                try {
                    match = td[td.keys.maxOrNull()].toString()
                } catch (e: Exception) { Log.e("E", "Should be impossible")}

                val result = match.split("|").last()

                //draw
                if (result=="½-½") {
                    toast("Draw")
                }
                //victory
                else if ( (result=="0-1" && ChessGame.myOnlineColor=="BLACK") || (result=="1-0" && ChessGame.myOnlineColor=="WHITE")) {
                    toast("You won!")
                }
                //loss
                else {
                    toast("You lost!")
                }

                resetFlags()

                drawResignButtons.visibility = View.INVISIBLE

            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun resetFlags() {

        removeListeners()

        ChessGame.waitingForAdversary = true
        ChessGame.gameInProgress = ""
        ChessGame.myOnlineColor = ""
        ChessGame.adversary = ""
        ChessGame.isOnlineMate = "false"
        ignoreFirstSave = false
        myLastMove = ""
        isDrawRefused = false
        isWaitingForDrawResult = 0
        hasAlreadyBeenNotified = false
        progressBar.visibility = View.INVISIBLE
    }
    private fun removeListeners() {

        try {
            myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
                    .child("currentMatch").removeEventListener(listenerForChallengeAccepted)
        } catch (e: Exception) {}

        try {
            myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
                    .child("savedMatches").removeEventListener(listenerSavedMatches)
        } catch (e: Exception) {}

        try {
            var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
            if (ChessGame.myOnlineColor=="BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
            dbRef.child("currentMatch").removeEventListener(listenerOnlineGame)
        } catch (e: Exception) {}
    }

    private fun win(color:String) {

        if (ChessGame.gameInProgress!="ONLINE") return
        Log.i("I", "win: ${ChessGame.gameInProgress}")

        //toast di notifica vittoria
        if (color == ChessGame.myOnlineColor) toast("You won!")
        else if (color!="draw") toast("You lost!")
        else (toast("Draw"))

        //get match until now
        var match = ""
        var dbRef = myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername)
        if (ChessGame.myOnlineColor == "BLACK") dbRef = myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary)
        dbRef.child("currentMatch").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                match = snapshot.value as String
                Log.i("I", "onDataChange: $match")

                //remove currentMatch from db
                dbRef.child("currentMatch").setValue(null)

                //update match with result (0-1)
                if (color == "BLACK") {
                    match = "$match|0-1"
                } else if (color == "WHITE") {
                    match = "$match|1-0"
                }

                val date = Calendar.getInstance().timeInMillis.toString()

                //write on adversary
                myRef.child("Users").child(ChessGame.adversary).child(ChessGame.myUsername).child("savedMatches").child(date).setValue(match)
                //write on me
                myRef.child("Users").child(ChessGame.myUsername).child(ChessGame.adversary).child("savedMatches").child(date).setValue(match)

            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}