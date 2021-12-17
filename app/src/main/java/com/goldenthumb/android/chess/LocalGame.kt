package com.goldenthumb.android.chess

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var listenButton: Button
    private lateinit var connectButton: Button
    private lateinit var turnTextView: TextView
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

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
            ChessGame.resetStockfishGame()
            turnTextView.setTextColor(Color.parseColor("#FFFFFF"))
            turnTextView.setBackgroundColor(Color.parseColor("#CCCCCC"))
            turnTextView.setText("White turn")
            chessView.invalidate()
            listenButton.isEnabled = true

        }
    }

    override fun movePiece(from: Square, to: Square) {}

    override fun updateProgressBar(type:String, value:Integer) {}

    override fun updateTurn(player: Player) {
        Log.d("player", player.toString())
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