package com.android.application.hazi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.android.application.hazi.R

class NetworkLossDialogFragment : DialogFragment() {

    companion object {
        val TAG: String = NetworkLossDialogFragment::class.java.simpleName
        val REQUEST_KEY = "$TAG:defaultRequestKey"
        const val KEY_RESPONSE = "RESPONSE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = DialogInterface.OnClickListener { _, which ->
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(KEY_RESPONSE to which))
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.no_connection))
            .setMessage(getString(R.string.check_connection))
            .setPositiveButton(getString(R.string.try_again), listener)
            .setNegativeButton(getString(R.string.exit), listener)
            .setCancelable(true)
            .create()
    }

}