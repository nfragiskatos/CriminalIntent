package com.nfragiskatos.criminalintent.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.nfragiskatos.criminalintent.domain.Crime
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CrimeDao {

    @Query("SELECT * FROM crime")
    fun getCrimes(): Flow<List<Crime>>

    @Query("SELECT * FROM crime WHERE id=(:id)")
    suspend fun getCrime(id: UUID): Crime

    @Update
    suspend fun updateCrime(crime: Crime)

    @Insert
    suspend fun addCrime(crime: Crime)

    @Delete
    suspend fun deleteCrime(crime: Crime)
}
