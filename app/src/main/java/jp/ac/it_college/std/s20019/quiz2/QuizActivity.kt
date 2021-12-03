package jp.ac.it_college.std.s20019.quiz2

import android.animation.ObjectAnimator
import android.os.*
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import jp.ac.it_college.std.s20019.quiz2.databinding.ActivityQuizBinding
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class QuizActivity : AppCompatActivity() {
    companion object {
        // URL
        private const val API_URL =
            "https://script.google.com/macros/s/AKfycbznWpk2m8q6lbLWSS6qaz3uS6j3L4zPwv7CqDEiC433YOgAdaFekGJmjoAO60quMg6l/exec?f="
        const val MAX_COUNT = 10
        const val TIME_LIMIT = 10000L
        const val TIMER_INTERVAL = 100L
        const val CHOICE_DELAY_TIME = 2000L
        const val TIME_UP_DELAY_TIME = 1500L
    }

    private lateinit var binding: ActivityQuizBinding
    private val helper = DatabaseHelper(this)
    private var current = -1
    private var timeLeftCountdown = TimeLeftCountdown()
    private var startTime = 0L
    private var totalElapsedTime = 0L
    private var correctCount = 0
    private val currentElapsedTime get() = SystemClock.elapsedRealtime() - startTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiveQuiz("${API_URL}data")
    }

    /**
     * 制限時間をカウントダウンするタイマー
     */
    inner class TimeLeftCountdown : CountDownTimer(TIME_LIMIT, TIMER_INTERVAL) {
        override fun onTick(millisUntilFinished: Long) {
            animateToProgress(millisUntilFinished.toInt())
        }

        override fun onFinish() {
            totalElapsedTime += TIME_LIMIT
            animateToProgress(0)
            binding.timeupIcon.visibility = View.VISIBLE
            next(TIME_UP_DELAY_TIME)
        }

        /**
         * API Level 24 であれば、ProgressBar 自体にアニメーションのパラメータがありますが
         * 今回は 23 なので、ObjectAnimator を使って実装
         */
        private fun animateToProgress(progress: Int) {
            val anim = ObjectAnimator.ofInt(binding.timeLeftBar, "progress", progress)
            anim.duration = TIMER_INTERVAL
            anim.start()
        }
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

                // データベースヘルパーオブジェクトからデータベース接続オブジェクトを取得
                val db = helper.writableDatabase

                val sqlDelete = """
                    DELETE FROM Quiz
                """.trimIndent()
                var stmt = db.compileStatement(sqlDelete)
                stmt.executeUpdateDelete()

                val sqlInsert = """
                    INSERT INTO Quiz (_id, question, answer, choiceA, choiceB, choiceC, choiceD, choiceE, choiceF)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """.trimIndent()
                stmt = db.compileStatement(sqlInsert)

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

                    // 選択肢が５個以上ならチェックボックスを使う
                    if (choicesJSON.length() >= 5) {
                        val quizArray = arrayOf(choiceA, choiceB, choiceC, choiceD, choiceE, choiceF)   // 取得した選択肢を配列に入れる
                        val list: List<Int> = mutableListOf(0, 1, 2, 3, 4, 5)                           // 選択肢と同じ数だけ番号を生成し、リストにする
                        val randomIndex: List<Int> = list.shuffled()                                    // リストをシャッフルし、0 ~ 6のランダムな数字を生成

                        binding.quizText.text = question
                        binding.checkA.text = quizArray[randomIndex[0]]
                        binding.checkB.text = quizArray[randomIndex[1]]
                        binding.checkC.text = quizArray[randomIndex[2]]
                        binding.checkD.text = quizArray[randomIndex[3]]
                        binding.checkE.text = quizArray[randomIndex[4]]
                        binding.checkF.text = quizArray[randomIndex[5]]

                        binding.radioGroup.visibility = View.GONE
                        binding.checkboxes.visibility = View.VISIBLE
                    }
                    // 選択肢が４個ならラジオボタンを使う
                    else {
                        val quizArray = arrayOf(choiceA, choiceB, choiceC, choiceD)     // 取得した選択肢を配列に入れる
                        val list: List<Int> = mutableListOf(0, 1, 2, 3)                 // 選択肢と同じ数だけ番号を生成し、リストにする
                        val randomIndex: List<Int> = list.shuffled()                    // リストをシャッフルし、0 ~ 3のランダムな数字を生成

                        binding.quizText.text = question
                        binding.radioA.text = quizArray[randomIndex[0]]
                        binding.radioB.text = quizArray[randomIndex[1]]
                        binding.radioC.text = quizArray[randomIndex[2]]
                        binding.radioD.text = quizArray[randomIndex[3]]

                        binding.radioGroup.visibility = View.VISIBLE
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

    private fun next(delay: Long) {
        // 次の問題がまだある場合
        if (++current < MAX_COUNT) {
            timeLeftCountdown.cancel()
            binding.timeLeftBar.progress = 10000
            binding.badIcon.visibility = View.GONE
            binding.goodIcon.visibility = View.GONE
            binding.timeupIcon.visibility = View.GONE
            timeLeftCountdown.start()
            startTime = SystemClock.elapsedRealtime()
            return
        }
    }
    override fun onDestroy() {
        helper.close()
        super.onDestroy()
    }
}