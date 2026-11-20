package com.mykey.passwords

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        // Splash sirf app ke cold start par chale, back dabake wapas aane par nahi
        private var splashShown = false
    }

    private lateinit var db: Db
    private lateinit var list: LinearLayout
    private lateinit var counter: TextView
    private lateinit var content: View
    private var pendingBackup = -1
    private var query = ""
    private var splashActive = false
    private var introDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        title = "Password Key"

        window.decorView.setBackgroundColor(Theme.BG)
        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(16), dp(20), dp(16), dp(28))
            isFocusableInTouchMode = true
        }

        // Header: bada dot-matrix title + red full stop + account counter
        val titleSpan = SpannableStringBuilder("PASSKEY").apply {
            val s = length
            append(".")
            setSpan(ForegroundColorSpan(Theme.ACCENT), s, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        root.addView(TextView(this).nd(44f, Theme.TEXT, 900, 0.02f).apply { text = titleSpan })
        counter = TextView(this).nd(14f, Theme.TEXT_DIM, 700, 0.12f)
        root.addView(counter, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })

        // Backup button: white pill (Nothing primary button)
        val backupBtn = Button(this).apply {
            text = "Backup (Excel / PDF)"
            textSize = 17f
            isAllCaps = false
            typeface = Theme.font(this@MainActivity, 800)
            setTextColor(Color.BLACK)
            background = Theme.pill(this@MainActivity, true)
            stateListAnimator = null
            setOnClickListener { chooseBackup() }
            pressable()
        }
        root.addView(backupBtn, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(22) })

        // Search box (backup button ke neeche), outlined pill
        val search = EditText(this).apply {
            hint = "Search (platform, label, username)"
            setHintTextColor(Theme.TEXT_DIM)
            setTextColor(Theme.TEXT)
            typeface = Theme.font(this@MainActivity, 700)
            textSize = 16f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(20), 0, dp(20), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(40).toFloat()
                setColor(Theme.SURFACE)
                setStroke(dp(1), Theme.DIVIDER)
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString() ?: ""
                    render()
                }
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)

        val sv = ScrollView(this)
        sv.setBackgroundColor(Theme.BG)
        sv.overScrollMode = View.OVER_SCROLL_NEVER
        sv.addView(root)
        content = sv

        val frame = FrameLayout(this)
        frame.setBackgroundColor(Theme.BG)
        frame.addView(sv, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        root.requestFocus()

        // Cold start par dot-matrix splash. App content tab tak chhupa rehta hai.
        if (!splashShown && savedInstanceState == null) {
            splashShown = true
            splashActive = true
            content.alpha = 0f
            val splash = DotSplashView(
                this,
                onReveal = { revealContent() },
                onDone = { frame.removeView(frame.findViewWithTag("splash")) }
            )
            splash.tag = "splash"
            frame.addView(splash, FrameLayout.LayoutParams(-1, -1))
        }
    }

    /** Splash dissolve hote hi app andar aati hai: content fade-in + rows stagger. */
    private fun revealContent() {
        splashActive = false
        content.animate().alpha(1f).setDuration(500).start()
        introDone = false
        render(animate = true)
    }

    override fun onResume() {
        super.onResume()
        render(animate = !splashActive && !introDone)
    }

    private fun dialog(): AlertDialog.Builder = dotDialog()

    private fun render(animate: Boolean = false) {
        list.removeAllViews()
        val all = db.all()
        counter.text = "[ ${all.size} ACCOUNTS ]"
        val q = query.trim().lowercase()
        var lastPlatform = ""
        var shown = 0
        var idx = 0
        for (a in all) {
            // Search: platform, label ya username me match ho to dikhao (password me nahi dhundte)
            if (q.isNotEmpty() &&
                !(a.platform.lowercase().contains(q) ||
                    a.label.lowercase().contains(q) ||
                    a.username.lowercase().contains(q))
            ) continue

            if (a.platform != lastPlatform) {
                lastPlatform = a.platform
                val h = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(4), dp(24), 0, dp(8))
                }
                h.addView(dotView(7, Theme.ACCENT).apply {
                    (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(10)
                })
                h.addView(TextView(this).nd(15f, Theme.TEXT_DIM, 800, 0.12f).apply {
                    text = a.platform.uppercase()
                })
                if (animate) h.revealIn(idx++)
                list.addView(h)
            }
            val r = row(a)
            if (animate) r.revealIn(idx++)
            list.addView(r)
            shown++
        }
        if (animate) introDone = true
        if (q.isNotEmpty() && shown == 0) {
            list.addView(TextView(this).nd(15f, Theme.TEXT_DIM, 700).apply {
                text = "Koi account nahi mila"
                setPadding(dp(4), dp(24), 0, 0)
            })
        }
    }

    private fun row(a: Account): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Theme.card(this@MainActivity)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = TextView(this).nd(20f, Theme.TEXT, 800)
        name.text = a.label
        val user = TextView(this).nd(14f, Theme.TEXT_DIM, 600)
        user.text = if (a.username.isEmpty()) "(username set nahi)" else a.username
        box.addView(name)
        box.addView(user)
        box.pressable()
        box.setOnClickListener {
            startActivity(Intent(this, DetailActivity::class.java).putExtra("id", a.id))
            overridePendingTransition(R.anim.nd_enter, R.anim.nd_exit)
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(10)
        box.layoutParams = lp
        return box
    }

    // ---------- Backup ----------

    private fun chooseBackup() {
        dialog()
            .setTitle("Backup kaise save karu?")
            .setItems(arrayOf("Excel (.xlsx)", "PDF")) { _, which -> startBackup(which) }
            .showDot()
    }

    private fun startBackup(kind: Int) {
        if (Build.VERSION.SDK_INT < 29 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingBackup = kind
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        doBackup(kind)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 1 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED && pendingBackup >= 0) {
            doBackup(pendingBackup)
        } else if (code == 1) {
            toast("Storage permission nahi mili, backup nahi ban saka")
        }
        pendingBackup = -1
    }

    private fun doBackup(kind: Int) {
        try {
            val accounts = db.all()
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
            val path = if (kind == 0) {
                Exporter.save(this, "passwords_backup_$stamp.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") {
                    Exporter.xlsx(accounts, it)
                }
            } else {
                Exporter.save(this, "passwords_backup_$stamp.pdf", "application/pdf") {
                    Exporter.pdf(accounts, it)
                }
            }
            dialog()
                .setTitle("Backup ban gaya")
                .setMessage("Yahan save hua:\n$path")
                .setPositiveButton("OK", null)
                .showDot()
        } catch (e: Exception) {
            toast("Backup fail: ${e.message}")
        }
    }
}
