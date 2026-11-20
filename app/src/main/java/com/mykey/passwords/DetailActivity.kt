package com.mykey.passwords

import android.app.Activity
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Random

class DetailActivity : Activity() {

    private lateinit var db: Db
    private lateinit var root: LinearLayout
    private var id = 0L
    private var animateIntro = true

    // Har password field ke liye alag hide/show state. Shuru me sab VISIBLE.
    private val hidden = HashMap<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        id = intent.getLongExtra("id", 0L)

        window.decorView.setBackgroundColor(Theme.BG)
        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(16), dp(16), dp(16), dp(32))
        }
        val sv = ScrollView(this)
        sv.setBackgroundColor(Theme.BG)
        sv.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.nd_pop_enter, R.anim.nd_pop_exit)
    }

    private var cardIndex = 0

    private fun render() {
        root.removeAllViews()
        val a = db.get(id) ?: run { finish(); return }
        title = a.label
        cardIndex = 0

        // Header: back button + label (dot-matrix)
        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).nd(22f, Theme.TEXT, 900).apply {
            text = "<"
            gravity = Gravity.CENTER
            background = Theme.pill(this@DetailActivity, false)
            setOnClickListener { finish() }
            pressable()
        }
        head.addView(back, LinearLayout.LayoutParams(dp(46), dp(46)))
        head.addView(TextView(this).nd(26f, Theme.TEXT, 900, 0.02f).apply {
            text = a.label.uppercase()
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14) })
        head.addView(dotView(10, Theme.ACCENT))
        root.addView(head)

        // Username / Email / Phone
        block("USERNAME / EMAIL / PHONE", a.username, "Last changed: ${fmtDate(a.usernameDate)}", null)
        bigButton("Edit Username / Label") { editUsernameDialog(a) }

        // Current password
        block("CURRENT PASSWORD", a.cur, "Last changed: ${fmtDate(a.curDate)}", "cur")
        bigButton("Edit Password") { editPasswordDialog() }

        // History
        block("PREVIOUS PASSWORD 1", a.p1, "Changed on: ${fmtDate(a.p1Date)}", "p1")
        block("PREVIOUS PASSWORD 2", a.p2, "Changed on: ${fmtDate(a.p2Date)}", "p2")

        animateIntro = false
    }

    /** Button ko theme ka look dena. primary = white pill, warna outlined pill. */
    private fun styleButton(b: Button, primary: Boolean) {
        b.isAllCaps = false
        b.typeface = Theme.font(this, 800)
        b.setTextColor(if (primary) Color.BLACK else Theme.TEXT)
        b.background = Theme.pill(this, primary)
        b.stateListAnimator = null
        b.pressable()
    }

    private fun smallLp(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(14), dp(3), 0) }

    private fun dialog(): AlertDialog.Builder = dotDialog()

    /**
     * Dot-matrix "decode" effect: text random characters se shuru hota hai aur
     * left se right ek-ek karke asli password ban jata hai.
     */
    private fun scrambleTo(tv: TextView, target: String) {
        if (target.isEmpty()) return
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#@%&*"
        val rnd = Random()
        ValueAnimator.ofInt(0, target.length).apply {
            duration = (200L + 45L * target.length).coerceAtMost(750L)
            addUpdateListener {
                val k = it.animatedValue as Int
                val sb = StringBuilder(target.length)
                for (i in target.indices) {
                    sb.append(if (i < k || target[i] == ' ') target[i] else chars[rnd.nextInt(chars.length)])
                }
                tv.text = sb.toString()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) { tv.text = target }
            })
            start()
        }
    }

    /** Ek field ka card: title, value, date, Copy button aur (password ho to) Hide/Show button. */
    private fun block(title: String, value: String, dateText: String, key: String?) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Theme.card(this@DetailActivity)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(16)
        card.layoutParams = lp

        card.addView(TextView(this).nd(13f, Theme.ACCENT, 800, 0.12f).apply { text = title })

        val valueView = TextView(this).nd(22f, Theme.TEXT, 800).apply {
            text = if (value.isEmpty()) "-" else value
            setTextIsSelectable(true)
            setPadding(0, dp(8), 0, dp(4))
        }
        card.addView(valueView)

        card.addView(TextView(this).nd(14f, Theme.TEXT_DIM, 600).apply { text = dateText })

        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        val copy = Button(this).apply {
            text = "Copy"
            setOnClickListener {
                if (value.isEmpty()) {
                    toast("Copy karne ke liye kuch nahi hai")
                } else {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("key", value))
                    toast("Copied")
                }
            }
        }
        styleButton(copy, false)
        buttons.addView(copy, smallLp())

        if (key != null) {
            val eye = Button(this)
            styleButton(eye, false)
            fun refreshEye() {
                val h = hidden[key] == true
                valueView.transformationMethod = if (h) PasswordTransformationMethod.getInstance() else null
                eye.text = if (h) "Show" else "Hide"
            }
            eye.setOnClickListener {
                hidden[key] = !(hidden[key] == true)
                refreshEye()
                if (hidden[key] != true) scrambleTo(valueView, value) // Show par decode animation
            }
            refreshEye()
            if (animateIntro && hidden[key] != true) scrambleTo(valueView, value)
            buttons.addView(eye, smallLp())

            val fix = Button(this).apply {
                text = "Fix"
                setOnClickListener { fixDialog(key, title, value) }
            }
            styleButton(fix, false)
            buttons.addView(fix, smallLp())
        }
        card.addView(buttons)
        root.addView(card)
        if (animateIntro) card.revealIn(cardIndex++)
    }

    private fun bigButton(text: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            this.text = text
            textSize = 17f
            setOnClickListener { onClick() }
        }
        styleButton(b, true)
        root.addView(b, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14) })
        if (animateIntro) b.revealIn(cardIndex++)
    }

    /** Galat likha password sudhrna: history shift nahi hoti, date wahi rehti hai. */
    private fun fixDialog(key: String, fieldTitle: String, oldValue: String) {
        val input = EditText(this).apply {
            setText(oldValue)
            setSelection(oldValue.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        Theme.skin(input)
        dialog()
            .setTitle("Fix: $fieldTitle")
            .setMessage("Sirf galti sudharne ke liye. Date aur history waisi hi rahegi.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                db.fixPassword(id, key, input.text.toString())
                toast("Sudhar diya")
                render()
            }
            .setNegativeButton("Cancel", null)
            .showDot()
    }

    private fun editPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Naya password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        dialog()
            .setTitle("Edit Password")
            .setMessage("Purana current password, Previous 1 ban jayega aur Previous 1, Previous 2.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val pw = input.text.toString()
                if (pw.isBlank()) {
                    toast("Password khaali hai, kuch nahi badla")
                } else {
                    db.changePassword(id, pw)
                    toast("Password badal gaya")
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .showDot()
    }

    private fun editUsernameDialog(a: Account) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val label = EditText(this).apply {
            hint = "Label (jaise Instagram 1)"
            setText(a.label)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val user = EditText(this).apply {
            hint = "Username / Email / Phone"
            setText(a.username)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        box.addView(label)
        box.addView(user, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        dialog()
            .setTitle("Edit Username / Label")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val l = label.text.toString().trim().ifEmpty { a.label }
                db.updateUsername(id, l, user.text.toString().trim())
                toast("Save ho gaya")
                render()
            }
            .setNegativeButton("Cancel", null)
            .showDot()
    }
}
