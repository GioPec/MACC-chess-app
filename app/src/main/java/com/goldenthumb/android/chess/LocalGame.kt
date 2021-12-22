package com.goldenthumb.android.chess

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class LocalGame : AppCompatActivity(), ChessDelegate {

    private lateinit var chessView: ChessView
    private lateinit var resetButton: Button
    private lateinit var listenButton: Button
    private lateinit var connectButton: Button
    private lateinit var turnTextView: TextView
    override fun pieceAt(square: Square): ChessPiece? = ChessGame.pieceAt(square)

    private var sensorManager: SensorManager? = null
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
            if (acceleration > 2) {
                ChessGame.reset()
                chessView.invalidate()
                //Log.i("listener", "acceleration = " + acceleration)
                //Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    override fun onResume() {
        sensorManager?.registerListener(sensorListener, sensorManager!!.getDefaultSensor(
                Sensor .TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL
        )
        super.onResume()
    }
    override fun onPause() {
        sensorManager!!.unregisterListener(sensorListener)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_game)

        chessView = findViewById<ChessView>(R.id.chess_view)
        resetButton = findViewById<Button>(R.id.reset_button)
        listenButton = findViewById<Button>(R.id.listen_button)
        connectButton = findViewById<Button>(R.id.connect_button)
        turnTextView = findViewById<Button>(R.id.turn)

        chessView.chessDelegate = this

        resetButton.setOnClickListener {
            ChessGame.reset()
            turnTextView.setTextColor(Color.parseColor("#FFFFFF"))
            turnTextView.setBackgroundColor(Color.parseColor("#CCCCCC"))
            turnTextView.text = "White turn"
            chessView.invalidate()
            listenButton.isEnabled = true
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Objects.requireNonNull(sensorManager)!!.registerListener(sensorListener, sensorManager!!
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL)
        acceleration = 10f
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH
    }

    override fun movePiece(from: Square, to: Square) {}
    override fun updateProgressBar(type: String, value: Int) {}

    override fun updateTurn(player: Player) {
        Log.d("player", player.toString())
        if (player == Player.WHITE) {
            turnTextView.setTextColor(Color.parseColor("#999999"))
            turnTextView.setBackgroundColor(Color.parseColor("#333333"))
            turnTextView.text = "Black turn"
        }
        else {
            turnTextView.setTextColor(Color.parseColor("#FFFFFF"))
            turnTextView.setBackgroundColor(Color.parseColor("#CCCCCC"))
            turnTextView.text = "White turn"
        }
    }
}