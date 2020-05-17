package co.kica.fileutils

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.*

class SmartFileInputStream : InputStream {
    private var buffer: ByteArray? = null
    private var byteptr = 0L
    private var file: SmartFile
    private var size: Long = 0

    constructor(var1: SmartFile) {
        file = var1
        if (var1.exists() && var1.isFile) {
            size = file.length()
            byteptr = 0L
            precache()
        } else {
            throw FileNotFoundException(file.absolutePath + " (not a valid smartfile)")
        }
    }

    constructor(var1: String) {
        val var2 = SmartFile(var1)
        file = var2
        if (var2.exists() && var2.isFile) {
            size = file.length()
            byteptr = 0L
            precache()
        } else {
            throw FileNotFoundException(file.absolutePath + " (not a valid smartfile)")
        }
    }

    private fun precache() {
        buffer = file.buffer
        println("precache(): sitting on " + buffer!!.size)
        size = buffer!!.size.toLong()
    }

    override fun available(): Int {
        return (size - byteptr).toInt()
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val var1: Int
        if (byteptr in 0L until size) {
            val var4 = buffer
            val var2 = byteptr
            byteptr = 1L + var2
            var1 = var4!![var2.toInt()].toInt() and 255
        } else {
            var1 = -1
        }
        return var1
    }

    @Throws(IOException::class)
    override fun read(var1: ByteArray): Int {
        var var2: Int
        if (byteptr >= size) {
            var2 = -1
        } else {
            val var7: Long
            var7 = if (size - byteptr >= var1.size.toLong()) {
                var1.size.toLong()
            } else {
                size - byteptr
            }
            val var4 = var7.toInt()
            println("Requested " + var1.size + ", can give it " + var4)
            if (var4 == 0) {
                var2 = -1
            } else {
                val var9 = buffer!!.copyOfRange(byteptr.toInt(), (byteptr + var4.toLong()).toInt())
                var var3 = 0
                val var5 = var9.size
                var2 = 0
                while (var2 < var5) {
                    val var10000 = var9[var2]
                    var1[var3] = var9[var3]
                    ++var3
                    ++var2
                }
                byteptr += var4.toLong()
                var2 = var4
            }
        }
        return var2
    }

    @Throws(IOException::class)
    override fun read(var1: ByteArray, var2: Int, var3: Int): Int {
        var var3 = var3
        var var4 = var3
        if (var3 - var2 >= var1.size) {
            var4 = var1.size - var2
        }
        var3 = var4
        if (var4 < 0) {
            var3 = 0
        }
        var4 = var2
        var var5 = 0
        while (var4 < var2 + var3) {
            val var6 = this.read()
            if (var6 < 0) {
                break
            }
            var1[var5] = (var6 and 255).toByte()
            ++var4
            ++var5
        }
        return var5
    }

    override fun reset() {
        byteptr = 0L
    }

    companion object {
        private val cache: HashMap<*, *> = HashMap<Any?, Any?>()
    }
}