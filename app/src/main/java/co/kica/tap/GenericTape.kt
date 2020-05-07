package co.kica.tap

import co.kica.fileutils.SmartFile
import co.kica.fileutils.SmartFileInputStream
import java.io.*
import java.lang.Exception
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.GZIPInputStream

abstract class GenericTape() {
    var fileName = ""
    var name: String? = null
        private set
    var status = 0
    var header = ByteArrayOutputStream()
    var data = ByteArrayOutputStream()
    var isValid = false
    protected open var dataPos = 0
    private var inDataStream = false
    private var dataStartSamplePos = 0
    private var dataDurationSamples = 0
    var targetSampleRate = 44100

    constructor(fn: String) : this() {
        fileName = fn
    }

    init {
        fileName = ""
        status = tapeStatusOk
        isValid = true
        inDataStream = false
        dataDurationSamples = 0
        dataStartSamplePos = 0
    }

    private fun isGZIPed(fn: String): Boolean {
        return try {
            val buff = ByteArray(2)
            val inputStream = SmartFileInputStream(fn)
            inputStream.read(buff)
            //System.out.println(len+": "+buff[0]+","+buff[1]);
            inputStream.close()

            buff[0] == 31.toByte() && buff[1] == (-117).toByte()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    open fun load(fn: String) {
        fileName = fn

        // set human name
        val f = SmartFile(fn)
        val tmp = f.name
        name = tmp.replaceFirst("[.][^.]+$".toRegex(), "")
        name = name!!.replace("[()]".toRegex(), "")
        name = name!!.replace("[-_]+".toRegex(), " ")
        val gz = isGZIPed(fn)

        //System.out.println("GZIPed == "+gz);
        if (gz) {
            LoadGZIP(fn)
            return
        }
        isValid = false

        // open the file
        try {
            val inputStream: InputStream = SmartFileInputStream(fn)
            val buff = ByteArray(CHUNK)
            if (parseHeader(inputStream)) {
                if (isHeaderData) {
                    data.write(header!!.toByteArray())
                }
                var len = inputStream.read(buff)
                while (len > 0) {
                    data.write(buff, 0, len)
                    len = inputStream.read(buff)
                }
                inputStream.close()
                println("*** Read in " + data.size() + " bytes")
                isValid = true
            } else {
                status = tapeStatusBadHeader
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun LoadGZIP(fn: String) {
        isValid = false

        // open the file
        try {
            val inputStream: InputStream = GZIPInputStream(SmartFileInputStream(fn))
            val buff = ByteArray(CHUNK)
            if (parseHeader(inputStream)) {
                if (isHeaderData) {
                    data.write(header!!.toByteArray())
                }
                var len = inputStream.read(buff)
                while (len > 0) {
                    data.write(buff, 0, len)
                    len = inputStream.read(buff)
                }
                inputStream.close()
                println("*** Read in " + data.size() + " bytes")
                isValid = true
            } else {
                status = tapeStatusBadHeader
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun save(fn: String) {
        try {
            val os = FileOutputStream(fn)

            // rebuild header
            while (data.size() < minPadding) {
                data.write(0)
            }
            header.reset()
            header.write(buildHeader())

            // write header
            header.writeTo(os)

            // write data
            data.writeTo(os)
            os.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun WriteByte(b: Byte) {
        data.write(b.toInt())
    }

    abstract fun parseHeader(f: InputStream): Boolean
    abstract fun buildHeader(): ByteArray
    abstract val headerSize: Int
    abstract val minPadding: Int
    abstract val magicBytes: ByteArray
    abstract fun writeAudioStreamData(path: String, base: String)
    abstract val isHeaderData: Boolean
    fun digest(): String {
        var res = ""
        try {
            val algorithm = MessageDigest.getInstance("MD5")
            algorithm.reset()
            algorithm.update(data.toByteArray())
            val md5 = algorithm.digest()
            var tmp = ""
            for (i in md5.indices) {
                tmp = Integer.toHexString(0xFF and md5[i].toInt())
                res += if (tmp.length == 1) {
                    "0$tmp"
                } else {
                    tmp
                }
            }
        } catch (ex: NoSuchAlgorithmException) {
        }
        return res
    }

    val percent: Float
        get() = 100 * (dataPos / data.size().toFloat())

    abstract val tapeType: String

    abstract val renderPercent: Float
    fun addSilence(w: IntermediateBlockRepresentation, duration: Double, amplitude: Double) {
        w.currentSystem = tapeType
        w.addSilence(duration, amplitude)
    }

    fun addSquareWave(w: IntermediateBlockRepresentation, duration: Double, ampHi: Double, ampLo: Double) {
        w.addSquareWave(duration, ampHi, ampLo)
        w.currentSystem = tapeType
    }

    companion object {
        const val tapeStatusOk = 0
        const val tapeStatusHeaderInvalid = 2
        const val tapeStatusDataMismatch = 3
        const val tapeStatusWriteError = 4
        const val tapeStatusReadError = 5
        const val tapeStatusBadHeader = 6
        const val CHUNK = 4096
    }
}