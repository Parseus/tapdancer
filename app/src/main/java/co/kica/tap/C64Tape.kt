package co.kica.tap

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

open class C64Tape : GenericTape() {
    var version: Byte = 0

    override val magicBytes: ByteArray
        get() = Arrays.copyOfRange(header.toByteArray(), 0, 12)

    val mAGICString: String
        get() {
            val b = magicBytes
            return String(b)
        }

    override fun parseHeader(f: InputStream): Boolean {
        val buff = ByteArray(headerSize)
        try {
            //f.reset();
            val len = f.read(buff)
            if (len == headerSize) {
                // reset and store into header
                header.reset()
                header.write(buff)
                val magic = byteArrayOf('C'.toByte(), '6'.toByte(), '4'.toByte(), '-'.toByte(), 'T'.toByte(), 'A'.toByte(), 'P'.toByte(), 'E'.toByte(), '-'.toByte(), 'R'.toByte(), 'A'.toByte(), 'W'.toByte())
                return if (Arrays.equals(magicBytes, magic)) {
                    println("*** File is a valid TAP by the looks of it.")
                    isValid = true
                    println("*** Header says data size is $sizeFromHeader")
                    version = buff[12]
                    println("*** Header says version is $version")
                    status = tapeStatusOk
                    true
                } else {
                    println("TAP File has unrecognized magic: $mAGICString")
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
        val first = byteArrayOf(
                'C'.toByte(), '6'.toByte(), '4'.toByte(), '-'.toByte(), 'T'.toByte(), 'A'.toByte(), 'P'.toByte(), 'E'.toByte(), '-'.toByte(), 'R'.toByte(), 'A'.toByte(), 'W'.toByte(),
                version,
                0, 0, 0)
        val second = sizeAsBytes
        val all = ByteArray(first.size + second.size)
        System.arraycopy(first, 0, all, 0, first.size)
        System.arraycopy(second, 0, all, first.size, second.size)
        return all
    }

    override val headerSize: Int = 20

    override val minPadding: Int = 4

    fun asByte(b: Byte): Short {
        return (b.toInt() and 0xff).toShort()
    }

    override fun writeAudioStreamData(path: String, base: String) {
        val w = IntermediateBlockRepresentation(path, base)

        //Data.reset();
        val raw = data.toByteArray()
        var bytesread = 0
        var duration = 0.0
        val cnv = 1.0
        while (bytesread < data.size()) {
            val p = asByte(raw[bytesread++])
            if (p > 0) {
                val cycles = p * 8.toDouble()
                duration = cnv * MU * (cycles / PAL_CLK)
                addSquareWave(w, duration, PULSE_AMPLITUDE, PULSE_REST)
            } else {
                var a: Short
                var b: Short
                var c: Short
                var cycles: Double
                when (version.toInt()) {
                    1 -> {
                        a = asByte(raw[bytesread++])
                        b = asByte(raw[bytesread++])
                        c = asByte(raw[bytesread++])
                        cycles = a + 256 * b + (65536 * c).toDouble()
                        println("*** a = $a, b = $b, c = $c")
                        duration = cnv * MU * (cycles / PAL_CLK)
                    }
                    0 -> {
                        cycles = 2048.0
                        a = raw[bytesread++].toShort()
                        while (a.toInt() == 0) {
                            cycles += 2048
                            a = asByte(raw[bytesread++])
                        }
                        bytesread--
                        duration = cnv * MU * (cycles / PAL_CLK)
                    }
                }
                println("Silence duration = $duration")
                addSilence(w, duration, PULSE_AMPLITUDE)
            }
        }

        // do cue
        w.done()

        //return w;
    }

    override val isHeaderData: Boolean = false

    override val tapeType: String = "TAP"

    override val renderPercent: Float
        get() = dataPos.toFloat() / data.size().toFloat()

    companion object {
        const val PULSE_AMPLITUDE = 1.0
        const val PULSE_MIDDLE = -1.0
        const val PULSE_REST = -1.0
        const val PAL_CLK = 985248.0
        const val MU = 1000000.0
    }

}