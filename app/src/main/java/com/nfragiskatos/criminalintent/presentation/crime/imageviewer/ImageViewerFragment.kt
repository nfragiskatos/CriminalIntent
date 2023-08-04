package com.nfragiskatos.criminalintent.presentation.crime.imageviewer

import android.app.Dialog
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.nfragiskatos.criminalintent.R
import com.nfragiskatos.criminalintent.utils.getScaledBitmap
import java.io.File

class ImageViewerFragment : DialogFragment() {

    private val args : ImageViewerFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {

            val photoFileName = args.crimeImage
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.fragment_image_viewer, null)
            val imageView = view.findViewById<ImageView>(R.id.crime_photo)

            val photoFile = File(requireContext().applicationContext.filesDir, photoFileName)

            if (photoFile.exists()) {
                imageView.doOnLayout { measuredView ->
                    val scaledBitmap = getScaledBitmap(
                        photoFile.path,
                        measuredView.measuredWidth,
                        measuredView.measuredHeight
                    )
                    imageView.setImageBitmap(scaledBitmap)
                    imageView.tag = photoFileName
                }
            }
//            builder.setTitle("Photo Evidence")

            builder.setView(view)


            builder.create()

        } ?: throw IllegalStateException("activity cannot be null")

    }
}