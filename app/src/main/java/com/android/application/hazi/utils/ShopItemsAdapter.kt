package com.android.application.hazi.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.application.hazi.R
import com.android.application.hazi.databinding.ShopItemBinding
import com.android.application.hazi.models.ShopItem
import com.bumptech.glide.Glide

interface ShopItemActionListener {
    fun onShopItemClick(shopItem: ShopItem)
}

class ShopItemsAdapter(
    private val shopItems: MutableList<ShopItem>,
    private val shopItemClickListener: ShopItemActionListener)
    : RecyclerView.Adapter<ShopItemsAdapter.ShopItemsViewHolder>(), View.OnClickListener {

    override fun onClick(v: View) {
        val shopItem = v.tag as ShopItem

        shopItemClickListener.onShopItemClick(shopItem)
    }

    class ShopItemsViewHolder (
        val binding: ShopItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopItemsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ShopItemBinding.inflate(inflater, parent, false)

        binding.root.setOnClickListener(this)

        return ShopItemsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopItemsViewHolder, position: Int) {
        val shopItem = shopItems[position]

        with(holder.binding) {
            holder.itemView.tag = shopItem

            shopItemNameTextView.text = shopItem.name
            shopItemPriceTextView.text = shopItem.price.toString()
            if (shopItem.image!!.isNotBlank()) {
                Glide.with(shopItemImageView.context)
                    .load(shopItem.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_shop)
                    .error(R.drawable.ic_shop)
                    .into(shopItemImageView)
            } else {
                Glide.with(shopItemImageView.context).clear(shopItemImageView)
                shopItemImageView.setImageResource(R.drawable.ic_shop)
            }
        }
    }

    override fun getItemCount(): Int {
        return shopItems.size
    }
}