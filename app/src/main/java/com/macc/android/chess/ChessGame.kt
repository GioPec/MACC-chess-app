package com.macc.android.chess

import android.graphics.Color
import android.hardware.SensorEventListener
import android.util.Log
import android.widget.Button
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ChessGame {

    var myOnlineColor = "" //WHITE, BLACK
    var isOnlineMate = "false"
    var isLocalMate= "false"
    var link_online= "https://JaR.pythonanywhere.com"
    //var link_online= "https://giacomovenneri.pythonanywhere.com"
    var Chessdata: JSONArray? = null

    var myUsername = ""
    var waitingForAdversary: Boolean = true
    var adversary: String = ""
    var challengeAlreadyNotified = false
    var stockfishGameEnded: Boolean = false
    var localGameEnded: Boolean = false

    var gameInProgress: String = "" //LOCAL, STOCKFISH, ONLINE
    var resettedGame: Boolean = false
    val sensorListener:SensorEventListener? = null
    var firstMove = true
    var matchId: Int = 404
    var hintAlreadyUsed = false
    var startedmatch =0
    var stockmatch=0

    var fromSquareHighlight: Square? = null
    var toSquareHighlight: Square? = null

    var piecesBox = mutableSetOf<ChessPiece>()
    var lettere= arrayOf("a","b","c","d","e","f","g","h")
    var numeri= arrayOf("8","7","6","5","4","3","2","1")

    var myChessPoints = 100
    var chessPointsFloatArray = FloatArray(10000) { 0f } //MAGIC NUMBER
    var chessPointsList = IntArray(10000) { 0 }
    var chessPointsListLength = 0

    ///

    private lateinit var resetButton: Button
    private lateinit var startButton: Button



    var evaluationsArray = mutableListOf<Int>()

    ///

    val lightColor: Int = Color.parseColor("#F2E6D6") //"#EEEEEE"
    val darkColor: Int = Color.parseColor("#D8B27E")  //"#BBBBBB"

    init {
        reset(ChessGame.matchId)
    }

    private fun clear() {
        piecesBox.clear()
    }

    fun addPiece(piece: ChessPiece) {
        piecesBox.add(piece)
    }

    fun movePiece(from: Square, to: Square) {
        movePiece(from.col, from.row, to.col, to.row)
    }
    fun movePiece(fromCol: Int, fromRow: Int, toCol: Int, toRow: Int) {
        if (fromCol == toCol && fromRow == toRow) return
        val movingPiece = pieceAt(fromCol, fromRow) ?: return

        pieceAt(toCol, toRow)?.let {
            if (it.player == movingPiece.player) {
                return
            }
            piecesBox.remove(it)
        }

        //promozione
        if (movingPiece.chessman==Chessman.PAWN) {
            if (movingPiece.player==Player.WHITE && fromRow==6 && toRow==7) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qlt60, col = toCol, row = toRow))
                return
            }
            else if (movingPiece.player==Player.BLACK && fromRow==1 && toRow==0) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qdt60, col = toCol, row = toRow))
                return
            }
        }

        piecesBox.remove(movingPiece)
        addPiece(movingPiece.copy(col = toCol, row = toRow))
    }

    fun reset(id: Int) {
        resetStockfishGame(id)
        firstMove=true
        fromSquareHighlight = null
        toSquareHighlight = null
        Log.d("!", "#############\nRESET\n#############")
        clear()
        for (i in 0 until 2) {
            addPiece(ChessPiece(0 + i * 7, 0, Player.WHITE, Chessman.ROOK, R.drawable.chess_rlt60))
            addPiece(ChessPiece(0 + i * 7, 7, Player.BLACK, Chessman.ROOK, R.drawable.chess_rdt60))

            addPiece(ChessPiece(1 + i * 5, 0, Player.WHITE, Chessman.KNIGHT, R.drawable.chess_nlt60))
            addPiece(ChessPiece(1 + i * 5, 7, Player.BLACK, Chessman.KNIGHT, R.drawable.chess_ndt60))

            addPiece(ChessPiece(2 + i * 3, 0, Player.WHITE, Chessman.BISHOP, R.drawable.chess_blt60))
            addPiece(ChessPiece(2 + i * 3, 7, Player.BLACK, Chessman.BISHOP, R.drawable.chess_bdt60))
        }

        for (i in 0 until 8) {
            addPiece(ChessPiece(i, 1, Player.WHITE, Chessman.PAWN, R.drawable.chess_plt60))
            addPiece(ChessPiece(i, 6, Player.BLACK, Chessman.PAWN, R.drawable.chess_pdt60))
        }

        addPiece(ChessPiece(3, 0, Player.WHITE, Chessman.QUEEN, R.drawable.chess_qlt60))
        addPiece(ChessPiece(3, 7, Player.BLACK, Chessman.QUEEN, R.drawable.chess_qdt60))

        addPiece(ChessPiece(4, 0, Player.WHITE, Chessman.KING, R.drawable.chess_klt60))
        addPiece(ChessPiece(4, 7, Player.BLACK, Chessman.KING, R.drawable.chess_kdt60))
    }

    fun reset_white(){

        firstMove=true
        fromSquareHighlight = null
        toSquareHighlight = null
        //Log.d("!", "############# RESET_Black #############")
        clear()
        Log.d("!", "#############\nRESETWHITE\n#############")
        clear()
        for (i in 0 until 2) {
            addPiece(ChessPiece(0 + i * 7, 0, Player.WHITE, Chessman.ROOK, R.drawable.chess_rlt60))
            addPiece(ChessPiece(0 + i * 7, 7, Player.BLACK, Chessman.ROOK, R.drawable.chess_rdt60))

            addPiece(ChessPiece(1 + i * 5, 0, Player.WHITE, Chessman.KNIGHT, R.drawable.chess_nlt60))
            addPiece(ChessPiece(1 + i * 5, 7, Player.BLACK, Chessman.KNIGHT, R.drawable.chess_ndt60))

            addPiece(ChessPiece(2 + i * 3, 0, Player.WHITE, Chessman.BISHOP, R.drawable.chess_blt60))
            addPiece(ChessPiece(2 + i * 3, 7, Player.BLACK, Chessman.BISHOP, R.drawable.chess_bdt60))
        }

        for (i in 0 until 8) {
            addPiece(ChessPiece(i, 1, Player.WHITE, Chessman.PAWN, R.drawable.chess_plt60))
            addPiece(ChessPiece(i, 6, Player.BLACK, Chessman.PAWN, R.drawable.chess_pdt60))
        }

        addPiece(ChessPiece(3, 0, Player.WHITE, Chessman.QUEEN, R.drawable.chess_qlt60))
        addPiece(ChessPiece(3, 7, Player.BLACK, Chessman.QUEEN, R.drawable.chess_qdt60))

        addPiece(ChessPiece(4, 0, Player.WHITE, Chessman.KING, R.drawable.chess_klt60))
        addPiece(ChessPiece(4, 7, Player.BLACK, Chessman.KING, R.drawable.chess_kdt60))



    }

    fun reset_black(id: Int) {
        //resetStockfishGame(id)
        firstMove=true
        fromSquareHighlight = null
        toSquareHighlight = null
        //Log.d("!", "############# RESET_Black #############")
        clear()
        for (i in 0 until 2) {
            addPiece(ChessPiece(0 + i * 7, 0, Player.BLACK, Chessman.ROOK, R.drawable.chess_rdt60))
            addPiece(ChessPiece(0 + i * 7, 7, Player.WHITE, Chessman.ROOK, R.drawable.chess_rlt60))

            addPiece(ChessPiece(1 + i * 5, 0, Player.BLACK, Chessman.KNIGHT, R.drawable.chess_ndt60))
            addPiece(ChessPiece(1 + i * 5, 7, Player.WHITE, Chessman.KNIGHT, R.drawable.chess_nlt60))

            addPiece(ChessPiece(2 + i * 3, 0, Player.BLACK, Chessman.BISHOP, R.drawable.chess_bdt60))
            addPiece(ChessPiece(2 + i * 3, 7, Player.WHITE, Chessman.BISHOP, R.drawable.chess_blt60))
        }

        for (i in 0 until 8) {
            addPiece(ChessPiece(i, 1, Player.BLACK, Chessman.PAWN, R.drawable.chess_pdt60))
            addPiece(ChessPiece(i, 6, Player.WHITE, Chessman.PAWN, R.drawable.chess_plt60))
        }

        addPiece(ChessPiece(4, 0, Player.BLACK, Chessman.QUEEN, R.drawable.chess_qdt60))
        addPiece(ChessPiece(4, 7, Player.WHITE, Chessman.QUEEN, R.drawable.chess_qlt60))

        addPiece(ChessPiece(3, 0, Player.BLACK, Chessman.KING, R.drawable.chess_kdt60))
        addPiece(ChessPiece(3, 7, Player.WHITE, Chessman.KING, R.drawable.chess_klt60))
    }

    private fun resetStockfishGame(id: Int) {
        resettedGame = true
        stockfishGameEnded = false
        //println("resetto"+id)

        val job = GlobalScope.launch(Dispatchers.IO) { run {
                val id_string=id
                val name = "https://JaR.pythonanywhere.com/reset?index="+id_string
                //println("andiamo"+ChessGame.matchId.toString())
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "GET"
                        val r = JSONObject(InputStreamReader(inputStream).readText())
                        val reset_need=r.get("errore") as Boolean
                        val reset_id =r.get("reset_id") as Int
                        Log.d("RESET", reset_id.toString()+" "+reset_need.toString())
                    }
                } catch (e: Exception) {
                    Log.e("Reset error", e.toString())
                }
            }
        }
        runBlocking {
            job.join()
        }
    }



    fun startMatchId() : Int {
        resettedGame = true
        stockfishGameEnded = false
        localGameEnded = false
        //println("iniziamoilmatch")
        var id_match=404
        val job = GlobalScope.launch(Dispatchers.IO) { run {
            val name = "https://JaR.pythonanywhere.com"+"/startMatch"
            val url = URL(name)
            val conn = url.openConnection() as HttpsURLConnection
            try {
                conn.run {
                    requestMethod = "GET"
                    //id_match = InputStreamReader(inputStream).readText()
                    val r = JSONObject(InputStreamReader(inputStream).readText())
                    //Log.d("info", r.toString())
                    id_match = r.get("response") as Int
                    Log.d("matchid", id_match.toString())
                }
            } catch (e: Exception) {
                Log.e("Error on match required", e.toString())
            }
        }
        }
        runBlocking {
            job.join()

        }
        return id_match
    }



    fun pieceAt(square: Square): ChessPiece? {
        return pieceAt(square.col, square.row)
    }

    fun pieceAt(col: Int, row: Int): ChessPiece? {
        for (piece in piecesBox) {
            if (col == piece.col && row == piece.row) {
                return piece
            }
        }
        return null
    }

    fun pgnBoard(): String {
        var desc = " \n"
        desc += "  a b c d e f g h\n"
        for (row in 7 downTo 0) {
            desc += "${row + 1}"
            desc += boardRow(row)
            desc += " ${row + 1}"
            desc += "\n"
        }
        desc += "  a b c d e f g h"

        return desc
    }

    override fun toString(): String {
        var desc = " \n"
        for (row in 7 downTo 0) {
            desc += "$row"
            desc += boardRow(row)
            desc += "\n"
        }
        desc += "  0 1 2 3 4 5 6 7"

        return desc
    }

    private fun boardRow(row: Int) : String {
        var desc = ""
        for (col in 0 until 8) {
            desc += " "
            desc += pieceAt(col, row)?.let {
                val white = it.player == Player.WHITE
                when (it.chessman) {
                    Chessman.KING -> if (white) "K" else "k"
                    Chessman.QUEEN -> if (white) "Q" else "q"
                    Chessman.BISHOP -> if (white) "B" else "b"
                    Chessman.ROOK -> if (white) "R" else "r"
                    Chessman.KNIGHT -> if (white) "N" else "n"
                    Chessman.PAWN -> if (white) "P" else "p"
                }
            } ?: "."
        }
        return desc
    }

    fun convertMoveStringToSquares(move: String): Array<Square> {

        assert(move.length >= 4)  //è 5 in caso di promozione! (es: e2f1q)
        var fromCol = 0
        when (move.substring(0, 1)) {
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
        val thirdChar = move.substring(2, 3)
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

    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun promotion(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int):String {
        if (movingPiece!!.chessman == Chessman.PAWN) {
            if (movingPiece.player == Player.WHITE && fromRow==6 && row==7) {
                ChessGame.piecesBox.remove(movingPiece)

                ChessGame.addPiece(
                    movingPiece.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qlt60,
                        col = col,
                        row = row
                    )
                )
                return "Q"
            }
            else if (movingPiece.player == Player.BLACK && fromRow==1 && row==0) {
                ChessGame.piecesBox.remove(movingPiece)

                ChessGame.addPiece(
                    movingPiece.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qdt60,
                        col = col,
                        row = row
                    )
                )
                return "q"
            }
        }
        return ""
    }

    fun onlinePromotion(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int):String {
        if (myOnlineColor == "BLACK") {
            if (movingPiece!!.chessman==Chessman.PAWN) {
                if (movingPiece.player==Player.WHITE && fromRow==6 && row==7) {
                    piecesBox.remove(movingPiece)

                    addPiece(
                        movingPiece.copy(
                            chessman = Chessman.QUEEN,
                            resID = R.drawable.chess_qlt60,
                            col = ICBO(col),
                            row = ICBO(row)
                        )
                    )
                    return "Q"

                } else if (movingPiece.player==Player.BLACK && fromRow==1 && row==0) {
                    piecesBox.remove(movingPiece)

                    addPiece(
                        movingPiece.copy(
                            chessman = Chessman.QUEEN,
                            resID = R.drawable.chess_qdt60,
                            col = ICBO(col),
                            row = ICBO(row)
                        )
                    )
                    return "q"
                }
            }

        } else {
            if (movingPiece!!.chessman==Chessman.PAWN) {
                if (movingPiece.player==Player.WHITE && fromRow == 6 && row == 7) {
                    piecesBox.remove(movingPiece)

                    ChessGame.addPiece(
                        movingPiece.copy(
                            chessman = Chessman.QUEEN,
                            resID = R.drawable.chess_qlt60,
                            col = col,
                            row = row
                        )
                    )
                    return "Q"

                } else if (movingPiece.player==Player.BLACK && fromRow==1 && row==0) {
                    ChessGame.piecesBox.remove(movingPiece)

                    ChessGame.addPiece(
                        movingPiece.copy(
                            chessman = Chessman.QUEEN,
                            resID = R.drawable.chess_qdt60,
                            col = col,
                            row = row
                        )
                    )
                    return "q"
                }
            }
        }
        return ""
    }

    fun castle(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int):String {

        if (movingPiece!!.chessman == Chessman.KING) {
            if (movingPiece.player == Player.WHITE && fromCol==4 && fromRow==0 && col==6 && row==0) {
                return "whiteshort"
            }
            if (movingPiece.player == Player.WHITE && fromCol==4 && fromRow==0 && col==2 && row==0) {
                return "whitelong"
            }
            if (movingPiece.player == Player.BLACK && fromCol==4 && fromRow==7 && col==6 && row==7) {
                return "blackshort"
            }
            if (movingPiece.player == Player.BLACK && (fromCol)==4 && (fromRow)==7 && (col)==2 && (row)==7) {
                return "blacklong"
            }
        }
        return ""
    }

    private fun ICBO(value: Int) : Int {

        var converted = 9

        when (value) {
            0 -> converted = 7
            1 -> converted = 6
            2 -> converted = 5
            3 -> converted = 4
            4 -> converted = 3
            5 -> converted = 2
            6 -> converted = 1
            7 -> converted = 0
        }
        return converted
    }

    fun removeEnpassantPawn(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int) {
        if (movingPiece!!.chessman==Chessman.PAWN) {
            if(fromCol!=col){
                if(pieceAt(col, row)==null){
                    piecesBox.remove(pieceAt(col,fromRow))
                }
            }
        }
    }
}