package com.mykey.passwords

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Nothing OS jaisa theme: pure black, white dot-matrix text, Nothing-red accent.
 * Font: "Doto" (open source dot-matrix font, SIL OFL) -> assets/fonts/doto.ttf
 */
object Theme {
    val BG = Color.parseColor("#000000")
    val SURFACE = Color.parseColor("#0C0C0C")
    val SURFACE_HIGH = Color.parseColor("#1A1A1A")
    val DIVIDER = Color.parseColor("#2C2C2C")
    val TEXT = Color.parseColor("#FFFFFF")
    val TEXT_DIM = Color.parseColor("#8A8A8A")
    val ACCENT = Color.parseColor("#D71921") // Nothing red

    private val fonts = HashMap<Int, Typeface>()

    /** Doto variable font. weight 100..900, dots hamesha round (ROND=100). */
    fun font(ctx: Context, weight: Int = 700): Typeface = fonts.getOrPut(weight) {
        try {
            Typeface.Builder(ctx.applicationContext.assets, "fonts/doto.ttf")
                .setFontVariationSettings("'wght' $weight, 'ROND' 100")
                .build() ?: Typeface.MONOSPACE
        } catch (e: Exception) {
            Typeface.MONOSPACE
        }
    }

    /** Card: dark surface + patli border + bade round corners. */
    fun card(ctx: Context, radiusDp: Int = 22): GradientDrawable = GradientDrawable().apply {
        cornerRadius = ctx.dp(radiusDp).toFloat()
        setColor(SURFACE)
        setStroke(ctx.dp(1), DIVIDER)
    }

    /** Primary button = white pill; secondary = outlined pill. */
    fun pill(ctx: Context, primary: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = ctx.dp(40).toFloat()
        if (primary) {
            setColor(TEXT)
        } else {
            setColor(Color.TRANSPARENT)
            setStroke(ctx.dp(1), Color.parseColor("#4A4A4A"))
        }
    }

    /** Dot-matrix text ka style ek line me. */
    fun skin(v: View, dialog: Boolean = false) {
        when (v) {
            is EditText -> {
                v.typeface = font(v.context, 700)
                v.setTextColor(TEXT)
                v.setHintTextColor(TEXT_DIM)
                v.background = GradientDrawable().apply {
                    cornerRadius = v.context.dp(16).toFloat()
                    setColor(SURFACE_HIGH)
                    setStroke(v.context.dp(1), DIVIDER)
                }
                v.setPadding(v.context.dp(16), v.context.dp(12), v.context.dp(16), v.context.dp(12))
            }
            is Button -> {
                v.typeface = font(v.context, 800)
                if (dialog) v.setTextColor(ACCENT)
            }
            is TextView -> {
                v.typeface = font(v.context, 700)
                if (dialog) v.setTextColor(TEXT)
            }
        }
        if (v is ViewGroup) for (i in 0 until v.childCount) skin(v.getChildAt(i), dialog)
    }
}

/** Chhota red/white dot (section header, etc). */
fun Context.dotView(sizeDp: Int, color: Int): View = View(this).apply {
    background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }
    layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
}

/** Ek call me size / color / weight / letter-spacing set karo. */
fun TextView.nd(size: Float, color: Int = Theme.TEXT, weight: Int = 700, spacing: Float = 0f): TextView {
    typeface = Theme.font(context, weight)
    textSize = size
    setTextColor(color)
    letterSpacing = spacing
    return this
}

/** Dabane par halka sa chhota hone wala "press" feel (Nothing OS jaisa). */
fun View.pressable() {
    setOnTouchListener { v, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.85f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start()
        }
        false // click listener ko chalne do
    }
}

/** View ko neeche se fade + slide karke dikhao. index se stagger delay milta hai. */
fun View.revealIn(index: Int, baseDelay: Long = 0L) {
    alpha = 0f
    translationY = context.dp(28).toFloat()
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(baseDelay + (index.coerceAtMost(12)) * 55L)
        .setDuration(420)
        .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
        .start()
}

/** Nothing-style dialog: black card, dot font, red buttons, pop-in animation. */
fun Activity.dotDialog(): AlertDialog.Builder =
    AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

fun AlertDialog.Builder.showDot(): AlertDialog {
    val d = create()
    d.show()
    val decor = d.window?.decorView
    d.window?.setBackgroundDrawable(Theme.card(d.context, 28).apply { setColor(Color.parseColor("#0C0C0C")) })
    if (decor != null) {
        Theme.skin(decor, dialog = true)
        d.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Theme.TEXT_DIM)
        // list items (setItems) baad me inflate hote hain, unko bhi skin karo
        d.listView?.apply {
            divider = ColorDrawable(Theme.DIVIDER)
            dividerHeight = d.context.dp(1)
            setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View?, child: View?) {
                    if (child != null) Theme.skin(child, dialog = true)
                }
                override fun onChildViewRemoved(parent: View?, child: View?) {}
            })
        }
        decor.alpha = 0f
        decor.scaleX = 0.9f
        decor.scaleY = 0.9f
        decor.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f)).start()
    }
    return d
}
 
