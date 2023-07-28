package com.nfragiskatos.criminalintent.presentation.crime.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nfragiskatos.criminalintent.domain.Crime
import com.nfragiskatos.criminalintent.domain.CrimeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

private  const val TAG = "CrimeListViewModel"
class CrimeListViewModel : ViewModel() {

    private val crimeRepository : CrimeRepository = CrimeRepository.get()

    private val _crimes: MutableStateFlow<List<Crime>> = MutableStateFlow(emptyList())
    val crimes : StateFlow<List<Crime>>
        get() = _crimes.asStateFlow()

    init {
        viewModelScope.launch {
            crimeRepository.getCrimes().collect {
                Log.d(TAG, "Collecting Crime Flow")
                _crimes.value = it
            }
        }
    }
}