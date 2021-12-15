package com.goldenthumb.android.chess

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.JsonReader
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import kotlin.math.min
import com.android.volley.toolbox.RequestFuture
import kotlinx.coroutines.*
import org.json.JSONArray
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main


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

    fun IntegerPosition_toString(move: Int, tipo:String): String{

        //assert(move>=0 && move<=7)
        var Converted = "To_Convert"
        if(tipo.equals("column")) {
            when (move) {
                0 -> Converted = "a"
                1 -> Converted = "b"
                2 -> Converted = "c"
                3 -> Converted = "d"
                4 -> Converted = "e"
                5 -> Converted = "f"
                6 -> Converted = "g"
                7 -> Converted = "h"

            }
        }else if (tipo.equals("row")){
            when (move) {
                0 -> Converted = "1"
                1 -> Converted = "2"
                2 -> Converted = "3"
                3 -> Converted = "4"
                4 -> Converted = "5"
                5 -> Converted = "6"
                6 -> Converted = "7"
                7 -> Converted = "8"
            }
        }

        return Converted
    }

    fun ask_move(from_column: Int, from_row: Int, to_column:Int, to_row: Int, prom: String = ""):Boolean? {

        var usable_from_colum= IntegerPosition_toString(from_column,"column");
        var usable_from_row= IntegerPosition_toString(from_row,"row");
        var usable_to_colum= IntegerPosition_toString(to_column,"column");
        var usable_to_row= IntegerPosition_toString(to_row,"row");

        var name="https://giacomovenneri.pythonanywhere.com/?move=" +
                ""+usable_from_colum+usable_from_row+usable_to_colum+usable_to_row+prom

        val url = URL(name)
        val conn = url.openConnection() as HttpsURLConnection
        var chek_validity=false;

        try {
            conn.run {
                requestMethod="POST"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                var res = ""
                Log.i("info",r.toString())
                chek_validity = r.get("valid") as Boolean

                //chek_validity = res.equals("true")

                //Log.i("ask_move", res)
                Log.i("equals", chek_validity.toString())

                return chek_validity
            }
        }
        catch (e: Exception){
            Log.i("Error ask move",""+e.toString())
        }
        return null
    }

    fun promozione(movingPiece:ChessPiece? ,fromRow:Int ,fromCol:Int ,row: Int, col:Int) :String{
        if (movingPiece!!.chessman.equals(Chessman.PAWN)) {
            if (movingPiece!!.player.equals(Player.WHITE) && fromRow==6 && row==7) {
                ChessGame.piecesBox.remove(movingPiece)

                ChessGame.addPiece(
                    movingPiece!!.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qlt60,
                        col = col,
                        row = row,
                        moved = true
                    )
                )
                return "Q"
                invalidate()

            }
            else if (movingPiece!!.player.equals(Player.BLACK) && fromRow==1 && row==0) {
                ChessGame.piecesBox.remove(movingPiece)

                ChessGame.addPiece(
                    movingPiece!!.copy(
                        chessman = Chessman.QUEEN,
                        resID = R.drawable.chess_qdt60,
                        col = col,
                        row = row,
                        moved = true
                    )
                )
                return "q"
                invalidate()
            }
        }
        return ""
    }

    fun arrocco(movingPiece:ChessPiece? ,fromRow:Int ,fromCol:Int ,row: Int, col:Int) :Boolean{
        if (movingPiece!!.chessman.equals(Chessman.KING)) {
            if (movingPiece!!.player.equals(Player.WHITE) && fromCol==4 && fromRow==0 && col==6 && row==0) {
                return true
            }
            if (movingPiece!!.player.equals(Player.WHITE) && fromCol==4 && fromRow==0 && col==2 && row==0) {
                return true
            }
            if (movingPiece!!.player.equals(Player.BLACK) && fromCol==4 && fromRow==7 && col==6 && row==7) {
                return true
            }
            if (movingPiece!!.player.equals(Player.BLACK) && fromCol==4 && fromRow==7 && col==2 && row==7) {
                return true
            }
        }
        return false
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {

        event ?: return false

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
                movingPieceX = event.x
                movingPieceY = event.y
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                val col = ((event.x - originX) / cellSide).toInt()
                val row = 7 - ((event.y - originY) / cellSide).toInt()
                if (fromCol != col || fromRow != row) {
                    if (!ChessGame.waitTurn) {
                        //chessDelegate?.movePiece(Square(fromCol, fromRow), Square(col, row))

                        var promozione=promozione(movingPiece,fromRow,fromCol,row,col)
                        Log.e("removing piece", movingPiece.toString())


                        var check: Boolean? = null
                        val job =GlobalScope.launch {
                            val c1 = async { ask_move(fromCol, fromRow, col, row, promozione) }
                            check = c1.await()
                            Log.e("check", check.toString());
                        }

                        runBlocking {
                            job.join()
                            Log.e("check2", check.toString());
                            var arrocco_check=arrocco(movingPiece,fromRow,fromCol,row,col)
                            if(check== true && arrocco_check==false){

                                Log.e("removing piece _parte2", movingPiece.toString())
                                ChessGame.piecesBox.remove(movingPiece)
                                if(promozione.equals("")) {
                                    movingPiece?.let {
                                        ChessGame.addPiece(
                                            it.copy(
                                                col = col,
                                                row = row,
                                                moved = true
                                            )
                                        )
                                    }
                                }


                                if(movingPiece!=null) {
                                    ChessGame.pieceAt(col, row)?.let {
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }
                            }
                            if(check== true && arrocco_check==true){

                                var arrocco_Piece: ChessPiece? = null
                                ChessGame.piecesBox.remove(movingPiece)
                                ChessGame.piecesBox.remove(arrocco_Piece!!.copy(col = 7, row = 0, moved = true))


                                if(promozione.equals("")) {
                                    movingPiece?.let {
                                        ChessGame.addPiece(
                                            it.copy(
                                                col = col,
                                                row = row,
                                                moved = true
                                            )
                                        )
                                    }
                                    ChessGame.addPiece(
                                        arrocco_Piece!!.copy(
                                            chessman = Chessman.ROOK,
                                            resID = R.drawable.rook_white,
                                            col = col-1,
                                            row = row,
                                            moved = true
                                        )
                                    )


                                }


                                if(movingPiece!=null) {
                                    ChessGame.pieceAt(col, row)?.let {
                                        if (it.player != movingPiece?.player) {
                                            ChessGame.piecesBox.remove(it)
                                        }
                                    }
                                }


                            }
                        }

                        Log.e("check3", check.toString());
                    }
                }
                movingPiece = null
                movingPieceBitmap = null
                invalidate()
            }
        }
        return true

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
}
