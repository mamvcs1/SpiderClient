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

    private val black = Color.rgb(8, 8, 12)
    private val panelColor = Color.rgb(19, 17, 24)
    private val red = Color.rgb(255, 35, 65)
    private val white = Color.rgb(245, 245, 250)
    private val gray = Color.rgb(150, 145, 160)
    private val green = Color.rgb(75, 220, 145)

    private lateinit var statusText: TextView
    private lateinit var connectButton: GlassButton
    private lateinit var disconnectButton: GlassButton
    private lateinit var configInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = black
        window.navigationBarColor = black
        window.decorView.systemUiVisibility = 0

        createInterface()
    }

    private fun createInterface() {

        val root = FrameLayout(this).apply {
            setBackgroundColor(black)
        }

        val backgroundGlow = View(this).apply {
            setBackground(
                GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(
                        Color.rgb(65, 8, 24),
                        black,
                        black
                    )
                )
            )
        }

        root.addView(
            backgroundGlow,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(360),
                Gravity.TOP
            )
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(28))
        }

        scroll.addView(
            container,
            ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        // Logo

        val logo = TextView(this).apply {
            text = "SPIDER"
            textSize = 36f
            letterSpacing = 0.20f
            gravity = Gravity.CENTER
            setTextColor(red)
            typeface = Typeface.create(
                "sans-serif-black",
                Typeface.BOLD
            )
        }

        container.addView(
            logo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(62)
            )
        )

        logo.alpha = 0f
        logo.translationY = dp(-15).toFloat()

        logo.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(650)
            .setInterpolator(OvershootInterpolator())
            .start()

        // Subtitle

        val subtitle = makeText(
            "CLIENT  /  SECURE CONNECTION",
            10f,
            gray,
            Typeface.DEFAULT_BOLD
        )

        subtitle.letterSpacing = 0.18f

        container.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        // Main status card

        val statusCard = createCard()

        statusCard.setPadding(
            dp(20),
            dp(20),
            dp(20),
            dp(20)
        )

        val cardTitle = makeText(
            "CONNECTION STATUS",
            11f,
            gray,
            Typeface.DEFAULT_BOLD
        )

        statusCard.addView(
            cardTitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        )

        statusText = makeText(
            "● DISCONNECTED",
            18f,
            red,
            Typeface.DEFAULT_BOLD
        )

        statusText.gravity = Gravity.CENTER_VERTICAL

        statusCard.addView(
            statusText,
