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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

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

    fun startGameAgainstStockfish(view: View) {
        //ChessGame.reset()
        startActivity(Intent(this, StockfishGame::class.java))
        //setContentView(R.layout.activity_stockfish)
    }
}