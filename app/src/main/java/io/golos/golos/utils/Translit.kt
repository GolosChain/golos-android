package io.golos.golos.utils

import java.lang.StringBuilder

object Translit {
    val rusLetters = "щ    ш  ч  ц  й  ё  э  ю  я  х  ж  а б в г д е з и к л м н о п р с т у ф ъ  ы ь ґ є і ї".split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val engLetters = "shch sh ch cz ij yo ye yu ya kh zh a b v g d e z i k l m n o p r s t u f xx y x g e i i".split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    fun lat2Ru(string: String): String {

        val current = StringBuilder(string.toLowerCase())

        var place = current.indexOf("yie")
        if (place >= 0) {
            current.replace(place, place + 3, "ые")
        }

        for (i in rusLetters.indices) {
            place = current.indexOf(engLetters[i])
            if (place >= 0) {
                current.replace(place, place + engLetters[i].length, rusLetters[i])
            }
        }
        return current.toString()
    }


    fun ru2lat(string: String): String {
        var current = string.toLowerCase()
        current = current.replace("ые", "yie")
        for (i in engLetters.indices) {
            current = current.replace(rusLetters[i], engLetters[i])
        }
        return current
    }
}

