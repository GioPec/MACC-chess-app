package com.goldenthumb.android.chess

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
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
    private var printWriter: PrintWriter? = null
    private var serverSocket: ServerSocket? = null
    private val isEmulator = Build.FINGERPRINT.contains("generic")

    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private lateinit var editText: EditText
    private lateinit var button: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stockfish)

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)

        editText = findViewById<EditText>(R.id.text)
        button = findViewById<ImageView>(R.id.button)

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
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
        assert(move.length==4)
        assert(move[0] in "abcdefgh")
        assert(move[1] in "12345678")
        assert(move[2] in "abcdefgh")
        assert(move[3] in "12345678")

        editText.setText(move)

        var squares = convertMoveStringToSquares(move)
        assert(ChessGame.canMove(squares[0], squares[1]))

        //TODO chiedere conferma della mossa
        //ChessGame.movePiece(squares[0], squares[1])
        //ChessGame.moveNum++
        //chessView.invalidate()
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

    override fun movePiece(from: Square, to: Square) {

        if (!ChessGame.canMove(from, to)) {
            Log.w("warning: ", "mossa errata")
            return
        }

        ChessGame.waitTurn = true

        //TODO? ChessGame.highlightLastMove(from, to)
        //chessView.invalidate()

        ChessGame.movePiece(from, to)
        ChessGame.moveNum++
        chessView.invalidate()

        val moveStr = "${from.col},${from.row},${to.col},${to.row}"
        //Log.i("moveStr", moveStr)
        //Log.i("from, to", "$from, $to")
        Log.i("PGN", ChessGame.pgnBoard())
        Log.i("FEN", ChessGame.boardToFen())

        Log.i("!", "######################################")
        Log.i("!", "############# BLACK TURN #############")
        Log.i("!", "######################################")

        sendMoveToStockfish(ChessGame.boardToFen())

/*        printWriter?.let {
            Executors.newSingleThreadExecutor().execute {
                it.println(moveStr)
            }
        }*/
    }

    fun sendMoveToStockfish(fen: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://chess.apurn.com/nextmove"

        // NB: usa StringRequest (anziché JsonObjectRequest), ma con override di body e headers
        val stringRequest : StringRequest =
                object : StringRequest(Method.POST, url,
                        Response.Listener<String> { response ->
                            Log.i("STOCKFISH SAYS", response.toString())
                            makeStockfishMove(response)    //fa la mossa del nero
                        },
                        Response.ErrorListener { error ->
                            Log.e("STOCKFISH ERROR!", error.toString())
                            Toast.makeText(this, "End of match: Stockfish can't make any sense of this position", Toast.LENGTH_LONG).show()
                            //TODO: catch API errors
                        }
                ) {
                    override fun getBody(): ByteArray? {
                        var fenba = fen.toByteArray(Charsets.UTF_8)
                        //Log.i("fenba", fenba.toString(Charsets.UTF_8))
                        return fenba
                    }
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Content-Type"] = "text/plain"  //essenziale!!!
                        return headers
                    }
                }

        queue.add(stringRequest)
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

    fun makeStockfishMove(move: String) {

        var squares = convertMoveStringToSquares(move)

        if (!ChessGame.canMove(squares[0], squares[1])) {
            Log.e("error: ", "Stockfish sta cercando di fare una mossa errata! :(")
            Toast.makeText(this, "Stockfish sta cercando di fare una mossa errata! :(", Toast.LENGTH_LONG).show()
        }

        ChessGame.movePiece(squares[0], squares[1])
        ChessGame.moveNum++
        chessView.invalidate()

        ChessGame.waitTurn = false

        Log.i("PGN", ChessGame.pgnBoard())
        Log.i("FEN", ChessGame.boardToFen())

        Log.i("!", "######################################")
        Log.i("!", "############# WHITE TURN #############")
        Log.i("!", "######################################")
    }
}