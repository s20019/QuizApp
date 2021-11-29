package jp.ac.it_college.std.s20019.quiz2

import android.animation.ObjectAnimator
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import jp.ac.it_college.std.s20019.quiz2.databinding.FragmentQuizBinding

class QuizFragment : Fragment() {
    companion object {
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

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

}