package com.nfragiskatos.criminalintent

import android.app.Application
import com.nfragiskatos.criminalintent.domain.CrimeRepository

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}
