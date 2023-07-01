package com.android.application.hazi.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentCalendarBinding
import com.android.application.hazi.models.Task
import com.android.application.hazi.utils.MyApplication
import com.android.application.hazi.adapters.TaskActionListener
import com.android.application.hazi.adapters.TasksAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat

class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding

    private lateinit var dateTasks: MutableList<Task>
    private lateinit var userDatabaseReference: DatabaseReference
    private lateinit var tasksListener: ChildEventListener

    companion object {
        val TAG: String = CalendarFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentCalendarBinding.bind(view)

        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        initRecyclerView()
        initTasksListener()

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            userDatabaseReference.child("tasks").addChildEventListener(tasksListener)
            val currentDate = binding.tasksCalendarView.date
            // Need this exact formatting for the database
            val format = SimpleDateFormat("yyyy-MM-dd")
            val date = format.format(currentDate)
            showTasksForSelectedDate(date)
        }

        binding.tasksCalendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Months start from 0 for some reason so add +1
            val correctMonth = month + 1

            val day = if (dayOfMonth in 0..9) {
                "0$dayOfMonth"
            } else {
                dayOfMonth.toString()
            }

            val date = if (correctMonth in 0..9) {
            "$year-0${correctMonth}-$day"
        } else {
            "$year-${correctMonth}-$day"
        }
            showTasksForSelectedDate(date)
        }
    }

    private fun initRecyclerView() {
        dateTasks = mutableListOf()

        binding.tasksCalendarRecyclerView.layoutManager = LinearLayoutManager(context)

        val adapter = TasksAdapter(dateTasks, object : TaskActionListener {
            override fun onTaskClick(task: Task) {
                editTask(task)
            }

            override fun onTaskCompleted(task: Task, checkBox: CheckBox) {
                val energy = MyApplication.energy
                if (energy >= 5) {
                    completeTask(task, checkBox)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Your pet is too tired!", Toast.LENGTH_SHORT
                    ).show()

                    checkBox.isChecked = false
                }
            }
        })

        binding.tasksCalendarRecyclerView.adapter = adapter
    }

    private fun initTasksListener() {
        tasksListener = object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // This is left empty on purpose, because if we would use the same code that we use for
                // the main task recyclerview, it would just add all the tasks
            }

            // Task edited
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val task = snapshot.getValue<Task>()

                if (task != null) {
                    var taskPosition = 0

                    setFragmentResultListener(EditTaskFragment.REQUEST_KEY) { _, bundle ->
                        taskPosition = bundle.getInt(EditTaskFragment.RESPONSE_KEY)
                    }

                    dateTasks[taskPosition] = task
                    binding.tasksCalendarRecyclerView.adapter?.notifyItemChanged(taskPosition)
                }
            }

            // Task deleted
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val task = snapshot.getValue<Task>()

                if (task != null) {
                    val taskPosition = dateTasks.indexOf(task)
                    dateTasks.remove(task)
                    binding.tasksCalendarRecyclerView.adapter?.notifyItemRemoved(taskPosition)
                }
            }

            // Task moved (not sure what to do with this one
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                TODO("Not yet implemented")
            }

            // On database error
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTasksForSelectedDate(date: String) {
        val tasksAmount = dateTasks.size
        dateTasks.clear()
        binding.tasksCalendarRecyclerView.adapter?.notifyItemRangeRemoved(0, tasksAmount)

        val tasksRef = userDatabaseReference.child("tasks")

        val taskQuery = tasksRef.orderByChild("date").equalTo(date)

        taskQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (taskSnapshot in snapshot.children) {
                    val task = taskSnapshot.getValue<Task>()

                    if (task != null) {
                        dateTasks.add(task)
                        binding.tasksCalendarRecyclerView.adapter?.notifyItemInserted(dateTasks.size)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error occurred: $error")
            }
        })
    }

    private fun editTask(task: Task) {
        val direction = CalendarFragmentDirections.actionCalendarFragmentToEditTaskFragment(
            task.name!!,
            task.description,
            task.difficulty!!,
            task.priority!!,
            task.date,
            dateTasks.indexOf(task)
        )

        findNavController().navigate(direction)
    }

    private fun completeTask(task: Task, checkBox: CheckBox) {
        if (checkBox.isChecked) {
            val taskQuery =
                userDatabaseReference.child("tasks").orderByChild("name").equalTo(task.name)

            taskQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        taskSnapshot.ref.removeValue()
                        break
                    }

                    // Possibly make award depend on if the task was completed in time
                    val coinsAward = 5 + 1 * task.difficulty!!

                    addCoinsToCurrentUser(coinsAward)
                    handlePetStateOnTaskComplete()

                    Toast.makeText(context, "+$coinsAward coins!", Toast.LENGTH_SHORT).show()
                    checkBox.isChecked = false
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        context,
                        "Couldn't complete the task",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    private fun addCoinsToCurrentUser(coinsToAdd: Int) {
        val userCoins = userDatabaseReference.child("coins")

        val currentCoins = MyApplication.coins
        val updatedCoins = currentCoins + coinsToAdd

        userCoins.setValue(updatedCoins)
        MyApplication.coins = updatedCoins
    }

    private fun handlePetStateOnTaskComplete() {
        val petRef = userDatabaseReference.child("pet")

        val energy = MyApplication.energy
        val updatedEnergy = energy - 5

        petRef.child("energy").setValue(updatedEnergy)
        MyApplication.energy = updatedEnergy
    }

    override fun onDestroyView() {
        super.onDestroyView()


        if (this::userDatabaseReference.isInitialized) {
            userDatabaseReference.child("tasks").removeEventListener(tasksListener)
        }
    }
}