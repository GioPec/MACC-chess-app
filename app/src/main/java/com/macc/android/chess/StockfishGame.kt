package com.macc.android.chess

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class StockfishGame : AppCompatActivity(), ChessDelegate {
    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    lateinit var progressBar: ProgressBar

    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private lateinit var button: FloatingActionButton
    private lateinit var lightbulbButton: ImageButton

    private lateinit var evaluationLayout: LinearLayout
    private lateinit var evaluationChart: EvaluationChart

    ///////////////// SHAKE DETECTION //////////////////////////////////////////////////////////////

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
            //Log.i("listener", "acceleration = $acceleration")

            if (!ChessGame.firstMove && acceleration > 4 && !ChessGame.hintAlreadyUsed) {

                ChessGame.hintAlreadyUsed = true

                var bestMove = "THREAD ERROR"
                val job = GlobalScope.launch(Dispatchers.IO) {
                    bestMove = askForAdvice(ChessGame.matchId)
                }
                runBlocking {
                    job.join()
                    Toast.makeText(applicationContext, bestMove, Toast.LENGTH_LONG).show()
                    lightbulbButton.tag = "off"
                    lightbulbButton.setBackgroundResource(R.drawable.light_bulb_off)
                    unregisterListener()
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun unregisterListener() {
        sensorManager!!.unregisterListener(sensorListener)
        //Log.i("Listener", "unregistered")
    }
    fun registerListener() {
        sensorManager?.registerListener(ChessGame.sensorListener, sensorManager!!.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        //Log.i("Listener", "registered")
    }

    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }
    override fun onPause() {
        ChessGame.firstMove=true
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    var resumeButton: Button? = null
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stockfish)

        chessView = findViewById(R.id.chess_view)
        resetButton = findViewById(R.id.reset_button)
        progressBar = findViewById(R.id.progress_bar)
        evaluationLayout = findViewById(R.id.evaluation_layout)
        evaluationChart = findViewById(R.id.evaluation_chart)
        resumeButton = findViewById(R.id.resume_button)
        resumeButton?.visibility = View.VISIBLE

        button = findViewById(R.id.button)
        lightbulbButton = findViewById(R.id.imageButton)
        if (!ChessGame.hintAlreadyUsed) {
            lightbulbButton.tag = "on"
            lightbulbButton.setBackgroundResource(R.drawable.light_bulb_on)
        }
        else {
            lightbulbButton.tag = "off"
            lightbulbButton.setBackgroundResource(R.drawable.light_bulb_off)
        }

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            chessView.invalidate()
            ChessGame.hintAlreadyUsed=false
            ChessGame.reset(ChessGame.matchId)
            ChessGame.matchId=404
            progressBar.progress = progressBar.max / 2
            lightbulbButton.tag = "on"
            lightbulbButton.setBackgroundResource(R.drawable.light_bulb_on)
            evaluationChart.invalidate()
            showEvalChart()
        }

        resetButton.visibility = View.VISIBLE
        button.visibility = View.VISIBLE
        lightbulbButton.visibility = View.VISIBLE
        evaluationLayout.visibility = View.GONE

        ////////////////////////////////////////////////////////////////////////////////////////////

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        ////////////////////////////////////////////////////////////////////////////////////////////

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkPermission()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent!!.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        if (SpeechRecognizer.isRecognitionAvailable(this)) {

            speechRecognizer!!.setRecognitionListener(object : RecognitionListener {

                override fun onResults(bundle: Bundle) { parseMove(bundle) }

                override fun onPartialResults(p0: Bundle?) { Log.i("AUDIO", "onPartialResults!")  }

                override fun onEvent(p0: Int, p1: Bundle?) { Log.i("AUDIO", "onEvent!")  }

                override fun onReadyForSpeech(p0: Bundle?) { Log.i("AUDIO", "onReadyForSpeech!")  }

                override fun onBeginningOfSpeech() { Log.i("AUDIO", "onBeginningOfSpeech!")  }

                override fun onRmsChanged(p0: Float) { Log.i("AUDIO", "audio rmsdb changing...")  }

                override fun onBufferReceived(p0: ByteArray?) { Log.i("AUDIO", "onBufferReceived!")  }

                override fun onEndOfSpeech() { Log.i("AUDIO", "onEndOfSpeech!")  }

                override fun onError(p0: Int) { Log.e("AUDIO", "onError! $p0")  }

            })
        } else {
            Log.e("AUDIO", "Recognition not available!")
        }

        button.setOnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.isPressed = true
                    speechRecognizer!!.startListening(speechRecognizerIntent)
                }
                MotionEvent.ACTION_UP -> {
                    button.isPressed = false
                    speechRecognizer!!.stopListening()
                }
            }
            //return v?.onTouchEvent(event) ?: true
            true
        }
    }

    private fun parseMove(bundle: Bundle) {

        val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        var move = data!![0]
        val moveBeforeParsing = move
        move = move.toLowerCase(Locale.ROOT).filterNot { it.isWhitespace() }
        move = move.replace("uno","1")
        move = move.replace("due","2")
        move = move.replace("tre","3")
        move = move.replace("quattro","4")
        move = move.replace("cinque","5")
        move = move.replace("sei","6")
        move = move.replace("sette","7")
        move = move.replace("otto","8")
        move = move.replace(":0","e")

        for (l in move) {
            if (!arrayOf("a","b","c","d","e","f","g","h","1","2","3","4","5","6","7","8").contains(l.toString())) {
                move = move.replace(l.toString(), "")
            }
        }

        if (move.length != 4
                        || move[0] !in "abcdefgh" || move[1] !in "12345678" || move[2] !in "abcdefgh" || move[3] !in "12345678") {

            alert("Error", "Your move ($moveBeforeParsing parsed into $move) could not be recognized.\nTry speaking more clearly", "OK")
            return
        }

        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val squares = ChessGame.convertMoveStringToSquares(move)
                    //ChessGame.movePiece(squares[0], squares[1])

                    move(squares)

                    chessView.invalidate()
                }
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@StockfishGame)
        builder.setMessage("Are you sure you want to play:\n$move?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show()
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            else Log.e("E", "error1")
        }
        else Log.e("E", "error2")
    }

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    override fun movePiece(from: Square, to: Square) {}

    override fun updateProgressBar(type: String, value: Int) {

        val movesWeight = 5
        if (type=="cp") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                progressBar.setProgress(progressBar.max / 2 - value * movesWeight, true)
            }
            /* Alternative method:
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress",
                progressBar.progress, (progressBar.max/2 - value.toInt()))
                checkNotNull(progressAnimator).setDuration(1000).start()
             */
            Log.d("Progress bar", progressBar.progress.toString())
        }
        else if (type=="mate"){
            if (value<0) progressBar.progress = progressBar.max
            else if (value>0) progressBar.progress = 0
        }
        else {
            Log.e("Evaluation", type + value.toString())
        }
    }

    override fun updateTurn(player: Player, move: String) {}

    private fun askForAdvice(id: Int): String {
        //if (lightbulbButton.tag =="off") return
        var id_string=id.toString()
        val url = URL("https://JaR.pythonanywhere.com"+"/bestmove?index="+id_string)
        val conn = url.openConnection() as HttpsURLConnection
        var bestMove = ""

        try {
            conn.run {
                requestMethod="GET"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                bestMove = r.get("move") as String
            }
        }
        catch (e: Exception) {
            Log.e("Request error: ", e.toString())
            bestMove = "ERROR"
        }
        finally {
            return bestMove
        }
    }

    private fun alert(title:String, message:String, button:String) {
        val alertDialog = AlertDialog.Builder(this@StockfishGame).create()
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, button) { dialog, _ -> dialog.dismiss() }
        alertDialog.show()
    }

    ///////////////////

    private fun convertRowColFromIntToString(move: Int, type: String): String {
        //assert(move>=0 && move<=7)
        var converted = ""
        if (type == "column") {
            when (move) {
                0 -> converted = "a"
                1 -> converted = "b"
                2 -> converted = "c"
                3 -> converted = "d"
                4 -> converted = "e"
                5 -> converted = "f"
                6 -> converted = "g"
                7 -> converted = "h"

            }
        } else if (type == "row"){
            when (move) {
                0 -> converted = "1"
                1 -> converted = "2"
                2 -> converted = "3"
                3 -> converted = "4"
                4 -> converted = "5"
                5 -> converted = "6"
                6 -> converted = "7"
                7 -> converted = "8"
            }
        }
        return converted
    }

    private fun move(squares: Array<Square>) {

        val fromRow = squares[0].row
        val fromCol = squares[0].col
        val row = squares[1].row
        val col = squares[1].col
        val movingPiece = ChessGame.pieceAt(squares[0])

        var moveIsValid = false

        ChessGame.firstMove = false
        resetButton.isEnabled = true

        var response = ""
        var mate = ""
        val usableFromColumn = convertRowColFromIntToString(fromCol, "column")
        val usableFromRow = convertRowColFromIntToString(fromRow, "row")
        val usableToCol = convertRowColFromIntToString(col, "column")
        val usableToRow = convertRowColFromIntToString(row, "row")
        val promotionCheck = ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)

        val job = GlobalScope.launch(Dispatchers.IO) {
            run {
                val name = "https://giacomovenneri.pythonanywhere.com/stockfish/?move=" +
                        "" + usableFromColumn + usableFromRow + usableToCol + usableToRow + promotionCheck
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "POST"
                        val r = JSONObject(InputStreamReader(inputStream).readText())
                        Log.d("Stockfish response", r.toString())
                        moveIsValid = r.get("valid") as Boolean
                        response = r.get("response") as String
                        mate = r.get("mate") as String
                    }
                } catch (e: Exception) {
                    Log.e("Move error: ", e.toString())
                }
            }
        }

        runBlocking {
            job.join()
            if (moveIsValid) {

                // Player move
                ChessGame.removeEnpassantPawn(movingPiece, fromRow, fromCol, row, col)
                val castleCheck = ChessGame.castle(movingPiece, fromRow, fromCol, row, col)
                when (castleCheck) {
                    "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                    "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                    "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                    "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
                }
                ChessGame.piecesBox.remove(movingPiece)
                if (promotionCheck=="") {
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
                        if (it.player != movingPiece.player) {
                            ChessGame.piecesBox.remove(it)
                        }
                    }
                }
                if (mate == "player") ChessGame.stockfishGameEnded = true

                // Stockfish response
                else {
                    val squaresR = ChessGame.convertMoveStringToSquares(response)
                    val movingPieceR = ChessGame.pieceAt(squaresR[0])

                    val promotionCheckR = ChessGame.promotion(movingPieceR, squaresR[0].row, squaresR[0].col, squaresR[1].row, squaresR[1].col)
                    ChessGame.removeEnpassantPawn(movingPieceR, squaresR[0].row, squaresR[0].col, squaresR[1].row, squaresR[1].col)
                    when (ChessGame.castle(movingPieceR, squaresR[0].row, squaresR[0].col, squaresR[1].row, squaresR[1].col)) {
                        "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                        "whitelong" -> ChessGame.movePiece(0, 0, 3, 0)
                        "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                        "blacklong" -> ChessGame.movePiece(0, 7, 3, 7)
                    }
                    ChessGame.piecesBox.remove(movingPieceR)
                    if (promotionCheckR == "") {
                        movingPieceR?.let {
                            ChessGame.addPiece(
                                it.copy(
                                    col = squaresR[1].col,
                                    row = squaresR[1].row
                                )
                            )
                        }
                    }
                    if (movingPieceR != null) {
                        ChessGame.pieceAt(squaresR[1].col, squaresR[1].row)?.let {
                            if (it.player != movingPieceR.player) {
                                ChessGame.piecesBox.remove(it)
                            }
                        }
                    }
                    ChessGame.toString()
                    //invalidate()
                    if (mate == "stockfish") ChessGame.stockfishGameEnded = true
                }

                //playSound()
            }
        }
        alert("Error", "Your move is invalid", "OK")
    }

    override fun showEvalChart() {
        resetButton.visibility = View.INVISIBLE
        button.visibility = View.INVISIBLE
        lightbulbButton.visibility = View.INVISIBLE

        evaluationChart.invalidate()

        evaluationLayout.visibility = View.VISIBLE

        Toast.makeText(applicationContext,"Game ended!",Toast.LENGTH_LONG).show()

        unregisterListener()

        ChessGame.gameInProgress=""
        ChessGame.resettedGame = true
        ChessGame.hintAlreadyUsed = false

        //ChessGame.evaluationsArray.clear()
    }
}