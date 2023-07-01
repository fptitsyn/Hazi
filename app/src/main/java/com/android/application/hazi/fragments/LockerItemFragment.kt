package com.android.application.hazi.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.android.application.hazi.R
import com.android.application.hazi.databinding.FragmentLockerItemBinding
import com.bumptech.glide.Glide

class LockerItemFragment : Fragment() {

    private lateinit var binding: FragmentLockerItemBinding

    private val args by navArgs<LockerItemFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_locker_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentLockerItemBinding.bind(view)

        val lockerItemName = args.lockerItemName
        val lockerItemImage = args.lockerItemImage

        binding.lockerItemNameTextView.text = lockerItemName

        if (lockerItemImage != null) {
            Glide.with(binding.lockerItemImageView.context)
                .load(lockerItemImage)
                .centerCrop()
                .placeholder(R.drawable.ic_shop)
                .error(R.drawable.ic_shop)
                .into(binding.lockerItemImageView)
        } else {
            Glide.with(binding.lockerItemImageView.context).clear(binding.lockerItemImageView)
            binding.lockerItemImageView.setImageResource(R.drawable.ic_shop)
        }

        binding.equipLockerItemButton.setOnClickListener {
            var status = ""
            if (binding.equipLockerItemButton.text == getString(R.string.equip)) {
                binding.equipLockerItemButton.text = getString(R.string.unequip)
                status = "$lockerItemName equipped"
            } else {
                binding.equipLockerItemButton.text = getString(R.string.equip)
                status = "$lockerItemName unequipped"
            }
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
    }

}