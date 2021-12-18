package com.goldenthumb.android.chess

import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.abs

object ChessGame {

    var gameInProgress: String = "" //LOCAL, STOCKFISH, ONLINE
    var resettedGame: Boolean = false

    var piecesBox = mutableSetOf<ChessPiece>()

    var moveNum = 0 //num of moves in game

    var castlingAvailability = "KQkq"   //for fen notation

    var waitTurn = false    //flag to avoid double moves by malicious white players

    val lightColor: Int = Color.parseColor("#F2E6D6") //"#EEEEEE"
    val darkColor: Int = Color.parseColor("#D8B27E")  //"#BBBBBB"

    init {
        reset()
    }

    fun resetStockfishGame() {
        resettedGame = true
        val job = GlobalScope.launch {
            val reset = async {
                var name = "https://giacomovenneri.pythonanywhere.com/reset/"
                val url = URL(name)
                val conn = url.openConnection() as HttpsURLConnection
                try {
                    conn.run {
                        requestMethod = "GET"
                        val r = InputStreamReader(inputStream).readText()
                        Log.d("RESET", "")
                    }
                }
                catch (e: Exception){
                    Log.e("Reset error", e.toString())
                }
            }
            reset.await()
        }
        runBlocking {
            job.join()
        }
    }

    fun clear() {
        piecesBox.clear()
    }

    fun addPiece(piece: ChessPiece) {
        piecesBox.add(piece)
    }

    private fun canKnightMove(from: Square, to: Square): Boolean {
        return abs(from.col - to.col) == 2 && abs(from.row - to.row) == 1 ||
                abs(from.col - to.col) == 1 && abs(from.row - to.row) == 2
    }

    private fun canRookMove(from: Square, to: Square): Boolean {
        if (from.col == to.col && isClearVerticallyBetween(from, to) ||
            from.row == to.row && isClearHorizontallyBetween(from, to)) {
            return true
        }
        return false
    }

    private fun isClearVerticallyBetween(from: Square, to: Square): Boolean {
        if (from.col != to.col) return false
        val gap = abs(from.row - to.row) - 1
        if (gap == 0) return true
        for (i in 1..gap) {
            val nextRow = if (to.row > from.row) from.row + i else from.row - i
            if (pieceAt(Square(from.col, nextRow)) != null) {
                return false
            }
        }
        return true
    }

    private fun isClearHorizontallyBetween(from: Square, to: Square): Boolean {
        if (from.row != to.row) return false
        val gap = abs(from.col - to.col) - 1
        if (gap == 0) return true
        for (i in 1..gap) {
            val nextCol = if (to.col > from.col) from.col + i else from.col - i
            if (pieceAt(Square(nextCol, from.row)) != null) {
                return false
            }
        }
        return true
    }

    private fun isClearDiagonally(from: Square, to: Square): Boolean {
        if (abs(from.col - to.col) != abs(from.row - to.row)) return false
        val gap = abs(from.col - to.col) - 1
        for (i in 1..gap) {
            val nextCol = if (to.col > from.col) from.col + i else from.col - i
            val nextRow = if (to.row > from.row) from.row + i else from.row - i
            if (pieceAt(nextCol, nextRow) != null) {
                return false
            }
        }
        return true
    }

    private fun canBishopMove(from: Square, to: Square): Boolean {
        if (abs(from.col - to.col) == abs(from.row - to.row)) {
            return isClearDiagonally(from, to)
        }
        return false
    }

    private fun canQueenMove(from: Square, to: Square): Boolean {
        return canRookMove(from, to) || canBishopMove(from, to)
    }

