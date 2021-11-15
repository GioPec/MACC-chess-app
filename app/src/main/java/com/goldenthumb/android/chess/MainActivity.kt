package com.goldenthumb.android.chess

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import java.io.PrintWriter
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.Executors

import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.nio.charset.Charset

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), ChessDelegate {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)
        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
            chessView.invalidate()
            serverSocket?.close()
            listenButton.isEnabled = true
        }

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
            Log.d(TAG, "socket client connecting ...")
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
    }

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

    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    override fun movePiece(from: Square, to: Square) {
        if (!ChessGame.canMove(from, to)) {
            Log.w("warning: ", "mossa errata")
            return
        }
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

        //var stockfishHelloWorld = getHelloWorldFromStockfishAPI()
        //Log.i("RETURN VALUE STOCKFISH:", stockfishHelloWorld)

        sendMoveToStockfish(ChessGame.boardToFen())

        //var stockfishAnswer = sendMoveToStockfish(ChessGame.boardToFen())
        //Log.i("Stockfish says", stockfishAnswer)

        //TODO: catch API errors

        //makeStockfishMove("e7e5")
        //Thread.sleep(2_000)
        //makeStockfishMove(stockfishAnswer)

        printWriter?.let {
            Executors.newSingleThreadExecutor().execute {
                it.println(moveStr)
            }
        }
    }

    fun getHelloWorldFromStockfishAPI() {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)
        val url = "https://chess.apurn.com"

        // Request a string response from the provided URL.
        val stringRequest = StringRequest(Request.Method.GET, url,
                Response.Listener<String> { response ->
                    Log.i("STOCKFISH SAYS", response.toString())
                },
                Response.ErrorListener { error ->
                    Log.e("STOCKFISH ERROR!!!", error.toString())
                }
        )

        queue.add(stringRequest)
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
                    Log.e("STOCKFISH ERROR!!!", error.toString())
                }
        ) {
            override fun getBody(): ByteArray? {
                var fenba = fen.toByteArray(Charsets.UTF_8)
                //Log.i("BBBBBBBAAAAAAA", fenba.toString(Charsets.UTF_8))
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

    fun makeStockfishMove(move: String) {
        //Log.i("move: ", move)
        assert(move.length==4)
        var fromCol = 0
        var firstChar = move.substring(0,1)
        when (firstChar) {
            "a" -> fromCol=0
            "b" -> fromCol=1
            "c" -> fromCol=2
            "d" -> fromCol=3
            "e" -> fromCol=4
            "f" -> fromCol=5
            "g" -> fromCol=6
            "h" -> fromCol=7
        }
        val fromRow = (move.substring(1,2).toInt()-1)

        var toCol = 0
        var thirdChar = move.substring(2,3)
        when (thirdChar) {
            "a" -> toCol=0
            "b" -> toCol=1
            "c" -> toCol=2
            "d" -> toCol=3
            "e" -> toCol=4
            "f" -> toCol=5
            "g" -> toCol=6
            "h" -> toCol=7
        }
        val toRow = (move.substring(3,4).toInt()-1)

        val fromSquare = Square(fromCol, fromRow)
        val toSquare = Square(toCol, toRow)

        if (!ChessGame.canMove(fromSquare, toSquare)) {
            Log.e("error: ", "Stockfish sta cercando di fare una mossa errata! :(")
        }

        ChessGame.movePiece(fromSquare, toSquare)
        ChessGame.moveNum++
        chessView.invalidate()
        Log.i("PGN", ChessGame.pgnBoard())
        Log.i("FEN", ChessGame.boardToFen())

        Log.i("!", "######################################")
        Log.i("!", "############# WHITE TURN #############")
        Log.i("!", "######################################")
    }
}