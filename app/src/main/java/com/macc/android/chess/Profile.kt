package com.macc.android.chess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.LinkedHashMap

class Profile : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var username: TextView
    private lateinit var chessPoints: TextView
    private lateinit var logoutButton: Button
    private lateinit var matchesHistoryLayout: LinearLayout

    private var matchResult = mutableListOf<String>()
    private var matchMoves = mutableListOf<Any>()
    private var matchDate = mutableListOf<Date>()
    private var matchAdversary = mutableListOf<String>()

    data class myDataUse(var matchDate:Date,var matchAdversary:String, var matchMoves: Any, var matchResult: String)
    val myDataUseList: MutableList<myDataUse>? = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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

        mAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://macc-chess-dcd2a-default-rtdb.europe-west1.firebasedatabase.app")
        myRef = database.reference

        //TODO: fix this below (chronological order)

        getChessPoints()
        getMatchesHistory()


        Handler(Looper.getMainLooper()).postDelayed({
            setContentView(R.layout.activity_profile)

            username = findViewById(R.id.username)
            chessPoints = findViewById(R.id.chess_points)
            //logoutButton = findViewById(R.id.logout_button)
            matchesHistoryLayout = findViewById(R.id.matches_history_layout)

            username.text = ChessGame.myUsername
            if (ChessGame.chessPointsListLength<1) chessPoints.text = "Chess Points: 100"
            else chessPoints.text = "Chess Points: ${ChessGame.chessPointsList[ChessGame.chessPointsListLength - 1]}"

            //////////////////////////////////

            val n = matchResult.size - 1 // total number of textviews to add
/*            val theArray = mutableListOf<Any>(n)

            for (i in 0..n) {
                theArray[i] = arrayOf(matchResult[i], matchMoves[i], matchDate[i], matchAdversary[i])
                println(theArray)
            }
            theArray.sortBy { Comparable(it) }  //???*/
            //////////////////////////////////


            if (BuildConfig.DEBUG && !(matchResult.size == matchMoves.size && matchMoves.size == matchDate.size)) {
                error("Assertion failed")
            }

            if (n<0) return@postDelayed

            //val myTextViews = arrayOfNulls<TextView>(n) // create an empty array;

            for (i in 0..n) {

                //TODO: restyle

                //TODO: sort on date attribute?

                ////

                val rowTextView = TextView(this)
                var sortedByDate =  myDataUseList?.sortedBy { myDataUse -> myDataUse.matchDate }?.asReversed()
                val result = sortedByDate?.get(i)?.matchResult
                var resultExtended = ""
                when (result) {
                    "w" -> resultExtended = "victory"
                    "l" -> resultExtended = "defeat"
                    "d" -> resultExtended = "draw"
                }

                //sortedByNumber[0].name
                println("stiamo stampando le date: "+matchDate)
                rowTextView.text = "${sortedByDate?.get(i)?.matchDate} \nAdversary: ${sortedByDate?.get(i)?.matchAdversary} \nResult: $resultExtended \n${sortedByDate?.get(i)?.matchMoves}"
                rowTextView.textSize = 16f

                matchesHistoryLayout.addView(rowTextView)

                ////

                //padding between matches
                val paddingTextView = TextView(this)
                paddingTextView.text = ""
                matchesHistoryLayout.addView(paddingTextView)
            }
        }, 1000)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {


        val inflater2 = menuInflater
        inflater2.inflate(R.menu.setting, menu)

        val inflater = menuInflater
        inflater.inflate(R.menu.chiudi, menu)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_chiudi -> {
            this.finish();
            true
        }
        R.id.action_setting -> {
            startActivity(Intent(this, Impostazioni::class.java))
            true
        }

        else -> false
    }

    private fun getMatchesHistory() {

        var i=0

        myRef.child("Users").child(ChessGame.myUsername).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val adversaries = snapshot.value as HashMap<*, *>
                //println(adversaries)
                for (adv in adversaries.keys) {
                    if (adv == "chessPoints") continue
                    //println("### adv = $adv")
                    val maybeSM = adversaries[adv] as HashMap<*, *>
                    //println("### maybeSM = $maybeSM")
                    for (smKey in maybeSM.keys) {
                        if (smKey.toString() == "savedMatches") {
                            //println("### smKey = $smKey")
                            //this sometimes fails, gives list???
                            val m = maybeSM[smKey] as HashMap<*, *>
                            for (date in m.keys) {

                                val dateLong = (date as String).toLong()
                                val match = m[date] as String
                                //println("### dateLong = $dateLong")
                                //println("### match = $match")
                                if (BuildConfig.DEBUG && match.split("|").size < 2) {
                                    error("Assertion failed")
                                }

                                matchResult.add(match.split("|")[0])

                                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm")
                                matchDate.add(Date(dateLong))
                                //println("le date sono: "+matchDate)

                                matchMoves.add(match.split("|").slice(IntRange(2, match.split("|").size - 2)))

                                matchAdversary.add(adv as String)

                                myDataUseList!!.add(myDataUse(Date(dateLong),adv as String,match.split("|").slice(IntRange(2, match.split("|").size - 2)),match.split("|")[0]))

                                i += 1
                            }
                        }
                    }
                }

                //println("matchResult = $matchResult")
                //println("matchDate = $matchDate")
                //println("matchMoves = $matchMoves")
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }



    private fun getChessPoints() {

        val matchesMap = LinkedHashMap<String, Int>()

        var i=0

        myRef.child("Users").child(ChessGame.myUsername).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                val adversaries = snapshot.value as HashMap<*, *>
                println(adversaries)
                for (adv in adversaries.keys) {
                    if (adv == "chessPoints") continue
                    //println("### adv = $adv")
                    val maybeSM = adversaries[adv] as HashMap<*, *>
                    //println("### maybeSM = $maybeSM")
                    for (smKey in maybeSM.keys) {
                        if (smKey.toString() == "savedMatches") {
                            //println("### smKey = $smKey")
                            //this sometimes fails, gives list???
                            val m = maybeSM[smKey] as HashMap<*, *>
                            for (date in m.keys) {
                                val match = m[date] as String
                                //println("### date = $date")
                                //println("### match = $match")
                                if (BuildConfig.DEBUG && match.split("|").size < 2) {
                                    error("Assertion failed")
                                }
                                val matchChessPoints = match.split("|")[1]
                                //println("****************** $matchChessPoints ***********************")
                                //ChessGame.chessPointsList[i] = matchChessPoints.toInt()
                                matchesMap[date as String] = matchChessPoints.toInt()
                                i += 1
                            }
                        }
                    }
                }
                ChessGame.chessPointsListLength = i
                //println("matchesMap = ${matchesMap.toString()}")
                //println("matchesMap size = ${matchesMap.size}")
                //println("matchesMap size = ${matchesMap.size}")

                val matchesMapSorted = matchesMap.toSortedMap()     //to correct the temporal order of matches
                //println("matchesMapSorted = $matchesMapSorted")
                ChessGame.chessPointsList = matchesMapSorted.values.toIntArray()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}