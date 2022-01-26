package com.goldenthumb.android.chess

import android.Manifest
import android.content.Context
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
import com.google.android.material.snackbar.Snackbar
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
    private lateinit var editText: EditText
    private lateinit var button: ImageView
    private lateinit var lightbulbButton: ImageButton

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

            if (!ChessGame.firstMove && acceleration > 6) {

                var bestMove = "THREAD ERROR"
                val job = GlobalScope.launch(Dispatchers.IO) {
                    bestMove = askForAdvice()
                }
                runBlocking {
                    job.join()
                    Toast.makeText(applicationContext, bestMove,Toast.LENGTH_LONG).show()
                    lightbulbButton.tag = "off"
                    lightbulbButton.setBackgroundResource(R.drawable.hint_off)
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
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stockfish)

        chessView = findViewById(R.id.chess_view)
        resetButton = findViewById(R.id.reset_button)
        progressBar = findViewById(R.id.progress_bar)

        editText = findViewById(R.id.text)
        button = findViewById(R.id.button)
        lightbulbButton = findViewById(R.id.imageButton)
        lightbulbButton.tag = "on"
        lightbulbButton.setBackgroundResource(R.drawable.hint_on)

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
            progressBar.progress = progressBar.max / 2
            chessView.invalidate()
            lightbulbButton.tag = "on"
            lightbulbButton.setBackgroundResource(R.drawable.hint_on)
        }

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

                override fun onResults(bundle: Bundle) {
                    parseMove(bundle)
                }

                override fun onPartialResults(p0: Bundle?) {
                    Log.e("AUDIO", "onPartialResults!")
                }

                override fun onEvent(p0: Int, p1: Bundle?) {
                    Log.e("AUDIO", "onEvent!")
                }

                override fun onReadyForSpeech(p0: Bundle?) {
                    Log.e("AUDIO", "onReadyForSpeech!")
                }

                override fun onBeginningOfSpeech() {
                    Log.e("AUDIO", "onBeginningOfSpeech!")
                    editText.setText("")
                    editText.hint = "Listening..."
                }

                override fun onRmsChanged(p0: Float) {
                    Log.i("AUDIO", "audio rmsdb changing...")
                }

                override fun onBufferReceived(p0: ByteArray?) {
                    Log.e("AUDIO", "onBufferReceived!")
                }

                override fun onEndOfSpeech() {
                    Log.e("AUDIO", "onEndOfSpeech!")
                }

                override fun onError(p0: Int) {
                    Log.e("AUDIO", "onError!$p0")
                }

            })
        } else {
            Log.e("AUDIO", "Recognition not available!")
        }

        button.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    button.setImageResource(R.drawable.ic_mic_black_on)
                    speechRecognizer!!.startListening(speechRecognizerIntent)
                }
                MotionEvent.ACTION_UP -> {
                    speechRecognizer!!.stopListening()
                }
            }
            //return v?.onTouchEvent(event) ?: true
            true
        }
    }

    private fun parseMove(bundle: Bundle) {
        button.setImageResource(R.drawable.ic_mic_black_off)
        val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        var move = data!![0]
        move = move.toLowerCase(Locale.ROOT).filterNot { it.isWhitespace() }
        assert(move.length == 4)
        assert(move[0] in "abcdefgh")
        assert(move[1] in "12345678")
        assert(move[2] in "abcdefgh")
        assert(move[3] in "12345678")

        editText.setText(move)

        //TODO chiedere conferma della mossa
    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.size > 0) {
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
                progressBar.setProgress(progressBar.max/2 - value*movesWeight, true)
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

    override fun moveGreenSquares(from: Square, to: Square) {}

    private fun askForAdvice():String {
        //if (lightbulbButton.tag =="off") return

        val url = URL("https://giacomovenneri.pythonanywhere.com/bestmove")
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
}