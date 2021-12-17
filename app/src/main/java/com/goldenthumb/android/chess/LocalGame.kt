package com.goldenthumb.android.chess

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.*


class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var listenButton: Button
    private lateinit var connectButton: Button
    private lateinit var turnTextView: TextView
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    var initial_position="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    fun resetStockfish() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://giacomovenneri.pythonanywhere.com/reset/"

        val stringRequest = StringRequest(Request.Method.GET, url,

            Response.Listener<String> { response ->
                if (response.equals(initial_position)) {
                    Log.i("Info","Succesful reset")
                }
            }
            ,Response.ErrorListener { error ->
                Log.e("Error","Reset error")
            },

            )
        queue.add(stringRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_game)
        //resetStockfish()

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)
        turnTextView = findViewById<Button>(R.id.turn)

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
            resetStockfish()
            turnTextView.setTextColor(Color.parseColor("#FFFFFF"))
            turnTextView.setBackgroundColor(Color.parseColor("#CCCCCC"))
            turnTextView.setText("White turn")
            chessView.invalidate()
            listenButton.isEnabled = true

        }
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.size > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            else Log.e("E", "Error with audio permissions")
        }
        else Log.e("E", "Error with audio permissions")
    }

    fun convertMoveStringToSquares(move: String): Array<Square> {

        assert(move.length >= 4)  //è 5 in caso di promozione! (es: e2f1q)
        var fromCol = 0
        var firstChar = move.substring(0, 1)
        when (firstChar) {
            "a" -> fromCol = 0
            "b" -> fromCol = 1
            "c" -> fromCol = 2
            "d" -> fromCol = 3
            "e" -> fromCol = 4
            "f" -> fromCol = 5
            "g" -> fromCol = 6
            "h" -> fromCol = 7
        }
        val fromRow = (move.substring(1, 2).toInt()-1)

        var toCol = 0
        var thirdChar = move.substring(2, 3)
        when (thirdChar) {
            "a" -> toCol = 0
            "b" -> toCol = 1
            "c" -> toCol = 2
            "d" -> toCol = 3
            "e" -> toCol = 4
            "f" -> toCol = 5
            "g" -> toCol = 6
            "h" -> toCol = 7
        }
        val toRow = (move.substring(3, 4).toInt()-1)

        val fromSquare = Square(fromCol, fromRow)
        val toSquare = Square(toCol, toRow)

        return arrayOf(fromSquare, toSquare)
    }

    fun sendMovetoStockfish(move: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://giacomovenneri.pythonanywhere.com/?move="+move;

        val stringRequest = StringRequest(
            Request.Method.POST, url,

            Response.Listener { response ->
                Log.e("Pythonanywhere move: ", response)
            }
            ,
            Response.ErrorListener { error ->
                Log.e("Pythonanywhere error: ", error.toString())
            },

            )
        queue.add(stringRequest)
    }

    override fun movePiece(from: Square, to: Square) {}

    override fun updateProgressBar(type:String, value:Integer) {}

    override fun updateTurn(player: Player) {
        //Log.d("player", player.toString())
        if (player.equals(Player.WHITE)) {
            turnTextView.setTextColor(Color.parseColor("#999999"))
            turnTextView.setBackgroundColor(Color.parseColor("#333333"))
            turnTextView.setText("Black turn")
        }
        else {
            turnTextView.setTextColor(Color.parseColor("#FFFFFF"))
            turnTextView.setBackgroundColor(Color.parseColor("#CCCCCC"))
            turnTextView.setText("White turn")
        }
    }
}