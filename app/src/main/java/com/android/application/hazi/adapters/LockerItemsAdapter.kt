package com.android.application.hazi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.application.hazi.R
import com.android.application.hazi.databinding.LockerItemBinding
import com.android.application.hazi.models.ShopItem
import com.bumptech.glide.Glide

interface LockerItemActionListener {
    fun onLockerItemClick(shopItem: ShopItem)
}

class LockerItemsAdapter(
    private val lockerItems: MutableList<ShopItem>,
    private val lockerItemClickListener: LockerItemActionListener
    ) : RecyclerView.Adapter<LockerItemsAdapter.LockerItemViewHolder>(), View.OnClickListener {

    override fun onClick(view: View) {
        val lockerItem = view.tag as ShopItem

        lockerItemClickListener.onLockerItemClick(lockerItem)
    }

    class LockerItemViewHolder (
        val binding: LockerItemBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LockerItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LockerItemBinding.inflate(inflater, parent, false)

        binding.root.setOnClickListener(this)

        return LockerItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LockerItemViewHolder, position: Int) {
        val lockerItem = lockerItems[position]

        with(holder.binding) {
            holder.itemView.tag = lockerItem

            lockerItemNameTextView.text = lockerItem.name
            if (!lockerItem.image.isNullOrBlank()) {
                Glide.with(lockerItemImageView.context)
                    .load(lockerItem.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_shop)
                    .error(R.drawable.ic_shop)
                    .into(lockerItemImageView)
            } else {
                Glide.with(lockerItemImageView.context).clear(lockerItemImageView)
                lockerItemImageView.setImageResource(R.drawable.ic_shop)
            }
        }
    }

    override fun getItemCount(): Int {
        return lockerItems.size
    }

}