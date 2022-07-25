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
import androidx.navigation.fragment.navArgs
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentShopItemBinding
import com.android.application.hazi.models.ShopItem
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ShopItemFragment : Fragment() {

    private lateinit var binding: FragmentShopItemBinding

    private val args by navArgs<ShopItemFragmentArgs>()

    private lateinit var userDatabaseReference: DatabaseReference

    private var userCoins = 0

    private var canBuyItem = true

    companion object {
        val TAG: String = ShopItemFragment::class.java.simpleName
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_shop_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShopItemBinding.bind(view)

        // Get shop item info
        val shopItemName = args.shopItemName
        val shopItemPrice = args.shopItemPrice
        val shopItemImage = args.shopItemImage
        userCoins = args.userCoins

        binding.buyShopItemButton.setOnClickListener {
            buyShopItem(
                shopItemName,
                shopItemPrice,
                shopItemImage.toString(),
                userCoins
            )
        }

        // Update UI
        binding.shopItemNameTextView.text = shopItemName
        binding.shopItemPriceTextView.text = shopItemPrice
        if (!shopItemImage.isNullOrBlank()) {
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

        // Make button disabled if user doesn't have enough coins and display the coins amount
        binding.userCoins.text = "$userCoins coins"
        binding.userCoins.visibility = View.VISIBLE

        // Checking if an item is already bought
        // This is being done in that method because it looks like the database operations complete faster this way
        val database =
            FirebaseDatabase.getInstance("https://hazi-8190a-default-rtdb.europe-west1.firebasedatabase.app/")

        val currentUserId = Firebase.auth.currentUser?.uid
        lifecycleScope.launch {
            userDatabaseReference = database.reference.child("users")
                .orderByChild("id").equalTo(currentUserId).get()
                .await().children.first().ref

            val userShopItemsRef = userDatabaseReference.child("shopItems")

            userShopItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value != null) {
                        val shopItems: HashMap<*, HashMap<String, *>> =
                            snapshot.value as HashMap<String, HashMap<String, *>>

                        for (shopItem in shopItems.values) {
                            Log.d(TAG, shopItem.toString())
                            val userShopItemName = shopItem["name"]
                            if (shopItemName == userShopItemName) {
                                binding.buyShopItemButton.isEnabled = false
                                canBuyItem = false

                                Toast.makeText(
                                    requireContext(),
                                    "Item already owned",
                                    Toast.LENGTH_SHORT
                                ).show()

                                break
                            }
                        }

                        // If an item is not already bought, check if user has enough money to buy the item
                        if (canBuyItem) {
                            binding.buyShopItemButton.isEnabled = userCoins >= shopItemPrice.toInt()
                        }
                    }
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

        val userCoinsRef = userDatabaseReference.child("coins")
        val coinsUpdated = coins - shopItemPrice.toInt()
        userCoinsRef.setValue(coinsUpdated)

        binding.userCoins.text = coinsUpdated.toString()

        Toast.makeText(
            requireContext(),
            "$shopItemName has been bought successfully",
            Toast.LENGTH_SHORT
        ).show()

        findNavController().navigateUp()
    }
}