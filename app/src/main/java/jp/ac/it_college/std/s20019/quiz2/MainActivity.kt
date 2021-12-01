package jp.ac.it_college.std.s20019.quiz2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import jp.ac.it_college.std.s20019.quiz2.databinding.FragmentQuizBinding
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    companion object {
        // URL
        private const val API_URL =
            "https://script.google.com/macros/s/AKfycbznWpk2m8q6lbLWSS6qaz3uS6j3L4zPwv7CqDEiC433YOgAdaFekGJmjoAO60quMg6l/exec?f="
    }
    private lateinit var binding: FragmentQuizBinding
    private val helper = DatabaseHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiveQuiz("${API_URL}data")
    }

    @UiThread
    private fun receiveQuiz(urlFull: String) {
        val handler = HandlerCompat.createAsync(Looper.getMainLooper())
        val executeService = Executors.newSingleThreadExecutor()

        executeService.submit @WorkerThread {
            var result = ""
            val url = URL(urlFull)
            val con = url.openConnection() as? HttpURLConnection
            con?.let {
                try {
                    it.connectTimeout = 1000
                    it.readTimeout = 1000
                    it.requestMethod = "GET"
                    it.connect()
                    val stream = it.inputStream
                    result = is2String(stream)
                    stream.close()
                } catch (e: SocketTimeoutException) {
                    Log.w("Quiz2", "通信タイムアウト", e)
                }
                it.disconnect()
            }

            handler.post @UiThread {
                val rootJSON = JSONArray(result)

                val db = helper.writableDatabase
                val sqlInsert = """
                    INSERT INTO Quiz (_id, question, answer, choiceA, choiceB, choiceC, choiceD, choiceE, choiceF)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """.trimIndent()

                val stmt = db.compileStatement(sqlInsert)

                for (i in 0 until rootJSON.length()) {
                    val dataJSON = rootJSON.getJSONObject(i)
                    val idJSON = dataJSON.getLong("id")                 // idを取得
                    val question = dataJSON.getString("question")       // 問題を取得
                    val answer = dataJSON.getLong("answer")             // 正解を取得
                    val choicesJSON = dataJSON.getJSONArray("choices")  // 選択肢オブジェクト配列を取得
                    val choiceA = choicesJSON.getString(0)              // １つ目の選択肢を取得
                    val choiceB = choicesJSON.getString(1)              // ２つ目の選択肢を取得
                    val choiceC = choicesJSON.getString(2)              // ３つ目の選択肢を取得
                    val choiceD = choicesJSON.getString(3)              // ４つ目の選択肢を取得
                    val choiceE = choicesJSON.getString(4)              // ５つ目の選択肢を取得
                    val choiceF = choicesJSON.getString(5)              // ６つ目の選択肢を取得

                    stmt.run {
                        bindLong(1, idJSON)
                        bindString(2, question)
                        bindLong(3, answer)
                        bindString(4, choiceA)
                        bindString(5, choiceB)
                        bindString(6, choiceC)
                        bindString(7, choiceD)
                        bindString(8, choiceE)
                        bindString(9, choiceF)
                    }
                    stmt.executeInsert()

                    // 選択肢が５個以上ならチェックボックスにする
                    if (choicesJSON.length() >= 5) {
                        binding.quizText.text = question
                        binding.checkA.text = choiceA
                        binding.checkB.text = choiceB
                        binding.checkC.text = choiceC
                        binding.checkD.text = choiceD
                        binding.checkE.text = choiceE
                        binding.checkF.text = choiceF

                        binding.radioGroup.visibility = View.GONE
                    }
                    // 選択肢が４個ならラジオボタンにする
                    else {
                        binding.quizText.text = question
                        binding.radioA.text = choiceA
                        binding.radioB.text = choiceB
                        binding.radioC.text = choiceC
                        binding.radioD.text = choiceD

                        binding.checkboxes.visibility = View.GONE
                    }


                }
            }
        }
    }

    private fun is2String(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        return reader.readText()
    }

    override fun onDestroy() {
        helper.close()
        super.onDestroy()
    }
}