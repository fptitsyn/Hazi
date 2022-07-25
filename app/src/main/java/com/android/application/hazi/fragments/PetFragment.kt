package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentPetBinding
import com.android.application.hazi.utils.EnergyWorker
import com.android.application.hazi.utils.HungerWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PetFragment : Fragment() {

    private lateinit var binding: FragmentPetBinding

    private lateinit var userDatabaseReference: DatabaseReference

    companion object {
        val TAG: String = PetFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val hungerWorkRequest = PeriodicWorkRequestBuilder<HungerWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(hungerWorkRequest)

        val energyWorkRequest = PeriodicWorkRequestBuilder<EnergyWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(requireContext()).enqueue(energyWorkRequest)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentPetBinding.bind(view)

        binding.openLockerButton.setOnClickListener { openLocker() }
        binding.feedPetButton.setOnClickListener { feedPet(binding.hungerProgressBar.progress) }

        binding.hungerProgressBar.max = 100
        binding.energyProgressBar.max = 100

        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val petRef = userDatabaseReference.child("pet")

            petRef.child("hunger").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val hunger = snapshot.value.toString().toInt()

                    binding.hungerProgressBar.progress = hunger
                    binding.feedPetButton.isEnabled = hunger >= 10
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(ShopItemFragment.TAG, "Database error occurred: $error")
                }
            })

            petRef.child("energy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val energy = snapshot.value.toString().toInt()

                    binding.energyProgressBar.progress = energy
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(ShopItemFragment.TAG, "Database error occurred: $error")
                }
            })
        }

//        binding.feedPetButton.isEnabled = binding.hungerProgressBar.progress >= 10
    }

    private fun feedPet(hunger: Int) {
        val hungerRef = userDatabaseReference.child("pet").child("hunger")

        if (hunger >= 10) {
            val updatedHunger = hunger - 10
            hungerRef.setValue(updatedHunger)
            binding.hungerProgressBar.progress = updatedHunger
            binding.feedPetButton.isEnabled = binding.hungerProgressBar.progress >= 10
        }
    }

    private fun openLocker() {
        findNavController().navigate(R.id.action_petFragment_to_lockerFragment)
    }
}