package co.kica.fileutils

import co.kica.tap.T64Format
import co.kica.tap.T64Format.DirEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class SmartFile : File {
    private val T64 = false
    private var filePath = ""
    private var shadowFileReader: ByteArray?
    private var shadowSelf: File? = null
    private var shadowT64: T64Format? = null
    private var shadowT64Entry: DirEntry?
    private var shadowZIP: ZipFile? = null
    private var shadowZIPEntry: ZipEntry?
    private var subpath = ""
    var type: SmartType

    constructor(var1: File?, var2: String?) : super(var1, var2) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(this.absolutePath)
    }

    constructor(var1: String?) : super(var1) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(this.absolutePath)
    }

    constructor(var1: String?, var2: String?) : super(var1, var2) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(this.absolutePath)
    }

    constructor(var1: URI?) : super(var1) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(this.absolutePath)
    }

    private fun entryT64(var1: String, var2: String): DirEntry? {
        val var3: Iterator<*> = shadowT64!!.dir.iterator()
        var var4: DirEntry?
        do {
            if (!var3.hasNext()) {
                var4 = null
                break
            }
            var4 = var3.next() as DirEntry?
        } while (var4!!.filename.replace("[ ]+$".toRegex(), "") != var2)
        return var4
    }

    @Throws(IOException::class)
    private fun extractFile(var1: ZipInputStream): ByteArray {
        val var6 = ByteArray(length().toInt())
        val var5 = ByteArray(512)
        var var2 = 0
        while (true) {
            val var4 = var1.read(var5)
            if (var4 == -1) {
                return var6
            }
            for (var3 in 0 until var4) {
                var6[var2 + var3] = var5[var3]
            }
            var2 += var4
        }
    }

    private fun sizeT64(): Long {
        val var3: Iterator<*> = shadowT64!!.dir.iterator()
        val var1: Long
        while (true) {
            if (!var3.hasNext()) {
                var1 = 0L
                break
            }
            val (_, filename, _, _, _, _, size) = var3.next() as DirEntry
            if (filename == subpath) {
                var1 = (size + 2).toLong()
                break
            }
        }
        return var1
    }

    private fun sizeZIP(): Long {
        val var3 = shadowZIP!!.getEntry(subpath)
        val var1: Long
        var1 = var3?.size ?: 0L
        return var1
    }

    private fun smartBreak(var1: String) {
        var var1 = var1
        val var5 = ""
        var var4 = File(var1)
        var var3 = var1
        var1 = var5
        while (!var4.exists() && var3.length > 0) {
            val var9 = var3.split(separator).toTypedArray()
            var1 = if (var1.length == 0) {
                var9[var9.size - 1]
            } else {
                var9[var9.size - 1] + separator + var1
            }
            var3 = ""
            for (var2 in 0 until var9.size - 1) {
                var3 = if (var2 == 0) {
                    ""
                } else {
                    var3 + separator + var9[var2]
                }
            }
            var4 = File(var3)
        }
        filePath = var3
        subpath = var1
        shadowSelf = File(filePath)
        if (filePath.toLowerCase().endsWith(".t64") && T64) {
            shadowT64 = T64Format(filePath, false)
            if (shadowT64!!.validHeader()) {
                type = SmartType.T64FILE
            }
        }
        if (filePath.toLowerCase().endsWith(".zip")) {
            try {
                val var8 = ZipFile(shadowSelf)
                shadowZIP = var8
                type = SmartType.ZIPFILE
            } catch (var6: ZipException) {
                var6.printStackTrace()
            } catch (var7: IOException) {
                var7.printStackTrace()
            }
        }
    }

    private fun subpathExistsT64(var1: String, var2: String): Boolean {
        val var4: Iterator<*> = shadowT64!!.dir.iterator()
        val var3: Boolean
        while (true) {
            if (!var4.hasNext()) {
                var3 = false
                break
            }
            if ((var4.next() as DirEntry).filename.replace("[ ]+$".toRegex(), "") == var2) {
                var3 = true
                break
            }
        }
        return var3
    }

    private fun subpathExistsZIP(var1: String, var2: String): Boolean {
        val var3: Boolean
        var3 = if (shadowZIP!!.getEntry(var2) != null) {
            true
        } else {
            false
        }
        return var3
    }

    fun byteAt(var1: Long): Int {
        val var4: Byte = -1
        var var3: Int
        if (type == SmartType.PHYSICAL) {
            if (shadowFileReader == null) {
                try {
                    shadowFileReader = ByteArray(length().toInt())
                    val var5 = FileInputStream(shadowSelf)
                    var5.read(shadowFileReader)
                    var5.close()
                    System.err.println("read it")
                } catch (var6: FileNotFoundException) {
                    System.err.println("not found: " + this.absolutePath)
                    var3 = var4.toInt()
                    return var3
                } catch (var7: IOException) {
                    System.err.println("i/o: " + this.absolutePath)
                    var3 = var4.toInt()
                    return var3
                }
            }
            var3 = if (var1 < shadowFileReader!!.size.toLong() && var1 >= 0L) {
                shadowFileReader!![var1.toInt()].toInt() and 255
            } else {
                System.err.println("out of range seek: " + this.absolutePath)
                var4.toInt()
            }
        } else if (type == SmartType.T64FILE) {
            if (shadowT64Entry == null) {
                shadowT64Entry = entryT64(filePath, subpath)
                var3 = var4.toInt()
                if (shadowT64Entry == null) {
                    return var3
                }
            }
            if (var1 == 0L) {
                var3 = shadowT64Entry!!.start % 256
            } else if (var1 == 1L) {
                var3 = shadowT64Entry!!.start / 256
            } else {
                val var8 = shadowT64Entry!!.programData
                var3 = var4.toInt()
                if (var1 - 2L < var8.size.toLong()) {
                    var3 = var4.toInt()
                    if (var1 - 2L >= 0L) {
                        var3 = var8[(var1 - 2L).toInt()].toInt() and 255
                    }
                }
            }
        } else {
            var3 = var4.toInt()
            if (type == SmartType.ZIPFILE) {
                if (shadowFileReader == null) {
                    shadowFileReader = decompressFile()
                }
                if (var1 < shadowFileReader!!.size.toLong() && var1 >= 0L) {
                    var3 = shadowFileReader!![var1.toInt()].toInt() and 255
                } else {
                    System.err.println("out of range seek: " + this.absolutePath)
                    var3 = var4.toInt()
                }
            }
        }
        return var3
    }

    fun decompressFile(): ByteArray {
        try {
            val localZipInputStream = ZipInputStream(FileInputStream(filePath))
            var localZipEntry: ZipEntry
            var localObject: Any? = localZipInputStream.nextEntry
            while (true) {
                val arrayOfByte: ByteArray
                if (localObject == null) {
                    localZipInputStream.close()
                    arrayOfByte = ByteArray(0)
                    return arrayOfByte
                }
                while (true) {
                    if ((localObject as ZipEntry).name != subpath) break
                    arrayOfByte = extractFile(localZipInputStream)
                    localZipInputStream.closeEntry()
                    localZipInputStream.close()
                    return arrayOfByte
                }
                localZipInputStream.closeEntry()
                localZipEntry = localZipInputStream.nextEntry
                localObject = localZipEntry
            }
        } catch (localIOException: IOException) {
            return ByteArray(0)
        }
    }

    override fun exists(): Boolean {
        val var1: Boolean
        var1 = if (subpath.length == 0) {
            shadowSelf!!.exists()
        } else if (!shadowSelf!!.exists()) {
            false
        } else if (type == SmartType.T64FILE) {
            subpathExistsT64(filePath, subpath)
        } else if (type == SmartType.ZIPFILE) {
            subpathExistsZIP(filePath, subpath)
        } else {
            true
        }
        return var1
    }

    val buffer: ByteArray?
        get() {
            var var2: ByteArray?
            if (type == SmartType.PHYSICAL) {
                try {
                    var2 = ByteArray(length().toInt())
                    val var3 = FileInputStream(shadowSelf)
                    var3.read(var2, 0, var2.size)
                    var3.close()
                } catch (var5: FileNotFoundException) {
                    var2 = null
                } catch (var6: IOException) {
                    var2 = null
                }
            } else if (type == SmartType.T64FILE) {
                if (shadowT64Entry == null) {
                    shadowT64Entry = entryT64(filePath, subpath)
                    if (shadowT64Entry == null) {
                        var2 = null
                        return var2
                    }
                }
                val var4 = shadowT64Entry!!.programData
                val var7 = ByteArray(var4.size + 2)
                var7[0] = (shadowT64Entry!!.start % 256).toByte()
                var7[1] = (shadowT64Entry!!.start / 256).toByte()
                var var1 = 0
                while (true) {
                    var2 = var7
                    if (var1 >= var4.size) {
                        break
                    }
                    var7[var1 + 2] = var4[var1]
                    ++var1
                }
            } else if (type == SmartType.ZIPFILE) {
                var2 = decompressFile()
                println("Buffer from decompressFile() is " + var2.size + " bytes ")
            } else {
                var2 = null
            }
            return var2
        }

    override fun getParentFile(): SmartFile {
        return SmartFile(this.parent)
    }

    override fun isDirectory(): Boolean {
        val var2 = true
        var var1: Boolean
        if (type == SmartType.PHYSICAL && shadowSelf!!.isDirectory) {
            var1 = var2
        } else {
            if (type == SmartType.T64FILE) {
                var1 = var2
                if (subpath == "") {
                    return var1
                }
            }
            if (type == SmartType.ZIPFILE) {
                var1 = var2
                if (subpath.length != 0) {
                    val var3 = shadowZIP!!.getEntry(subpath)
                    if (var3 == null) {
                        var1 = false
                    } else {
                        System.err.println("exists: " + var3.name + " " + var3.size)
                        var1 = var2
                        if (var3.size != 0L) {
                            var1 = var3.isDirectory
                        }
                    }
                }
            } else {
                var1 = false
            }
        }
        return var1
    }

    override fun isFile(): Boolean {
        return exists() && !isDirectory
    }

    override fun length(): Long {
        return if (exists()) {
            when {
                subpath.isEmpty() -> super.length()
                type == SmartType.T64FILE -> sizeT64()
                type == SmartType.ZIPFILE -> sizeZIP()
                else -> 0L
            }
        } else 0L
    }

    override fun listFiles(): Array<SmartFile>? {
        return if (this.isDirectory) {
            when {
                shadowSelf!!.isDirectory -> {
                    val directoryList = shadowSelf!!.listFiles()
                    Array(directoryList.size) { index -> SmartFile(directoryList[index], "") }
                }

                type == SmartType.T64FILE -> {
                    val t64DirectoryList = shadowT64!!.dir
                    Array(t64DirectoryList.size) { index -> SmartFile(filePath + separator + t64DirectoryList[index].filename.replace("[ ]+$".toRegex(), "")) }
                }

                type == SmartType.ZIPFILE -> {
                    val zipEntries = shadowZIP!!.entries()
                    val smartFilesList = arrayListOf<SmartFile>()
                    while (zipEntries.hasMoreElements()) {
                        val zipEntry = zipEntries.nextElement()
                        val isValidFile = !zipEntry.isDirectory && zipEntry.size != 0L

                        if (isValidFile && zipEntry.name != subpath + separator
                                && !zipEntry.name.startsWith("_")
                                && !zipEntry.name.endsWith(".DS_Store")) {
                            smartFilesList.add(SmartFile(filePath + separator + subpath, zipEntry.name))
                        }
                    }
                    smartFilesList.toTypedArray()
                }

                else -> emptyArray()
            }
        } else null
    }

    enum class SmartType {
        PHYSICAL, T64FILE, ZIPFILE
    }

    companion object {
        private const val serialVersionUID = 491916781031060975L
    }
}