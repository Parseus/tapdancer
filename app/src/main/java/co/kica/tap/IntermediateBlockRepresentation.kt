package co.kica.tap

import co.kica.tap.OGDLDocument.Companion.ReadOGDLFile
import co.kica.tap.OGDLDocument.Companion.WriteOGDLFile
import java.io.*

/*
 * This class represents a simple container for PCM 8 bit data.
 * Format: 8 bit, unsigned, mono, 44100
 */
class IntermediateBlockRepresentation(path: String, base: String) {
    private var baseName = "cowsarecool"
    private var basePath = "."
    private var baseExt = "pcm_u8"
    var sampleRate = 44100
    val bitsPerSample = 8
    val channels = 1
    var blockIndex = 1
    var totalBytes = 0
    var startOfBlock = 0
    private var totalBlocks = 0
    private var totalData = 0
    private var totalGap = 0
    var manifest: OGDLDocument = OGDLDocument()
    var currentSystem = "TAP"
    var blockData: BufferedOutputStream? = null
    var earLevel: Byte = 0

    // TODO Auto-generated method stub
    var playingBlock = 1
        private set
    private var playingByteInBlock = 0
    private var playingBuffer: ByteArray? = ByteArray(0)
    private var bytesWritten = 0
    var played = 0
        private set
    private var accumulatedTimeClock = 0.0
    private var accumulatedTimeSamples = 0.0

    inner class SampleTable(sampleRate: Int, sine: Boolean, duration: Int, frequency: Double, amp: Double) {
        var values: DoubleArray
        var sampleRate = 44100
        var amp = 0.5
        var frequency = 5327.0
        var duration: Int
        var index = 0
        val sample: Double
            get() {
                val v = values[index]
                index = (index + 1) % values.size
                return v
            }

        fun reset() {
            index = 0
        }

        init {
            values = DoubleArray(sampleRate * duration)
            // generate waveform at the given frequency
            this.sampleRate = sampleRate
            this.duration = duration
            this.frequency = frequency
            this.amp = amp

            // generate data
            val samplesPerWave = sampleRate / frequency
            val halfSamplesPerWave = samplesPerWave / 2.0
            for (i in values.indices) {
                val remainder = i % samplesPerWave
                val rval = remainder / samplesPerWave * (2 * Math.PI)
                if (sine) {
                    values[i] = amp * Math.sin(rval)
                } else {
                    if (remainder <= halfSamplesPerWave) {
                        values[i] = amp
                    } else {
                        values[i] = -amp
                    }
                }
            }
        }
    }

    fun addSampleTableForDuration(tbl: SampleTable, duration: Double) {
        val samples = Math.round(duration / 1000000.0 * sampleRate)
        for (i in 0 until samples) {
            addSample(tbl.sample)
        }
    }

    fun addSample(amplitude: Double) {
        add8Bit(asByte((amplitude * 127 + 128).toByte()))
    }

    fun reset() {
        playingBlock = 1
        playingByteInBlock = 0
        playingBuffer = null
        System.gc()
        playingBuffer = blockData(playingBlock)
        played = 0
    }

    fun getCurrentBuffer(invertWaveform: Boolean): ByteArray? {
        return if (validBlock(playingBlock)) {
            if (invertWaveform) {
                for (i in playingBuffer!!.indices) {
                    val v: Int = 0xff - (playingBuffer!![i].toInt() and 0xff)
                    playingBuffer!![i] = v.toByte()
                }
            }
            playingBuffer
        } else {
            ByteArray(0)
        }
    }

    fun hasBuffer(): Boolean {
        return validBlock(playingBlock)
    }

    fun nextBuffer(): Int {
        played += playingBuffer!!.size
        playingBlock++
        if (hasBuffer()) {
            playingBuffer = null
            System.gc()
            playingBuffer = blockData(playingBlock)
            return playingBuffer!!.size
        }
        return 0
    }

