package com.macc.android.chess

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.item_bis.view.*
import java.text.SimpleDateFormat
import java.util.*


class Classification : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private lateinit var matchesPoint: LinearLayout
    data class datiInfo(var NomeUtente:String,var PuntiUtente:Long)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification)

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


        matchesPoint = findViewById(R.id.matches_pp)

        readData(object : MyCallback {
            override fun onCallback(risposta: List<datiInfo>?) {

                for (i in 0..(risposta?.size!!-1)) {
                    val view= getLayoutInflater().inflate(R.layout.item_bis, null);
                    view.nome.text= risposta?.get(i)?.NomeUtente
                    view.live.text = risposta?.get(i)?.PuntiUtente.toString() + " points"
                    view.live.textSize= 15.5f
                    if(i+1<=99){
                        view.immagine.text= (i+1).toString()
                    }else{
                        view.immagine.text= "..."
                    }


                    matchesPoint.addView(view)

                }

            }

        })

    }


    fun readData(myCallback: MyCallback) {

        //var myDataPoint: MutableList<ChessPointValue>? = mutableListOf()
        val datiUtente: MutableList<datiInfo>? = mutableListOf()
        var sortedPoint: MutableList<datiInfo>? = mutableListOf()

        myRef.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot)  {
                val adversaries = snapshot.value as HashMap<*, *>
                //println("### adversaries"+ adversaries)

                for (adv in adversaries.keys) {
                    //if (adv == "chessPoints") continue
                    println("### adv = $adv")


                    val maybeSM = adversaries[adv] as HashMap<*, *>
                    //println("### maybeSM = $maybeSM")

                    for (smKey in maybeSM.keys) {
                        //println("### smKey = $smKey")
                        if (smKey.toString() == "chessPoints") {
                            //println("### smKey = $smKey")
                            //this sometimes fails, gives list???
                            val m = maybeSM[smKey]
                            //println("valore: " +m)
                            datiUtente!!.add(datiInfo(adv as String,m as Long))

                        }
                    }
                }
                println("prima"+datiUtente)
                var sortedPoint = datiUtente?.sortedBy { datiInfo -> datiInfo.PuntiUtente }?.asReversed()
                println("dopo"+sortedPoint)
                myCallback.onCallback(sortedPoint)

            }

            override fun onCancelled(error: DatabaseError) {}
        })


    }

    interface MyCallback {
        fun onCallback(value: List<datiInfo>?)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.chiudi, menu)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_chiudi -> {
            this.finish();
            true
        }

        else -> false
    }

}
