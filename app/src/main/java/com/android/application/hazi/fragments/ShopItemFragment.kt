package com.android.application.hazi.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentShopItemScreenBinding
import com.android.application.hazi.models.ShopItem
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopItemFragment : Fragment() {

    private lateinit var binding: FragmentShopItemScreenBinding

    private val args by navArgs<ShopItemFragmentArgs>()

    private lateinit var database: FirebaseDatabase
    private lateinit var userDatabaseReference: DatabaseReference
    private var currentUserId: String? = ""

    private var coins = 0

    companion object {
        val TAG = ShopItemFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop_item_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShopItemScreenBinding.bind(view)

        // Get shop item info
        val shopItemName = args.shopItemName
        val shopItemPrice = args.shopItemPrice
        val shopItemImage = args.shopItemImage

        binding.buyShopItemButton.setOnClickListener {
            buyShopItem(
                shopItemName,
                shopItemPrice,
                shopItemImage.toString(),
                coins
            )
        }

        // Update UI
        binding.shopItemNameTextView.text = shopItemName
        binding.shopItemPriceTextView.text = shopItemPrice
        if (shopItemImage!!.isNotBlank()) {
            Glide.with(binding.shopItemImageView.context)
                .load(shopItemImage)
                .centerCrop()
                .placeholder(R.drawable.ic_shop)
                .error(R.drawable.ic_shop)
                .into(binding.shopItemImageView)
        } else {
            Glide.with(binding.shopItemImageView.context).clear(binding.shopItemImageView)
            binding.shopItemImageView.setImageResource(R.drawable.ic_shop)
        }

        // Get user coins
        database = FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val userCoins = userDatabaseReference.child("coins")

            userCoins.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    coins = snapshot.value.toString().toInt()
                    Log.d(TAG, coins.toString())

                    // Make button disabled if user doesn't have enough coins
                    binding.buyShopItemButton.isEnabled = coins >= shopItemPrice.toInt()
                    Toast.makeText(requireContext(), "$coins", Toast.LENGTH_SHORT).show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "Database error occurred: $error")
                }
            })
        }
    }

    private fun buyShopItem(
        shopItemName: String,
        shopItemPrice: String,
        shopItemImage: String,
        coins: Int
    ) {
        val userShopItems = userDatabaseReference.child("shopItems")
        val shopItem = ShopItem(shopItemName, shopItemPrice.toInt(), shopItemImage)
        userShopItems.push().setValue(shopItem)

        val userCoins = userDatabaseReference.child("coins")
        userCoins.setValue(coins - shopItemPrice.toInt())

        Toast.makeText(
            requireContext(),
            "$shopItemName has been bought successfully",
            Toast.LENGTH_SHORT
        ).show()
    }
}