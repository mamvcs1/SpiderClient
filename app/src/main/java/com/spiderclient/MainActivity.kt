package com.spiderclient

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType

class MainActivity : Activity() {

    private val red = Color.rgb(255, 35, 55)
    private val white = Color.WHITE
    private val muted = Color.rgb(175, 180, 195)

    private lateinit var status: TextView
    private lateinit var connectButton: GlassButton
    private lateinit var disconnectButton: GlassButton
    private lateinit var configInput: EditText

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 8, 12)
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(5, 5, 9))
        }

        val background = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(35, 5, 12),
                    Color.rgb(5, 5, 10),
                    Color.rgb(20, 5, 10)
                )
            )
        }

        root.addView(
            background,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(20), dp(22), dp(20), dp(24))
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val logo = TextView(this).apply {
            text = "SPIDER"
            textSize = 36f
            letterSpacing = 0.2f
            setTextColor(red)
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
            gravity = Gravity.CENTER
        }

        page.addView(logo, itemParams(65))

        val subtitle = TextView(this).apply {
            text = "SECURE CONNECTION CLIENT"
            textSize = 10f
            letterSpacing = 0.18f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }

        page.addView(subtitle, itemParams(28))

        val statusCard = glassCard()

        val statusLabel = TextView(this).apply {
            text = "SYSTEM STATUS"
            textSize = 11f
            letterSpacing = 0.15f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }

        statusCard.addView(statusLabel, itemParams(30))

        status = TextView(this).apply {
            text = "● DISCONNECTED"
            textSize = 20f
            setTextColor(red)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        statusCard.addView(status, itemParams(48))

        val statusParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(100)
        )
        statusParams.topMargin = dp(24)
        statusParams.bottomMargin = dp(24)

        page.addView(statusCard, statusParams)

        val configTitle = TextView(this).apply {
            text = "SERVER CONFIGURATION"
            textSize = 12f
            letterSpacing = 0.08f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
        }

        page.addView(configTitle, itemParams(32))

        configInput = EditText(this).apply {
            hint = "Paste configuration here..."
            setHintTextColor(Color.rgb(115, 115, 130))
            setTextColor(white)
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            maxLines = 6
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(
                Color.rgb(18, 18, 27),
                Color.rgb(100, 35, 50),
                14
            )
        }

        val inputParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(130)
        )
        inputParams.bottomMargin = dp(22)

        page.addView(configInput, inputParams)

        connectButton = GlassButton(
            "⚡  CONNECT",
            red
        )

        val connectParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(58)
        )
        connectParams.bottomMargin = dp(14)

        page.addView(connectButton, connectParams)

        disconnectButton = GlassButton(
            "DISCONNECT",
            Color.rgb(55, 20, 30)
        )

        val disconnectParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(52)
        )
        disconnectParams.bottomMargin = dp(24)

        page.addView(disconnectButton, disconnectParams)

        connectButton.setOnClickListener {
            animatePress(connectButton)

            if (configInput.text.toString().trim().isEmpty()) {
                status.text = "● CONFIG REQUIRED"
                status.setTextColor(red)
            } else {
                status.text = "● DEMO MODE"
                status.setTextColor(Color.rgb(255, 185, 60))
            }
        }

        disconnectButton.setOnClickListener {
            animatePress(disconnectButton)
            status.text = "● DISCONNECTED"
            status.setTextColor(red)
        }

        val footer = TextView(this).apply {
            text = "SPIDER CLIENT  •  ANDROID"
            textSize = 10f
            letterSpacing = 0.12f
            setTextColor(Color.rgb(115, 95, 105))
            gravity = Gravity.CENTER
        }

        page.addView(footer, itemParams(36))

        scroll.addView(
            page,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        page.alpha = 0f
        page.translationY = dp(18).toFloat()
        page.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator(0.7f))
            .start()
    }

    private fun itemParams(height: Int): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(height)
        )
    }

    private fun glassCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(
                Color.rgb(20, 14, 22),
                Color.rgb(115, 30, 48),
                18
            )
        }
    }

    private fun rounded(
        fill: Int,
        stroke: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun animatePress(view: View) {
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }

    inner class GlassButton(
        label: String,
        accent: Int
    ) : TextView(this) {

        init {
            text = label
            textSize = 14f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            isClickable = true
            isFocusable = true

            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.rgb(65, 20, 35),
                    Color.rgb(22, 15, 24),
                    Color.rgb(35, 12, 22)
                )
            ).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), accent)
            }

            elevation = dp(5).toFloat()
        }
    }
}
