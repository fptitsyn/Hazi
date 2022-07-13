package com.android.application.hazi.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentEditTaskBinding
import com.android.application.hazi.utils.DatePickerDialogFragment
import com.android.application.hazi.utils.DeleteTaskDialogFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditTaskFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var tasksDatabaseReference: DatabaseReference
    private var taskPosition = 0
    private var taskDate = ""

    private lateinit var taskToEditName: String

    private val args by navArgs<EditTaskFragmentArgs>()

    private lateinit var binding: FragmentEditTaskBinding

    companion object {
        const val REQUEST_KEY = "taskPositionRK"
        const val RESPONSE_KEY = "taskPosition"
        const val TASK_DATE = "taskDate"
        const val TASK_POSITION = "taskPosition"
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
        return inflater.inflate(R.layout.fragment_edit_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEditTaskBinding.bind(view)

        // Get task info and update ui accordingly
        taskPosition = args.taskPosition
        taskToEditName = args.taskName
        val taskDescription = args.taskDescription.toString()
        val taskDifficulty = args.taskDifficulty
        val taskPriority = args.taskPriority
        taskDate = args.taskDate.toString()

        binding.editTaskNameEditText.setText(taskToEditName)
        binding.editTaskDescriptionEditText.setText(taskDescription)
        binding.editDifficultySpinner.setSelection(taskDifficulty - 1)
        binding.editPrioritySpinner.setSelection(taskPriority - 1)

        if (taskDate.isNotEmpty() && taskDate != "Date") {
            binding.editDateTextView.text = taskDate
            binding.editDateTextView.visibility = View.VISIBLE
        } else {
            binding.editDateTextView.visibility = View.INVISIBLE
        }

        binding.editDateButton.setOnClickListener { showDatePickerDialog() }

        // Update date
        setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY) { _, bundle ->
            binding.editDateTextView.text = bundle.getString(DatePickerDialogFragment.KEY_RESPONSE).toString()
            taskPosition = bundle.getInt(TASK_POSITION)
        }

        createMenu()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // Restoring task date on screen rotation
        if (savedInstanceState != null) {
            with (savedInstanceState) {
                taskDate = getString(AddTaskFragment.TASK_DATE).toString()
                binding.editDateTextView.text = taskDate
            }
        }
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.edit_task_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.save_task -> {
                        editTask()
                        true
                    }
                    R.id.delete_task -> {
                        deleteTask()
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

            setupDeletionConfirmDialog()
        }
    }

    private fun editTask() {
        val taskQuery = tasksDatabaseReference.orderByChild("name").equalTo(taskToEditName)

        val taskName = binding.editTaskNameEditText.text.toString()
        val taskDifficulty = binding.editDifficultySpinner.selectedItemId.toInt() + 1
        val taskDescription = binding.editTaskDescriptionEditText.text.toString()
        val taskPriority = binding.editPrioritySpinner.selectedItemId.toInt() + 1
        val taskDate = binding.editDateTextView.text.toString()

        taskQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (taskSnapshot in snapshot.children) {
                    taskSnapshot.ref.child("name").setValue(taskName)
                    taskSnapshot.ref.child("difficulty").setValue(taskDifficulty)
                    taskSnapshot.ref.child("description").setValue(taskDescription)
                    taskSnapshot.ref.child("priority").setValue(taskPriority)
                    taskSnapshot.ref.child("date").setValue(taskDate)
                }

                setFragmentResult(REQUEST_KEY, bundleOf(RESPONSE_KEY to taskPosition))
                Thread.sleep(10)
                findNavController().navigateUp()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(activity, "Couldn't edit the task", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteTask() {
        showDeletionConfirmDialog()
    }

    private fun showDeletionConfirmDialog() {
        val dialogFragment = DeleteTaskDialogFragment()
        dialogFragment.show(parentFragmentManager, DeleteTaskDialogFragment.TAG)
    }

    private fun setupDeletionConfirmDialog() {
        val taskQuery = tasksDatabaseReference.orderByChild("name").equalTo(taskToEditName)

        parentFragmentManager.setFragmentResultListener(DeleteTaskDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
            FragmentResultListener {_, result ->
                val which = result.getInt(DeleteTaskDialogFragment.KEY_RESPONSE)
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> taskQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (taskSnapshot in snapshot.children) {
                                taskSnapshot.ref.removeValue()
                                break
                            }

                            findNavController().popBackStack()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                activity,
                                "Couldn't delete the task",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
        })
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerDialogFragment()
        datePicker.show(parentFragmentManager, DatePickerDialogFragment.TAG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save date on screen rotation
        outState.run {
            putString(TASK_DATE, binding.editDateTextView.text.toString())
            putInt(TASK_POSITION, taskPosition)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        clearFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY)
    }
}