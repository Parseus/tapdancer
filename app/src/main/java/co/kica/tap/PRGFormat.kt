package co.kica.tap

import co.kica.fileutils.SmartFile
import co.kica.fileutils.SmartFileInputStream
import co.kica.tap.Turbo.getHeaderBlock
import co.kica.tap.Turbo.oneVal
import co.kica.tap.Turbo.zeroVal
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class PRGFormat(fn: String, idx: Int) {

    var tapData = ByteArrayOutputStream()
    lateinit var filename: String
    private lateinit var data: ByteArray
    private var progtype = 0
    private var start = 0
    private var end = 0
    private var checkByte: Byte = 0
    var turboMode = 1

    init {
        if (fn.toUpperCase(Locale.getDefault()).endsWith(".T64")) {
            val t64 = T64Format(fn, true)
            if (t64.hasValidHeader()) {
                val dir = t64.dir
                val d = dir[idx]
                start = d.programLoadAddress
                progtype = 3
                data = d.programData
                end = start + data.size
                filename = d.filename.toUpperCase(Locale.getDefault()).replace(".PRG".toRegex(), "")
            }
        } else {
            if (fn.toUpperCase(Locale.getDefault()).endsWith(".P00")) {
                loadP00File(fn)
            } else {
                loadFile(fn)
            }
        }
    }

    private fun loadP00File(filename: String) {
        val header = ByteArray(26)
        val laddr = ByteArray(2)
        val f = SmartFile(filename)
        data = ByteArray((f.length() - 28).toInt())
        try {
            val bis = BufferedInputStream(SmartFileInputStream(f))
            bis.read(header)
            bis.read(laddr)
            bis.read(data)
            bis.close()
            val addr: Int = (laddr[0].toInt() and 0xff) + 256 * (laddr[1].toInt() and 0xff)
            start = addr
            end = addr + data.size
            progtype = 3
            this.filename = f.name.toUpperCase(Locale.getDefault()).replaceFirst(".P00$".toRegex(), "")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadFile(filename: String) {
        val loadAddr = ByteArray(2)
        val f = SmartFile(filename)
        data = ByteArray((f.length() - 2).toInt())
        try {
            val bis = BufferedInputStream(SmartFileInputStream(f))
            val laSize = bis.read(loadAddr)
            val plSize = bis.read(data)
            bis.close()
            val addr: Int = (loadAddr[0].toInt() and 0xff) + (loadAddr[1].toInt() and 0xff) * 256
            start = addr
            end = addr + data.size
            progtype = 3
            this.filename = f.name.toUpperCase(Locale.getDefault()).replaceFirst(".PRG$".toRegex(), "")
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }

    fun generate(): ByteArrayOutputStream? {
        writeHeader()
        writeHeaderRepeat()
        writeSilence()
        writeData()
        writeDataRepeat()
        return tapData
    }

    fun generateWithTurboTape(): ByteArrayOutputStream {
        // save real start and end addresses
        val savedStartAddress = start
        val savedEndAddress = end
        val savedProgData = data

        // seed the turbo tape start and end addresses
        start = Turbo.CBMDataLoadAddress
        end = Turbo.CBMDataEndAddress
        data = Turbo.CBMDataBlock_02a7

        // write the header
        writeTurboHeader()
        writeTurboHeaderRepeat()
        writeSilence()

        // write the turbo loader code
        writeData()
        writeDataRepeat()

        // reset the data
        start = savedStartAddress
        end = savedEndAddress
        data = savedProgData

        // now write ze turbo blocken
        writeSilence()
        writeTurboData()
        return tapData
    }

    private fun writeTurboHeaderData() {
        checkByte = 0
        writeDataByte(progtype, true)
        // start address lSB, mSB
        val sl = start and 0xff
        val sm = start / 256 and 0xff
        writeDataByte(sl, true)
        writeDataByte(sm, true)
        // end address lSB, mSB
        val el = end and 0xff
        val em = end / 256 and 0xff
        writeDataByte(el, true)
        writeDataByte(em, true)
        // filename padded to 16 chars, lower case
        var tmp: String = filename.toUpperCase(Locale.getDefault())
        while (tmp.length < 16) tmp = "$tmp "
        tmp = tmp.substring(0, 16)
        val fndata = tmp.toByteArray()
        for (b in fndata) {
            writeDataByte(b.toInt(), true)
        }
        // header code here
        val header = getHeaderBlock(turboMode)
        for (b in header) {
            writeDataByte(b.toInt() and 0xff, true)
        }

        // pad out rest of it with spaces
        for (i in 0 until 171 - header.size) {
            writeDataByte(32, true)
        }
        // checkbyte
        writeDataByte(checkByte.toInt() and 0xff, false)
    }

    fun writeTurboHeader() {
        writeHeaderPilot()
        writeHeaderDataSyncTrain()
        writeTurboHeaderData()
    }

    fun writeTurboHeaderRepeat() {
        writeHeaderRepeatPilot()
        writeRepeatSyncTrain()
        writeTurboHeaderData()
        writeRepeatTrailer()
    }

    fun writeTurboData() {
        // pre pilot
        for (i in 0 until Turbo.PrePilotLength) {
            writeTurboDataByte(Turbo.PrePilot)
        }

        // first pilot bytes ?
        for (i in 0..2047) {
            writeTurboDataByte(Turbo.PilotByte)
        }

        // countdown synchro sequence $09, .., $01
        for (b in Turbo.SyncTrain) {
            writeTurboDataByte(b.toInt() and 0xff)
        }

        // write type byte
        writeTurboDataByte(0x01)

        // start address lo, hi
        // start address lSB, mSB
        val sl = start and 0xff
        val sm = start / 256 and 0xff
        writeTurboDataByte(sl)
        writeTurboDataByte(sm)
        // end address lSB, mSB
        val el = end and 0xff
        val em = end / 256 and 0xff
        writeTurboDataByte(el)
        writeTurboDataByte(em)

        // six bytes in zero page
        // countdown synchro sequence $09, .., $01
        var z = Turbo.SixBytes
        if (start != 0x0801) {
            z = Turbo.SixBytesExec
        }
        for (b in z) {
            writeTurboDataByte(b.toInt() and 0xff)
        }

        // write data
        for (b in data) {
            writeTurboDataByte(b.toInt() and 0xff)
        }

        // checkbyte
        val cb = calculateChecksum(data)
        writeTurboDataByte(cb)
        writeTurboDataByte(cb)

        // repeat for happiness
        //this.writeTurboDataByte(cb);
        //this.writeSilence();
    }

    fun writeByte(value: Int) {
        tapData.write((value and 0xff).toByte().toInt())
    }

    fun writeBit(bit: Int) {
        when (bit and 1) {
            0 -> {
                writeByte(shortPulse)
                writeByte(mediumPulse)
            }
            1 -> {
                writeByte(mediumPulse)
                writeByte(shortPulse)
            }
        }
    }

    fun writeTurboBit(bit: Int) {
        when (bit and 1) {
            0 -> writeByte(zeroVal(turboMode))
            1 -> writeByte(oneVal(turboMode))
        }
    }

    fun newDataMarker() {
        writeByte(longPulse)
        writeByte(mediumPulse)
    }

    fun endDataMarker() {
        writeByte(longPulse)
        writeByte(shortPulse)
    }

    fun writeDataByte(value: Int, moreData: Boolean) {
        var tmp = value
        var cb = 1
        for (i in 0..7) {
            val bit = tmp and 1
            tmp = tmp ushr 1
            cb = cb xor bit
            writeBit(bit)
        }
        // write check bit
        writeBit(cb and 1)
        // write data marker
        if (moreData) newDataMarker() else endDataMarker()
        checkByte = (checkByte.toInt() xor value).toByte()
    }

    fun writeTurboDataByte(value: Int) {
        var tmp = value
        for (i in 0..7) {
            val bit = tmp and 128 ushr 7
            tmp = tmp shl 1
            writeTurboBit(bit)
        }
        checkByte = (checkByte.toInt() xor value).toByte()
    }

    fun writeByteStream(raw: ByteArray) {
        for ((idx, b) in raw.withIndex()) {
            writeDataByte((b.toInt() and 0xff), idx < raw.size - 1)
        }
    }

    fun writeHeaderPilot() {
        for (i in 0 until 0x6A00) {
            writeByte(shortPulse);
        }
    }

    fun writeHeaderRepeatPilot() {
        for (i in 0 until 0x4f) {
            writeByte(shortPulse)
        }
    }

    fun writeDataPilot() {
		for (i in 0 until 0x1A00) {
			writeByte(shortPulse);
		}
	}

    fun writeDataRepeatPilot() {
		for (i in 0 until 0x4f) {
			writeByte(shortPulse)
		}
	}

    fun writeHeaderDataSyncTrain() {
        val data = byteArrayOf(0x89.toByte(), 0x88.toByte(), 0x87.toByte(), 0x86.toByte(),
                0x85.toByte(), 0x84.toByte(), 0x83.toByte(), 0x82.toByte(), 0x81.toByte())
        // new data marker
        newDataMarker()
        // bytes
        for ((i, b) in data.withIndex()) {
            writeDataByte(b.toInt() and 0xff, i < 8)
        }
    }

    fun writeRepeatSyncTrain() {
        val data = byteArrayOf(0x09.toByte(), 0x08.toByte(), 0x07.toByte(), 0x06.toByte(),
                0x05.toByte(), 0x04.toByte(), 0x03.toByte(), 0x02.toByte(), 0x01.toByte())
        // new data marker
        newDataMarker()
        // bytes
        for ((i, b) in data.withIndex()) {
            writeDataByte(b.toInt() and 0xff, i < 8)
        }
    }

    fun writeHeader() {
        writeHeaderPilot()
        writeHeaderDataSyncTrain()
        writeHeaderData()
    }

    fun writeHeaderRepeat() {
        writeHeaderRepeatPilot()
        writeRepeatSyncTrain()
        writeHeaderData()
        writeRepeatTrailer()
    }

    fun writeData() {
        writeDataPilot()
        writeHeaderDataSyncTrain()
        writeDataBlock()
    }

    fun writeDataRepeat() {
        writeDataRepeatPilot()
        writeRepeatSyncTrain()
        writeDataBlock()
        writeRepeatTrailer()
    }

    fun writeDataBlock() {
        // reset checkbyte
        checkByte = 0
        for (b in data) {
            writeDataByte(b.toInt(), true)
        }
        // checkbyte
        writeDataByte(calculateChecksum(data), false)
    }

    fun writeRepeatTrailer() {
		for (i in 0 until 0x4e) {
			writeByte(shortPulse);
		}
	}

    fun writeSilence() {
        for (i in 0 until 10) {
            writeByte(0)
        }
    }

    private fun writeHeaderData() {
        // now the type of data
        // reset checkbyte
        checkByte = 0
        writeDataByte(progtype, true)
        // start address lSB, mSB
        val sl = start and 0xff
        val sm = start / 256 and 0xff
        writeDataByte(sl, true)
        writeDataByte(sm, true)
        // end address lSB, mSB
        val el = end and 0xff
        val em = end / 256 and 0xff
        writeDataByte(el, true)
        writeDataByte(em, true)
        // filename padded to 16 chars, lower case
        val fndata = filename.toUpperCase(Locale.getDefault()).padEnd(16, ' ').toByteArray()
        for (b in fndata) {
            writeDataByte(b.toInt(), true)
        }
        // 171 bytes..
        for (i in 0..170) {
            writeDataByte(32, true)
        }
        // checkbyte
        writeDataByte(checkByte.toInt() and 0xff, false)
    }

    fun calculateChecksum(block: ByteArray): Int {
        var b = 0
        for (x in block) {
            b = b xor x.toInt()
        }
        return b and 0xff
    }

    companion object {
        const val shortPulse = 0x30
        const val mediumPulse = 0x42
        const val longPulse = 0x56
    }

}