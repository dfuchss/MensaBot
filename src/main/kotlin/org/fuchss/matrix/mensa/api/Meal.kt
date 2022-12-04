package org.fuchss.matrix.mensa.api

import com.vdurmont.emoji.EmojiManager

/**
 * This data class defines all information that should be provided by a meal.
 * @param[name] the name of the meal
 * @param[foodAdditiveNumbers] some specific additive numbers of the meal (e.g., "Nuts (PA)")
 * @param[priceStudent] the price for a student
 * @param[priceGuest] the price for a guest
 * @param[priceEmployee] the price for an employee
 * @param[pricePupil] the price for a pupil
 * @param[fish] indicator for fish
 * @param[pork] indicator for pork
 * @param[cow] indicator for cow
 * @param[vegan] indicator for vegan
 * @param[vegetarian] indicator for vegetarian
 */
data class Meal(
    val name: String,
    val foodAdditiveNumbers: List<String>,
    val priceStudent: Double,
    val priceGuest: Double,
    val priceEmployee: Double,
    val pricePupil: Double,

    val fish: Boolean,
    val pork: Boolean,
    val cow: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean
) {
    /**
     * Calculate a nice looking textual entry for a food (including emojis).
     */
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
        if (vegetarian) emojis.add(":carrot:".toEmoji())
    }

    private fun addVegan(emojis: MutableList<String>) {
        if (vegan) emojis.add(":ear_of_rice:".toEmoji())
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
