package com.mykey.passwords

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var db: Db
    private lateinit var list: LinearLayout
    private var pendingBackup = -1
    private var query = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark base theme (action bar aur dialogs ke liye)
        setTheme(android.R.style.Theme_DeviceDefault)
        super.onCreate(savedInstanceState)
        db = Db(this)
        title = "Password Key"

        window.decorView.setBackgroundColor(Theme.BG)
        window.statusBarColor = Theme.BG
        window.navigationBarColor = Theme.BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        actionBar?.setBackgroundDrawable(ColorDrawable(Theme.SURFACE))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(12), dp(12), dp(12), dp(24))
            isFocusableInTouchMode = true
        }

        // Backup button (pehle jaisa, apni jagah par)
        val backupBtn = Button(this).apply {
            text = "Backup (Excel / PDF)"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Theme.ACCENT)
            }
            setOnClickListener { chooseBackup() }
        }
        root.addView(backupBtn, LinearLayout.LayoutParams(-1, dp(56)))

        // Search box (backup button ke neeche)
        val search = EditText(this).apply {
            hint = "Search (platform, label, username)"
            setHintTextColor(Theme.TEXT_DIM)
            setTextColor(Theme.TEXT)
            textSize = 16f
            isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setPadding(dp(16), 0, dp(16), 0)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
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
        root.addView(search, LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(12) })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)

        val sv = ScrollView(this)
        sv.setBackgroundColor(Theme.BG)
        sv.addView(root)
        setContentView(sv)
        root.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dialog(): AlertDialog.Builder =
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    private fun render() {
        list.removeAllViews()
        val q = query.trim().lowercase()
        var lastPlatform = ""
        var shown = 0
        for (a in db.all()) {
            // Search: platform, label ya username me match ho to dikhao (password me nahi dhundte)
            if (q.isNotEmpty() &&
                !(a.platform.lowercase().contains(q) ||
                    a.label.lowercase().contains(q) ||
                    a.username.lowercase().contains(q))
            ) continue

            if (a.platform != lastPlatform) {
                lastPlatform = a.platform
                val h = TextView(this).apply {
                    text = a.platform.uppercase()
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Theme.ACCENT)
                    setPadding(dp(4), dp(20), 0, dp(6))
                }
                list.addView(h)
            }
            list.addView(row(a))
            shown++
        }
        if (q.isNotEmpty() && shown == 0) {
            list.addView(TextView(this).apply {
                text = "Koi account nahi mila"
                textSize = 15f
                setTextColor(Theme.TEXT_DIM)
                setPadding(dp(4), dp(24), 0, 0)
            })
        }
    }

    private fun row(a: Account): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Theme.SURFACE)
                setStroke(dp(1), Theme.DIVIDER)
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = TextView(this).apply {
            text = a.label
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Theme.TEXT)
        }
        val user = TextView(this).apply {
            text = if (a.username.isEmpty()) "(username set nahi)" else a.username
            textSize = 14f
            setTextColor(Theme.TEXT_DIM)
        }
        box.addView(name)
        box.addView(user)
        box.setOnClickListener {
            startActivity(Intent(this, DetailActivity::class.java).putExtra("id", a.id))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(8)
        box.layoutParams = lp
        return box
    }

    // ---------- Backup ----------

    private fun chooseBackup() {
        dialog()
            .setTitle("Backup kaise save karu?")
            .setItems(arrayOf("Excel (.xlsx)", "PDF")) { _, which -> startBackup(which) }
            .show()
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
                .show()
        } catch (e: Exception) {
            toast("Backup fail: ${e.message}")
        }
    }
}
