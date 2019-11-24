package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.collections.HashMap


data class Dictionary(val word: String? = "", val meaning: String? = "")

private class FirestoreRecyclerAdapter internal constructor(options: FirestoreRecyclerOptions<Dictionary>): FirestoreRecyclerAdapter<Dictionary, DictionaryViewHolder>(options) {

    override fun onBindViewHolder(viewHolder: DictionaryViewHolder, position: Int, dictionary: Dictionary) {
        viewHolder.setWord(dictionary.word!!)
        viewHolder.setMeaning(dictionary.meaning!!)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DictionaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dictionary_layout, parent, false)
        return DictionaryViewHolder(view)
    }
}

private class DictionaryViewHolder internal constructor(private val view: View) : RecyclerView.ViewHolder(view) {
    fun setWord(word: String) {
        val wordTextView = view.findViewById<TextView>(R.id.word_tw)
        wordTextView.text = word
    }

    fun setMeaning(meaning: String) {
        val meaningTextView = view.findViewById<TextView>(R.id.meaning_tw)
        meaningTextView.text = meaning
    }
}

class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var viewAdapter: com.example.myapplication.FirestoreRecyclerAdapter
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var database: FirebaseFirestore
    private val dictionary = HashMap<String, String>()
    private val optionsBuilder = FirestoreRecyclerOptions.Builder<Dictionary>()
    private lateinit var options: FirestoreRecyclerOptions<Dictionary>
    private lateinit var query: Query

    private val dictionaryList = ArrayList<Dictionary>()

    private val RECORD_AUDIO_PERMISSION = 1;
    private val TAG = "TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.color1),
            ContextCompat.getColor(this, R.color.color2),
            ContextCompat.getColor(this, R.color.color3),
            ContextCompat.getColor(this, R.color.color4),
            ContextCompat.getColor(this, R.color.color5)
        )

        database = FirebaseFirestore.getInstance()

        query = database.collection("dictionary")
        options = optionsBuilder.setQuery(query, Dictionary::class.java).build()

        //loadFromDatabase()

        viewAdapter = com.example.myapplication.FirestoreRecyclerAdapter(options)

        recyclerView.apply {
            // use a linear layout manager
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

            adapter = viewAdapter
        }

        val heights = intArrayOf(20, 24, 18, 23, 16)

        //viewManager = LinearLayoutManager(this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognition_view.setSpeechRecognizer(speechRecognizer)
        recognition_view.setRecognitionListener(this)
        recognition_view.setColors(colors)
        recognition_view.setBarMaxHeightsInDp(heights)

        // click listeners
        microphone_btn.setOnClickListener {
            checkRecognitionSpeechSupport()
            runOnUiThread { askForAudioRecordPermission() }
        }

        add_btn.setOnClickListener {
            addToDatabase()
        }
    }

    override fun onStart() {
        super.onStart()
        viewAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        viewAdapter.stopListening()
    }


    private fun addToDatabase() {
        if (word_tw.text.isNotEmpty() && word_tw.text.isNotEmpty()) {

            dictionary["word"] = word_tw.text.toString()
            dictionary["meaning"] = meaning_tw.text.toString()

            database.collection("dictionary")
                .add(dictionary)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot added with ID: " + it.id)
                    Toast.makeText(this, "DocumentSnapshot added with ID: " + it.id, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Log.d(TAG, "Error adding document", it)
                    Toast.makeText(this, "Error adding document: $it", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "One of the fields is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFromDatabase() {
        database.collection("dictionary")
            .get()
            .addOnCompleteListener {
                for (snapshot in it.result!!) {
                    val dictionary = Dictionary(snapshot.getString("word")!!, snapshot.getString("meaning")!!)
                    dictionaryList.add(dictionary)

                }

            }
        }


    private fun startRecognition() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en")
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, application.packageName)
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    private fun askForAudioRecordPermission() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.RECORD_AUDIO)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        } else {
            startRecognition()
        }

    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Requires RECORD_AUDIO permission", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission has been denied by user")
                } else {
                    Log.i(TAG, "Permission has been granted by user")
                }
            }
        }
    }

    private fun checkRecognitionSpeechSupport() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, R.string.no_recognition_available, Toast.LENGTH_LONG).show()
        }
    }

//    // tells you the value of the sounds recording at the instance in float representation.
    override fun onRmsChanged(p0: Float) {
        Log.i(TAG, "onRmsChanged")
    }

    override fun onBufferReceived(p0: ByteArray?) {
        Log.i(TAG, "onBufferReceived")
    }

    override fun onPartialResults(p0: Bundle?) {
        Log.i(TAG, "onPartialResults")
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        Log.i(TAG, "onEvent")
    }

    override fun onBeginningOfSpeech() {
        Log.i(TAG, "onBeginningOfSpeech")
        recognition_view.play()
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        Log.i(TAG, "onReadyForSpeech")
    }

    override fun onEndOfSpeech() {
        Log.i(TAG, "onEndOfSpeech")
        recognition_view.stop()
    }

    override fun onError(p0: Int) {
        Log.i("Error", p0.toString())
    }

    override fun onResults(results: Bundle?) {
        val result = results?.getStringArrayList("results_recognition")
        val first = result?.get(0)

        result?.let {
            if (it.contains("word")) {
                word_tw.setText(it.get(0))
            }

        }
    }
}
