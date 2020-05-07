package co.kica.tap

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.pow

class UEFTape : GenericTape() {

    data class UEFChunk(val id: Int, val chunkData: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as UEFChunk

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
    private var baseFrequency = 1200f // Hz
    private var baudRate = 1200f // Baud rate
    private val phase = 180f // wavePhase
    private val carrierFrequency = baseFrequency * 2
    private var lastChunk: UEFChunk? = null
    private val fudge = 1.0
    override val magicBytes: ByteArray
        get() = Arrays.copyOfRange(header.toByteArray(), 0, 10)

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
                val magic = "UEF File!\u0000".toByteArray()
                if (magicBytes.contentEquals(magic)) {
                    println("*** File is a valid UEF by the looks of it.")
                    isValid = true
                    versionMinor = buff[10]
                    versionMajor = buff[11]
                    println("*** Header says version is $versionMajor.$versionMinor")
                    status = tapeStatusOk
                } else {
                    //System.out.println("xxx File has unrecognized magic: "+byteArrayToString(getMAGIC()));
                    status = tapeStatusHeaderInvalid
                    return false
                }
            }
        } catch (e: IOException) {
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
                'U'.toByte(), 'E'.toByte(), 'F'.toByte(), ' '.toByte(),
                'F'.toByte(), 'i'.toByte(), 'l'.toByte(), 'e'.toByte(), '!'.toByte(),
                0,
                versionMinor,
                versionMajor)
    }

    override val headerSize: Int = 12
    override val minPadding: Int = 0

    fun asByte(b: Byte): Short {
        return (b.toInt() and 0xff).toShort()
    }

    private fun hasData(): Boolean = dataPos < data.size()

    private fun getDataByte(data: ByteArray): Byte = if (!hasData()) 0 else data[dataPos++]

    private fun getSizeFromChunk(data: ByteArray): Int {
        val b = ByteBuffer.allocate(4)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.mark()
        b.put(data.copyOfRange(dataPos, dataPos + 3))
        b.reset()
        dataPos += 4
        return b.int
    }

    private fun getFloatFromChunk(data: ByteArray): Float {
        val b = ByteBuffer.allocate(4)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.mark()
        b.put(data.copyOfRange(0, 3))
        b.reset()
        return b.float
    }

    private fun getNextChunk(data: ByteArray): UEFChunk? {
        if (!hasData()) {
            return null
        }

        val size = getSizeFromChunk(data)
        val chunkId = getDataByte(data) + 256 * getDataByte(data)
        val chunkData = ByteArray(size) { getDataByte(data) }

        return UEFChunk(chunkId, chunkData)
    }

    override fun writeAudioStreamData(path: String, base: String) {
        val w = IntermediateBlockRepresentation(path, base)

        //Data.reset();
        val raw = data.toByteArray()
        val bytesread = 0
        val duration = 0.0
        val cnv = 1.0
        while (hasData()) {
            val chunk = getNextChunk(raw)
            //System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" with size "+chunk.chunkData.length+" bytes.");
            try {
                handleChunk(chunk, w)
            } catch (e: Exception) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            // store last block for &101
            lastChunk = chunk
        }

        // do cue
        w.done()

        //return w;
    }

    private fun zeroBit(w: IntermediateBlockRepresentation) {
        if (baudRate == 1200f) {
            // 1 cycle high low at base frequency
            val cycleduration = 1000000.0 / baseFrequency
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            //System.out.print("0");
        } else if (baudRate == 300f) {
            // 4 cycles high low at base frequency
            val cycleduration = 1000000.0 / baseFrequency
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            //System.out.print("0");
        }
    }

    private fun oneBit(w: IntermediateBlockRepresentation) {
        if (baudRate == 1200f) {
            // 2 cycle high low at base frequency
            val cycleduration = 1000000.0 / (2 * baseFrequency)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            //System.out.print("1");
        } else if (baudRate == 300f) {
            // 8 cycles high low at base frequency
            val cycleduration = 1000000.0 / (2 * baseFrequency)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
            //System.out.print("1");
        }
    }

    private fun handleChunk0100(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
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
        for (i in chunk!!.chunkData.indices) {
            var b = chunk.chunkData[i]
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
            oneBit(w)
        }
    }

    private fun triWordFromChunk(chunk: UEFChunk?, offset: Int): Int {
        val lo: Int = chunk!!.chunkData[0 + offset].toInt() and 0xFF
        val hi: Int = chunk.chunkData[1 + offset].toInt() and 0xFF
        val vhi: Int = chunk.chunkData[2 + offset].toInt() and 0xFF

        //System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
        return lo + 256 * hi + 65536 * vhi
    }

    private fun wordFromChunk(chunk: UEFChunk?, offset: Int = 0): Int {
        val lo: Int = chunk!!.chunkData[0 + offset].toInt() and 0xFF
        val hi: Int = chunk.chunkData[1 + offset].toInt() and 0xFF

        //System.out.println("======================> lobyte = "+lo+", hibyte = "+hi);
        return lo + 256 * hi
    }

    private fun handleChunk0110(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val cycles = wordFromChunk(chunk)
        println("CARRIER TONE $cycles cycles...")
        val cycleduration = 1000000.0 / carrierFrequency
        for (i in 0 until cycles) {
            w.addSquareWave(cycleduration, PULSE_AMPLITUDE, PULSE_REST)
        }
    }

    private fun handleChunk0111(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val cycles_before = wordFromChunk(chunk, 0)
        val cycles_after = wordFromChunk(chunk, 2)
        val dummy = 0xaa
        println("CARRIER TONE WITH DUMMY BYTE...")

        // lead carrier
        val cycleduration_before = 1000000.0 / carrierFrequency
        for (i in 0 until cycles_before) {
            w.addSquareWave(cycleduration_before, PULSE_AMPLITUDE, PULSE_REST)
        }

        // dummy byte
        // start bit
        zeroBit(w)

        // byte 0xaa = 0b10101010
        oneBit(w)
        zeroBit(w)
        oneBit(w)
        zeroBit(w)
        oneBit(w)
        zeroBit(w)
        oneBit(w)
        zeroBit(w)

        // stop bit
        oneBit(w)

        // trailing carrier
        val cycleduration_after = 1000000.0 / carrierFrequency
        for (i in 0 until cycles_after) {
            w.addSquareWave(cycleduration_after, PULSE_AMPLITUDE, PULSE_REST)
        }
    }

    private fun handleChunk0112(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val cycles = wordFromChunk(chunk)
        val cycleduration = 1000000.0 * cycles / baseFrequency
        w.addSilence(cycleduration, PULSE_REST)
        println("SILENCE $cycles cycles...")
    }

    private fun handleChunk0117(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val cycles = wordFromChunk(chunk)
        baudRate = cycles.toFloat()
    }

    private fun floatFromChunk(chunk: UEFChunk?): Float {
        /* assume a four byte array named Float exists, where Float[0]
		was the first byte read from the UEF, Float[1] the second, etc */
        val Float = IntArray(4)
        Float[0] = chunk!!.chunkData[0].toInt() and 0xFF
        Float[1] = chunk.chunkData[1].toInt() and 0xFF
        Float[2] = chunk.chunkData[2].toInt() and 0xFF
        Float[3] = chunk.chunkData[3].toInt() and 0xFF

        /* decode mantissa */
        val Mantissa: Int
        Mantissa = Float[0] or (Float[1] shl 8) or (Float[2] and 0x7f or 0x80 shl 16)
        var Result = Mantissa.toFloat()
        Result = ldexp(Result.toDouble(), -23).toFloat()

        /* decode exponent */
        var Exponent: Int
        Exponent = Float[2] and 0x80 shr 7 or (Float[3] and 0x7f) shl 1
        Exponent -= 127
        Result = ldexp(Result.toDouble(), Exponent).toFloat()

        /* flip sign if necessary */if (Float[3] and 0x80 > 0) Result = -Result

        /* floating point number is now in 'Result' */return Result
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
    private fun handleChunk(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        when (chunk!!.id) {
            0x0000 -> println("SOURCE: [" + byteArrayToString(chunk.chunkData) + "]")
            0x0100 -> handleChunk0100(chunk, w)
            0x0101 -> handleChunk0101(chunk, w)
            0x0102 -> handleChunk0102(chunk, w)
            0x0104 -> handleChunk0104(chunk, w)
            0x0110 -> handleChunk0110(chunk, w)
            0x0111 -> handleChunk0111(chunk, w)
            0x0112 -> handleChunk0112(chunk, w)
            0x0113 -> handleChunk0113(chunk, w)
            0x0114 -> handleChunk0114(chunk, w)
            0x0115 -> handleChunk0115(chunk, w)
            0x0116 -> handleChunk0116(chunk, w)
            0x0117 -> handleChunk0117(chunk, w)
            else -> throw Exception("Unhandled chunk type " + Integer.toHexString(chunk.id))
        }
    }

    private fun handleChunk0115(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        // TODO Auto-generated method stub
    }

    private fun handleChunk0116(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val f = floatFromChunk(chunk)
        val cycleduration = 1000000.0 * f
        w.addSilence(cycleduration, PULSE_REST)
    }

    private fun handleChunk0113(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val f = floatFromChunk(chunk)
        println("Changing frequency to " + f + "Hz")
        baseFrequency = f
    }

    private fun handleChunk0101(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        if (lastChunk == null || lastChunk!!.id != 0x0100 && lastChunk!!.id != 0x0102) {
            return
        }
        if (lastChunk!!.id == 0x0100) handleChunk0100(lastChunk, w)
        if (lastChunk!!.id == 0x0102) handleChunk0102(lastChunk, w)
    }

    private fun handleChunk0102(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val bitCount: Int = chunk!!.chunkData.size * 8 - (chunk.chunkData[0].toInt() and 0xFF)
        var byteIndex = 0
        var byteValue: Byte = 0
        for (currentBit in 0 until bitCount) {
            if (currentBit % 8 == 0) {
                // new byte
                byteValue = chunk.chunkData[byteIndex]
                byteIndex++
            }
            if (byteValue.toInt() and 1 == 1) {
                oneBit(w)
            } else {
                zeroBit(w)
            }
            byteValue = (byteValue.toInt() shr 1).toByte()
        }
    }

    private fun handleChunk0114(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        /* TODO: implement security cycles */
        val numCycles = triWordFromChunk(chunk, 0)
    }

    private fun handleChunk0104(chunk: UEFChunk?, w: IntermediateBlockRepresentation) {
        val bitsPerPacket: Int = chunk!!.chunkData[0].toInt() and 0xFF
        val parity = (chunk.chunkData[1].toInt() and 0xFF).toChar()
        val stopBitCount = chunk.chunkData[0].toInt()
        val needsExtra = stopBitCount < 0
        val bytesRemaining = chunk.chunkData.size - 3
        for (bytePos in 0 until bytesRemaining) {
            // start bit
            zeroBit(w)
            // get new byte
            var newByte = chunk.chunkData[3 + bytePos]
            var internalBitCount = bitsPerPacket
            var oneCount = 0
            while (internalBitCount > 0) {
                if (newByte.toInt() and 1 == 1) {
                    oneBit(w)
                    oneCount++
                } else {
                    zeroBit(w)
                }
                newByte = (newByte.toInt() shr 1).toByte()
                internalBitCount--
            }
            // output parity bit if needed
            if (parity == 'O') {
                if (oneCount % 1 == 0) {
                    oneBit(w)
                } else {
                    zeroBit(w)
                }
            }
            if (parity == 'E') {
                if (oneCount % 1 == 0) {
                    zeroBit(w)
                } else {
                    oneBit(w)
                }
            }
            // now stop bits
            var internalStopCount = stopBitCount
            while (internalStopCount > 0) {
                oneBit(w)
                internalStopCount--
            }
            //
            if (needsExtra) {
                val cycleDuration = 1000000.0 / carrierFrequency
                w.addSquareWave(cycleDuration, PULSE_REST, PULSE_AMPLITUDE)
            }
        }
    }

    override val isHeaderData: Boolean  = false
    override val tapeType: String = "UEF"

    override val renderPercent: Float
        get() = dataPos.toFloat() / data.size().toFloat()

    companion object {
        const val PULSE_AMPLITUDE = -0.99
        const val PULSE_MIDDLE = 0.99
        const val PULSE_REST = 0.99
        const val PAL_CLK = 985248.0
        const val MU = 1000000.0
        fun ldexp(v: Double, w: Int): Double {
            return v * 2.0.pow(w.toDouble())
        }
    }

}