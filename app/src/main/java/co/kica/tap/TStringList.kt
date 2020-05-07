package co.kica.tap

import java.util.*

class TStringList : ArrayList<String>() {
    fun Text(): String {
        var result = ""
        for (x in this.indices) {
            if (result !== "") {
                result = """
                    $result

                    """.trimIndent()
            }
            result += this[x]
        }
        return result
    }

    fun setText(key: String) {
        val parts = key.split("[\r\n]+").toTypedArray()
        this.clear()
        for (x in parts.indices) this.add(parts[x])
    }

    fun Count(): Int {
        return size
    }
}