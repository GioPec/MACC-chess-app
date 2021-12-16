package com.goldenthumb.android.chess

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class MainMenu : AppCompatActivity()  {

    private lateinit var StockfishStatus: TextView

    var resumeButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        resumeButton = findViewById<Button>(R.id.resume_button)
        Log.i("info", ChessGame.gameInProgress)
        Log.i("info", ChessGame.resettedGame.toString())
        if (ChessGame.gameInProgress=="" || ChessGame.resettedGame) resumeButton?.setVisibility(View.GONE)
        else resumeButton?.setVisibility(View.VISIBLE)  //TODO: fix

        StockfishStatus = findViewById<TextView>(R.id.stockfishStatus)
        getHelloWorldFromStockfishAPI()
    }

    fun getHelloWorldFromStockfishAPI() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://giacomovenneri.pythonanywhere.com/hello/"

        val stringRequest = StringRequest(Request.Method.GET, url,

                Response.Listener<String> { response ->
                    response.subSequence(1,3)

                    if (response.subSequence(1,3).equals("OK")) {
                        StockfishStatus.setText("Chess API is online!")
                    }
                    else {
                        StockfishStatus.setText("Chess API is offline! ")
                    }
                }
                ,Response.ErrorListener { error ->
                    StockfishStatus.setText("Chess API"+error)
                    StockfishStatus.setTextColor(Color.RED)
                },

        )
        queue.add(stringRequest)
    }

    /* TODO: ask confirmation to start new game when one is already in progress
    this should help with popup. https://stackoverflow.com/a/2478662/14126301 */

    fun startGameAgainstStockfish(view: View) {
        ChessGame.reset()
        ChessGame.resetStockfishGame()
        ChessGame.gameInProgress="STOCKFISH"
        startActivity(Intent(this, StockfishGame::class.java))
        resumeButton?.setVisibility(View.VISIBLE)
    }

    fun startGameLocal(view: View) {
        ChessGame.reset()
        ChessGame.resetStockfishGame()
        ChessGame.gameInProgress="LOCAL"
        startActivity(Intent(this, LocalGame::class.java))
        resumeButton?.setVisibility(View.VISIBLE)
    }

    fun resumeGame(view: View) {
        when (ChessGame.gameInProgress) {
            "LOCAL" -> startActivity(Intent(this, LocalGame::class.java))
            "STOCKFISH" -> startActivity(Intent(this, StockfishGame::class.java))
            "ONLINE" -> return //TODO
            "" -> return
        }
    }
}