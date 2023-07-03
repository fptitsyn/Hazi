package com.android.application.hazi.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.android.application.hazi.R

class RequestNotificationsPermissionDialogFragment : DialogFragment() {

    companion object {
        val TAG: String = RequestNotificationsPermissionDialogFragment::class.java.simpleName
        val REQUEST_KEY = "$TAG:defaultRequestKey"
        const val KEY_RESPONSE = "RESPONSE"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val listener = DialogInterface.OnClickListener { _, which ->
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY, bundleOf(KEY_RESPONSE to which)
            )
        }

        return AlertDialog.Builder(requireContext())
            .setMessage(R.string.request_notification_permission_dialog_message)
            .setPositiveButton(android.R.string.ok, listener)
            .setNegativeButton(R.string.cancel, listener)
            .setCancelable(true)
            .create()
    }
}