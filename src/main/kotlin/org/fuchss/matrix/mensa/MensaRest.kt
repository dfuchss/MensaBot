package org.fuchss.matrix.mensa

import org.fuchss.matrix.mensa.request.MensaAPI


suspend fun main() {
    val mensa = MensaAPI()
    val mensaFoodToday = mensa.foodAtDate()

    for (m in mensaFoodToday) {
        println("# ${m.name}")
        for (l in m.mensaLinesAtDate()!!) {
            println("## ${l.title}")
            for (m in l.meals) {
                println("* ${m.name}")
            }
        }
    }
}