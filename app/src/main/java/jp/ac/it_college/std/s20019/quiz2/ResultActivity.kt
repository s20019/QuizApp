package jp.ac.it_college.std.s20019.quiz2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.ac.it_college.std.s20019.quiz2.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val receiveIntent = getIntent()
        val correctCount = receiveIntent.getStringExtra("CORRECTCOUNT")?.toInt()
        binding.pointText.text = getString(R.string.point_format, correctCount)

        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}