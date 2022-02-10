package com.macc.android.chess

import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.json.JSONObject
import java.lang.Exception
import java.util.HashMap

class MainMenu : AppCompatActivity() {

    private lateinit var stockfishStatus: TextView
    var resumeButton: Button? = null
    var number = 0

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var againistStockfishButton: Button
    private lateinit var localButton: Button
    private lateinit var onlineButton: Button
    private lateinit var resetButton: Button
    private lateinit var startButton: Button
    private lateinit var whatsAppButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        /**snip  */
        val intentFilter = IntentFilter()
        intentFilter.addAction("com.package.ACTION_LOGOUT")
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("onReceive", "Logout in progress")
                //At this point you should start the login activity and finish this one
                finish()
            }
        }, intentFilter)

        ///
        ChessGame.waitingForAdversary = true
        ChessGame.gameInProgress = ""
        ChessGame.myOnlineColor = ""
        ChessGame.adversary = ""
        ChessGame.isOnlineMate = "false"
        ChessGame.isLocalMate = "false"
        ///
        againistStockfishButton=findViewById(R.id.button2)
        localButton=findViewById(R.id.button3)
        onlineButton=findViewById(R.id.button)


        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference
        listenForChallenges()

        Log.d("errore","mainmenu")

        resumeButton = findViewById(R.id.resume_button)
    }

    override fun onStart() {
        super.onStart()
        Log.d("Game in progress", ChessGame.gameInProgress)
        Log.d("Resetted game", ChessGame.resettedGame.toString())
        if (ChessGame.gameInProgress=="" || ChessGame.resettedGame) resumeButton?.visibility = View.GONE
        else resumeButton?.visibility = View.VISIBLE  //TODO: fix

        stockfishStatus = findViewById(R.id.stockfishStatus)
        getHelloWorldFromStockfishAPI()
        whatsAppButton = findViewById(R.id.button_WhatsApp)


        againistStockfishButton.setOnClickListener {
            startGameAgainstStockfish(this,ChessGame.matchId)
        }
        localButton.setOnClickListener {
            startGameLocal(this,ChessGame.matchId)
        }
        onlineButton.setOnClickListener {
            startGameOnline(this,ChessGame.matchId)
        }

        whatsAppButton.setOnClickListener{
            onClickWhatsApp(this,"Hi! Challenge me on ChessApp!!!, My Username is: "+ChessGame.myUsername)
        }

        //listenForChallenges()
    }


    override fun onRestart() {
        super.onRestart()
        println("restartttttttttttttttt")
        Log.d("Game in progress", ChessGame.gameInProgress)
        Log.d("Resetted game", ChessGame.resettedGame.toString())
        if (ChessGame.gameInProgress=="" || ChessGame.resettedGame) resumeButton?.visibility = View.GONE
        else resumeButton?.visibility = View.VISIBLE  //TODO: fix

        stockfishStatus = findViewById(R.id.stockfishStatus)
        getHelloWorldFromStockfishAPI()
        whatsAppButton = findViewById(R.id.button_WhatsApp)


        againistStockfishButton.setOnClickListener {
            startGameAgainstStockfish(this,ChessGame.matchId)
        }
        localButton.setOnClickListener {
            startGameLocal(this,ChessGame.matchId)
        }
        onlineButton.setOnClickListener {
            startGameOnline(this,ChessGame.matchId)
        }

        whatsAppButton.setOnClickListener{
            onClickWhatsApp(this,"Hi! Challenge me on ChessApp!!!, My Username is: "+ChessGame.myUsername)
        }


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater2 = menuInflater
        inflater2.inflate(R.menu.television, menu)

        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)


        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_profile -> {
            // User chose the "Profile" item, show the app profile page
            startActivity(Intent(this, Profile::class.java))
            true
        }

        R.id.action_television -> {
            // User chose the "Profile" item, show the app profile page
            startActivity(Intent(this, Stream::class.java))
            true
        }
        else -> false
    }

    fun onClickWhatsApp(view: MainMenu, msg: String) {
        val pm = packageManager
        try {
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.type = "text/plain"
            val text = msg
            val info = pm.getPackageInfo("com.whatsapp", PackageManager.GET_META_DATA)
            //Check if package exists or not. If not then code
            //in catch block will be called
            waIntent.setPackage("com.whatsapp")
            waIntent.putExtra(Intent.EXTRA_TEXT, text)
            startActivity(Intent.createChooser(waIntent, "Share with"))
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(this, "WhatsApp not Installed", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun listenForChallenges() {
        myRef.child("Users").child(ChessGame.myUsername).addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                if (ChessGame.gameInProgress=="ONLINE") return
                try {
                    val td = snapshot.value as HashMap<*, *>
                    for (key in td.keys) {
                        //println(key)
                        if (key=="chessPoints") continue
                        val username = td[key] as HashMap<*, *>
                        for (cm in username.keys) {
                            //println("$key $username $cm")
                            if (cm.toString() == "currentMatch") {
                                val adversaryUsername = key as String
                                if (!ChessGame.challengeAlreadyNotified || (username[cm] as String)=="") {
                                    ChessGame.challengeAlreadyNotified = true

                                    ////////////////////////////////////////////////////////////////////

                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        val notifyMe = Notifications()
                                        notifyMe.notify(applicationContext, "$adversaryUsername has challenged you!", number)
                                        number++
                                    }

                                    val dialogClickListener = DialogInterface.OnClickListener { _, which ->
                                        when (which) {
                                            DialogInterface.BUTTON_POSITIVE -> {
                                                myRef.child("Users").child(ChessGame.myUsername).child(adversaryUsername).child("currentMatch")
                                                        .setValue("accepted")

                                                myRef.child("Users").child(ChessGame.myUsername).child(adversaryUsername).child("matchId").get().addOnSuccessListener {
                                                    Log.i("firebase", "Got value ${it.value}")
                                                    ChessGame.matchId=it.value.toString().toInt()
                                                    Log.i("firebase2", ChessGame.matchId.toString())
                                                }.addOnFailureListener{
                                                    Log.e("firebase", "Error getting data", it)
                                                }

                                                ChessGame.myOnlineColor = "BLACK"
                                                ChessGame.adversary = adversaryUsername
                                                confirmOnline("BLACK",ChessGame.matchId)
                                            }
                                            DialogInterface.BUTTON_NEGATIVE -> {
                                                myRef.child("Users").child(ChessGame.myUsername).child(adversaryUsername).child("currentMatch")
                                                        .setValue("refused")
                                            }
                                        }
                                    }
                                    val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
                                    builder.setMessage("You have been challenged by $adversaryUsername!\nAccept?")
                                        .setPositiveButton("Yes", dialogClickListener)
                                        .setNegativeButton("No", dialogClickListener).show()

                                    ////////////////////////////////////////////////////////////////////
                                }
                            }
                        }
                    }
                } catch (e:Exception) { Log.e ("MainMenu", "No challenges found: $e")}
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("E", "Failed to read value.", error.toException())
            }
        })
    }
    /*
    suspend fun asincStartMatchId() : Int{
        val queue = Volley.newRequestQueue(this)
        val url = "https://JaR.pythonanywhere.com"+"/startMatch"
        ChessGame.resettedGame = true
        ChessGame.stockfishGameEnded = false
        //println("iniziamoilmatch")
        var id_match=404

            val stringRequest = StringRequest(
                Request.Method.GET, url,

                { response ->
                    //response.subSequence(1, 3)
                    //Log.e("aa",response)
                    var stato = JSONObject(response)
                    id_match = stato.get("response") as Int
                    Log.d("matchid", id_match.toString())

                },
                { error ->

                    Log.e("errore", "stamo in errore")
                },

                )
            queue.add(stringRequest)



        return id_match
    }*/

    private fun getHelloWorldFromStockfishAPI() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://JaR.pythonanywhere.com"+"/hello"


        val stringRequest = StringRequest(
                Request.Method.GET, url,

                { response ->
                    //response.subSequence(1, 3)
                    //Log.e("aa",response)
                    var stato= JSONObject(response)

                    if (stato.get("state") == "OK") {
                        stockfishStatus.text = "Server is online!"
                    } else {
                        stockfishStatus.text = "Server is offline! :("
                    }
                },
                { error ->
                    stockfishStatus.text = "Chess API $error"
                    stockfishStatus.setTextColor(Color.RED)
                    Log.e("errore","stamo in errore")
                },

                )
        queue.add(stringRequest)
    }

    fun startGameAgainstStockfish(view: MainMenu, id: Int) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmStockfish(id)
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="" && !ChessGame.resettedGame) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmStockfish(id)
    }
    private fun confirmStockfish(id: Int) {
        ChessGame.reset(id)
        startActivity(Intent(this, StockfishGame::class.java))
        ChessGame.stockmatch=0
        ChessGame.hintAlreadyUsed=false
        ChessGame.firstMove = true
        /*
        ChessGame.matchId=ChessGame.startMatchId()
        if(ChessGame.matchId!=404){
            ChessGame.gameInProgress="STOCKFISH"
        }else{
            Toast.makeText(applicationContext, "Si stanno giocando molti match, prova tra poco ;-)", Toast.LENGTH_LONG).show()
        }*/
    }

    fun startGameLocal(view: MainMenu, id: Int) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmLocal(id)
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="" && !ChessGame.resettedGame) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmLocal(id)
    }
    private fun confirmLocal(id: Int) {
        ChessGame.reset(id)
        startActivity(Intent(this, LocalGame::class.java))
        ChessGame.startedmatch=0

        /*
        ChessGame.matchId=ChessGame.startMatchId()
        println(ChessGame.matchId)
        if(ChessGame.matchId!=404) {
            ChessGame.gameInProgress = "LOCAL"
        }else{
            Toast.makeText(applicationContext, "Si stanno giocando molti match, prova tra poco ;-)", Toast.LENGTH_LONG).show()
        }*/
    }

    fun startGameOnline(view: MainMenu, id: Int) {
        //create interface
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> confirmOnline("",id)
                DialogInterface.BUTTON_NEGATIVE -> {}
            }
        }
        //ask user
        if (ChessGame.gameInProgress!="" && !ChessGame.resettedGame) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainMenu)
            builder.setMessage("You already have an active game.\nIf you start a new one " +
                    "you will lose all your progress!\nDo you want to proceed?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show()
        } else confirmOnline("",id)
    }

    private fun confirmOnline(c:String, id: Int) {
        //ChessGame.reset(id)
        ChessGame.reset_white();
        ChessGame.gameInProgress="ONLINE"

        val intent = Intent(this, OnlineGame::class.java)
        intent.putExtra("color", c)

        startActivity(intent)
        //resumeButton?.visibility = View.VISIBLE
    }

    fun resumeGame(view: View) {

        when (ChessGame.gameInProgress) {
            "LOCAL" -> {
                startActivity(Intent(this, LocalGame::class.java))
                println("eskere")}

            "STOCKFISH" -> {startActivity(Intent(this, StockfishGame::class.java))}



            "ONLINE" -> startActivity(Intent(this, OnlineGame::class.java))
            "" -> return
        }
    }
}




