package co.kica.tap

import co.kica.tap.IntermediateBlockRepresentation.SampleTable
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class AtariTape : GenericTape() {

    data class CASChunk(
            var id: CharArray = charArrayOf(' ', ' ', ' ', ' '),
            var length: Int = 0,
            var aux: Int = 0,
            var chunkData: ByteArray = ByteArray(0)) {

        val recordType: String
            get() = id.joinToString("")

        override fun toString(): String {
            return "$recordType (length $length byte(s), aux = $aux)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CASChunk

            if (!id.contentEquals(other.id)) return false
            if (length != other.length) return false
            if (aux != other.aux) return false
            if (!chunkData.contentEquals(other.chunkData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.contentHashCode()
            result = 31 * result + length
            result = 31 * result + aux
            result = 31 * result + chunkData.contentHashCode()
            return result
        }
    }

    var versionMinor: Byte = 0
    var versionMajor: Byte = 0
    private var bitCount: Long = 0
    private val baseFrequency = 1200f // Hz
    private var baudRate = 600f // STANDARD ATARI CASSETTE Baud rate per SIO specs.
    private val phase = 180f // wavePhase
    private val carrierFrequency = baseFrequency * 2
    private var lastChunk: CASChunk? = null
    private val markToneDuration = MU / MARK_TONE // MARK 	= 5327Hz @ 600 Baud
    private val spaceToneDuration = MU / SPACE_TONE // SPACE	= 3995Hz @ 600 Baud
    private val fudge = 1.0
    private var markTone: SampleTable? = null
    private var spaceTone: SampleTable? = null
    override val magicBytes: ByteArray
        get() = Arrays.copyOfRange(header.toByteArray(), 0, 4)

    override fun parseHeader(f: InputStream): Boolean {
        val buff = ByteArray(headerSize)
        isValid = false
        try {
            //f.reset();
            val len = f.read(buff)
            if (len == headerSize) {
                // reset and store into header
                header.reset()
                header.write(buff)
                val magic = "FUJI".toByteArray()
                if (Arrays.equals(magicBytes, magic)) {
                    println("*** File is a valid Atari/FUJI File by the looks of it.")
                    isValid = true
                    status = tapeStatusOk
                } else {
                    //System.out.println("xxx File has unrecognized magic: "+byteArrayToString(getMAGIC()));
                    status = tapeStatusHeaderInvalid
                    return false
                }
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return false
        }
        return true
    }

    val sizeAsBytes: ByteArray
        get() {
            val b = ByteBuffer.allocate(4)
            b.order(ByteOrder.LITTLE_ENDIAN)
            b.putInt(data.size())
            return b.array()
        }

    val sizeFromHeader: Int
        get() {
            val b = ByteBuffer.allocate(4)
            b.order(ByteOrder.LITTLE_ENDIAN)
            b.mark()
            b.put(Arrays.copyOfRange(header.toByteArray(), 16, 19))
            b.reset()
            return b.int
        }

    override fun buildHeader(): ByteArray {
        return byteArrayOf(
                'F'.toByte(), 'U'.toByte(), 'J'.toByte(), 'I'.toByte(), 0, 0, 0, 0)
    }

    override val headerSize: Int = 4

    override val minPadding: Int = 0

    fun asByte(b: Byte): Short {
        return (b.toInt() and 0xff).toShort()
    }

    fun hasData(): Boolean {
        return dataPos < data.size()
    }

    fun getDataByte(data: ByteArray): Byte {
        return if (!hasData()) {
            0
        } else data[dataPos++]
    }

    fun getWordFromChunk(data: ByteArray): Int {
        val b: Int = (data[dataPos].toInt() and 0xff) + 256 * (data[dataPos + 1].toInt() and 0xff)
        dataPos += 2
        return b
    }

    fun getFloatFromChunk(data: ByteArray): Float {
        val b = ByteBuffer.allocate(4)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.mark()
        b.put(data.copyOfRange(0, 3))
        b.reset()
        return b.float
    }

    fun getNextChunk(data: ByteArray): CASChunk? {
        if (!hasData()) {
            return null
        }
        val chunk = CASChunk()
        for (i in 0..3) {
            chunk.id[i] = getDataByte(data).toChar()
        }
        chunk.length = getWordFromChunk(data)
        chunk.aux = getWordFromChunk(data)
        chunk.chunkData = ByteArray(chunk.length)
        for (i in 0 until chunk.length) {
            chunk.chunkData[i] = getDataByte(data)
        }
        return chunk
    }

    override fun writeAudioStreamData(path: String, base: String) {
        val w = IntermediateBlockRepresentation(path, base)

        // init needed sample tables
        markTone = w.SampleTable(44100, true, 1, MARK_TONE, 0.99)
        spaceTone = w.SampleTable(44100, true, 1, SPACE_TONE, 0.99)

        //Data.reset();
        val raw = data.toByteArray()
        val bytesread = 0
        val duration = 0.0
        val cnv = 1.0
        while (hasData()) {
            val chunk = getNextChunk(raw)
            //System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" with size "+chunk.chunkData.length+" bytes.");
            chunk?.let { println(it) }
            try {
                handleChunk(chunk, w)
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            // store last block for &101
            lastChunk = chunk
        }
        w.done()

        //return w;
    }

    private fun amplitudeAtTime(w: IntermediateBlockRepresentation, freq: Double, hi: Double, rest: Double): Double {
        val pos = w.duration.toDouble()
        val toneDuration = 1000000 / freq
        val toneDurationHalf = toneDuration / 2
        val remainder = pos % spaceToneDuration
        return if (remainder < toneDurationHalf) {
            hi
        } else {
            rest
        }
    }

    private fun zeroBit(w: IntermediateBlockRepresentation) {
        // SPACE bit 3995 Hz
        val bitDuration = 1000000.0 / baudRate
        spaceTone!!.reset()
        w.addSampleTableForDuration(spaceTone!!, bitDuration)
        bitCount++
    }

    private fun oneBit(w: IntermediateBlockRepresentation) {
        // SPACE bit 3995 Hz
        val bitDuration = 1000000 / baudRate.toDouble()
        markTone!!.reset()
        w.addSampleTableForDuration(markTone!!, bitDuration)
        bitCount++
    }

    private fun triWordFromChunk(chunk: CASChunk, offset: Int): Int {
        val lo: Int = chunk.chunkData[0 + offset].toInt() and 0xff
        val hi: Int = chunk.chunkData[1 + offset].toInt() and 0xff
        val vhi: Int = chunk.chunkData[2 + offset].toInt() and 0xff

        //System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
        return lo + 256 * hi + 65536 * vhi
    }

    private fun wordFromChunk(chunk: CASChunk, offset: Int = 0): Int {
        val lo: Int = chunk.chunkData[0 + offset].toInt() and 0xff
        val hi: Int = chunk.chunkData[1 + offset].toInt() and 0xff

        //System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
        return lo + 256 * hi
    }

    private fun byteArrayToString(bytes: ByteArray): String {
        var r = ""
        for (b in bytes) {
            val c = (b.toInt() and 0xff).toChar()
            r += c
        }
        return r
    }

    @Throws(Exception::class)
    private fun handleChunk(chunk: CASChunk?, w: IntermediateBlockRepresentation) {
        when (chunk!!.recordType) {
            "FUJI" -> {
            }
            "baud" -> {
                baudRate = chunk.aux.toFloat()
                println("Setting default baudrate to " + Integer.toString(chunk.aux))
            }
            "data" -> {
                handleDATAChunk(chunk, w)
            }
            else -> {
                throw Exception("Unhandled chunk type " + chunk.id[0] + chunk.id[1] + chunk.id[2] + chunk.id[3])
            }
        }
    }

    private fun handleDATAChunk(chunk: CASChunk?, w: IntermediateBlockRepresentation) {
        /*
			while bytes remain in UEF chunk
			output a zero bit (the start bit)
			read a byte from the UEF chunk, store it to NewByte
			let InternalBitCount = 8
			while InternalBitCount > 0
			output least significant bit of NewByte
			shift NewByte right one position
			decrement InternalBitCount
			output a one bit (the stop bit)
		 */

        // add IRG
        val irg = chunk!!.aux * 1000.0

        //w.addSilence(irg, PULSE_MIDDLE);
        w.addSampleTableForDuration(markTone!!, irg)
        for (i in chunk.chunkData.indices) {
            var b = chunk.chunkData[i]
            zeroBit(w) // start bit
            var bitcount = 8
            while (bitcount > 0) {
                if (b.toInt() and 1 == 1) {
                    oneBit(w)
                } else {
                    zeroBit(w)
                }
                b = (b.toInt() ushr 1).toByte()
                bitcount--
            }
            oneBit(w) // stop bit
        }
        w.addSilence(MU / baudRate, PULSE_MIDDLE)
    }

    override val isHeaderData: Boolean = true

    override val tapeType: String = "FUJI"

    override val renderPercent: Float
        get() = dataPos.toFloat() / data.size().toFloat()

    companion object {
        const val PULSE_AMPLITUDE = -0.99
        const val PULSE_MIDDLE = 0.99
        const val PULSE_REST = 0.99
        const val PAL_CLK = 985248.0
        const val MU = 1000000.0
        private const val MARK_TONE = 5327.0
        private const val SPACE_TONE = 3995.0
    }

}