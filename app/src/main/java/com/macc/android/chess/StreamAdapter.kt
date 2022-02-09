package com.macc.android.chess

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.item.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class StreamAdapter(c: Context?) : RecyclerView.Adapter<StreamAdapter.ViewHolder>() {


    private var data: JSONArray? = null



    init{
        val queue = Volley.newRequestQueue(c)
        val url = "https://lichess.org/streamer/live"

        val stringRequest = StringRequest(
            Request.Method.GET, url,

            { response ->
                //response.subSequence(1, 3)
                //Log.e("aa",response)
                data= JSONArray(response.toString())
                Log.i("data", data!!.toString())
                notifyDataSetChanged()

            },
            { error ->

                Log.e("errore","siamo in errore")
            },

            )
        queue.add(stringRequest)


    }



    fun makeCall(what: Int = 0):JSONArray? {
        //what determines which field to get from the JSON object
        var name="https://api.chess.com/pub/streamers"
        val url = URL(name)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.run {
                requestMethod="GET"
                val r = JSONObject(InputStreamReader(inputStream).readText())
                var res = (r.get("streamers") as JSONArray)

                Log.i("infotmazioni",res[0].toString())
                return res
            }
        }
        catch (e: Exception){
            Log.i("info",""+e.toString())
        }
        return null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //TODO("Not yet implemented")
        val view=LayoutInflater.from(parent.context).inflate(R.layout.item,parent,false)
        return ViewHolder(view)
    }
    var i=0;
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //TODO("Not yet implemented")
        var jsonObject = JSONObject(data!![position].toString())
        //var jsonObject2= JSONObject(jsonObject.getString("streamers").toString())
        println(jsonObject)



        with(holder.itemView){
            with(jsonObject){
                nome.text= (Html.fromHtml(getString("name")+ "<small>" + "   is online" + "</small>"))
                //time.text= "https://lichess.org/streamer/"+getString("id")+"/redirect"
                i=i+1
                live.isClickable = true
                live.movementMethod = LinkMovementMethod.getInstance()
                var link="https://lichess.org/streamer/"+getString("id")+"/redirect"
                val text = "<font color='grey'><a href="+link+ "> Start following the live </a></font>"
                live.text = Html.fromHtml(text)

                if(i%5==0){
                    immagine.setImageResource(R.drawable.chess_qdt60)
                }else if(i%5==1){
                    immagine.setImageResource(R.drawable.chess_rdt60)
                }else if(i%5==2){
                    immagine.setImageResource(R.drawable.chess_kdt60)
                }else if(i%5==3){
                    immagine.setImageResource(R.drawable.chess_pdt60)
                }else if(i%5==4){
                    immagine.setImageResource(R.drawable.chess_ndt60)
                }

            }

        }
    }

    override fun getItemCount(): Int {
        //TODO("Not yet implemented")
        if (data==null) return 0
        return data!!.length()


    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

    }


}
