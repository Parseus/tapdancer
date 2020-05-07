package co.kica.tap

import co.kica.fileutils.SmartFile
import co.kica.fileutils.SmartFileInputStream
import java.io.*
import java.util.*

class T64Format(fn: String, smart: Boolean) {
    var filename: String? = null
    private lateinit var data: ByteArray
    private val progtype = 0
    private val start = 0
    private val end = 0

    data class DirEntry(
            var dref: ByteArray,
            var filename: String = "FILE",
            var type: Int = 0,
            var type_1541: Int = 0,
            var start: Int = 0,
            var end: Int = 0,
            var size: Int = 0,
            var offset: Int = 0
    ) {
        val programData: ByteArray
            get() = dref.copyOfRange(offset, offset + size)

        val programLoadAddress: Int
            get() = start
        val programEndAddress: Int
            get() = end

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DirEntry

            if (!dref.contentEquals(other.dref)) return false
            if (filename != other.filename) return false
            if (type != other.type) return false
            if (type_1541 != other.type_1541) return false
            if (start != other.start) return false
            if (end != other.end) return false
            if (size != other.size) return false
            if (offset != other.offset) return false

            return true
        }

        override fun hashCode(): Int {
            var result = dref.contentHashCode()
            result = 31 * result + filename.hashCode()
            result = 31 * result + type
            result = 31 * result + type_1541
            result = 31 * result + start
            result = 31 * result + end
            result = 31 * result + size
            result = 31 * result + offset
            return result
        }
    }

    private fun loadFileSmart(filename: String) {
        val loadAddr = ByteArray(2)
        val f = SmartFile(filename)
        data = ByteArray(f.length().toInt())
        try {
            val bis = BufferedInputStream(SmartFileInputStream(f))
            val plSize = bis.read(data)
            bis.close()
            this.filename = f.name.toUpperCase(Locale.getDefault()).replaceFirst(".T64$".toRegex(), "")
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    private fun loadFile(filename: String) {
        val loadAddr = ByteArray(2)
        val f = File(filename)
        data = ByteArray(f.length().toInt())
        try {
            val bis = BufferedInputStream(FileInputStream(f))
            val plSize = bis.read(data)
            bis.close()
            this.filename = f.name.toUpperCase(Locale.getDefault()).replaceFirst(".T64$".toRegex(), "")
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    val mAGIC: ByteArray
        get() = data.copyOfRange(0, 3)

    val versionMinor: Int
        get() = data[0x20].toInt() and 0xff

    val versionMajor: Int
        get() = data[0x21].toInt() and 0xff

    val maxDirEntries: Int
        get() = (data[0x22].toInt() and 0xff) + 256 * (data[0x23].toInt() and 0xff)

    val usedDirEntries: Int
        get() = (data[0x24].toInt() and 0xff) + 256 * (data[0x25].toInt() and 0xff)

    private fun byteArrayToString(bytes: ByteArray): String {
        var r = ""
        for (b in bytes) {
            val c = (b.toInt() and 0xff).toChar()
            r += c
        }
        return r
    }

    val tapeName: String
        get() {
            val d = data.copyOfRange(0x28, 0x40)
            return byteArrayToString(d)
        }

    fun validHeader(): Boolean {
        val magic = mAGIC
        val valid = byteArrayOf(0x43, 0x36, 0x34 /*, 0x53*/)
        return magic.contentEquals(valid)
    }

    fun getDirEntry(index: Int): DirEntry? {
        if (index >= maxDirEntries) {
            return null
        }
        val offset = 0x0040 + 0x20 * index
        val end = offset + 32
        val rec = data.copyOfRange(offset, end)
        val ftype: Int = rec[0x00].toInt() and 0xff
        if (ftype == 0) {
            return null // empty
        }
        var ext = ".PRG"
        val d = DirEntry(data)
        d.type = ftype
        d.type_1541 = rec[0x01].toInt() and 0xff
        if (d.type_1541 == 0) {
            ext = if (ftype > 1) {
                ".FRZ"
            } else {
                ".FRE"
            }
        }
        d.start = (rec[0x02].toInt() and 0xff) + 256 * (rec[0x03].toInt() and 0xff)
        d.end = (rec[0x04].toInt() and 0xff) + 256 * (rec[0x05].toInt() and 0xff)
        d.size = d.programEndAddress - d.programLoadAddress
        d.offset = (rec[0x08].toInt() and 0xff) + 256 * (rec[0x09].toInt() and 0xff) + 65536 * (rec[0x0a].toInt() and 0xff) + 16777216 * (rec[0x0b].toInt() and 0xff)
        d.filename = byteArrayToString(rec.copyOfRange(0x10, 0x20)).replace("[ ]+$".toRegex(), ext)
        return d
    }

    // fix for bad end addresses
    val dir: ArrayList<DirEntry>
        get() {
            val dir = ArrayList<DirEntry>()
            for (i in 0 until maxDirEntries) {
                val d = getDirEntry(i)
                if (d != null) {
                    dir.add(d)
                }
            }

            // fix for bad end addresses
            var end_offset = data.size
            for (j in dir.indices.reversed()) {
                val d = dir[j]
                if (d.programEndAddress == 0xc3c6) {
                    d.size = end_offset - d.offset
                    d.end = d.programLoadAddress + d.size
                }
                end_offset = d.offset + d.size
            }
            return dir
        }

    init {
        if (smart) loadFileSmart(fn) else loadFile(fn)
    }
}