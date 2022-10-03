package jm.music.client.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import jm.music.client.MusicProxy
import jm.music.client.databinding.FragmentNotificationsBinding
import jm.music.client.ui.BaseFragment
import jm.music.server.AbsMediaViewModel
import jm.music.server.LLog

class NotificationsFragment : BaseFragment() {

    companion object {
        private const val TAG = "NotificationsFragment"
    }
    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    private val notificationsViewModel by viewModels<NotificationsViewModel> {
        AbsMediaViewModel.Factory("notify", MusicProxy.helper)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        notificationsViewModel.mediaItems.observe(viewLifecycleOwner) {
            it.forEach { item ->
                LLog.i(TAG, "item:$item")
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
