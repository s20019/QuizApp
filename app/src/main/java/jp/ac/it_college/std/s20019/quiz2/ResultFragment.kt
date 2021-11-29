package jp.ac.it_college.std.s20019.quiz2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import jp.ac.it_college.std.s20019.quiz2.databinding.FragmentResultBinding

class ResultFragment : Fragment() {
    private val args by navArgs<ResultFragmentArgs>()

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NavController による想定外の移動をしないよう
        // 明示的に TitleFragment へ移動
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.action_to_title)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.pointText.text = getString(R.string.point_format, args.correctCount)
        val minutes = args.totalElapsedTime / 1000 / 60
        val second = args.totalElapsedTime / 1000 % 60
        val millis = args.totalElapsedTime % 1000 / 10
        binding.timeText.text = getString(
            R.string.time_format,
            minutes, second, millis
        )
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_title)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}