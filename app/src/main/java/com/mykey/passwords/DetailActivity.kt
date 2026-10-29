package com.mykey.passwords

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Db(this)
        title = "Password Key"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(24))
        }

        val backupBtn = Button(this).apply {
            text = "Backup (Excel / PDF)"
            textSize = 16f
            setOnClickListener { chooseBackup() }
        }
        root.addView(backupBtn, LinearLayout.LayoutParams(-1, dp(56)))

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        list.removeAllViews()
        var lastPlatform = ""
        for (a in db.all()) {
            if (a.platform != lastPlatform) {
                lastPlatform = a.platform
                val h = TextView(this).apply {
                    text = a.platform.uppercase()
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#37474F"))
                    setPadding(dp(4), dp(20), 0, dp(6))
                }
                list.addView(h)
            }
            list.addView(row(a))
        }
    }

    private fun row(a: Account): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#ECEFF1"))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = TextView(this).apply {
            text = a.label
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        val user = TextView(this).apply {
            text = if (a.username.isEmpty()) "(username set nahi)" else a.username
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }
        box.addView(name)
        box.addView(user)
        box.setOnClickListener {
            startActivity(Intent(this, DetailActivity::class.java).putExtra("id", a.id))
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(6)
        box.layoutParams = lp
        return box
    }

    // ---------- Backup ----------

    private fun chooseBackup() {
        AlertDialog.Builder(this)
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
            AlertDialog.Builder(this)
                .setTitle("Backup ban gaya")
                .setMessage("Yahan save hua:\n$path")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            toast("Backup fail: ${e.message}")
        }
    }
}
