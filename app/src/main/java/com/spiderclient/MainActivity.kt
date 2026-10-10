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
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val black = Color.rgb(8, 8, 12)
    private val panel = Color.rgb(20, 17, 25)
    private val red = Color.rgb(255, 35, 65)
    private val white = Color.rgb(245, 245, 250)
    private val gray = Color.rgb(155, 150, 165)

    private lateinit var statusText: TextView
    private lateinit var configInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = black
        window.navigationBarColor = black

        buildUI()
    }

    private fun buildUI() {
        val root = FrameLayout(this)
        root.setBackgroundColor(black)

        val glow = View(this)
        glow.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(75, 8, 28),
                black,
                black
            )
        )

        root.addView(
            glow,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(360),
                Gravity.TOP
            )
        )

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.clipToPadding = false

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL
        content.setPadding(
            dp(24),
            dp(24),
            dp(24),
            dp(30)
        )

        scroll.addView(
            content,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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

        val logo = TextView(this)
        logo.text = "SPIDER"
        logo.textSize = 36f
        logo.letterSpacing = 0.20f
        logo.gravity = Gravity.CENTER
        logo.setTextColor(red)
        logo.typeface = Typeface.create(
            "sans-serif-black",
            Typeface.BOLD
        )

        content.addView(
            logo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(65)
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

        val subtitle = makeText(
            "CLIENT  /  SECURE CONNECTION",
            10f,
            gray,
            Typeface.DEFAULT_BOLD
        )
        subtitle.gravity = Gravity.CENTER

        content.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        val statusCard = createCard()
        statusCard.setPadding(
            dp(18),
            dp(16),
            dp(18),
            dp(16)
        )

        statusCard.addView(
            makeText(
                "CONNECTION STATUS",
                11f,
                gray,
                Typeface.DEFAULT_BOLD
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(26)
            )
        )

        statusText = makeText(
            "● DISCONNECTED",
            18f,
            red,
            Typeface.DEFAULT_BOLD
        )

        statusCard.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(44)
            )
        )

        statusCard.addView(
            makeText(
                "Ready to configure your connection",
                12f,
                gray
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(25)
            )
        )

        content.addView(
            statusCard,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(28)
            }
        )
                val configTitle = makeText(
            "CONFIGURATION",
            12f,
            white,
            Typeface.DEFAULT_BOLD
        )

        content.addView(
            configTitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(18)
            }
        )

        configInput = EditText(this).apply {
            hint = "Paste your configuration here..."
            setHintTextColor(Color.rgb(105, 100, 115))
            setTextColor(white)
            textSize = 13f
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            minLines = 4
            maxLines = 7
            setPadding(dp(14), dp(14), dp(14), dp(14))

            background = GradientDrawable().apply {
                setColor(Color.rgb(17, 15, 22))
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.rgb(75, 30, 45))
            }
        }

        content.addView(
            configInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(140)
            )
        )

        val connect = GlassButton("⚡  CONNECT", red)

        content.addView(
            connect,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                topMargin = dp(22)
            }
        )

        connect.setOnClickListener {
            animatePress(connect)

            if (configInput.text.toString().trim().isEmpty()) {
                statusText.text = "● CONFIG REQUIRED"
                statusText.setTextColor(red)
            } else {
                statusText.text = "● DEMO MODE"
                statusText.setTextColor(
                    Color.rgb(255, 190, 75)
                )
            }
        }

        val disconnect = GlassButton(
            "DISCONNECT",
            Color.rgb(100, 35, 50)
        )

        content.addView(
            disconnect,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(12)
            }
        )

        disconnect.setOnClickListener {
            animatePress(disconnect)
            statusText.text = "● DISCONNECTED"
            statusText.setTextColor(red)
        }

        val info = createCard()
        info.setPadding(dp(16), dp(14), dp(16), dp(14))

        info.addView(
            makeText(
                "SPIDER SYSTEM",
                12f,
                red,
                Typeface.DEFAULT_BOLD
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30)
            )
        )

        info.addView(
            makeText(
                "Interface: Ready\n" +
                    "Engine: Not connected\n" +
                    "VPN Service: Not active",
                12f,
                gray
            ).apply {
                setLineSpacing(dp(5).toFloat(), 1f)
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            info,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(24)
            }
        )

        val footer = makeText(
            "SPIDER CLIENT  •  ANDROID",
            10f,
            Color.rgb(95, 85, 100),
            Typeface.DEFAULT_BOLD
        )
        footer.gravity = Gravity.CENTER

        content.addView(
            footer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        content.alpha = 0f
        content.translationY = dp(15).toFloat()

        content.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .start()
    } 
        private fun createCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.rgb(65, 28, 42))
            }

            elevation = dp(5).toFloat()
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        typeface: Typeface = Typeface.DEFAULT
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            this.typeface = typeface
            gravity = Gravity.CENTER_VERTICAL
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
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    inner class GlassButton(
        label: String,
        accent: Int
    ) : TextView(this@MainActivity) {

        init {
            text = label
            textSize = 14f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER

            setTextColor(white)

            typeface = Typeface.create(
                "sans-serif",
                Typeface.BOLD
            )

            isClickable = true
            isFocusable = true

            val buttonBackground = GradientDrawable(
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

            setBackground(buttonBackground)
            elevation = dp(5).toFloat()
        }
    }
}
