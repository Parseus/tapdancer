package co.kica.tap

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

open class TZXTape(sampleRate: Int) : GenericTape() {
    var minorVersion = 1
    var majorVersion = 20
    private var lastChunk: TZXChunk? = null
    var blockCounts = IntArray(256)
    private var coreCounter = 0
    private var savePosition = 0

    override fun parseHeader(f: InputStream): Boolean {
        val buff = ByteArray(headerSize)
        for (i in blockCounts.indices) {
            blockCounts[i] = 0
        }
        try {
            //f.reset();
            val len = f.read(buff)
            if (len == headerSize) {
                // reset and store into header
                header.reset()
                header.write(buff)
                val magic = byteArrayOf('Z'.toByte(), 'X'.toByte(), 'T'.toByte(), 'a'.toByte(), 'p'.toByte(), 'e'.toByte(), '!'.toByte())
                return if (Arrays.equals(magicBytes, magic)) {
                    println("*** File is a valid TZX by the looks of it.")
                    isValid = true
                    majorVersion = buff[0x08].toInt() and 0xff
                    minorVersion = buff[0x09].toInt() and 0xff
                    println("*** Header says version is $majorVersion.$minorVersion")
                    status = tapeStatusOk
                    true
                } else {
                    println("xxx File has unrecognized magic: $magicBytes")
                    status = tapeStatusHeaderInvalid
                    false
                }
            }
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            return false
        }
        return true
    }

    fun hasData(): Boolean {
        return dataPos < data.size()
    }

    fun getDataByte(data: ByteArray): Int {
        return if (!hasData()) {
            0
        } else data[dataPos++].toInt() and 0xff
    }

    fun getSizeFromChunk(data: ByteArray): Int {
        val b = ByteBuffer.allocate(4)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.mark()
        b.put(Arrays.copyOfRange(data, dataPos, dataPos + 3))
        b.reset()
        dataPos += 4
        return b.int
    }

    fun getFloatFromChunk(data: ByteArray): Float {
        val b = ByteBuffer.allocate(4)
        b.order(ByteOrder.LITTLE_ENDIAN)
        b.mark()
        b.put(Arrays.copyOfRange(data, 0, 3))
        b.reset()
        return b.float
    }

    fun getDataWord(data: ByteArray): Int {
        return getDataByte(data) + 256 * getDataByte(data)
    }

    fun getDataTriplet(data: ByteArray): Int {
        return getDataByte(data) + 256 * getDataByte(data) + 65536 * getDataByte(data)
    }

    fun getDataDWORD(data: ByteArray): Int {
        return getDataByte(data) + 256 * getDataByte(data) + 65536 * getDataByte(data) + 16777216 * getDataByte(data)
    }

