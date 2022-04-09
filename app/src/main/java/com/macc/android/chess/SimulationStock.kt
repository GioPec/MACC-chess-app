package com.macc.android.chess

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class SimulationStock : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var restartButton: Button
    lateinit var StockprogressBar: ProgressBar
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    var resumeButton: Button? = null


    private val mInterval = 2000 // 5 seconds by default, can be changed later

    private var mHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ChessGame.lettere= arrayOf("a","b","c","d","e","f","g","h")
        ChessGame.numeri= arrayOf("8","7","6","5","4","3","2","1")

        /**snip **/
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

        setContentView(R.layout.activity_simulation_stock)

        chessView = findViewById(R.id.chess_view10)
        resetButton = findViewById(R.id.reset_button10)
        startButton = findViewById(R.id.start_button10)
        pauseButton = findViewById(R.id.pausa_button10)
        restartButton = findViewById(R.id.restart_button10)
        resumeButton = findViewById(R.id.resume_button)
        StockprogressBar = findViewById(R.id.progress_bar_local10)

        if(resumeButton?.getVisibility() == View.GONE){

        }else{

            print("ciaooMare")

        }
        print("ciaooMarezzzzzzz")


        if(ChessGame.startedmatch==0){
            ChessGame.simulationvinto=0
            resetButton.setEnabled(false)
            startButton.setEnabled(true)
            pauseButton.setEnabled(false)
            restartButton.setEnabled(false)
        }else{
            resetButton.setEnabled(true)
            startButton.setEnabled(false)
            pauseButton.setEnabled(false)
            restartButton.setEnabled(true)
        }
        if(ChessGame.simulationvinto>0){
            resetButton.setEnabled(true)
            startButton.setEnabled(false)
            pauseButton.setEnabled(false)
            restartButton.setEnabled(false)
        }
        /*println("startedmatch"+startedmatch)
        startedmatch=startedmatch+1
        */
        ChessGame.startedmatch=ChessGame.startedmatch+1
        chessView.chessDelegate = this

        mHandler = Handler()
        resumeButton?.visibility = View.VISIBLE

        resetButton.setOnClickListener {
            ChessGame.reset(ChessGame.matchId)
            ChessGame.matchId=404
            ChessGame.resettedGame = true
            stopRepeatingTask()
            resetButton.setEnabled(false)
            startButton.setEnabled(true)
            pauseButton.setEnabled(false)
            restartButton.setEnabled(false)
            chessView.invalidate()


        }

        pauseButton.setOnClickListener{
            stopRepeatingTask()
            resumeButton?.visibility = View.VISIBLE
            pauseButton.setEnabled(false)
            restartButton.setEnabled(true)
        }
        restartButton.setOnClickListener {
            startRepeatingTask()
            resumeButton?.visibility = View.VISIBLE
            pauseButton.setEnabled(true)
            restartButton.setEnabled(false)
        }

        startButton.setOnClickListener {
            //StockprogressBar.visibility = View.VISIBLE
            //val start = System.currentTimeMillis()
            ChessGame.matchId=ChessGame.startMatchId()
            println("chenepensi"+ChessGame.matchId)
            if(ChessGame.matchId!=404) {
                ChessGame.gameInProgress = "SIMULATION"
                //Toast.makeText(applicationContext, "Buona partita", Toast.LENGTH_LONG).show()

                startButton.setEnabled(false)
                resetButton.setEnabled(true)
                pauseButton.setEnabled(true)
                restartButton.setEnabled(false)

                //StockprogressBar.visibility = View.INVISIBLE

                //val runTime = System.currentTimeMillis() - start
                //println("iltempi "+runTime)
                /*
                ChessGame.resettedGame = false
                val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> startRepeatingTask()
                        DialogInterface.BUTTON_NEGATIVE -> resetButton.setEnabled(false)
                    }
                }
                //ask user
                if (ChessGame.gameInProgress!="" && !ChessGame.resettedGame) {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@SimulationStock)
                    builder.setMessage("booo?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show()
                } else resetButton.setEnabled(false)
*/
                /*
                var MioRun: Runnable

                ha.postDelayed(object : Runnable {
                    override fun run() {
                        risposta=doOptimalSimulation()
                        if(risposta!="true") {
                            ha.postDelayed(this, 2000)
                        }else{
                            ha.removeCallbacks(this);
                            alert("Victory","You won", "OK")
                        }

                    }
                }, 2000)
                */

                //doFirstMove()
                Handler().postDelayed(this::doFirstMove, 2000)

                startRepeatingTask()


            }else{
                Toast.makeText(applicationContext, "Server full, please try again later", Toast.LENGTH_LONG).show()
            }

        }

    }


    var mStatusChecker: Runnable = object : Runnable {
        override fun run() {
            var risposta=doOptimalSimulation() //this function can change value of mInterval.

            if(risposta!="bianco" &&  risposta!="nero") {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                resumeButton?.visibility = View.VISIBLE
                mHandler!!.postDelayed(this, mInterval.toLong())
            }else if(risposta=="bianco"){
                mHandler!!.removeCallbacks(this)
                ChessGame.simulationvinto=1
                pauseButton.setEnabled(false)
                restartButton.setEnabled(false)
                alert("Victory","White won","Ok")

            }else if(risposta=="nero"){
                mHandler!!.removeCallbacks(this)
                ChessGame.simulationvinto=2
                pauseButton.setEnabled(false)
                restartButton.setEnabled(false)
                alert("Victory","Black won","Ok")
            }

        }
    }

    fun startRepeatingTask() {
        mStatusChecker.run()

    }

    fun stopRepeatingTask() {
        mHandler!!.removeCallbacks(mStatusChecker)
    }

    fun doFirstMove() : String{
        var moveIsValid=false
        var response=""
        var mate= ""
        var turno= 0
        var risposta=""

        var mossebianco: Array<String> = arrayOf("a2a4", "a2a3", "b2b4", "b2b3", "c2c4", "c2c3", "d2d4",
            "d2d3", "e2e4", "e2e3", "f2f4", "f2f3", "g2g4", "g2g3",
            "h2h4", "h2h3", "b1a3", "b1c3", "g1f3", "g1h3")

        val rnds = (0..19).random()

        response=(mossebianco[rnds])

        val job = GlobalScope.launch(Dispatchers.IO) {
            Log.i("Thread job: ", Thread.currentThread().name)
            run {
                var id_string = ChessGame.matchId.toString()
                val name =
                    "https://JaR.pythonanywhere.com" + "/?move="+response + "&index=" + id_string
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "POST"
                        val r = JSONObject(InputStreamReader(inputStream).readText())
                        Log.d("Stockfish response", r.toString())
                        moveIsValid = r.get("valid") as Boolean
                        mate = r.get("mate") as String
                    }
                } catch (e: Exception) {
                    Log.e("Move error: ", e.toString())
                }
            }
        }

        runBlocking {
            job.join()
            Log.i("Thread runblocking: ", Thread.currentThread().name)
            if (moveIsValid) {

                if (mate == "player") {
                    ChessGame.stockfishGameEnded = true
                }


                // Stockfish response
                else {
                    val squares = ChessGame.convertMoveStringToSquares(response)
                    var movingPiece = ChessGame.pieceAt(squares[0])

                    val promotionCheck = ChessGame.promotion(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )
                    ChessGame.removeEnpassantPawn(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )
                    when (ChessGame.castle(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )) {
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
                                    col = squares[1].col,
                                    row = squares[1].row
                                )
                            )
                        }
                    }

                    if (movingPiece != null) {
                        ChessGame.pieceAt(squares[1].col, squares[1].row)?.let {
                            if (it.player != movingPiece?.player) {
                                ChessGame.piecesBox.remove(it)
                            }
                        }
                    }
                    ChessGame.toString()
                    chessView.invalidate()
                    if (mate == "true" && turno%2==1) {
                        ChessGame.stockfishGameEnded = true
                        risposta="bianco"
                    }else if (mate == "true" && turno%2==0){
                        ChessGame.stockfishGameEnded = true
                        risposta="nero"
                    }
                }

            }
        }
        return risposta
    }

    fun doOptimalSimulation() : String{
        var moveIsValid=false
        var response=""
        var mate= ""
        var turno= 0
        var risposta=""
        val job = GlobalScope.launch(Dispatchers.IO) {
            Log.i("Thread job: ", Thread.currentThread().name)
            run {
                var id_string = ChessGame.matchId.toString()
                val name =
                    "https://JaR.pythonanywhere.com" + "/optimal?" + "index=" + id_string
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "GET"
                        val r = JSONObject(InputStreamReader(inputStream).readText())
                        Log.d("Stockfish response", r.toString())
                        moveIsValid = r.get("valid") as Boolean
                        response = r.get("move") as String
                        mate = r.get("mate") as String
                        turno = r.get("turno") as Int
                    }
                } catch (e: Exception) {
                    Log.e("Move error: ", e.toString())
                }
            }
        }

        runBlocking {
            job.join()
            Log.i("Thread runblocking: ", Thread.currentThread().name)
            if (moveIsValid) {

                if (mate == "player") {
                    ChessGame.stockfishGameEnded = true
                }


                // Stockfish response
                else {
                    val squares = ChessGame.convertMoveStringToSquares(response)
                    var movingPiece = ChessGame.pieceAt(squares[0])

                    val promotionCheck = ChessGame.promotion(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )
                    ChessGame.removeEnpassantPawn(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )
                    when (ChessGame.castle(
                        movingPiece,
                        squares[0].row,
                        squares[0].col,
                        squares[1].row,
                        squares[1].col
                    )) {
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
                                    col = squares[1].col,
                                    row = squares[1].row
                                )
                            )
                        }
                    }

                    if (movingPiece != null) {
                        ChessGame.pieceAt(squares[1].col, squares[1].row)?.let {
                            if (it.player != movingPiece?.player) {
                                ChessGame.piecesBox.remove(it)
                            }
                        }
                    }
                    ChessGame.toString()
                    chessView.invalidate()
                    if (mate == "true" && turno%2==0) {
                        ChessGame.stockfishGameEnded = true
                        risposta="bianco"
                    }else if (mate == "true" && turno%2==1){
                        ChessGame.stockfishGameEnded = true
                        risposta="nero"
                    }
                }

            }
        }
        return risposta
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

    override fun onDestroy() {
        super.onDestroy()
        stopRepeatingTask()
        resumeButton?.visibility = View.VISIBLE
        println("ciaomareesco")
    }




    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}
    override fun showEvalChart() {}

    override fun updateTurn(player: Player, move: String) {
        Log.d("player", player.toString())
        ChessGame.firstMove=false
    }

    private fun alert(title:String, message:String, button:String) {
        val alertDialog = AlertDialog.Builder(this@SimulationStock).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, button) { dialog, _ -> dialog.dismiss() }
        alertDialog.show()
    }

}