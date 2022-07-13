package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentShopBinding
import com.android.application.hazi.models.ShopItem
import com.android.application.hazi.utils.ShopItemActionListener
import com.android.application.hazi.utils.ShopItemsAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopFragment : Fragment() {

    private lateinit var binding: FragmentShopBinding
    private lateinit var adapter: ShopItemsAdapter

    private lateinit var shopItems: MutableList<ShopItem>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShopBinding.bind(view)

        initShopItemsRecyclerView()

        val database = FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid

        lifecycleScope.launch {
            val userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val userCoins = userDatabaseReference.child("coins")

            userCoins.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val coins = snapshot.value.toString().toInt()

                    // Make button disabled if user doesn't have enough coins and display the coins amount
                    binding.userCoins.text = "$coins coins"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(ShopItemFragment.TAG, "Database error occurred: $error")
                }
            })
        }
    }

    private fun initShopItemsRecyclerView() {
        shopItems = mutableListOf(
            ShopItem("DaBaby Skin", 50, "https://apeconcerts.com/wp-content/uploads/tm_attraction/dababy.jpg"),
            ShopItem("Cool Sunglasses", 100, "https://art.pixilart.com/4ca200e5dcab17a.png"),
            ShopItem("Cat Ears", 20, "https://avatars.mds.yandex.net/i?id=8f5da23f44c5f3e0d2db3ae0418873dd-5240247-images-thumbs&n=13"),
            ShopItem("Louis Vuitton bag", 40, "https://avatars.mds.yandex.net/i?id=2a0000017a0a75ab4dc7a6d3254c8c45cd96-4054771-images-thumbs&n=13"),
            ShopItem("A sweater", 50, "https://avatars.mds.yandex.net/i?id=4b3837071cca0fdb256aa2893a62168b-5583010-images-thumbs&n=13")
        )
        
        adapter = ShopItemsAdapter(shopItems, object : ShopItemActionListener {
            override fun onShopItemClick(shopItem: ShopItem) {
                val direction = ShopFragmentDirections.actionShopFragmentToShopItemFragment(
                    shopItem.name.toString(),
                    shopItem.price.toString(),
                    shopItem.image
                )
                findNavController().navigate(direction)
            }
        })
        binding.shopRecyclerView.adapter = adapter
    }

}