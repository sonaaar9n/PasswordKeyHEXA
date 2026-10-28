package com.mykey.passwords

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.TypedValue
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ek account ki poori jaankari. Date = milliseconds, 0 matlab "abhi tak set nahi". */
data class Account(
    val id: Long,
    val platform: String,
    val label: String,
    val username: String,
    val usernameDate: Long,
    val cur: String,
    val curDate: Long,
    val p1: String,
    val p1Date: Long,
    val p2: String,
    val p2Date: Long
)

fun fmtDate(t: Long): String =
    if (t == 0L) "-" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(t))

fun Context.dp(v: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

/**
 * Data app ki internal storage me SQLite file me rehta hai.
 * Ye tab tak rahega jab tak aap app uninstall ya "Clear data" na karo.
 */
class Db(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "keys.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE accounts(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                platform TEXT NOT NULL,
                label TEXT NOT NULL,
                username TEXT NOT NULL DEFAULT '',
                username_date INTEGER NOT NULL DEFAULT 0,
                cur TEXT NOT NULL DEFAULT '',
                cur_date INTEGER NOT NULL DEFAULT 0,
                p1 TEXT NOT NULL DEFAULT '',
                p1_date INTEGER NOT NULL DEFAULT 0,
                p2 TEXT NOT NULL DEFAULT '',
                p2_date INTEGER NOT NULL DEFAULT 0
            )"""
        )
        val plan = listOf(
            "Facebook" to 4, "Instagram" to 4, "Gmail" to 4, "Amazon" to 3,
            "Flipkart" to 3, "Microsoft" to 3, "Music Distribution" to 2
        )
        for ((platform, count) in plan) {
            for (i in 1..count) {
                val cv = ContentValues()
                cv.put("platform", platform)
                cv.put("label", "$platform $i")
                db.insert("accounts", null, cv)
            }
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Abhi koi upgrade nahi. Purana data kabhi delete nahi hota.
    }

    private fun read(c: Cursor) = Account(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        platform = c.getString(c.getColumnIndexOrThrow("platform")),
        label = c.getString(c.getColumnIndexOrThrow("label")),
        username = c.getString(c.getColumnIndexOrThrow("username")),
        usernameDate = c.getLong(c.getColumnIndexOrThrow("username_date")),
        cur = c.getString(c.getColumnIndexOrThrow("cur")),
        curDate = c.getLong(c.getColumnIndexOrThrow("cur_date")),
        p1 = c.getString(c.getColumnIndexOrThrow("p1")),
        p1Date = c.getLong(c.getColumnIndexOrThrow("p1_date")),
        p2 = c.getString(c.getColumnIndexOrThrow("p2")),
        p2Date = c.getLong(c.getColumnIndexOrThrow("p2_date"))
    )

    fun all(): List<Account> {
        val out = ArrayList<Account>()
        readableDatabase.rawQuery("SELECT * FROM accounts ORDER BY id", null).use { c ->
            while (c.moveToNext()) out.add(read(c))
        }
        return out
    }

    fun get(id: Long): Account? =
        readableDatabase.rawQuery("SELECT * FROM accounts WHERE id=?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) read(c) else null
        }

    /** Label aur username/email/phone badalna. Username badalne par aaj ki date lag jaati hai. */
    fun updateUsername(id: Long, label: String, username: String) {
        val old = get(id) ?: return
        val cv = ContentValues()
        cv.put("label", label)
        cv.put("username", username)
        if (username != old.username) cv.put("username_date", System.currentTimeMillis())
        writableDatabase.update("accounts", cv, "id=?", arrayOf(id.toString()))
    }

    /**
     * Typo sudharna: sirf us field ka text badalta hai (cur / p1 / p2).
     * Koi shift nahi hota aur date wahi rehti hai.
     */
    fun fixPassword(id: Long, column: String, newValue: String) {
        if (column != "cur" && column != "p1" && column != "p2") return
        val cv = ContentValues()
        cv.put(column, newValue)
        writableDatabase.update("accounts", cv, "id=?", arrayOf(id.toString()))
    }

    /**
     * Auto-shift, ek hi transaction me:
     *   Previous 2  <-  Previous 1
     *   Previous 1  <-  Current (apni date ke saath)
     *   Current     <-  naya password, aaj ki date
     * Agar current khaali tha (pehli baar), to bas naya password set hota hai.
     */
    fun changePassword(id: Long, newPw: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val a = get(id) ?: return
            val cv = ContentValues()
            if (a.cur.isNotEmpty()) {
                cv.put("p2", a.p1)
                cv.put("p2_date", a.p1Date)
                cv.put("p1", a.cur)
                cv.put("p1_date", a.curDate)
            }
            cv.put("cur", newPw)
            cv.put("cur_date", System.currentTimeMillis())
            db.update("accounts", cv, "id=?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
