package com.macc.android.chess

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class EvaluationChart(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val whitePaint = Paint()
    private val blackPaint = Paint()
    private val bgPaint = Paint()
    private val paintText = Paint()

    override fun onDraw(canvas: Canvas?) {
        canvas ?: return

        //println("onDraw")
        //println(ChessGame.ChessGame.evaluationsArray.toString())

        drawChessboard(canvas)

        blackPaint.color = Color.parseColor("#444444")
        blackPaint.strokeWidth = 3f
        blackPaint.style = Paint.Style.FILL_AND_STROKE

        whitePaint.color = Color.parseColor("#444444")
        whitePaint.strokeWidth = 3f
        whitePaint.style = Paint.Style.STROKE

        //val evaluationsArray = mutableListOf(100,200,300,400,500,600,700,800,900,-100,-200,-300,-400,-500,-600,-700,-1200)
        val numberOfMoves = ChessGame.evaluationsArray.size
        println(ChessGame.evaluationsArray.toString())
        //println(numberOfMoves.toString())
        if (numberOfMoves<1) return
        val maxPoints = ChessGame.evaluationsArray.maxOrNull()
        val minPoints = ChessGame.evaluationsArray.minOrNull()
        val maxEval = if (kotlin.math.abs(maxPoints!!)> kotlin.math.abs(minPoints!!)) maxPoints else minPoints
        //println(maxEval.toString())
        val rectW = width/(numberOfMoves+1)

        for (i in 0 until numberOfMoves) {
            val rectHeight = kotlin.math.abs((height/2.0)*(ChessGame.evaluationsArray[i].toFloat() / maxEval.toFloat()))
            //println(ChessGame.evaluationsArray[i].toFloat() / maxEval.toFloat())
            //println(ChessGame.evaluationsArray[i].toString() + "  ###  " + rectHeight.toString())

            //white
            if (ChessGame.evaluationsArray[i]>0) {
                canvas.drawRect((0f+(i+1)*rectW), ((height/2)-rectHeight).toFloat(), (2*rectW+i*rectW).toFloat(), (height/2).toFloat(), whitePaint)
                //to remove white space at the beginning: replace (i+1)->i and 2*rectW->rectW (also below) and the +1 when initializing rectW
            }
            //black
            else {
                canvas.drawRect((0f+(i+1)*rectW), (height/2).toFloat(), (2*rectW+i*rectW).toFloat(), ((height/2)+rectHeight).toFloat(), blackPaint)
            }
        }

        paintText.color = Color.parseColor("#444444")
        //paintText.strokeWidth = 3f
        paintText.textSize = 60f
        canvas.drawText("0", 5f, (height/2)+20f, paintText)
        //println(maxEval)
        //println(maxPoints)
        val highestEvalHeight = if (kotlin.math.abs(maxEval)==kotlin.math.abs(maxPoints)) 50f else height.toFloat()
        //println(highestEvalHeight)
        canvas.drawText((maxEval/100).toString(), 5f, highestEvalHeight, paintText)

        ChessGame.evaluationsArray.clear()
    }

    private fun drawChessboard(canvas: Canvas) {
        for (row in 0 until 8)
            for (col in 0 until 8)
                drawSquareAt(canvas, col, row, (col + row) % 2 == 1)
    }

    private fun drawSquareAt(canvas: Canvas, col: Int, row: Int, isDark: Boolean) {
        bgPaint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(Rect(0, 1000 * col, 1000 * row, 0), bgPaint)    //TODO: 100
    }
}