    private fun canKingMove(from: Square, to: Square): Boolean {    //TODO: re non va attivamente sotto scacco
        val deltaCol = abs(from.col - to.col)
        val deltaRow = abs(from.row - to.row)

        if (true) { //pieceAt(from)!!.moved==false //TODO: condizioni per arrocco
            //arrocco lungo
            if (from.col==4 && from.row==0 && to.col==2 && to.row==0) { //bianco
                if (pieceAt(1,0)!=null || pieceAt(2,0)!=null || pieceAt(3,0)!=null) return false
                movePiece(0,0,3,0)
                movePiece(from.col,from.row,to.col,to.row)
                updateCastlingAvailability("Q")
                return true
            }
            if (from.col==4 && from.row==7 && to.col==2 && to.row==7) { //nero
                if (pieceAt(1,7)!=null || pieceAt(2,7)!=null || pieceAt(3,7)!=null) return false
                movePiece(0,7,3,7)
                movePiece(from.col,from.row,to.col,to.row)
                updateCastlingAvailability("q")
                return true
            }
            //arrocco corto
            if (from.col==4 && from.row==0 && to.col==6 && to.row==0) { //bianco
                if (pieceAt(5,0)!=null || pieceAt(6,0)!=null) return false
                movePiece(7,0,5,0)
                movePiece(from.col,from.row,to.col,to.row)
                updateCastlingAvailability("K")
                return true
            }
            if (from.col==4 && from.row==7 && to.col==6 && to.row==7) { //nero
                if (pieceAt(5,7)!=null || pieceAt(6,7)!=null) return false
                movePiece(7,7,5,7)
                movePiece(from.col,from.row,to.col,to.row)
                updateCastlingAvailability("k")
                return true
            }
        }
        if (canQueenMove(from, to)) {
            val deltaCol = abs(from.col - to.col)
            val deltaRow = abs(from.row - to.row)
            return deltaCol == 1 && deltaRow == 1 || deltaCol + deltaRow == 1
        }
        return false
    }

    private fun updateCastlingAvailability(castle: String) {    //for FEN notation
        if (castlingAvailability.length==2) {
            castlingAvailability="-"
            return
        }
        if (castle=="Q" || castle=="K") castlingAvailability="kq"
        else if (castle=="q" || castle=="k") castlingAvailability="KQ"
    }

    private fun canPawnMove(from: Square, to: Square): Boolean {
        val thePawn = pieceAt(from.col, from.row)

        if (from.col == to.col) {
            if (from.row == 1) {    //TODO: per FEN notation, inserire qui bersaglio en-passant
                return to.row == 2 || to.row == 3
            } else if (from.row == 6) {
                return to.row == 5 || to.row == 4
            }
            else if (pieceAt(to) == null) {
                if (thePawn!!.player == Player.WHITE) {
                    return (to.row - from.row) == 1
                }
                else if (thePawn!!.player == Player.BLACK) {
                    return (to.row - from.row) == -1
                }
            }
        }
        else if (from.col == to.col+1 || from.col == to.col-1) {
            if (thePawn!!.player == Player.WHITE && (from.row+1 == to.row)
                    || thePawn!!.player == Player.BLACK && (from.row-1 == to.row))
            if (pieceAt(to.col, to.row) != null) {
                return pieceAt(to.col, to.row)!!.player != thePawn!!.player
            }
        }
        //TODO: en-passant? e promozione!
        return false
    }


    fun canMove(from: Square, to: Square): Boolean {    //TODO: mossa forzata sotto scacco
        val movingPiece = pieceAt(from) ?: return false //no moving piece
        if (from.col==to.col && from.row==to.row) return false  //no false move
        if (pieceAt(to)!=null && movingPiece.player==pieceAt(to)!!.player) return false //no friendly fire

        return when(movingPiece.chessman) {
            Chessman.KNIGHT -> canKnightMove(from, to)
            Chessman.ROOK -> canRookMove(from, to)
            Chessman.BISHOP -> canBishopMove(from, to)
            Chessman.QUEEN -> canQueenMove(from, to)
            Chessman.KING -> canKingMove(from, to)
            Chessman.PAWN -> canPawnMove(from, to)
        }
    }

    fun movePiece(from: Square, to: Square) {
        if (canMove(from, to)) {
            movePiece(from.col, from.row, to.col, to.row)
        }
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
        Log.d("!", "############# GAME START #############")
        moveNum=0
        castlingAvailability = "KQkq"
        waitTurn=false
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

    // FEN starting position
    // rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1

    fun boardToFen(): String {
        var res = ""
        var emptySquares = 0
        for (row in 7 downTo 0) {
            for (col in 0 until 8) {
                if (col==0 && row!=7) {
                    if (emptySquares != 0) {
                        res += "$emptySquares"
                        emptySquares = 0
                    }
                    res += "/"
                }
                var piece = fenPieceAt(row, col)
                if (piece == ".") {
                    emptySquares++
                } else {
                    if (emptySquares != 0) {
                        res += "$emptySquares"
                        emptySquares = 0
                    }
                    res += piece
                }
                if (col==7 && row==0 && emptySquares!=0) {  //to fix last number bug
                    res += "$emptySquares"
                    emptySquares = 0
                }
            }
        }
        return res + " b " + castlingAvailability + " - 0 $moveNum" // TODO: complete this last part
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

    private fun fenPieceAt(row: Int, col: Int) : String {
        var res = pieceAt(col, row)?.let {
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
        return res
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
}