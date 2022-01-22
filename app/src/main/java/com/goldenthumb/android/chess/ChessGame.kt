package com.goldenthumb.android.chess

import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ChessGame {

    var myOnlineColor = "" //WHITE, BLACK
    var isOnlineMate = "false"
    var myUsername = ""
    var waitingForAdversary: Boolean = true
    var adversary: String = ""
    var challengeAlreadyNotified = false
    var stockfishGameEnded: Boolean = false
    var gameInProgress: String = "" //LOCAL, STOCKFISH, ONLINE
    var resettedGame: Boolean = false
    val sensorListener:SensorEventListener? = null
    var firstMove = true

    var piecesBox = mutableSetOf<ChessPiece>()

    val lightColor: Int = Color.parseColor("#F2E6D6") //"#EEEEEE"
    val darkColor: Int = Color.parseColor("#D8B27E")  //"#BBBBBB"

    init {
        reset()
    }

    fun clear() {
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
        if (movingPiece.chessman.equals(Chessman.PAWN)) {
            if (movingPiece.player.equals(Player.WHITE) && fromRow==6 && toRow==7) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qlt60, col = toCol, row = toRow))
                return
            }
            else if (movingPiece.player.equals(Player.BLACK) && fromRow==1 && toRow==0) {
                piecesBox.remove(movingPiece)
                addPiece(movingPiece.copy(chessman=Chessman.QUEEN, resID = R.drawable.chess_qdt60, col = toCol, row = toRow))
                return
            }
        }

        piecesBox.remove(movingPiece)
        addPiece(movingPiece.copy(col = toCol, row = toRow))
    }

    fun reset() {
        resetStockfishGame()
        firstMove=true
        Log.d("!", "############# GAME START #############")
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
    private fun resetStockfishGame() {
        resettedGame = true
        stockfishGameEnded = false
        val job = GlobalScope.launch(Dispatchers.IO) { run {
                val name = "https://giacomovenneri.pythonanywhere.com/reset/"
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "GET"
                        val r = InputStreamReader(inputStream).readText()
                        Log.d("RESET", "")
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
        val firstChar = move.substring(0, 1)
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
            if (movingPiece.player == Player.BLACK && fromCol==4 && fromRow==7 && col==2 && row==7) {
                return "blacklong"
            }
        }
        return ""
    }

    fun removeEnpassantPawn(movingPiece:ChessPiece?, fromRow:Int, fromCol:Int, row:Int, col:Int) {
        if (movingPiece!!.chessman.equals(Chessman.PAWN)) {
            if(fromCol!=col){
                if(ChessGame.pieceAt(col, row)==null){
                    ChessGame.piecesBox.remove(ChessGame.pieceAt(col,fromRow))
                }
            }
        }
    }
}