package com.mgexample.bluetooth.remotecontrol

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer
import java.util.*

object Utils {
    fun delay(secs: Int, delayCallback: DelayCallback) {
        val handler = Handler()
        handler.postDelayed(
            Runnable { delayCallback.afterDelay() },
            (secs * 1000).toLong()
        ) // afterDelay will be executed after (secs*1000) milliseconds.
    }

    // Delay mechanism
    interface DelayCallback {
        fun afterDelay()
    }

    fun Vibrate(context: Context, time: Long = 200) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(time)
        }
    }

    fun setFloor(leftT: String, rightT: String, side: Int) {
        val Left: TextView = (Settings.MainActivity as MainActivity).left
        val Right: TextView = (Settings.MainActivity as MainActivity).right
        var left = ""
        var right = ""
        //Here fix value
        left = when (leftT) {
            "10" -> {
                "9"
            }
            "-1" -> {
                "0"
            }
            else -> {
                leftT
            }
        }
        right = when (rightT) {
            "10" -> {
                "9"
            }
            "-1" -> {
                "0"
            }
            else -> {
                rightT
            }
        }
        var floor = left.toInt() * 10 + right.toInt()
        if (floor > Settings.floorMax) {
            floor = Settings.floorMax
        }
        if (floor < Settings.floorMin) {
            floor = Settings.floorMin
        }
        left = (floor / 10).toString()
        right = (floor % 10).toString()
        if (Left.text != left) {
            val before = ValueAnimator.ofFloat(0f, Left.height.toFloat() * side)

            before.interpolator = AccelerateInterpolator(1.5f)
            before.duration = 200

            before.addUpdateListener {
                val params: ViewGroup.MarginLayoutParams =
                    Left.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = it.animatedValue.toString().toFloat().toInt()
                Left.layoutParams = params

            }

            val after = ValueAnimator.ofFloat(-Left.height.toFloat() * side, 0f)

            after.interpolator = AccelerateInterpolator(1.5f)
            after.duration = 200

            after.addUpdateListener {
                val params: ViewGroup.MarginLayoutParams =
                    Left.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = it.animatedValue.toString().toFloat().toInt()
                Left.layoutParams = params

                Left.text = left
            }

            val animatorSet = AnimatorSet()
            animatorSet.play(before).before(after)
            animatorSet.start()

        }

        if (Right.text != right) {
            val before = ValueAnimator.ofFloat(0f, Right.height.toFloat() * side)

            before.interpolator = AccelerateInterpolator(1.5f)
            before.duration = 200

            before.addUpdateListener {
                val params: ViewGroup.MarginLayoutParams =
                    Right.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = it.animatedValue.toString().toFloat().toInt()
                Right.layoutParams = params

            }
            val after = ValueAnimator.ofFloat(-Right.height.toFloat() * side, 0f)

            after.interpolator = AccelerateInterpolator(1.5f)
            after.duration = 200

            after.addUpdateListener {
                val params: ViewGroup.MarginLayoutParams =
                    Right.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = it.animatedValue.toString().toFloat().toInt()
                Right.layoutParams = params

                Right.text = right
            }

            val animatorSet = AnimatorSet()
            animatorSet.play(before).before(after)
            animatorSet.start()
        }
    }
}