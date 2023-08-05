package com.nfragiskatos.criminalintent.presentation.crime.details

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.*
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nfragiskatos.criminalintent.R
import com.nfragiskatos.criminalintent.databinding.FragmentCrimeDetailsBinding
import com.nfragiskatos.criminalintent.domain.Crime
import com.nfragiskatos.criminalintent.presentation.crime.datepicker.DatePickerFragment
import com.nfragiskatos.criminalintent.utils.getLocalizedFormattedDate
import com.nfragiskatos.criminalintent.utils.getScaledBitmap
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

private const val TAG = "CrimeDetailFragment"
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeDetailsFragment : Fragment() {

    private var _binding: FragmentCrimeDetailsBinding? = null
    private val args: CrimeDetailsFragmentArgs by navArgs()

    private var photoName : String? = null

    private val viewModel: CrimeDetailsViewModel by viewModels {
        CrimeDetailsViewModelFactory(args.crimeId)
    }

    private val selectSuspect =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let { parseContactSelection(it) }
        }

    private val callContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let { parseContactSelectionForId(it) }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                callContact.launch(null)
            }
        }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) {photoTaken ->
        if (photoTaken && photoName != null) {
            viewModel.updateCrime { oldCrime ->
                oldCrime.copy(photoFileName = photoName)
            }
        }
    }


    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCrimeDetailsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, MyOnBackPressedCallback())

        binding.apply {
            crimeTitle.doOnTextChanged { text, _, _, _ ->
                viewModel.updateCrime { oldCrime ->
                    oldCrime.copy(title = text.toString())
                }
            }

            crimeSolved.setOnCheckedChangeListener { _, isChecked ->
                viewModel.updateCrime { oldCrime ->
                    oldCrime.copy(isSolved = isChecked)
                }
            }

            crimeSuspect.setOnClickListener {
                selectSuspect.launch(null)
            }

            val selectSuspectIntent = selectSuspect.contract.createIntent(requireContext(), null)
            crimeSuspect.isEnabled = canResolveIntent(selectSuspectIntent)

            crimeCallSuspect.setOnClickListener {
                requestContactPermission()
            }

            crimeCamera.setOnClickListener {
                photoName = "IMG_${Date()}.jpg"

                val photoFile = File(requireContext().applicationContext.filesDir, photoName)

                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "com.nfragiskatos.criminalintent.fileprovider",
                    photoFile
                    )

                takePhoto.launch(photoUri)
            }

            val takePhotoIntent = takePhoto.contract.createIntent(requireContext(), null)
            crimeCamera.isEnabled = canResolveIntent(takePhotoIntent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.crime.collect { crime ->
                    crime?.let { updateUi(crime) }
                }
            }
        }

        setFragmentResultListener(DatePickerFragment.REQUEST_KEY_DATE) { requestKey, bundle ->
            val newDate = bundle.getSerializable(DatePickerFragment.BUNDLE_KEY_DATE) as Date
            viewModel.updateCrime { it.copy(date = newDate) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(crime: Crime) {
        binding.apply {
            if (crimeTitle.text.toString() != crime.title) {
                crimeTitle.setText(crime.title)
            }
            crimeDate.text = getLocalizedFormattedDate(crime.date)
            crimeDate.setOnClickListener {
                findNavController().navigate(CrimeDetailsFragmentDirections.selectDate(crime.date))
            }

            crimeSolved.isChecked = crime.isSolved

            crimeReport.setOnClickListener {
                val reportIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getCrimeReport(crime))
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
                }

                val chooser = Intent.createChooser(reportIntent, getString(R.string.send_report))
                startActivity(chooser)
            }

            crimeSuspect.text = crime.suspect.ifEmpty {
                getString(R.string.crime_suspect_text)
            }

            updatePhoto(crime.photoFileName)

            if (crime.photoFileName != null) {
                crimePhoto.setOnClickListener {
                    findNavController().navigate(CrimeDetailsFragmentDirections.viewImage(crime.photoFileName))
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_details, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.delete_crime -> {
                deleteCrime()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun deleteCrime() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.crime.value?.let {
                viewModel.deleteCrime(it)
                findNavController().popBackStack()
            }

        }
    }

    private fun getCrimeReport(crime: Crime): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()

        val suspectText = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspectText)
    }

    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(Contacts.DISPLAY_NAME)

        val queryCursor =
            requireActivity().contentResolver.query(contactUri, queryFields, null, null, null)

        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                viewModel.updateCrime { oldCrime ->
                    oldCrime.copy(suspect = suspect)
                }
            }
        }
    }

    private fun parseContactSelectionForId(contactUri: Uri) {
        val contactQueryCursor =
            requireActivity().contentResolver.query(
                contactUri,
                arrayOf(Contacts._ID),
                null,
                null,
                null
            )

        contactQueryCursor?.use { contactCursor ->
            if (contactCursor.moveToFirst()) {
                val contactId = contactCursor.getLong(0)
                val phoneQueryCursor = requireActivity().contentResolver.query(
                    CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(CommonDataKinds.Phone.NUMBER),
                    "${CommonDataKinds.Phone.CONTACT_ID} = $contactId",
                    null,
                    null
                )

                phoneQueryCursor?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        phoneCursor.getString(0)?.let {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))
                            startActivity(callIntent)
                        }
                    }
                }
            }
        }
    }

    private fun requestContactPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            callContact.launch(null)
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager = requireActivity().packageManager
        val resolvedActivity =
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolvedActivity != null
    }

    private fun updatePhoto(photoFileName: String?) {
        if (binding.crimePhoto.tag != photoFileName) {
            val photoFile = photoFileName?.let {
                File(requireContext().applicationContext.filesDir, it)
            }

            if (photoFile?.exists() == true) {
                binding.crimePhoto.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.measuredWidth,
                        measuredView.height
                    )

                    binding.crimePhoto.setImageBitmap(scaledBitmap)
                    binding.crimePhoto.tag = photoFileName
                }
            } else {
                binding.crimePhoto.setImageBitmap(null)
                binding.crimePhoto.tag = null
            }
        }
    }

    private inner class MyOnBackPressedCallback : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.crimeTitle.text.isNotBlank()) {
                isEnabled = false
                requireActivity().onBackPressed()
            } else {
                Toast.makeText(context, "Please enter a title", Toast.LENGTH_LONG).show()
            }
        }

    }
}