    // simulates read from disk but its read from buffer
    fun read(b: ByteArray): Int {
        for (j in b.indices) {
            b[j] = (128.toInt() and 0xff).toByte()
        }
        //System.out.println("*** Block = "+this.playingBlock+", Offset = "+this.playingByteInBlock+", Size = "+playingBuffer.length);
        val bytesAvailable = playingBuffer!!.size - playingByteInBlock
        if (bytesAvailable >= b.size) {
            // fill buffer and return
            for (i in b.indices) {
                b[i] = playingBuffer!![playingByteInBlock]
                playingByteInBlock++
            }
            played += b.size
            return b.size
        } else if (bytesAvailable > 0) {
            played += bytesAvailable
            for (i in 0 until bytesAvailable) {
                b[i] = playingBuffer!![playingByteInBlock]
                playingByteInBlock++
            }
            for (j in 0 until b.size - bytesAvailable) {
                b[bytesAvailable + j] = (128.toInt() and 0xff).toByte()
            }
            return b.size
        } else {
            // in this case we load more data and return silence...
            playingBlock++
            playingByteInBlock = 0
            if (validBlock(playingBlock)) {
                playingBuffer = blockData(playingBlock)
                return b.size
            }
        }
        return 0
    }

    val duration: Int
        get() = blockDuration(playingBlock)

    val isFirstSilence: Boolean
        get() = type == "SILENCE" && playingBlock <= 2

    val remaining: Int
        get() = playingBuffer!!.size - playingByteInBlock

    val type: String
        get() = blockType(playingBlock)

    val isStopped: Boolean
        get() = !validBlock(playingBlock)

    private fun asByte(b: Byte): Short {
        return (b.toInt() and 0xff).toShort()
    }

