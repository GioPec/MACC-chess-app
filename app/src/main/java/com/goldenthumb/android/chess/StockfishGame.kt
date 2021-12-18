package com.goldenthumb.android.chess

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import java.io.PrintWriter
import java.net.ServerSocket
import java.util.*

class StockfishGame : AppCompatActivity(), ChessDelegate {
    private val socketHost = "127.0.0.1"
    private val socketPort: Int = 50000
    private val socketGuestPort: Int = 50001 // used for socket server on emulator
    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var listenButton: Button
    private lateinit var connectButton: Button
    lateinit var progressBar: ProgressBar
    private var printWriter: PrintWriter? = null
    private var serverSocket: ServerSocket? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private lateinit var editText: EditText
    private lateinit var button: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stockfish)
        //resetStockfish()

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)
        progressBar = findViewById<ProgressBar>(R.id.progress_bar)

        editText = findViewById<EditText>(R.id.text)
        button = findViewById<ImageView>(R.id.button)

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
            progressBar.setProgress(progressBar.max / 2)
            chessView.invalidate()
            serverSocket?.close()
            listenButton.isEnabled = true
        }

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
                    editText.setText("");
                    editText.setHint("Listening...");
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
                    Log.e("AUDIO", "onError!" + p0.toString())
                }

            })
        } else {
            Log.e("AUDIO", "Recognition not available!")
        }

        button.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
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
                return true
            }
        })

        /*
        listenButton.setOnClickListener {
            listenButton.isEnabled = false
            val port = if (isEmulator) socketGuestPort else socketPort
            Toast.makeText(this, "listening on $port", Toast.LENGTH_SHORT).show()
            Executors.newSingleThreadExecutor().execute {
                ServerSocket(port).let { srvSkt ->
                    serverSocket = srvSkt
                    try {
                        val socket = srvSkt.accept()
                        receiveMove(socket)
                    } catch (e: SocketException) {
                        // ignored, socket closed
                    }
                }
            }
        }
        connectButton.setOnClickListener {
            Log.d("LOG", "socket client connecting ...")
            Executors.newSingleThreadExecutor().execute {
                try {
                    val socket = Socket(socketHost, socketPort)
                    receiveMove(socket)
                } catch (e: ConnectException) {
                    runOnUiThread {
                        Toast.makeText(this, "connection failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        */
    }

    private fun parseMove(bundle: Bundle) {
        button.setImageResource(R.drawable.ic_mic_black_off)
        var data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        var move = data!![0]
        move = move.toLowerCase().filterNot { it.isWhitespace() }
        assert(move.length == 4)
        assert(move[0] in "abcdefgh")
        assert(move[1] in "12345678")
        assert(move[2] in "abcdefgh")
        assert(move[3] in "12345678")

        editText.setText(move)

        var squares = ChessGame.convertMoveStringToSquares(move)

        //TODO chiedere conferma della mossa
    }

    /*
    private fun receiveMove(socket: Socket) {
        val scanner = Scanner(socket.getInputStream())
        printWriter = PrintWriter(socket.getOutputStream(), true)
        while (scanner.hasNextLine()) {
            val move = scanner.nextLine().split(",").map { it.toInt() }
            runOnUiThread {
                ChessGame.movePiece(Square(move[0], move[1]), Square(move[2], move[3]))
                chessView.invalidate()
            }
        }
    }
    */

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

    override fun updateProgressBar(type: String, value: Integer) {
        val movesWeight = 5
        if (type=="cp") {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                progressBar.setProgress(progressBar.max/2 - value.toInt()*movesWeight, true)
            }
            /* Alternative method:
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress",
                progressBar.progress, (progressBar.max/2 - value.toInt()))
                checkNotNull(progressAnimator).setDuration(1000).start()
             */
            Log.d("Progress bar", progressBar.progress.toString())
        }
        else if (type=="mate"){
            if (value<0) progressBar.setProgress(progressBar.max)
            else if (value>0) progressBar.setProgress(0)
            //TODO: partita finita
        }
        else {
            Log.e("Evaluation", type + value.toString())
        }
    }

    override fun updateTurn(player: Player) {}
}