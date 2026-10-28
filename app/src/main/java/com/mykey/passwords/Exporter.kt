package com.mykey.passwords

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Plain (bina encryption) backup: Excel (.xlsx) aur PDF. Koi library nahi chahiye. */
object Exporter {

    // ---------- Save to Downloads ----------

    fun save(ctx: Context, name: String, mime: String, write: (OutputStream) -> Unit): String {
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues()
            cv.put(MediaStore.Downloads.DISPLAY_NAME, name)
            cv.put(MediaStore.Downloads.MIME_TYPE, mime)
            cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                ?: throw IllegalStateException("File nahi ban saki")
            val out = ctx.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("File nahi khul saki")
            out.use { write(it) }
            return "Downloads/$name"
        }
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val f = File(dir, name)
        FileOutputStream(f).use { write(it) }
        return f.absolutePath
    }

    // ---------- Excel ----------

    private val headers = listOf(
        "Platform", "Label", "Username", "Username Date",
        "Current Password", "Current Date",
        "Previous Password 1", "Date 1",
        "Previous Password 2", "Date 2"
    )

    private fun esc(s: String): String {
        val sb = StringBuilder()
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 32 && ch != '\n' && ch != '\t' -> {}
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun cells(a: Account) = listOf(
        a.platform, a.label, a.username, dateOrBlank(a.usernameDate),
        a.cur, dateOrBlank(a.curDate),
        a.p1, dateOrBlank(a.p1Date),
        a.p2, dateOrBlank(a.p2Date)
    )

    private fun dateOrBlank(t: Long) = if (t == 0L) "" else fmtDate(t)

    fun xlsx(list: List<Account>, out: OutputStream) {
        val sheet = StringBuilder()
        sheet.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sheet.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

        fun row(n: Int, values: List<String>) {
            sheet.append("<row r=\"$n\">")
            values.forEachIndexed { i, v ->
                val ref = "${('A' + i)}$n"
                sheet.append("<c r=\"$ref\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${esc(v)}</t></is></c>")
            }
            sheet.append("</row>")
        }
        row(1, headers)
        list.forEachIndexed { i, a -> row(i + 2, cells(a)) }
        sheet.append("</sheetData></worksheet>")

        ZipOutputStream(out).use { z ->
            fun entry(name: String, text: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(text.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>"""
            )
            entry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
            )
            entry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Passwords" sheetId="1" r:id="rId1"/></sheets></workbook>"""
            )
            entry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>"""
            )
            entry("xl/worksheets/sheet1.xml", sheet.toString())
        }
    }

    // ---------- PDF ----------

    fun pdf(list: List<Account>, out: OutputStream) {
        val doc = PdfDocument()
        val w = 595
        val h = 842
        val margin = 36f

        val normal = Paint().apply { textSize = 10f; typeface = Typeface.MONOSPACE; color = 0xFF000000.toInt() }
        val bold = Paint().apply { textSize = 11f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); color = 0xFF000000.toInt() }
        val head = Paint().apply { textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = 0xFF1A237E.toInt() }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, pageNo).create())
            y = margin + 10f
        }

        fun line(text: String, p: Paint, gap: Float) {
            if (page == null || y + gap > h - margin) newPage()
            page!!.canvas.drawText(text, margin, y, p)
            y += gap
        }

        newPage()
        line("Password Key - Backup", head, 22f)
        line("Date: ${fmtDate(System.currentTimeMillis())}", normal, 18f)

        var lastPlatform = ""
        for (a in list) {
            // Ek account ke 5 lines ek page par rahen
            if (y + 120f > h - margin) newPage()
            if (a.platform != lastPlatform) {
                lastPlatform = a.platform
                y += 8f
                line(a.platform.uppercase(), head, 20f)
            }
            line(a.label, bold, 14f)
            line("User : ${a.username.ifEmpty { "-" }}  (${fmtDate(a.usernameDate)})", normal, 13f)
            line("Now  : ${a.cur.ifEmpty { "-" }}  (${fmtDate(a.curDate)})", normal, 13f)
            line("Prev1: ${a.p1.ifEmpty { "-" }}  (${fmtDate(a.p1Date)})", normal, 13f)
            line("Prev2: ${a.p2.ifEmpty { "-" }}  (${fmtDate(a.p2Date)})", normal, 18f)
        }
        page?.let { doc.finishPage(it) }
        doc.writeTo(out)
        doc.close()
    }
}