    fun getNextChunk(data: ByteArray): TZXChunk? {
        if (!hasData()) {
            return null
        }
        val chunk = TZXChunk()
        chunk.id = getDataByte(data)
        var size = 0

        /* work out based on block how much data we need to snaffle */
        var ok = true
        when (chunk.id) {
            0x10 -> {
                chunk.description = "Standard speed data block"
                chunk.pauseAfter = getDataWord(data)
                size = getDataWord(data)
            }
            0x11 -> {
                chunk.description = "Turbo speed data block"
                chunk.pilotPulseLength = getDataWord(data)
                chunk.syncFirstPulseLength = getDataWord(data)
                chunk.syncSecondPulseLength = getDataWord(data)
                chunk.zeroBitPulseLength = getDataWord(data)
                chunk.oneBitPulseLength = getDataWord(data)
                chunk.pilotPulseCount = getDataWord(data)
                chunk.usedBitsLastByte = getDataByte(data)
                chunk.pauseAfter = getDataWord(data)
                size = getDataTriplet(data)
            }
            0x12 -> {
                chunk.description = "Pure tone block"
                chunk.pilotPulseLength = getDataWord(data)
                chunk.pilotPulseCount = getDataWord(data)
            }
            0x13 -> {
                chunk.description = "Pulse sequence"
                chunk.dataPulseCount = getDataByte(data)
                size = chunk.dataPulseCount * 2
            }
            0x14 -> {
                chunk.description = "Pure data block"
                chunk.zeroBitPulseLength = getDataWord(data)
                chunk.oneBitPulseLength = getDataWord(data)
                chunk.usedBitsLastByte = getDataByte(data)
                chunk.pauseAfter = getDataWord(data)
                size = getDataTriplet(data)
            }
            0x15 -> {
                chunk.description = "Direct recording"
                chunk.ticksPerBit = getDataWord(data)
                chunk.pauseAfter = getDataWord(data)
                chunk.usedBitsLastByte = getDataByte(data)
                size = getDataTriplet(data)
            }
            0x18 -> {
                chunk.description = "CSW recording"
                size = getDataDWORD(data) - 10
                chunk.pauseAfter = getDataWord(data)
                chunk.sampleRate = getDataTriplet(data)
                chunk.compressionType = getDataByte(data)
                chunk.CSWPulseCount = getDataWord(data)
            }
            0x19 -> {
                chunk.description = "Generalized torture block"
                /* TODO: YOU BASTARDS!!!!!!!!! */size = getDataDWORD(data)
            }
            0x20 -> {
                chunk.description = "Silence (stop tape)"
                chunk.pauseAfter = getDataWord(data)
            }
            0x21 -> {
                chunk.description = "Group start"
                size = getDataByte(data)
            }
            0x22 -> chunk.description = "Group end"
            0x23 -> {
                chunk.description = "Jump to block"
                chunk.relativeJump = getDataWord(data)
            }
            0x24 -> {
                chunk.description = "Loop start"
                chunk.numberRepetitions = getDataWord(data)
            }
            0x25 -> chunk.description = "Loop end"
            0x26 -> {
                chunk.description = "Call sequence"
                size = getDataWord(data) * 2
                chunk.description = "Return from sequence"
            }
            0x27 -> chunk.description = "Return from sequence"
            0x28 -> {
                chunk.description = "Select block"
                size = getDataWord(data)
            }
            0x2a -> {
                chunk.description = "Stop tape if 48K"
                getDataDWORD(data)
            }
            0x2b -> {
                chunk.description = "Set signal level"
                size = getDataDWORD(data)
            }
            0x30 -> {
                chunk.description = "Text description"
                size = getDataByte(data)
            }
            0x31 -> {
                chunk.description = "Message block"
                chunk.pauseAfter = getDataByte(data)
                size = getDataByte(data)
            }
            0x32 -> {
                chunk.description = "Archive information"
                size = getDataWord(data)
            }
            0x33 -> {
                chunk.description = "Hardware info"
                size = getDataByte(data) * 3
            }
            0x35 -> {
                chunk.description = "Custom info block"
                dataPos += 0x10
                size = getDataDWORD(data)
            }
            0x5a -> {
                chunk.description = "\"Glue\" block"
                size = 9
            }
            else -> {
                chunk.description = "UNKNOWN BLOCK TYPE " + Integer.toHexString(chunk.id)
                ok = false
            }
        }
        if (!ok) {
            println("[" + fileName + "] Unrecognized block: 0x0" + Integer.toHexString(chunk.id))
        }
        chunk.chunkData = ByteArray(size)
        if (ok) blockCounts[chunk.id] = blockCounts[chunk.id] + 1
        for (i in 0 until size) {
            chunk.chunkData[i] = getDataByte(data).toByte()
        }
        if (chunk.id == 0x24) {
            // save current position..
            coreCounter = chunk.numberRepetitions
            savePosition = dataPos
        }
        if (chunk.id == 0x25) {
            coreCounter--
            if (coreCounter > 0) {
                dataPos = savePosition
            }
        }
        return chunk
    }

    override fun buildHeader(): ByteArray {
        return ByteArray(0)
    }

