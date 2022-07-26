package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.*
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentPetBinding
import com.android.application.hazi.utils.EnergyWorker
import com.android.application.hazi.utils.HungerWorker
import com.android.application.hazi.utils.MyApplication
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

        val userCoins = MyApplication.coins
        binding.userCoinsTextView.text = "$userCoins coins"
        binding.userCoinsTextView.visibility = View.VISIBLE

        binding.feedPetButton.setOnClickListener { feedPet() }

        binding.hungerProgressBar.max = 100
        binding.energyProgressBar.max = 100

        createMenu()

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
    }

    private fun createMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.open_locker_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.openLocker -> {
                        openLocker()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun feedPet() {
        val hungerRef = userDatabaseReference.child("pet").child("hunger")

        val coins = MyApplication.coins
        val hunger = binding.hungerProgressBar.progress

        if (hunger >= 10) {
            Log.d(TAG, coins.toString())
            if (coins >= 5) {
                val updatedHunger = hunger - 10
                hungerRef.setValue(updatedHunger)
                binding.hungerProgressBar.progress = updatedHunger

                val updatedCoins = coins - 5
                userDatabaseReference.child("coins").setValue(updatedCoins)
                MyApplication.coins = updatedCoins
                binding.userCoinsTextView.text = "$updatedCoins coins"
            } else {
                Toast.makeText(requireContext(), "You don't have enough money", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "The pet is not hungry", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLocker() {
        findNavController().navigate(R.id.action_petFragment_to_lockerFragment)
    }
}