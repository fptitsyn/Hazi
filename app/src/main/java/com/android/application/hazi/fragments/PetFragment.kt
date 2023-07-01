package com.android.application.hazi.fragments

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentPetBinding
import com.android.application.hazi.utils.MyApplication
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PetFragment : Fragment() {

    private lateinit var binding: FragmentPetBinding

    private lateinit var userDatabaseReference: DatabaseReference

    companion object {
        val TAG: String = PetFragment::class.java.simpleName
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

        val hunger = MyApplication.hunger
        val energy = MyApplication.energy

        binding.hungerProgressBar.post {
            binding.hungerProgressBar.progress = hunger
        }

        binding.energyProgressBar.post {
            binding.energyProgressBar.progress = energy
        }

        createMenu()

        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref
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
            if (coins >= 5) {
                val updatedHunger = hunger - 10
                hungerRef.setValue(updatedHunger)
                MyApplication.hunger = updatedHunger
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