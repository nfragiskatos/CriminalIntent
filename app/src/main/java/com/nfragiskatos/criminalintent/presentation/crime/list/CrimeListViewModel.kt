package com.nfragiskatos.criminalintent.presentation.crime.list

import androidx.lifecycle.ViewModel
import com.nfragiskatos.criminalintent.domain.Crime
import java.util.Date
import java.util.UUID

class CrimeListViewModel : ViewModel() {

    val crimes = mutableListOf<Crime>()

    init {
        for (i in 0 until 100) {
            val crime = Crime(
                id = UUID.randomUUID(),
                title = "Crime #$i",
                date = Date(),
                isSolved = i % 2 == 0
            )
            crimes += crime
        }
    }
}