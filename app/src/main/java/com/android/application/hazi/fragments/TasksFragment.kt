package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.CheckBox
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentTasksBinding
import com.android.application.hazi.models.Task
import com.android.application.hazi.utils.MyApplication
import com.android.application.hazi.adapters.TaskActionListener
import com.android.application.hazi.adapters.TasksAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TasksFragment : Fragment() {

    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var tasks: MutableList<Task>
    private lateinit var tasksListener: ChildEventListener

    private lateinit var database: FirebaseDatabase
    private lateinit var userDatabaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var binding: FragmentTasksBinding

    companion object {
        val TAG: String = TasksFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentTasksBinding.bind(view)

        initDatabase()

        val currentUserId = auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            userDatabaseReference.child("tasks").addChildEventListener(tasksListener)
        }

        binding.addTaskFloatingActionButton.setOnClickListener { addTask() }

        initTaskRecyclerView()

        createMenu()
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.tasks_fragment_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.sign_out -> {
                        signOut()
                        true
                    }
                    R.id.calendar -> {
                        openCalendar()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initTaskRecyclerView() {
        tasks = mutableListOf()

        val taskRecyclerView = binding.taskRecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(context)
        tasksAdapter = TasksAdapter(tasks, object : TaskActionListener {
            // Handle click on task and open task editing mode
            override fun onTaskClick(task: Task) {
                editTask(task)
            }

            // Handle click on task's checkbox and complete the task
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

        taskRecyclerView.adapter = tasksAdapter
    }

    private fun initDatabase() {
        database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")
        auth = Firebase.auth

        tasksListener = object : ChildEventListener {
            // Task added
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val task = snapshot.getValue<Task>()

                if (task != null) {
                    tasks.add(task)
                    tasksAdapter.notifyItemInserted(tasks.size)
                }
            }

            // Task edited
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val task = snapshot.getValue<Task>()

                if (task != null) {
                    var taskPosition = 0

                    setFragmentResultListener(EditTaskFragment.REQUEST_KEY) { _, bundle ->
                        taskPosition = bundle.getInt(EditTaskFragment.RESPONSE_KEY)
                    }

                    tasks[taskPosition] = task
                    tasksAdapter.notifyItemChanged(taskPosition)
                }
            }

            // Task deleted
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val task = snapshot.getValue<Task>()

                if (task != null) {
                    tasks.remove(task)
                    tasksAdapter.notifyDataSetChanged()
                }
            }

            // Task moved (not sure what to do with this one
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            // On database error
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTask() {
        findNavController().navigate(R.id.action_tasksFragment_to_addTaskFragment)
    }

    // Sign out from the account
    private fun signOut() {
        userDatabaseReference.child("tasks").removeEventListener(tasksListener)
        auth.signOut()
        val topLevelHost =
            requireActivity().supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment?
        val topLevelNavController = topLevelHost?.navController ?: findNavController()
        topLevelNavController.navigate(R.id.signInFragment, null, navOptions {
            popUpTo(R.id.tabsFragment) {
                inclusive = true
            }
        })
    }

    // Remove listeners so app wouldn't crash on screen rotation
    override fun onDestroyView() {
        super.onDestroyView()


        if (this::userDatabaseReference.isInitialized) {
            userDatabaseReference.child("tasks").removeEventListener(tasksListener)
        }
    }

    private fun editTask(task: Task) {
        val direction = TasksFragmentDirections.actionTasksFragmentToEditTaskFragment(
            task.name!!,
            task.description,
            task.difficulty!!,
            task.priority!!,
            task.date,
            tasks.indexOf(task)
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

        Log.d(TAG, MyApplication.energy.toString())
    }

    private fun openCalendar() {
        findNavController().navigate(R.id.action_tasksFragment_to_calendarFragment)
    }
}