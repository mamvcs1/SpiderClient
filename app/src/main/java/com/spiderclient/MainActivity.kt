package com.spiderclient

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val red = Color.rgb(255, 35, 55)
    private val white = Color.WHITE
    private val muted = Color.rgb(175, 180, 195)

    private lateinit var status: TextView
    private lateinit var connectButton: GlassButton
    private lateinit var disconnectButton: GlassButton
    private lateinit var configInput: EditText

    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 8, 12)
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(5, 5, 9))
        }

        // Background image
        val background = android.widget.ImageView(this).apply {
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)

            try {
                setImageResource(R.drawable.spider_bg)
            } catch (_: Exception) {
                // The black background remains if no image is available.
            }

            alpha = 0.82f
            contentDescription = "Spider background"
        }

        root.addView(
            background,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Dark overlay for readability
        val overlay = View(this).apply {
            setBackgroundColor(Color.argb(125, 0, 0, 0))
        }

        root.addView(
            overlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(18), dp(24), dp(18), dp(24))
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Header
        val logo = TextView(this).apply {
            text = "SPIDER"
            textSize = 36f
            letterSpacing = 0.18f
            type