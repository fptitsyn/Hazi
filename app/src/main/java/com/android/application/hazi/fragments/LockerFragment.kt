package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentLockerBinding
import com.android.application.hazi.models.ShopItem
import com.android.application.hazi.adapters.LockerItemActionListener
import com.android.application.hazi.adapters.LockerItemsAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Suppress("UNCHECKED_CAST")
class LockerFragment : Fragment() {

    private lateinit var binding: FragmentLockerBinding

    companion object {
        val TAG: String = LockerFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_locker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLockerBinding.bind(view)

        initLockerItemsRecyclerView()
    }

    private fun initLockerItemsRecyclerView() {
        val lockerItems = mutableListOf<ShopItem>()

        val adapter = LockerItemsAdapter(lockerItems, object : LockerItemActionListener {
            override fun onLockerItemClick(shopItem: ShopItem) {
                val direction = LockerFragmentDirections.actionLockerFragmentToLockerItemFragment(
                    shopItem.name.toString(),
                    shopItem.image
                )

                findNavController().navigate(direction)
            }
        })

        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            val userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val userShopItemsRef = userDatabaseReference.child("shopItems")

            userShopItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val shopItems: HashMap<*, HashMap<String, *>> =
                            snapshot.value as HashMap<String, HashMap<String, *>>

                        for (item in shopItems.values) {
                            val lockerItemName = item["name"].toString()
                            val lockerItemImage = item["image"].toString()
                            val lockerItem = ShopItem(lockerItemName, 0, lockerItemImage)
                            lockerItems.add(lockerItem)
                            adapter.notifyItemInserted(lockerItems.size)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Database error occurred: $error")
                }
            })
        }

        binding.lockerRecyclerView.adapter = adapter
    }
}