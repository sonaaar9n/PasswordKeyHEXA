package com.mykey.passwords

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class DetailActivity : Activity() {

    private lateinit var db: Db
    private lateinit var root: LinearLayout
    private var id = 0L

    // Har password field ke liye alag hide/show state. Shuru me sab VISIBLE.
    private val hidden = HashMap<String, Boolean>()

    // --- Apple iOS Dark Theme Color Palette ---
    private val COLOR_BG = Color.parseColor("#000000")        // OLED Pure Black
    private val COLOR_CARD_BG = Color.parseColor("#1c1c1e")   // iOS Dark Gray Surface
    private val COLOR_BORDER = Color.parseColor("#38383a")    // Subtle Border
    private val COLOR_TEXT_PRIMARY = Color.parseColor("#f5f5f7") // Off-White
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#86868b") // Muted Gray
    private val COLOR_ACCENT = Color.parseColor("#0a84ff")    // iOS Blue
    private val COLOR_BUTTON_BG = Color.parseColor("#2c2c2e") // Secondary Button Gray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        id = intent.getLongExtra("id", 0L)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(32))
            setBackgroundColor(COLOR_BG)
        }
        val sv = ScrollView(this).apply {
            setBackgroundColor(COLOR_BG)
        }
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()
        val a = db.get(id) ?: run { finish(); return }
        title = a.label

        // Username / Email / Phone
        block("USERNAME / EMAIL / PHONE", a.username, "Last changed: ${fmtDate(a.usernameDate)}", null)
        bigButton("Edit Username / Label") { editUsernameDialog(a) }

        // Current password
        block("CURRENT PASSWORD", a.cur, "Last changed: ${fmtDate(a.curDate)}", "cur")
        bigButton("Edit Password") { editPasswordDialog() }

        // History
        block("PREVIOUS PASSWORD 1", a.p1, "Changed on: ${fmtDate(a.p1Date)}", "p1")
        block("PREVIOUS PASSWORD 2", a.p2, "Changed on: ${fmtDate(a.p2Date)}", "p2")
    }

    /** Ek field ka card: title, value, date, Copy button aur (password ho to) Hide/Show button. */
    private fun block(title: String, value: String, dateText: String, key: String?) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(COLOR_CARD_BG)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(14)
        card.layoutParams = lp

        card.addView(TextView(this).apply {
            text = title
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(COLOR_TEXT_SECONDARY)
        })

        val valueView = TextView(this).apply {
            text = if (value.isEmpty()) "-" else value
            textSize = 18f
            setTextColor(COLOR_TEXT_PRIMARY)
            setTextIsSelectable(true)
            setPadding(0, dp(6), 0, dp(4))
        }
        card.addView(valueView)

        card.addView(TextView(this).apply {
            text = dateText
            textSize = 12f
            setTextColor(COLOR_TEXT_SECONDARY)
        })

        val buttons = LinearLayout(this).apply { 
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }

        val copy = Button(this).apply {
            text = "Copy"
            setTextColor(COLOR_ACCENT)
            setBackgroundColor(COLOR_BUTTON_BG)
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
        buttons.addView(copy, LinearLayout.LayoutParams(0, dp(40), 1f).apply { rightMargin = dp(6) })

        if (key != null) {
            val eye = Button(this).apply {
                setTextColor(COLOR_TEXT_PRIMARY)
                setBackgroundColor(COLOR_BUTTON_BG)
            }
            fun refreshEye() {
                val h = hidden[key] == true
                valueView.transformationMethod = if (h) PasswordTransformationMethod.getInstance() else null
                eye.text = if (h) "Show" else "Hide"
            }
            eye.setOnClickListener {
                hidden[key] = !(hidden[key] == true)
                refreshEye()
            }
            refreshEye()
            buttons.addView(eye, LinearLayout.LayoutParams(0, dp(40), 1f).apply { 
                rightMargin = dp(6) 
                leftMargin = dp(6) 
            })

            val fix = Button(this).apply {
                text = "Fix"
                setTextColor(COLOR_TEXT_PRIMARY)
                setBackgroundColor(COLOR_BUTTON_BG)
                setOnClickListener { fixDialog(key, title, value) }
            }
            buttons.addView(fix, LinearLayout.LayoutParams(0, dp(40), 1f).apply { leftMargin = dp(6) })
        }
        card.addView(buttons)
        root.addView(card)
    }

    private fun bigButton(text: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setBackgroundColor(COLOR_ACCENT)
            setOnClickListener { onClick() }
        }
        val lp = LinearLayout.LayoutParams(-1, dp(48))
        lp.topMargin = dp(14)
        root.addView(b, lp)
    }

    /** Galat likha password sudharna: history shift nahi hoti, date wahi rehti hai. */
    private fun fixDialog(key: String, fieldTitle: String, oldValue: String) {
        val input = EditText(this).apply {
            setText(oldValue)
            setSelection(oldValue.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setTextColor(Color.BLACK)
        }
        AlertDialog.Builder(this)
            .setTitle("Fix: $fieldTitle")
            .setMessage("Sirf galti sudharne ke liye. Date aur history waisi hi rahegi.")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                db.fixPassword(id, key, input.text.toString())
                toast("Sudhar diya")
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Naya password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setTextColor(Color.BLACK)
        }
        AlertDialog.Builder(this)
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
            .show()
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
            setTextColor(Color.BLACK)
        }
        val user = EditText(this).apply {
            hint = "Username / Email / Phone"
            setText(a.username)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTions
            setTextColor(Color.BLACK)
        }
        box.addView(label)
        box.addView(user)
        AlertDialog.Builder(this)
            .setTitle("Edit Username / Label")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val l = label.text.toString().trim().ifEmpty { a.label }
                db.updateUsername(id, l, user.text.toString().trim())
                toast("Save ho gaya")
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