    override val headerSize: Int = 10
    override val minPadding: Int = 0

    override val magicBytes: ByteArray
        get() = Arrays.copyOfRange(header.toByteArray(), 0, 7)

    override fun writeAudioStreamData(path: String, base: String) {
        val w = IntermediateBlockRepresentation(path, base)
        w.sampleRate = targetSampleRate
        w.currentSystem = tapeType

        //Data.reset();
        val raw = data.toByteArray()
        val bytesread = 0
        val duration = 0.0
        val cnv = 1.0
        while (hasData()) {
            val chunk = getNextChunk(raw)

            //System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" ("+chunk.description+") with size "+chunk.chunkData.length+" bytes.");
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
    }

    fun hasBlock18(): Boolean {

        //Data.reset();
        val raw = data.toByteArray()
        while (hasData()) {
            val chunk = getNextChunk(raw)

            //System.out.println("Got a chunk with ID "+Integer.toHexString(chunk.id)+" ("+chunk.description+") with size "+chunk.chunkData.length+" bytes.");
            if (chunk!!.id == 0x18) {
                return true
            }

            // store last block for &101
            lastChunk = chunk
        }
        return false
    }

    private fun handleChunk(chunk: TZXChunk?, w: IntermediateBlockRepresentation) {
        when (chunk!!.id) {
            0x10 -> handleChunk0x10(w, chunk)
            0x11 -> handleChunk0x11(w, chunk)
            0x12 -> handleChunk0x12(w, chunk)
            0x13 -> handleChunk0x13(w, chunk)
            0x14 -> handleChunk0x14(w, chunk)
            0x15 -> handleChunk0x15(w, chunk)
            0x20 -> handleChunk0x20(w, chunk)
        }
    }

    override val isHeaderData: Boolean
        get() = false

    override val tapeType: String
        get() = "TZX"

    override val renderPercent: Float
        get() = dataPos.toFloat() / data.size().toFloat()

    fun writePilotTone(w: IntermediateBlockRepresentation, pulseLength: Int, numPulses: Int) {
        val duration = pulseLength.toDouble() * 1000000.0 / 3500000.0
        for (i in 0 until numPulses) {
            w.addPulse(duration, PULSE_AMPLITUDE)
        }
    }

    private fun writePulse(w: IntermediateBlockRepresentation,
                           len: Int) {
        val duration = len.toDouble() * 1000000.0 / 3500000.0
        //float duration = ((float)len * 1000000f) / 3500000f;
        w.addPulse(duration, PULSE_AMPLITUDE)
    }

    private fun writeDirectRecordingBlock(w: IntermediateBlockRepresentation,
                                          chunk: TZXChunk?) {
        val sampleCount = chunk!!.ticksPerBit / 79
        for (i in chunk.chunkData.indices) {
            var bc = 8
            if (i == chunk.chunkData.size - 1) {
                bc = chunk.usedBitsLastByte
            }
            var b: Int = chunk.chunkData[i].toInt() and 0xff
            while (bc > 0) {
                val bit = b and 0x80
                if (bit and 0x80 == 0x80) {
                    // one - ear = 1
                    w.writeSamples(sampleCount.toLong(), 1.toByte(), PULSE_AMPLITUDE)
                } else {
                    // zero - ear = 0
                    w.writeSamples(sampleCount.toLong(), 0.toByte(), PULSE_AMPLITUDE)
                }
                b = b shl 1
                bc--
            }
        }

        //System.out.println();
    }

    private fun writeDataBlock(w: IntermediateBlockRepresentation,
                               chunk: TZXChunk?) {
        for (i in chunk!!.chunkData.indices) {
            var bc = 8
            var fb = 0
            if (i == chunk.chunkData.size - 1) {
                bc = chunk.usedBitsLastByte
                fb = 8 - bc
            }
            var b: Int = chunk.chunkData[i].toInt() and 0xff

            //System.out.print(Integer.toHexString(b)+" ");

            //if ((i % 16) == 0)
            //System.out.println("\n");
            while (bc > 0) {
                val bit = b and 0x80
                if (bit and 0x80 == 0x80) {
                    // one
                    //System.out.print("1");
                    writePulse(w, chunk.oneBitPulseLength)
                    writePulse(w, chunk.oneBitPulseLength)
                } else {
                    // zero
                    //System.out.print("0");
                    writePulse(w, chunk.zeroBitPulseLength)
                    writePulse(w, chunk.zeroBitPulseLength)
                }
                b = b shl 1
                bc--
            }
        }

        //System.out.println();
    }

    /*
	Block 0x10: 2209 times.
	Block 0x11: 2851 times.
	Block 0x12: 182 times.
	Block 0x13: 160 times.
	Block 0x14: 12 times.
	Block 0x20: 44 times.
	 */
    /* silencio! */
    fun handleChunk0x20(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        w.addPause(1000 * chunk!!.pauseAfter.toDouble(), PULSE_AMPLITUDE)
    }

    fun handleChunk0x15(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        writeDirectRecordingBlock(w, chunk)
        w.addPause(1000 * chunk!!.pauseAfter.toDouble(), PULSE_AMPLITUDE)
    }

    /* standard data */
    fun handleChunk0x10(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        // write pilot
        val flag: Int = chunk!!.chunkData[0].toInt() and 0xff
        //System.out.println("Flag: "+Integer.toHexString(flag));
        if (flag >= 128) chunk.pilotPulseCount = 3220
        // PILOT TONE
        writePilotTone(w, chunk.pilotPulseLength, chunk.pilotPulseCount)
        // SYNC PULSE 1ST
        writePulse(w, chunk.syncFirstPulseLength)
        // SYNC PULSE 2ND
        writePulse(w, chunk.syncSecondPulseLength)
        // data
        writeDataBlock(w, chunk)
        // pause
        w.addPause(1000 * chunk.pauseAfter.toDouble(), PULSE_AMPLITUDE)
    }

    /* turbo data */
    fun handleChunk0x11(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        // PILOT TONE
        writePilotTone(w, chunk!!.pilotPulseLength, chunk.pilotPulseCount)
        // SYNC PULSE 1ST
        writePulse(w, chunk.syncFirstPulseLength)
        // SYNC PULSE 2ND
        writePulse(w, chunk.syncSecondPulseLength)
        // data
        writeDataBlock(w, chunk)
        // pause
        w.addPause(1000 * chunk.pauseAfter.toDouble(), PULSE_AMPLITUDE)
    }

    /* pure tone */
    fun handleChunk0x12(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        //System.out.println("INFO: "+chunk.pilotPulseCount+" pulses of "+chunk.pilotPulseLength+" T-States...");
        writePilotTone(w, chunk!!.pilotPulseLength, chunk.pilotPulseCount)
    }

    /* pulse sequence */
    fun handleChunk0x13(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        var idx = 0
        while (idx < chunk!!.chunkData.size) {
            val value: Int = (chunk.chunkData[idx++].toInt() and 0xff) + 256 * (chunk.chunkData[idx++].toInt() and 0xff)
            //System.out.println("INFO: PULSE of "+val+" T-states.");
            writePulse(w, value)
            //idx += 2;
        }
    }

    /* pure data block */
    fun handleChunk0x14(w: IntermediateBlockRepresentation, chunk: TZXChunk?) {
        writeDataBlock(w, chunk)
        // pause
        w.addPause(1000 * chunk!!.pauseAfter.toDouble(), PULSE_AMPLITUDE)
    }

    companion object {
        const val ZXTick = 1.0 / 3500000.0
        const val PULSE_AMPLITUDE = -0.99
        const val PULSE_MIDDLE = 0.99
        const val PULSE_REST = 0.99
        const val PAL_CLK = 3500000.0
        const val MU = 1000000.0
    }

    init {
        targetSampleRate = sampleRate
    }
}