package jm.music.client.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import jm.music.server.LLog

abstract class BaseFragment: Fragment() {
    companion object {
        private const val TAG = "BaseFragment"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LLog.i(TAG,"onCreate: $this")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LLog.i(TAG,"onViewCreated: $this")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LLog.i(TAG,"onCreateView: $this")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        LLog.i(TAG,"onAttach: $this")
    }
    override fun onStart() {
        super.onStart()
        LLog.i(TAG,"onStart: $this")
    }
    override fun onResume() {
        super.onResume()
        LLog.i(TAG,"onResume: $this")
    }

    override fun onPause() {
        super.onPause()
        LLog.i(TAG,"onPause: $this")
    }

    override fun onStop() {
        super.onStop()
        LLog.i(TAG,"onStop: $this")
    }

    override fun onDetach() {
        super.onDetach()
        LLog.i(TAG,"onDetach: $this")
    }
    override fun onDestroyView() {
        super.onDestroyView()
        LLog.i(TAG,"onDestroyView: $this")
    }
    override fun onDestroy() {
        super.onDestroy()
        LLog.i(TAG,"onDestroy: $this")
    }
}
