package com.android.application.hazi.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import java.util.*

class DatePickerDialogFragment : DialogFragment(),
    DatePickerDialog.OnDateSetListener {

    private var selectedDate: Long = 0

    companion object {
        val TAG: String = DatePickerDialogFragment::class.java.simpleName
        val REQUEST_KEY = "$TAG:defaultRequestKey"
        const val KEY_RESPONSE = "responseKey"
        const val SELECTED_DATE = "selectedDate"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dp = DatePickerDialog(requireContext(), this, year, month, day)
        selectedDate = c.timeInMillis
        dp.datePicker.minDate = selectedDate
        return dp
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        val correctMonth = month + 1

        val stringDate: String = if (correctMonth in 0..9) {
            "$year-0${correctMonth}-$dayOfMonth"
        } else {
            "$year-${correctMonth}-$dayOfMonth"
        }

        setFragmentResult(REQUEST_KEY, bundleOf(KEY_RESPONSE to stringDate))
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            with(savedInstanceState) {
                selectedDate = getString(SELECTED_DATE)?.toLong() ?: 0
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putLong(SELECTED_DATE, selectedDate)
        }

        super.onSaveInstanceState(outState)
    }
}