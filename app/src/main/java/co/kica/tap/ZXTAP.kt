package co.kica.tap

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/* This format is a catch-all for spectrum Tape format */
class ZXTAP(sampleRate: Int) : TZXTape(sampleRate) {
    val PAUSE = 1000
    val TZXBLOCK: Byte = 0x10
    override fun load(fn: String) {
        // we cheat here this reads in the file, and maps it into TZXChunks (type $10)
        this.isValid = false
        val f = File(fn)
        val totalSize = f.length()
        try {
            val fis = FileInputStream(f)
            data.reset()
            val dataSize = ByteArray(2) // header size...
            var bytesRead: Long = 0
            var err = false
            var r: Int
            val pause = ByteArray(2)
            pause[0] = (PAUSE % 256).toByte()
            pause[1] = (PAUSE / 256).toByte()
            while (bytesRead < totalSize && !err) {
                // read size of chunk
                r = fis.read(dataSize)
                if (r == 2) {
                    // got header
                    bytesRead += r.toLong()
                    val count: Int = (dataSize[0].toInt() and 0xff) + (dataSize[1].toInt() and 0xff) * 256
                    println("Block header says block is $count bytes...")
                    val chunk = ByteArray(count)
                    r = fis.read(chunk)
                    if (r != count) {
                        err = true
                        println("!!! Block is only $r bytes...")
                        break
                    } else {
                        println("*** Block is correctly $r bytes...")
                    }
                    bytesRead += r.toLong()
                    // here if block data ok
                    data.write(TZXBLOCK.toInt())
                    data.write(pause)
                    data.write(dataSize)
                    data.write(chunk)
                    println("Converted TAP block to TZX(10h) block...")
                } else {
                    err = true
                }
            }
            fis.close()
            this.isValid = !err
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }
}