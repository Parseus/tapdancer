package co.kica.fileutils

import android.util.Log
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
    private val isT64 = false
    private var filePath = ""
    private var shadowFileReader: ByteArray?
    private lateinit var shadowSelf: File
    private var shadowT64: T64Format? = null
    private var shadowT64Entry: DirEntry?
    private var shadowZIP: ZipFile? = null
    private var shadowZIPEntry: ZipEntry?
    private var subpath = ""
    var type: SmartType

    constructor(parent: File?, child: String) : super(parent, child) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(absolutePath)
    }

    constructor(pathname: String) : super(pathname) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(absolutePath)
    }

    constructor(parent: String, child: String) : super(parent, child) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(absolutePath)
    }

    constructor(uri: URI) : super(uri) {
        type = SmartType.PHYSICAL
        shadowFileReader = null
        shadowT64Entry = null
        shadowZIPEntry = null
        smartBreak(absolutePath)
    }

    private fun entryT64(subPath: String): DirEntry? {
        val dirEntries = shadowT64!!.dir.iterator()

        while (dirEntries.hasNext()) {
            val entry = dirEntries.next()

            if (entry.filename.replace("[ ]+$".toRegex(), "") == subPath) {
                return entry
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun extractFile(inputStream: ZipInputStream): ByteArray {
        return inputStream.readBytes()
    }

    private fun sizeT64(): Long {
        val dirEntries = shadowT64!!.dir.iterator()

        while (dirEntries.hasNext()) {
            val entry = dirEntries.next()

            if (entry.filename == subpath) {
                return (entry.size + 2).toLong()
            }
        }

        return 0L
    }

    private fun sizeZIP(): Long {
        return shadowZIP?.getEntry(subpath)?.size ?: 0L
    }

    private fun smartBreak(absolutePath: String) {
        var path = " "
        var file = File(absolutePath)
        var fileSubpath = ""

        while (!file.exists() && path.isNotEmpty()) {
            val pathSplit = absolutePath.split(separator).toTypedArray()
            fileSubpath = if (fileSubpath.isEmpty()) {
                pathSplit[pathSplit.size - 1]
            } else {
                pathSplit[pathSplit.size - 1] + separator + fileSubpath
            }

            for (i in 0 until pathSplit.size - 1) {
                path = if (i == 0) {
                    ""
                } else {
                    path + separator + pathSplit[i]
                }
            }

            file = File(path)
        }

        filePath = path
        subpath = fileSubpath
        shadowSelf = File(filePath)

        if (filePath.endsWith(".t64", true) && isT64) {
            shadowT64 = T64Format(filePath, false)

            if (shadowT64!!.hasValidHeader()) {
                type = SmartType.T64FILE
            }
        }

        if (filePath.endsWith(".zip", true)) {
            try {
                shadowZIP = ZipFile(shadowSelf)
                type = SmartType.ZIPFILE
            } catch (e: ZipException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun subpathExistsT64(subPath: String): Boolean {
        val dirEntries = shadowT64!!.dir.iterator()

        while (dirEntries.hasNext()) {
            val entry = dirEntries.next()

            if (entry.filename.replace("[ ]+$".toRegex(), "") == subPath) {
                return true
            }
        }

        return false
    }

    private fun subpathExistsZIP(subPath: String): Boolean {
        return shadowZIP!!.getEntry(subPath) != null
    }

    fun byteAt(index: Long): Int {
        var byteRead: Int = -1

        if (type == SmartType.PHYSICAL) {
            if (shadowFileReader == null) {
                try {
                    shadowFileReader = ByteArray(length().toInt())
                    shadowSelf.inputStream().use {
                        it.read(shadowFileReader!!)
                    }
                    Log.d(TAG, "read it")
                } catch (e: FileNotFoundException) {
                    Log.e(TAG, "not found: $absolutePath")
                    return -1
                } catch (e: IOException) {
                    Log.e(TAG, "i/o: $absolutePath")
                    return -1
                }
            }

            byteRead = if (index < shadowFileReader!!.size.toLong() && index >= 0L) {
                shadowFileReader!![index.toInt()].toInt() and 255
            } else {
                Log.e(TAG, "out of range seek: $absolutePath")
                -1
            }
        } else if (type == SmartType.T64FILE) {
            if (shadowT64Entry == null) {
                shadowT64Entry = entryT64(subpath)

                if (shadowT64Entry == null) {
                    return -1
                }
            }

            if (index == 0L) {
                byteRead = shadowT64Entry!!.start % 256
            } else if (index == 1L) {
                byteRead = shadowT64Entry!!.start / 256
            } else {
                val programData = shadowT64Entry!!.programData
                if (index - 2L < programData.size.toLong()) {
                    if (index - 2L >= 0L) {
                        byteRead = programData[(index - 2L).toInt()].toInt() and 255
                    }
                }
            }
        } else {
            if (type == SmartType.ZIPFILE) {
                if (shadowFileReader == null) {
                    shadowFileReader = decompressFile()
                }

                byteRead = if (index < shadowFileReader!!.size.toLong() && index >= 0L) {
                    shadowFileReader!![index.toInt()].toInt() and 255
                } else {
                    Log.e(TAG, "out of range seek: $absolutePath")
                    -1
                }
            }
        }

        return byteRead
    }

    private fun decompressFile(): ByteArray {
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
        return when {
            subpath.isEmpty() -> shadowSelf.exists()
            !shadowSelf.exists() -> false
            type == SmartType.T64FILE -> subpathExistsT64(subpath)
            type == SmartType.ZIPFILE -> subpathExistsZIP(subpath)
            else -> true
        }
    }

    val buffer: ByteArray?
        get() {
            return when (type) {
                SmartType.PHYSICAL -> {
                    try {
                        val byteArray = ByteArray(length().toInt())
                        FileInputStream(shadowSelf).use {
                            it.read(byteArray, 0, byteArray.size)
                        }
                        byteArray
                    } catch (e: FileNotFoundException) {
                        null
                    } catch (e: IOException) {
                        null
                    }
                }
                SmartType.T64FILE -> {
                    if (shadowT64Entry == null) {
                        shadowT64Entry = entryT64(subpath)
                        if (shadowT64Entry == null) {
                            return null
                        }
                    }

                    val programData = shadowT64Entry!!.programData
                    val completeData = ByteArray(programData.size + 2)
                    completeData[0] = (shadowT64Entry!!.start % 256).toByte()
                    completeData[1] = (shadowT64Entry!!.start / 256).toByte()

                    var index = 0
                    while (true) {
                        if (index >= programData.size) {
                            break
                        }

                        completeData[index + 2] = programData[index]
                        ++index
                    }

                    completeData
                }
                SmartType.ZIPFILE -> {
                    decompressFile().also { b -> Log.i(TAG, "Buffer from decompressFile() is ${b.size} bytes") }
                }
            }
        }

    override fun getParentFile(): SmartFile? {
        return if (parent != null) SmartFile(parent!!) else null
    }

    override fun isDirectory(): Boolean {
        return when (type) {
            SmartType.PHYSICAL -> shadowSelf.isDirectory

            SmartType.T64FILE -> subpath.isBlank()

            SmartType.ZIPFILE -> {
                if (subpath.isNotEmpty()) {
                    val entry = shadowZIP!!.getEntry(subpath)

                    if (entry != null) {
                        Log.d(TAG, "exists: ${entry.name} ${entry.size}")
                        entry.size != 0L && entry.isDirectory
                    }
                }

                false
            }
        }
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
                shadowSelf.isDirectory -> {
                    val directoryList = shadowSelf.listFiles()!!
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
        private const val TAG = "SmartFile"
        private const val serialVersionUID = 491916781031060975L
    }
}