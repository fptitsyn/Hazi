package com.android.application.hazi.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.work.*
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentTabsBinding
import com.android.application.hazi.dialogs.NetworkLossDialogFragment
import com.android.application.hazi.workers.EnergyWorker
import com.android.application.hazi.workers.HungerWorker
import com.android.application.hazi.utils.MyApplication
import com.android.application.hazi.workers.TasksNotificationWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class TabsFragment : Fragment() {

    private lateinit var binding: FragmentTabsBinding

//    private val activeNetworkStateObserver = Observer<Boolean> { isConnected ->
//        handleNetworkConnection(isConnected)
//    }
//
//    private fun handleNetworkConnection(isConnected: Boolean) {
//        if (!isConnected) {
//            showNetworkLossDialog()
//        } else {
//            fetchDataFromDatabase()
//        }
//    }

    companion object {
        val TAG: String = TabsFragment::class.java.simpleName
        const val PET_CHANNEL_ID = "1"
        const val TASKS_CHANNEL_ID = "2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            if (throwable.message.toString() == "Client is offline") {
                showNetworkLossDialog()
            }
        }

        initWorkers()
        registerNotificationChannels()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        NetworkStateManager.getInstance().networkConnectivityStatus.observe(viewLifecycleOwner, activeNetworkStateObserver)
        return inflater.inflate(R.layout.fragment_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTabsBinding.bind(view)

        val navHost = childFragmentManager.findFragmentById(R.id.tabsContainer) as NavHostFragment
        val navController = navHost.navController
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        setupNetworkLossDialog()
        fetchDataFromDatabase()
    }

    private fun setupNetworkLossDialog() {
        parentFragmentManager.setFragmentResultListener(
            NetworkLossDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            when(result.getInt(NetworkLossDialogFragment.KEY_RESPONSE)) {
                DialogInterface.BUTTON_POSITIVE -> {
                    fetchDataFromDatabase()
                    val navHost = childFragmentManager.findFragmentById(R.id.tabsContainer) as NavHostFragment
                    val navController = navHost.navController
                    navController.navigate(R.id.tasks)
                }

                DialogInterface.BUTTON_NEGATIVE -> {
                    exitProcess(0)
                }
            }
        }
    }

    private fun showNetworkLossDialog() {
        val dialog = NetworkLossDialogFragment()
        dialog.show(parentFragmentManager, NetworkLossDialogFragment.TAG)
    }

    private fun fetchDataFromDatabase() {
        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            val userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val userCoinsRef = userDatabaseReference.child("coins")

            userCoinsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userCoins = snapshot.value.toString().toInt()
                    MyApplication.coins = userCoins
                    Log.d(TAG, userCoins.toString())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Database error occurred: $error")
                }
            })

            val energyRef = userDatabaseReference.child("pet").child("energy")

            energyRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userPetEnergy  = snapshot.value.toString().toInt()
                    MyApplication.energy = userPetEnergy
                    Log.d(TAG, userPetEnergy.toString())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Database error occurred: $error")
                }
            })

            val hungerRef = userDatabaseReference.child("pet").child("hunger")

            hungerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userPetHunger = snapshot.value.toString().toInt()
                    MyApplication.hunger = userPetHunger
                    Log.d(TAG, userPetHunger.toString())
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Database error occurred: $error")
                }
            })
        }
    }

    private fun initWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val hungerWorkRequest = PeriodicWorkRequestBuilder<HungerWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "Hunger handling",
            ExistingPeriodicWorkPolicy.KEEP,
            hungerWorkRequest)

        val energyWorkRequest = PeriodicWorkRequestBuilder<EnergyWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "Energy handling",
            ExistingPeriodicWorkPolicy.KEEP,
            energyWorkRequest
        )

        val tasksNotificationWorkRequest = PeriodicWorkRequestBuilder<TasksNotificationWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "Tasks notifications",
            ExistingPeriodicWorkPolicy.KEEP,
            tasksNotificationWorkRequest
        )
    }

    private fun registerNotificationChannels() {
        val petChannelName = getString(R.string.pet)
        val petChannelDescText = getString(R.string.pet_channel_description_text)
        createNotificationChannel(petChannelName, petChannelDescText)

        val taskChannelName = getString(R.string.tasks)
        val taskChannelDescText = getString(R.string.task_channel_description_text)
        createNotificationChannel(taskChannelName, taskChannelDescText)
    }

    private fun createNotificationChannel(name: String, descText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(PET_CHANNEL_ID, name, importance).apply {
                description = descText
            }

            val notificationManager: NotificationManager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
