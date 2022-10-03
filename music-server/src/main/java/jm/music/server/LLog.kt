package jm.music.server

import android.util.Log

object LLog {
    fun v(t: String, msg: String) {
        Log.v("XXXX-$t", msg)
    }

    fun i(t: String, msg: String, th: Throwable? = null) {
        if (th == null) {
            Log.i("XXXX-$t", msg)
        } else {
            Log.i("XXXX-$t", msg, th)
        }
    }

    fun d(t: String, msg: String) {
        Log.d("XXXX-$t", msg)
    }

    fun w(t: String, msg: String) {
        Log.w("XXXX-$t", msg)
    }

    fun e(t: String, msg: String) {
        Log.e("XXXX-$t", msg)
    }
}
