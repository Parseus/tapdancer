package co.kica.tap

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class MSXTape : GenericTape() {

    data class MSXChunk(var id: Int = 0, var chunkData: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MSXChunk

            if (id != other.id) return false
            if (!chunkData.contentEquals(other.chunkData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + chunkData.contentHashCode()
            return result
        }
    }

    var versionMinor: Byte = 10
    var versionMajor: Byte = 0
    private val longSilence = 2000000.0
    private val shortSilence = 1000000.0
    private val baseFrequency = 1200f // Hz
    private val baudRate = 1200f // Baud rate
    private val phase = 180f // wavePhase
    private val carrierFrequency = baseFrequency * 2
    private val shortPulseDuration = 1000000 / 2400.toFloat()
    private val longPulseDuration = 1000000 / 1200.toFloat()
    private val longHeader = 16000
    private val shortHeader = 4000
    private val lastChunk: MSXChunk? = null
    private var eof = false
    private val ASCII = byteArrayOf(0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte(), 0xEA.toByte())
    private val BIN = byteArrayOf(0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte(), 0xD0.toByte())
    private val BASIC = byteArrayOf(0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte(), 0xD3.toByte())
    private val HEADER = byteArrayOf(0x1f, 0xa6.toByte(), 0xde.toByte(), 0xba.toByte(), 0xcc.toByte(), 0x13, 0x7d, 0x74)
    private val fudge = 1.0
    override val magicBytes: ByteArray
        get() = Arrays.copyOfRange(header.toByteArray(), 0, 8)

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
                val magic = byteArrayOf(0x1f, 0xa6.toByte(), 0xde.toByte(), 0xba.toByte(), 0xcc.toByte(), 0x13, 0x7d, 0x74)
                if (Arrays.equals(magicBytes, magic)) {
                    println("*** File is a valid MSX Tape by the looks of it.")
                    isValid = true
                    //setVersionMinor(buff[10]);
                    //setVersionMajor(buff[11]);
                    //System.out.println("*** Header says version is "+getVersionMajor()+"."+getVersionMinor());
                    status = tapeStatusOk
                } else {
                    //System.out.println("xxx File has unrecognized magic: "+getMAGIC());
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
                'U'.toByte(), 'E'.toByte(), 'F'.toByte(), ' '.toByte(), 'F'.toByte(), 'i'.toByte(), 'l'.toByte(), 'e'.toByte(), '!'.toByte(), 0,
                versionMinor,
                versionMajor)
    }

    override val headerSize: Int = 8
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

    private fun next8Bytes(data: ByteArray): ByteArray {
        // get next 8 bytes
        return Arrays.copyOfRange(data, dataPos, dataPos + 8)
    }

    private fun next10Bytes(data: ByteArray): ByteArray {
        // get next 8 bytes
        return Arrays.copyOfRange(data, dataPos, dataPos + 10)
    }

    fun writeData(w: IntermediateBlockRepresentation, data: ByteArray) {
        var buffer: ByteArray
        eof = false
        while (data.size - dataPos >= 8) {
            buffer = next8Bytes(data)
            if (Arrays.equals(buffer, HEADER)) return
            writeByte(w, buffer[0])
            if (buffer[0].toInt() and 0xff == 0x1a) eof = true
            dataPos++
        }

        // write remaining bytes
        if (data[dataPos].toInt() and 0xff == 0x1a) eof = true
        while (dataPos < data.size) {
            writeByte(w, data[dataPos])
            dataPos++
        }
        return
    }

    override fun writeAudioStreamData(path: String, base: String) {
        val w = IntermediateBlockRepresentation(path, base)

        //Data.reset();
        val raw = data.toByteArray()
        val bytesread = 0
        val duration = 0.0
        val cnv = 1.0
        dataPos = 0

        // start cuefile now
        val cuedata = ArrayList<String>()
        val lastData = false
        while (hasData()) {
            /* it probably works fine if a long header is used for every */
            /* header but since the msx bios makes a distinction between */
            /* them, we do also (hence a lot of code).                   */
            if (Arrays.equals(HEADER, next8Bytes(raw))) {
                dataPos += 8
                if (raw.size - dataPos >= 10) {
                    if (Arrays.equals(ASCII, next10Bytes(raw))) {
                        addSilence(w, longSilence, PULSE_MIDDLE)
                        writeHeader(w, longHeader)
                        writeData(w, raw)
                        do {
                            dataPos += 8 //fseek(input,position,SEEK_SET);
                            addSilence(w, shortSilence, PULSE_MIDDLE)
                            writeHeader(w, shortHeader)
                            writeData(w, raw)
                        } while (!eof && hasData())
                    } else if (Arrays.equals(BIN, next10Bytes(raw)) || Arrays.equals(BASIC, next10Bytes(raw))) {

                        //fseek(input,position,SEEK_SET);
                        addSilence(w, longSilence, PULSE_MIDDLE)
                        writeHeader(w, longHeader)
                        writeData(w, raw)
                        addSilence(w, shortSilence, PULSE_MIDDLE)
                        writeHeader(w, shortHeader)
                        dataPos += 8 // skip next header
                        writeData(w, raw)
                    } else {
                        println("unknown file type: using long header")
                        addSilence(w, longSilence, PULSE_MIDDLE)
                        writeHeader(w, longHeader)
                        writeData(w, raw)
                    }
                } else {
                    println("unknown file type: using long header")
                    addSilence(w, longSilence, PULSE_MIDDLE)
                    writeHeader(w, longHeader)
                    writeData(w, raw)
                }
            } else {

                /* should not occur */
                println("skipping unhandled data")
                dataPos++
            }
        }

        // lets add some silence to contemplate the finer things in life :-)
        addSilence(w, 3000000.0, PULSE_REST)

        // write cue
        w.done()
    }

    private fun writeHeader(w: IntermediateBlockRepresentation, pulseCount: Int) {
        for (i in 0 until pulseCount) {
            addSquareWave(w, shortPulseDuration.toDouble(), PULSE_AMPLITUDE, PULSE_REST)
        }
    }

    private fun zeroBit(w: IntermediateBlockRepresentation) {
        addSquareWave(w, longPulseDuration.toDouble(), PULSE_AMPLITUDE, PULSE_REST)
    }

    private fun oneBit(w: IntermediateBlockRepresentation) {
        // two short pulses
        addSquareWave(w, shortPulseDuration.toDouble(), PULSE_AMPLITUDE, PULSE_REST)
        addSquareWave(w, shortPulseDuration.toDouble(), PULSE_AMPLITUDE, PULSE_REST)
    }

    private fun writeByte(w: IntermediateBlockRepresentation, b: Byte) {
        // start bit
        var b = b
        zeroBit(w)
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
        // 2 stop bits
        oneBit(w)
        oneBit(w)
    }

    private fun writeChunkData(chunk: MSXChunk, w: IntermediateBlockRepresentation) {
        for (i in chunk.chunkData.indices) {
            writeByte(w, chunk.chunkData[i])
        }
    }

    private fun byteArrayToString(bytes: ByteArray): String {
        var r = ""
        for (b in bytes) {
            val c = (b.toInt() and 0xff).toChar()
            r += c
        }
        return r
    }

    override val isHeaderData: Boolean = true

    override val tapeType = "MSX"

    override val renderPercent: Float
        get() = dataPos.toFloat() / data.size().toFloat()

    companion object {
        private const val CID_UNKNOWN = 0x000
        private const val CID_ASCII = 0x100
        private const val CID_BINARY = 0x200
        private const val CID_BASIC = 0x400
        const val PULSE_AMPLITUDE = -1.0
        const val PULSE_MIDDLE = 1.0
        const val PULSE_REST = 1.0
        const val PAL_CLK = 985248.0
        const val MU = 1000000.0
    }

}