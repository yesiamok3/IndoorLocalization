package com.iot8.indoorlocalization

import WavAudioRecorder
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.*
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity: AppCompatActivity() {
    lateinit var txtSection: TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtSection = findViewById(R.id.section)
        val btnPlay : Button = findViewById(R.id.btnPlay)
        var mediaPlayer: MediaPlayer? = null

        val handler = Handler(Looper.getMainLooper())

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.chirp11to22)
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnPlay.setOnClickListener {
            mediaPlayer?.start()
            val fileName = Date().getTime().toString()
            val extension = ".pcm"
            val output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName + extension
            val audioRecorder = WavAudioRecorder(this, output)
            audioRecorder.startRecording()

            handler.postDelayed({
                audioRecorder.stopRecording()
                postAudio(fileName)
            }, 1200)
        }

    }



    fun postAudio(name: String) {
        val extension = ".pcm"
        val output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + name + extension
        val pcmFile = File(output)
        val requestBody = RequestBody.create(okhttp3.MediaType.parse("audio/*"), pcmFile)
        val audioPart = MultipartBody.Part.createFormData("pcm", pcmFile.name, requestBody)
        val nameRequestBody = RequestBody.create(okhttp3.MediaType.parse("name"), name)

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Set connect timeout to 30 seconds
            .readTimeout(30, TimeUnit.SECONDS)    // Set read timeout to 30 seconds
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://ad3c-2001-2d8-ea49-a242-385d-aa2c-2384-55df.ngrok-free.app")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val postAudioAPI = retrofit.create(ClassificationAPI::class.java)
        val call = postAudioAPI.classify(audioPart, nameRequestBody)

        call.enqueue(object: Callback<PostResponseModel> {
            override fun onResponse(
                call: Call<PostResponseModel>,
                response: Response<PostResponseModel>
            ) {
                val result = response.body()
                val predicted = result?.result
                if (predicted.equals("aa")) txtSection.text = "1열 앞"
                else if (predicted.equals("ad")) txtSection.text = "1열 중간"
                else if (predicted.equals("af")) txtSection.text = "1열 뒤"
                else if (predicted.equals("ba")) txtSection.text = "2열 앞"
                else if (predicted.equals("bc")) txtSection.text = "2열 중간"
                else if (predicted.equals("bg")) txtSection.text = "2열 뒤"
                else if (predicted.equals("cb")) txtSection.text = "3열 앞"
                else if (predicted.equals("cd")) txtSection.text = "3열 중간"
                else if (predicted.equals("cf")) txtSection.text = "3열 뒤"
                else {
                    val error = response.code().toString() + " " + response.message()
                    txtSection.text = error
                }
            }

            override fun onFailure(call: Call<PostResponseModel>, t: Throwable) {
                txtSection.text = t.message
            }
        })
    }
}