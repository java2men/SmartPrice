package ru.embtlab.smartprice.presentation.compare.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object KeyboardHelper {
    fun forceClose(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        (view.context as? Activity)?.currentFocus?.clearFocus()
    }
}