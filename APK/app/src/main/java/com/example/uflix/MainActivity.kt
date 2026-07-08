package com.example.uflix

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var noticeBoard: TextView
    
    private val apiUrl = "https://uflix.urbanlinknetwork.com/api.php"
    private val pingUrl = "https://uflix.urbanlinknetwork.com/ping.php"
    
    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatRunnable: Runnable? = null
    private var deviceId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        buttonContainer = findViewById(R.id.buttonContainer)
        noticeBoard = findViewById(R.id.noticeBoard)
        
        noticeBoard.isSelected = true

        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

        fetchData()
        startHeartbeat()
    }

    private fun fetchData() {
        val queue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, apiUrl, null,
            { response ->
                try {
                    val notice = response.optString("notice", "Welcome to UFLIX! Enjoy your premium streaming.")
                    noticeBoard.text = notice

                    val status = response.optString("status")
                    if (status == "success") {
                        val servers = response.optJSONArray("servers")
                        if (servers != null && servers.length() > 0) {
                            displayServers(servers)
                            tvStatus.text = "SELECT SERVER"
                        } else {
                            tvStatus.text = "NO SERVERS"
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            { 
                tvStatus.text = "CONNECTION ERROR"
            }
        )
        queue.add(jsonObjectRequest)
    }

    private fun displayServers(servers: JSONArray) {
        buttonContainer.removeAllViews()
        for (i in 0 until servers.length()) {
            val serverObj = servers.optJSONObject(i) ?: continue
            val name = serverObj.optString("name", "Server ${i+1}")
            val url = serverObj.optString("url", "")

            if (url.isNotEmpty()) {
                val button = Button(this).apply {
                    text = name
                    // Improved styling for both Phone and TV
                    textSize = 18f
                    setTextColor(android.graphics.Color.WHITE)
                    setBackgroundResource(android.R.drawable.btn_default) // Using default or custom background
                    
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(20, 15, 20, 15)
                        width = (resources.displayMetrics.widthPixels * 0.8).toInt() // 80% of screen width
                    }
                    
                    isFocusable = true
                    isClickable = true
                    
                    setOnClickListener {
                        val intent = Intent(this@MainActivity, WebViewActivity::class.java)
                        intent.putExtra("TARGET_URL", url)
                        startActivity(intent)
                    }
                }
                buttonContainer.addView(button)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatRunnable = object : Runnable {
            override fun run() {
                sendPing()
                handler.postDelayed(this, 60000)
            }
        }
        heartbeatRunnable?.let { handler.post(it) }
    }

    private fun sendPing() {
        val queue = Volley.newRequestQueue(this)
        val request = object : StringRequest(Request.Method.POST, pingUrl,
            { }, { }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["device_id"] = deviceId
                return params
            }
        }
        queue.add(request)
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatRunnable?.let { handler.removeCallbacks(it) }
    }
}