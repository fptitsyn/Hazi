package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentTabsBinding
import com.android.application.hazi.utils.MyApplication
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TabsFragment : Fragment() {

    private lateinit var binding: FragmentTabsBinding

    companion object {
        val TAG: String = TabsFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTabsBinding.bind(view)

        val navHost = childFragmentManager.findFragmentById(R.id.tabsContainer) as NavHostFragment
        val navController = navHost.navController
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

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
}