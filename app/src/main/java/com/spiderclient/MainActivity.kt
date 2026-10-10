package com.spiderclient

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType
import android.util.TypedValue

class MainActivity : Activity() {

    private val red = Color.rgb(255, 35, 55)
    private val white = Color.WHITE
    private val muted = Color.rgb(175, 180, 195)

    private lateinit var status: TextView
    private lateinit var connectButton: TextView
    private lateinit var disconnectButton: TextView
    private lateinit var configInput: EditText

    private var connected = false

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 8, 12)
        window.navigationBarColor = Color.BLACK

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(5, 5, 9))
        }

        val scroll = ScrollView(this).apply {
            is
