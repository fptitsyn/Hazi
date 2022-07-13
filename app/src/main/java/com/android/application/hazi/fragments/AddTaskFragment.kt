package com.android.application.hazi.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentAddTaskBinding
import com.android.application.hazi.models.Task
import com.android.application.hazi.utils.DatePickerDialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddTaskFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var tasksDatabaseReference: DatabaseReference

    private lateinit var binding: FragmentAddTaskBinding

    private var taskDate = ""

    companion object {
        const val TASK_DATE = "taskDate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initDatabase()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAddTaskBinding.bind(view)

        binding.dueDateButton.setOnClickListener { showDatePickerDialog() }

        setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY) { _, bundle ->
            taskDate = bundle.getString(DatePickerDialogFragment.KEY_RESPONSE).toString()
            binding.dateTextView.text = taskDate
            binding.dateTextView.visibility = View.VISIBLE
        }

        createMenu()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Retain saved date and update ui accordingly
        if (savedInstanceState != null) {
            with (savedInstanceState) {
                taskDate = getString(TASK_DATE).toString()
                binding.dateTextView.text = taskDate
            }
        }
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.add_task_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.add_task -> {
                        addTask()
                        true
                    }
                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initDatabase() {
        database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")
        val currentUserId = Firebase.auth.currentUser?.uid

        lifecycleScope.launch {
            tasksDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref.child("tasks")
        }
    }

    private fun addTask() {
        // Get task info from ui and add push it into database
        val taskName = binding.taskNameEditText.text.trim().toString()
        val taskDescription = binding.taskDescriptionEditText.text.trim().toString()
        val taskDifficulty = when (binding.chooseDifficultySpinner.selectedItem.toString()) {
            "Easy" -> 1
            "Medium" -> 2
            "Hard" -> 3
            else -> 1
        }
        val taskPriority = when (binding.choosePrioritySpinner.selectedItem.toString()) {
            "Low" -> 1
            "Medium" -> 2
            "High" -> 3
            else -> 1
        }
        val taskDate = if (binding.dateTextView.text.toString() != "Date") {
            binding.dateTextView.text.toString()
        } else {
            ""
        }

        if (taskName.isBlank()) {
            Toast.makeText(context, "Please enter task name", Toast.LENGTH_LONG).show()
        } else {
            val task = Task(taskName, taskDifficulty, taskDescription, taskPriority, taskDate)
            tasksDatabaseReference.push().setValue(task)
            Thread.sleep(10)
            findNavController().popBackStack()
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerDialogFragment()
        datePicker.show(parentFragmentManager, DatePickerDialogFragment.TAG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putString(TASK_DATE, taskDate)
        }

        super.onSaveInstanceState(outState)
    }
}