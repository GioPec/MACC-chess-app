package com.macc.android.chess;

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.firebase.database.*
import com.macc.android.chess.ChessGame.chessPointsFloatArray

class ChessPointsChart(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint()
    private val paintThin = Paint()
    private val padding = 30f

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        //println("onDraw")
        //println(ChessGame.chessPointsList.toString())

        drawChessboard(canvas)

/*
        OLD RANDOM CODE

        //val l = mutableListOf(100, 110, 125, 112, 101, 98, 90, 95, 104, 121, 132, 100)

        val min = 50
        val max = 180

        val l = IntArray(12)
        for (i in ChessGame.chessPointsList.indices) {
            ChessGame.chessPointsList[i] = Random.nextInt(max!! - min!! + 1) + min!! // storing random integers in an array
            //println(l[i]) // printing each array element
        }
*/

        val numberOfMatches = ChessGame.chessPointsListLength

        if (numberOfMatches<1) return

        val maxPoints = ChessGame.chessPointsList.maxOrNull()
        val minPoints = ChessGame.chessPointsList.copyOfRange(0, numberOfMatches).minOrNull() //TODO fix, always gives 0
        //println("numberOfMatches = $numberOfMatches")
        //println("maxPoints = $maxPoints")
        //println("minPoints = $minPoints")

        for (i in 0 until numberOfMatches) {
            //println("i=$i")
            //println("elem=${ChessGame.chessPointsList[i]}")
            createCoords(canvas, ChessGame.chessPointsList[i], i, numberOfMatches, minPoints!!, maxPoints!!)
        }

        chessPointsFloatArray[0] = chessPointsFloatArray[2]
        chessPointsFloatArray[1] = chessPointsFloatArray[3]

        chessPointsFloatArray[4 * numberOfMatches] = chessPointsFloatArray[(4 * numberOfMatches) - 2]
        chessPointsFloatArray[4 * numberOfMatches + 1] = chessPointsFloatArray[(4 * numberOfMatches) - 1]

        paint.color = Color.parseColor("#000000")
        paint.strokeWidth = 10f

        //draw lines
        canvas.drawLines(chessPointsFloatArray, 0, 4 * numberOfMatches, paint)

        //draw circles
        for (i in 0..(4*numberOfMatches)) {
            if (i%2==0)
                canvas.drawCircle(chessPointsFloatArray[i], chessPointsFloatArray[i + 1], 20f, paint)
        }

        //////////////////////

        val range = (canvas!!.height-2*padding)/3
        for (i in 1..3) {
            paintThin.color = Color.parseColor("#999999")
            paintThin.strokeWidth = 3f
            paintThin.textSize = 60f
            canvas.drawLine(0f, i * range.toFloat() + padding, canvas!!.width.toFloat(), i * range.toFloat() + padding, paintThin)
            canvas.drawText((maxPoints!! - ((maxPoints!! - minPoints!!) / 3) * (i)).toString(), 20f, i * range.toFloat() - (20f - padding), paintThin)
        }
        //max line
        canvas.drawLine(0f, padding, canvas!!.width.toFloat(), padding, paintThin)
        canvas.drawText(maxPoints!!.toString(), 20f, 60f + padding, paintThin)
    }

    private fun createCoords(canvas: Canvas?, n: Int, i: Int, size: Int, min: Int, max: Int) {

        var x = 0
        if (size==1) x = canvas!!.width/2
        else x = canvas!!.width/(size-1)*i

        val y = canvas!!.height-padding - (((canvas!!.height-padding - padding)*(n-min)) / (max-min))

        chessPointsFloatArray[2 + 4 * i + 0] = x.toFloat()
        chessPointsFloatArray[2 + 4 * i + 1] = y.toFloat()

        chessPointsFloatArray[2 + 4 * i + 2] = x.toFloat()
        chessPointsFloatArray[2 + 4 * i + 3] = y.toFloat()
    }

    private fun drawChessboard(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                drawSquareAt(canvas, col, row, (col + row) % 2 == 1)
    }

    private fun drawSquareAt(canvas: Canvas, col: Int, row: Int, isDark: Boolean) {
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(Rect(0, 1000 * col, 1000 * row, 0), paint)
    }

    /////


}
