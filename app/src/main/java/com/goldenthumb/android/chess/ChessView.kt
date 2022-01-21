package com.goldenthumb.android.chess

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.min

class ChessView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val scaleFactor = 1.0f
    private var originX = 20f
    private var originY = 200f
    private var cellSide = 130f
    private val imgResIDs = setOf(
            R.drawable.chess_bdt60,
            R.drawable.chess_blt60,
            R.drawable.chess_kdt60,
            R.drawable.chess_klt60,
            R.drawable.chess_qdt60,
            R.drawable.chess_qlt60,
            R.drawable.chess_rdt60,
            R.drawable.chess_rlt60,
            R.drawable.chess_ndt60,
            R.drawable.chess_nlt60,
            R.drawable.chess_pdt60,
            R.drawable.chess_plt60,
    )
    private val bitmaps = mutableMapOf<Int, Bitmap>()
    private val paint = Paint()

    private var movingPieceBitmap: Bitmap? = null
    private var movingPiece: ChessPiece? = null
    private var fromCol: Int = -1
    private var fromRow: Int = -1
    private var movingPieceX = -1f
    private var movingPieceY = -1f

    var chessDelegate: ChessDelegate? = null

    init {
        loadBitmaps()
    }

    /////////// CHESS RULES FUNCTIONS //////////////////////////////////////////////////////////////

    private fun convertRowColFromIntToString(move: Int, type:String): String {
        //assert(move>=0 && move<=7)
        var converted = ""
        if (type.equals("column")) {
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
        } else if (type.equals("row")){
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

    private fun checkMoveValidity(fromCol:Int, fromRow:Int, toCol:Int, toRow:Int, prom:String=""):Boolean? {

        val usableFromColumn = convertRowColFromIntToString(fromCol,"column")
        val usableFromRow = convertRowColFromIntToString(fromRow,"row")
        val usableToCol = convertRowColFromIntToString(toCol,"column")
        val usableToRow = convertRowColFromIntToString(toRow,"row")

        val name="https://giacomovenneri.pythonanywhere.com/?move=" +
                "" + usableFromColumn + usableFromRow + usableToCol + usableToRow + prom

        val url = URL(name)
        val conn = url.openConnection() as HttpsURLConnection
        var checkValidity: Boolean

        try {
            conn.run {
                requestMethod="POST"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                //Log.d("info", r.toString())
                checkValidity = r.get("valid") as Boolean
                Log.d("Move validity", checkValidity.toString())
                return checkValidity
            }
        }
        catch (e: Exception){
            Log.e("Move error: ", e.toString())
        }
        return null
    }

    private fun getEvaluation():Pair<String, Int>? {

        val name = "https://giacomovenneri.pythonanywhere.com/info"
        val url = URL(name)
        val conn = url.openConnection() as HttpsURLConnection
        var pair: Pair<String, Int>?

        try {
            conn.run {
                requestMethod="GET"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                val t = r.get("type") as String
                val v = r.get("value") as Int
                pair = Pair(t,v)
                Log.d("Evaluation", pair.toString())
                return pair
            }
        }
        catch (e: Exception){
            Log.e("Info error", e.toString())
        }
        return null
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val smaller = min(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(smaller, smaller)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        val chessBoardSide = min(width, height) * scaleFactor
        cellSide = chessBoardSide / 8f
        originX = (width - chessBoardSide) / 2f
        originY = (height - chessBoardSide) / 2f

        drawChessboard(canvas)
        drawPieces(canvas)
    }

    private fun drawPieces(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                chessDelegate?.pieceAt(Square(col, row))?.let { piece ->
                    if (piece != movingPiece) {
                        drawPieceAt(canvas, col, row, piece.resID)
                    }
                }

        movingPieceBitmap?.let {
            canvas.drawBitmap(it, null, RectF(movingPieceX - cellSide/2, movingPieceY - cellSide/2,movingPieceX + cellSide/2,movingPieceY + cellSide/2), paint)
        }
    }

    private fun drawPieceAt(canvas: Canvas, col: Int, row: Int, resID: Int) =
        canvas.drawBitmap(bitmaps[resID]!!, null, RectF(originX + col * cellSide,originY + (7 - row) * cellSide,originX + (col + 1) * cellSide,originY + ((7 - row) + 1) * cellSide), paint)

    private fun loadBitmaps() =
        imgResIDs.forEach { imgResID ->
            bitmaps[imgResID] = BitmapFactory.decodeResource(resources, imgResID)
        }

    private fun drawChessboard(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                drawSquareAt(canvas, col, row, (col + row) % 2 == 1)
    }

    private fun drawSquareAt(canvas: Canvas, col: Int, row: Int, isDark: Boolean) {
        paint.color = if (isDark) ChessGame.darkColor else ChessGame.lightColor
        canvas.drawRect(originX + col * cellSide, originY + row * cellSide, originX + (col + 1)* cellSide, originY + (row + 1) * cellSide, paint)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        event ?: return false

        if (ChessGame.stockfishGameEnded) return false
        if (ChessGame.gameInProgress=="ONLINE" && ChessGame.waitingForAdversary) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                fromCol = ((event.x - originX) / cellSide).toInt()
                fromRow = 7 - ((event.y - originY) / cellSide).toInt()

                chessDelegate?.pieceAt(Square(fromCol, fromRow))?.let {
                    movingPiece = it
                    movingPieceBitmap = bitmaps[it.resID]
                }
            }
            MotionEvent.ACTION_MOVE -> {
                var myOnlineColorNum = Player.BLACK
                if (ChessGame.myOnlineColor == "WHITE") myOnlineColorNum = Player.WHITE
                if (
                        ChessGame.gameInProgress == "LOCAL" ||
                        (ChessGame.gameInProgress == "STOCKFISH" && movingPiece?.player?.equals(Player.WHITE) == true) ||
                        (ChessGame.gameInProgress == "ONLINE" && movingPiece?.player?.equals(myOnlineColorNum) == true)
                ) {
                    //Log.i("I", "onTouchEvent: ${ChessGame.gameInProgress == "ONLINE"}")
                    //Log.i("I", "onTouchEvent: ${movingPiece?.player?.equals(myOnlineColorNum) == true}")
                    movingPieceX = event.x
                    movingPieceY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val col = ((event.x - originX) / cellSide).toInt()
                val row = 7 - ((event.y - originY) / cellSide).toInt()
                var moveIsValid = false
                if (movingPiece!=null && (fromCol != col || fromRow != row)) {

                    if (ChessGame.gameInProgress == "LOCAL" || ChessGame.gameInProgress == "ONLINE") {

                        val promotionCheck = ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)

                        val job = GlobalScope.launch {
                            val c1 = async { checkMoveValidity(fromCol, fromRow, col, row, promotionCheck) }
                            moveIsValid = c1.await()==true
                        }

                        runBlocking {
                            job.join()

                            if (moveIsValid) {

                                ChessGame.removeEnpassantPawn(movingPiece, fromRow, fromCol, row, col)

                                val castleCheck = ChessGame.castle(movingPiece, fromRow, fromCol, row, col)
                                when (castleCheck) {
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
                                                        col = col,
                                                        row = row
                                                )
                                        )
                                    }
                                }
                                if (movingPiece != null) {
                                    ChessGame.pieceAt(col, row)?.let {
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }
                                if (ChessGame.gameInProgress == "ONLINE") {
                                    val usableFromColumn = convertRowColFromIntToString(fromCol, "column")
                                    val usableFromRow = convertRowColFromIntToString(fromRow, "row")
                                    val usableToCol = convertRowColFromIntToString(col, "column")
                                    val usableToRow = convertRowColFromIntToString(row, "row")
                                    val move = usableFromColumn + usableFromRow + usableToCol + usableToRow + promotionCheck
                                    chessDelegate?.updateTurn(movingPiece!!.player, move)
                                }
                            }
                        }
                    }

                    else if (ChessGame.gameInProgress == "STOCKFISH") {

                        ChessGame.firstMove=false
                        var response = ""
                        var mate = ""
                        val usableFromColumn = convertRowColFromIntToString(fromCol, "column")
                        val usableFromRow = convertRowColFromIntToString(fromRow, "row")
                        val usableToCol = convertRowColFromIntToString(col, "column")
                        val usableToRow = convertRowColFromIntToString(row, "row")
                        val promotionCheck = ChessGame.promotion(movingPiece, fromRow, fromCol, row, col)

                        val job = GlobalScope.launch(Dispatchers.IO) { run {
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
                                if (promotionCheck.equals("")) {
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
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }
                                if (mate=="player") ChessGame.stockfishGameEnded = true

                                // Stockfish response
                                else {
                                    val squares = ChessGame.convertMoveStringToSquares(response)
                                    movingPiece = ChessGame.pieceAt(squares[0])

                                    val promotionCheck = ChessGame.promotion(movingPiece,squares[0].row,squares[0].col,squares[1].row,squares[1].col)
                                    ChessGame.removeEnpassantPawn(movingPiece,squares[0].row,squares[0].col,squares[1].row,squares[1].col)
                                    when (ChessGame.castle(movingPiece,squares[0].row,squares[0].col,squares[1].row,squares[1].col)) {
                                        "whiteshort" -> ChessGame.movePiece(7, 0, 5, 0)
                                        "whitelong" -> ChessGame.movePiece(0,0,3,0)
                                        "blackshort" -> ChessGame.movePiece(7, 7, 5, 7)
                                        "blacklong" -> ChessGame.movePiece(0,7,3,7)
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
                                    invalidate()
                                    if (mate=="stockfish") ChessGame.stockfishGameEnded = true
                                }
                            }
                        }
                    }

                }
                movingPiece = null
                movingPieceBitmap = null
                invalidate()
                ChessGame.resettedGame = false

                // Get position evaluation
                if (ChessGame.gameInProgress == "STOCKFISH" && moveIsValid) {
                    val job = GlobalScope.launch {
                        withContext(Dispatchers.Default) {
                            val (evaluationType, evaluationValue) = checkNotNull(getEvaluation())
                            chessDelegate?.updateProgressBar(evaluationType, evaluationValue)
                        }
                    }
                    runBlocking {
                        job.join()
                    }
                }
            }
        }
        return true
    }
}
