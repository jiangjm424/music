package jm.music.client.ui.home

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import jm.music.client.MusicProxy
import jm.music.client.databinding.FragmentHomeBinding
import jm.music.client.ui.BaseFragment
import jm.music.client.ui.notifications.NotificationsViewModel
import jm.music.server.AbsMediaViewModel
import jm.music.server.LLog
import java.util.*

class HomeFragment : BaseFragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val homeViewModel by viewModels<HomeViewModel> {
        AbsMediaViewModel.Factory("home", MusicProxy.helper)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        binding.bb.setOnClickListener {
            LLog.i(TAG, "play")
            homeViewModel.play()
        }
        binding.prev.setOnClickListener {
            LLog.i(TAG, "prev")
            homeViewModel.prev()
        }
        binding.next.setOnClickListener {
            LLog.i(TAG, "next")
            homeViewModel.next()
        }
        binding.repeatMode.setOnClickListener {
            val r = Random(System.currentTimeMillis()).nextInt(3)
            LLog.i(TAG,"reapeat mode:$r")
            homeViewModel.setRepeatMode(r)
        }
        binding.command.setOnClickListener {
            homeViewModel.command("home")
        }
        homeViewModel.rootMediaId.observe(viewLifecycleOwner) {
            LLog.i(TAG,"root id:$it")
            if (!it.isNullOrEmpty()){
//                homeViewModel.getList(it)
            }
        }
        homeViewModel.mediaItems.observe(viewLifecycleOwner) {
            LLog.i(TAG,"list size : ${it.size}")
            it?.forEach { item ->
                LLog.i(TAG,"item: $item")
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
