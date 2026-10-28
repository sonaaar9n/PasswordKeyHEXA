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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        id = intent.getLongExtra("id", 0L)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(32))
        }
        val sv = ScrollView(this)
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
            setBackgroundColor(Color.parseColor("#ECEFF1"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.topMargin = dp(16)
        card.layoutParams = lp

        card.addView(TextView(this).apply {
            text = title
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#546E7A"))
        })

        val valueView = TextView(this).apply {
            text = if (value.isEmpty()) "-" else value
            textSize = 20f
            setTextColor(Color.BLACK)
            setTextIsSelectable(true)
            setPadding(0, dp(6), 0, dp(2))
        }
        card.addView(valueView)

        card.addView(TextView(this).apply {
            text = dateText
            textSize = 13f
            setTextColor(Color.DKGRAY)
        })

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
        buttons.addView(copy, LinearLayout.LayoutParams(0, dp(48), 1f))

        if (key != null) {
            val eye = Button(this)
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
            buttons.addView(eye, LinearLayout.LayoutParams(0, dp(48), 1f))

            val fix = Button(this).apply {
                text = "Fix"
                setOnClickListener { fixDialog(key, title, value) }
            }
            buttons.addView(fix, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        card.addView(buttons)
        root.addView(card)
    }

    private fun bigButton(text: String, onClick: () -> Unit) {
        val b = Button(this).apply {
            this.text = text
            textSize = 16f
            setOnClickListener { onClick() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1, dp(52)))
    }

    /** Galat likha password sudharna: history shift nahi hoti, date wahi rehti hai. */
    private fun fixDialog(key: String, fieldTitle: String, oldValue: String) {
        val input = EditText(this).apply {
            setText(oldValue)
            setSelection(oldValue.length)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(20), dp(16), dp(20), dp(16))
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
            setPadding(dp(20), dp(16), dp(20), dp(16))
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
        }
        val user = EditText(this).apply {
            hint = "Username / Email / Phone"
            setText(a.username)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
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