    private fun add8Bit(value: Short) {
        bytesWritten++
        if (blockData == null) {
            try {
                blockData = BufferedOutputStream(FileOutputStream("$basePath/$currentFile"), 32768)
            } catch (e: FileNotFoundException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        try {
            blockData!!.write(value.toInt() and 0xff)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun blockSize(): Int {
        return bytesWritten
    }

    fun addSquareWave(duration: Double, amplitude: Double, rest_amplitude: Double) {
        if (duration < 1) {
            return
        }
        val neededSamples = Math.round(sampleRate * (duration / 1000000))
        val a = Math.round(neededSamples / 2.toFloat()).toLong()
        var i: Long

        // high part
        i = 0
        while (i < a) {
            add8Bit(asByte((rest_amplitude * 127 + 128).toByte()))
            i++
        }

        // low part
        i = 0
        while (i < a) {
            add8Bit(asByte((amplitude * 127 + 128).toByte()))
            i++
        }
        totalBytes += neededSamples.toInt()
    }

    fun addPauseOld(duration: Double, amplitude: Double) {
        if (duration < 1000.0) {
            return
        }
        addPulse(1000.0, amplitude)
        earLevel = 0 // low
        addPulse(duration - 1000.0, amplitude)
        earLevel = 0
        accumulatedTimeClock = 0.0
        accumulatedTimeSamples = 0.0
    }

    fun addPause(duration: Double, amplitude: Double) {
        if (duration < 1000.0) {
            return
        }
        earLevel = 0 // low
        addPulseFlat(1000.0, amplitude)

        /*double fillsamples = 3000;
		double val = 1.0/fillsamples;
		double v = -1.0;
		while (Math.abs(v) > 0.0001) {
			add8Bit( asByte((byte)(v * 127 + 128)) );
			v += val;
			val = val*1.0001;
		}*/addSilence(duration - 1000.0, amplitude)
        earLevel = 0
        accumulatedTimeClock = 0.0
        accumulatedTimeSamples = 0.0
    }

    fun addPulseFlat(duration: Double, amplitude: Double) {
        var rest_amplitude = amplitude
        if (earLevel.toInt() == 1) {
            rest_amplitude = amplitude * -1.0
        }
        if (duration < 1) {
            return
        }
        var neededSamples = Math.round(sampleRate * (duration / 1000000.0))
        //System.out.println("needed samples = "+neededSamples);
        val skewval = accumulatedTimeClock - accumulatedTimeSamples
        val onesample = 1000000.0 / sampleRate.toDouble()

        // check if we need to either ADD or SUBTRACT samples to keep timing
        if (Math.abs(skewval) > onesample) {
            val fillSamples = Math.round(skewval / onesample)
            //System.out.println("*** Adjusted by LEAP SAMPLES ("+fillSamples+")");
            neededSamples += fillSamples
        }

        // high part
        var amp = 0.0
        val gradient = 0.0
        for (i in 0 until neededSamples) {
            amp = rest_amplitude * (1 - gradient * i)
            add8Bit(asByte((amp * 127 + 128).toByte()))
        }
        totalBytes += neededSamples.toInt()

        /* adjust clocks */accumulatedTimeSamples += neededSamples * (1000000.0 / sampleRate.toDouble())
        accumulatedTimeClock += duration

        //System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");

        /* invert the pulse at the end */earLevel = (earLevel + 1 and 1).toByte()
    }

    fun addPulse(duration: Double, amplitude: Double) {
        var rest_amplitude = amplitude
        if (earLevel.toInt() == 1) {
            rest_amplitude = amplitude * -1.0
        }
        if (duration < 1) {
            return
        }
        var neededSamples = Math.round(sampleRate * (duration / 1000000.0))
        //System.out.println("needed samples = "+neededSamples);
        val skewval = accumulatedTimeClock - accumulatedTimeSamples
        val onesample = 1000000.0 / sampleRate.toDouble()

        // check if we need to either ADD or SUBTRACT samples to keep timing
        if (Math.abs(skewval) > onesample) {
            val fillSamples = Math.round(skewval / onesample)
            //System.out.println("*** Adjusted by LEAP SAMPLES ("+fillSamples+")");
            neededSamples += fillSamples
        }

        // high part
        var amp = 0.0
        //double gradient = 0.08 / (double)neededSamples;
        val gradient = 0.0
        for (i in 0 until neededSamples) {
            amp = rest_amplitude * (1 - gradient * i)
            add8Bit(asByte((amp * 127 + 128).toByte()))
        }
        totalBytes += neededSamples.toInt()

        /* adjust clocks */accumulatedTimeSamples += neededSamples * (1000000.0 / sampleRate.toDouble())
        accumulatedTimeClock += duration

        //System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");

        /* invert the pulse at the end */earLevel = (earLevel + 1 and 1).toByte()
    }

    fun writeSamples(neededSamples: Long, eLevel: Byte, amplitude: Double) {
        var rest_amplitude = amplitude
        earLevel = eLevel
        if (earLevel.toInt() == 1) {
            rest_amplitude = amplitude * -1.0
        }

        // high part
        var amp = 0.0
        //double gradient = 0.08 / (double)neededSamples;
        val gradient = 0.0
        for (i in 0 until neededSamples) {
            amp = rest_amplitude * (1 - gradient * i)
            add8Bit(asByte((amp * 127 + 128).toByte()))
        }
        totalBytes += neededSamples.toInt()

        //System.err.println("SYSCLOCK: "+this.accumulatedTimeClock+"us, WAVCLOCK: "+this.accumulatedTimeSamples+"us");

        /* invert the pulse at the end */earLevel = (earLevel + 1 and 1).toByte()
    }

    fun addSilence(duration: Double, amplitude: Double) {
        flushChunkIfNeeded()
        if (duration < 1) {
            return
        }
        val neededSamples = Math.round(sampleRate.toLong() * (duration / 1000000))
        var i: Long

        //for (i=0;i<neededSamples;i++) {
        //	add8Bit( asByte((byte)128) );
        //}
        totalBytes += neededSamples.toInt()

        // add entry to manifest
        manifest!!.setValue("Data." + Integer.toString(blockIndex) + ".Type", "SILENCE")
        manifest.setValue("Data." + Integer.toString(blockIndex) + ".Duration", java.lang.Long.toString(neededSamples))
        manifest.setValue("Data." + Integer.toString(blockIndex) + ".Start", Integer.toString(startOfBlock))
        startOfBlock = totalBytes
        blockIndex++
        //this.blockData.reset();
        totalGap++
        totalBlocks++
    }

    private fun flushChunkIfNeeded() {
        // if we have any pcm data, write it out to a file
        if (blockSize() > 0) {
            try {
                // close current file
                blockData!!.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            // add entry to manifest
            manifest!!.setValue("Data." + Integer.toString(blockIndex) + ".Source", currentFile)
            manifest.setValue("Data." + Integer.toString(blockIndex) + ".Type", "DATA")
            manifest.setValue("Data." + Integer.toString(blockIndex) + ".Duration", Integer.toString(bytesWritten))
            manifest.setValue("Data." + Integer.toString(blockIndex) + ".Start", Integer.toString(startOfBlock))

            // now reset for next block
            blockIndex++
            startOfBlock = totalBytes
            totalData++
            totalBlocks++
            bytesWritten = 0
            blockData = null
        }
    }

    val currentFile: String
        get() = baseName + "_" + Integer.toString(blockIndex) + "." + baseExt

    val manifestName: String
        get() = basePath + "/" + baseName + ".manifest"

    @Throws(Throwable::class)
    protected fun finalize() {
        flushChunkIfNeeded()
    }

    fun done() {
        flushChunkIfNeeded()
        writeMeta()
    }

    fun commit() {
        WriteOGDLFile(manifestName, manifest!!)
    }

    private fun writeMeta() {
        manifest!!.setValue("Info.Blocks.Total", Integer.toString(totalBlocks))
        manifest.setValue("Info.Blocks.Data", Integer.toString(totalData))
        manifest.setValue("Info.Blocks.Gap", Integer.toString(totalGap))
        manifest.setValue("Info.SampleRate", Integer.toString(sampleRate))
        manifest.setValue("Info.System", currentSystem)
        WriteOGDLFile(manifestName, manifest)
    }

    fun getSystem(): String {
        return manifest!!.getValue("Info.System")
    }

    fun setSystem(system: String) {
        this.currentSystem = system
        manifest!!.setValue("Info.System", system)
    }

    fun getBaseName(): String {
        return manifest!!.getValue("Info.BaseName")
    }

    fun setBaseName(baseName: String) {
        this.baseName = baseName
        manifest!!.setValue("Info.BaseName", baseName)
    }

    fun getBasePath(): String {
        return manifest!!.getValue("Info.BasePath")
    }

    fun setBasePath(basePath: String) {
        this.basePath = basePath
        manifest!!.setValue("Info.BasePath", basePath)
    }

    fun getBaseExt(): String {
        return manifest!!.getValue("Info.Extension")
    }

    fun setBaseExt(baseExt: String) {
        this.baseExt = baseExt
        manifest!!.setValue("Info.Extension", baseExt)
    }

    fun getTotalBlocks(): Int {
        return manifest!!.getValue("Info.Blocks.Total").toInt()
    }

    fun setTotalBlocks(totalBlocks: Int) {
        this.totalBlocks = totalBlocks
        manifest!!.setValue("Info.Blocks.Total", Integer.toString(totalBlocks))
    }

    fun getTotalData(): Int {
        return manifest!!.getValue("Info.Blocks.Data").toInt()
    }

    fun setTotalData(totalData: Int) {
        this.totalData = totalData
        manifest!!.setValue("Info.Blocks.Data", Integer.toString(totalData))
    }

    fun getTotalGap(): Int {
        return manifest!!.getValue("Info.Blocks.Gap").toInt()
    }

    fun setTotalGap(totalGap: Int) {
        this.totalGap = totalGap
        manifest!!.setValue("Info.Blocks.Gap", Integer.toString(totalGap))
    }

    // block specific accessors
    fun validBlock(index: Int): Boolean {
        return index >= 1 && index <= getTotalBlocks()
    }

    fun blockDuration(index: Int): Int {
        return if (validBlock(index)) {
            manifest!!.getValue("Data." + Integer.toString(index) + ".Duration").toInt()
        } else 0
    }

    fun blockStart(index: Int): Int {
        return if (validBlock(index)) {
            manifest!!.getValue("Data." + Integer.toString(index) + ".Start").toInt()
        } else 0
    }

    fun blockSource(index: Int): String {
        return if (validBlock(index)) {
            getBasePath() + "/" + manifest!!.getValue("Data." + Integer.toString(index) + ".Source")
        } else ""
    }

    fun blockType(index: Int): String {
        return if (validBlock(index)) {
            manifest!!.getValue("Data." + Integer.toString(index) + ".Type")
        } else "INVALID"
    }

    fun blockData(index: Int): ByteArray? {
        if (validBlock(index)) {
            val type = blockType(index)
            if (type == "DATA") {
                return blockSourceLoad(blockSource(index))
            } else if (type == "SILENCE") {
                return blockSourceGenerate(blockDuration(index))
            }
        }
        return ByteArray(0)
    }

    private fun blockSourceGenerate(blockDuration: Int): ByteArray? {
        playingBuffer = null
        System.gc()
        playingBuffer = ByteArray(blockDuration)
        for (i in playingBuffer!!.indices) playingBuffer!![i] = (128.toInt() and 0xff).toByte()
        return playingBuffer
    }

    private fun blockSourceLoad(blockSource: String): ByteArray? {
        try {
            val f = File(blockSource)
            val fis = FileInputStream(f)
            playingBuffer = null
            System.gc()
            playingBuffer = ByteArray(f.length().toInt())
            val x = fis.read(playingBuffer)
            fis.close()
            return playingBuffer
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return null
    }

    val length: Int
        get() {
            var res = 0
            for (i in 0..getTotalBlocks()) {
                res += blockDuration(i)
            }
            return res
        }

    /*
		 * Returns the block index of the next silence AFTER the current block
		 */
    val nextSilence: Int
        get() {
            /*
		 * Returns the block index of the next silence AFTER the current block
		 */
            for (i in playingBlock..totalBlocks) {
                if (blockType(i) == "SILENCE") return i
            }
            return -1
        }

    var loaderType: Int
        get() {
            var s = manifest!!.getValue("Info.Loader.Model")
            if (s == null || s == "") {
                s = "-1"
            }
            return s.toInt()
        }
        set(model) {
            manifest!!.setValue("Info.Loader.Model", Integer.toString(model))
        }

    fun toRawAudio(filename: String?) {
        reset()
        //byte[] buff = new byte[1024];
        try {
            val fos = FileOutputStream(File(filename))
            var buff = getCurrentBuffer(false)
            while (buff!!.isNotEmpty()) {
                fos.write(buff)
                val i = nextBuffer()
                buff = getCurrentBuffer(false)
            }
            fos.close()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    val renderedSampleRate: Int
        get() {
            var v = 44100
            val s = manifest!!.getValue("Info.SampleRate")
            if (s != null && s.isNotEmpty()) {
                v = s.toInt()
            }
            return v
        }

    init {
        //blockData.reset();
        baseName = base
        basePath = path
        val f = File("$path/$base.manifest")

        //System.out.println(f.getPath());
        if (f.exists()) {
            //System.out.println("Exists");
            manifest = ReadOGDLFile(f.path)!!
            //this.sampleRate = Integer.parseInt(this.manifest.getValue("Info.SampleRate"));
            //this.manifest.Root().Dump();
        } else {
            manifest!!.setValue("Info.BaseName", baseName)
            manifest.setValue("Info.BasePath", basePath)
            manifest.setValue("Info.System", currentSystem)
            manifest.setValue("Info.Extension", baseExt)
            manifest.setValue("Info.Blocks.Total", "0")
            manifest.setValue("Info.Blocks.Data", "0")
            manifest.setValue("Info.Blocks.Gap", "0")
            manifest.setValue("Info.BitsPerSample", Integer.toString(bitsPerSample))
            manifest.setValue("Info.SampleRate", Integer.toString(sampleRate))
            manifest.setValue("Info.Channels", Integer.toString(channels))
            // create a file
            bytesWritten = 0
            blockData = null
        }
    }
}