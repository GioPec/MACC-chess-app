package com.macc.android.chess

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.LinearLayout


class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    var resumeButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_game)

        chessView = findViewById(R.id.chess_view)
        resetButton = findViewById(R.id.reset_button)
        resumeButton = findViewById(R.id.resume_button)
        resumeButton?.visibility = View.VISIBLE

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset(ChessGame.matchId)
            ChessGame.matchId=404
            ChessGame.resettedGame = true
            chessView.invalidate()
        }


    }


    private fun drawTextAt(canvas: Canvas, col: Int, row: Int) {
        val paintThin = Paint()
        val padding = 30f
        paintThin.color = Color.parseColor("#999999")
        paintThin.strokeWidth = 3f
        paintThin.textSize = 60f
        //originX+col*cellSide, originY+row*cellSide
        //canvas.drawText("shish",originX+1*cellSide,originY+8*cellSide,paint)
        canvas.drawText("s", +50f, 50f, paintThin)
        //canvas.drawRect(originX+col*cellSide, originY+row*cellSide, originX+(col+1)*cellSide, originY+(row+1)*cellSide, paint)
    }

    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun showEvalChart() {}

    override fun updateTurn(player: Player, move: String) {
        Log.d("player", player.toString())
        ChessGame.firstMove=false
    }
}