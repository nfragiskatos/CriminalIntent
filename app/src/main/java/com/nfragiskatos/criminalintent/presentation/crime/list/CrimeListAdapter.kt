package com.nfragiskatos.criminalintent.presentation.crime.list

import android.icu.text.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nfragiskatos.criminalintent.R
import com.nfragiskatos.criminalintent.databinding.ListItemCrimeBinding
import com.nfragiskatos.criminalintent.domain.Crime
import com.nfragiskatos.criminalintent.utils.getLocalizedFormattedDate
import java.util.UUID

class CrimeHolder(private val binding: ListItemCrimeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(crime: Crime, onCrimeClicked: (crimeId: UUID) -> Unit) {
        binding.crimeTitle.text = crime.title
        binding.crimeDate.text = getLocalizedFormattedDate(crime.date)

        binding.root.setOnClickListener {
            onCrimeClicked(crime.id)
        }

        itemView.contentDescription = buildCrimeSummary(crime)

        if (crime.isSolved) {
            binding.crimeSolved.apply {
                contentDescription = context.getString(R.string.crime_solved_icon_solved_description)
                visibility = View.VISIBLE
            }
        } else {
            binding.crimeSolved.apply {
                contentDescription = context.getString(R.string.crime_solved_icon_not_solved_description)
                visibility = View.INVISIBLE
            }
        }
    }

    private fun buildCrimeSummary(crime: Crime): String {
        val descDate = DateFormat.getDateInstance().format(crime.date)
        val solvedSummary =
            if (crime.isSolved) R.string.crime_solved_icon_solved_description else R.string.crime_solved_icon_not_solved_description
        return itemView.context.getString(
            R.string.crime_list_item_summary_description,
            crime.title,
            descDate,
            itemView.context.getString(solvedSummary)
        )
    }
}

class CrimeListAdapter(
    private val crimes: List<Crime>,
    private val onCrimeClicked: (crimeId: UUID) -> Unit
) : RecyclerView.Adapter<CrimeHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemCrimeBinding.inflate(inflater, parent, false)
        return CrimeHolder(binding)
    }

    override fun getItemCount(): Int = crimes.size

    override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
        holder.bind(crimes[position], onCrimeClicked)
    }

}