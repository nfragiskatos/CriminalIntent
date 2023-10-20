package com.nfragiskatos.criminalintent.utils

import android.icu.text.DateFormat
import java.util.Date

fun getLocalizedFormattedDate(date: Date): String {
    return DateFormat.getInstance().format(date)
}
