package jm.music.client.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import jm.music.client.MusicProxy
import jm.music.client.databinding.FragmentDashboardBinding
import jm.music.client.ui.BaseFragment
import jm.music.client.ui.home.HomeViewModel
import jm.music.server.AbsMediaViewModel
import jm.music.server.LLog

class DashboardFragment : BaseFragment() {

    companion object {
        private const val TAG = "DashboardFragment"
    }
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val dashboardViewModel by viewModels<DashboardViewModel> {
        AbsMediaViewModel.Factory("dash", MusicProxy.helper)
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        dashboardViewModel.isConnected.observe(viewLifecycleOwner) {
        }
        dashboardViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.forEach { item ->
                LLog.i(TAG,"item:$item")
            }
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
