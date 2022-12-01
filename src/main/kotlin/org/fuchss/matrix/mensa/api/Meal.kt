package org.fuchss.matrix.mensa.api

import com.vdurmont.emoji.EmojiManager

data class Meal(
    val name: String,
    val foodAdditiveNumbers: List<String>,
    val priceStudent: Double,
    val priceGuest: Double,
    val priceEmployee: Double,
    val pricePupil: Double,
    val priceAdditive: Double,

    val fish: Boolean,
    val pork: Boolean,
    val cow: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean
) {
    fun entry(): String {
        val emojis = mutableListOf<String>()
        addEmojis(emojis)
        val prefix = emojis.joinToString("/")
        return "$prefix $name"
    }

    private fun addEmojis(emojis: MutableList<String>) {
        addFish(emojis)
        addPork(emojis)
        addCow(emojis)
        addVegan(emojis)
        addVegetarian(emojis)
    }

    private fun addVegetarian(emojis: MutableList<String>) {
        if (vegetarian) emojis.add(":seedling:".toEmoji())
    }

    private fun addVegan(emojis: MutableList<String>) {
        if (vegan) emojis.add(":sunflower:".toEmoji())
    }

    private fun addCow(emojis: MutableList<String>) {
        if (cow) emojis.add(":cow:".toEmoji())
    }

    private fun addPork(emojis: MutableList<String>) {
        if (pork) emojis.add(":pig:".toEmoji())
    }

    private fun addFish(emojis: MutableList<String>) {
        if (fish) emojis.add(":fish:".toEmoji())
    }

    private fun String.toEmoji(): String = EmojiManager.getForAlias(this).unicode
}
