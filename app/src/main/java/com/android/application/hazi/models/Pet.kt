package com.android.application.hazi.models

data class Pet(var hunger: Int? = null, var energy: Int? = null, var equippedItems: MutableList<ShopItem>? = null)
