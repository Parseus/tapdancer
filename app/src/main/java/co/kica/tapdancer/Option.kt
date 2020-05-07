package co.kica.tapdancer

import java.util.*

class Option(val name: String?, val data: String, val path: String) : Comparable<Option> {

    override fun compareTo(o: Option): Int {
        return name?.toLowerCase(Locale.getDefault())?.compareTo(o.name!!.toLowerCase(Locale.getDefault()))
                ?: throw IllegalArgumentException()
    }

}