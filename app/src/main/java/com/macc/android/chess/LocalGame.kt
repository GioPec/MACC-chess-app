package com.macc.android.chess

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Intent

import android.content.BroadcastReceiver
import android.content.Context

import android.content.IntentFilter
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import kotlin.properties.Delegates


class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var startButton: Button
    lateinit var StockprogressBar: ProgressBar
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    var resumeButton: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**snip **/
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
        //** snip **//

        setContentView(R.layout.activity_local_game)

        chessView = findViewById(R.id.chess_view)
        resetButton = findViewById(R.id.reset_button)
        startButton = findViewById(R.id.start_button)
        resumeButton = findViewById(R.id.resume_button)
        StockprogressBar = findViewById(R.id.progress_bar_local)


        if(resumeButton?.getVisibility() == View.GONE){

        }else{

            print("ciaooMare")

        }
        print("ciaooMarezzzzzzz")

        if(ChessGame.startedmatch==0){
            resetButton.setEnabled(false)
            startButton.setEnabled(true)
        }else{
            resetButton.setEnabled(true)
            startButton.setEnabled(false)
        }
        /*println("startedmatch"+startedmatch)
        startedmatch=startedmatch+1
        */
        ChessGame.startedmatch=ChessGame.startedmatch+1
        chessView.chessDelegate = this



        resumeButton?.visibility = View.VISIBLE

        resetButton.setOnClickListener {
            ChessGame.reset(ChessGame.matchId)
            ChessGame.matchId=404
            ChessGame.resettedGame = true
            resetButton.setEnabled(false)
            startButton.setEnabled(true)
            chessView.invalidate()

        }

        startButton.setOnClickListener {
            StockprogressBar.visibility = View.VISIBLE
            ChessGame.matchId=ChessGame.startMatchId()
            println("chenepensi"+ChessGame.matchId)
            if(ChessGame.matchId!=404) {
                ChessGame.gameInProgress = "LOCAL"
                Toast.makeText(applicationContext, "Buona partita", Toast.LENGTH_LONG).show()

                resetButton.setEnabled(true)
                startButton.setEnabled(false)
                StockprogressBar.visibility = View.INVISIBLE

            }else{
                Toast.makeText(applicationContext, "Si stanno giocando molti match, prova tra poco ;-)", Toast.LENGTH_LONG).show()
            }

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



    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun showEvalChart() {}

    override fun updateTurn(player: Player, move: String) {
        Log.d("player", player.toString())
        ChessGame.firstMove=false
    }
}