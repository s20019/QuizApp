package jp.ac.it_college.std.s20019.quiz2

import android.animation.ObjectAnimator
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.os.HandlerCompat
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import jp.ac.it_college.std.s20019.quiz2.databinding.FragmentQuizBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Executors

class QuizFragment : Fragment() {
    companion object {
        // URL
        private const val API_URL = "https://script.google.com/macros/s/AKfycbznWpk2m8q6lbLWSS6qaz3uS6j3L4zPwv7CqDEiC433YOgAdaFekGJmjoAO60quMg6l/exec?f="

        const val MAX_COUNT = 10
        const val TIME_LIMIT = 10000L
        const val TIMER_INTERVAL = 100L
        const val CHOICE_DELAY_TIME = 2000L
        const val TIME_UP_DELAY_TIME = 1500L
    }

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private var current = -1
    private var timeLeftCountdown = TimeLeftCountdown()
    private var startTime = 0L
    private var totalElapsedTime = 0L
    private var correctCount = 0
    private val currentElapsedTime get() = SystemClock.elapsedRealtime() - startTime


    /**
     * 制限時間をカウントダウンするタイマー
     */
    inner class TimeLeftCountdown : CountDownTimer(TIME_LIMIT, TIMER_INTERVAL) {
        override fun onTick(millisUntilFinished: Long) {
            animateToProgress(millisUntilFinished.toInt())
        }

        override fun onFinish() {
            totalElapsedTime += TIME_LIMIT
            //isBulkEnableButton(false)
            animateToProgress(0)
            binding.timeupIcon.visibility = View.VISIBLE
            //delayNext(TIME_UP_DELAY_TIME)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                val dataJsonArray = JSONArray(result)
              //  val quizJson = data.getJSONObject()
                val dataJson = dataJsonArray.getJSONObject(0)
                val question = dataJson.getString("question")

                binding.quizText.text = question
            }
        }
    }

    private fun is2String(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
        return reader.readText()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

}