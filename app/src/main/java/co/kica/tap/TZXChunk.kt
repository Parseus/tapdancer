package co.kica.tap

data class TZXChunk(var id: Int = 0,
                    var pauseAfter: Int = 1000,
                    var dataSize: Int = 0,
                    var chunkData: ByteArray = ByteArray(0),
                    var pilotPulseLength: Int = 2168,
                    var syncFirstPulseLength: Int = 667,
                    var syncSecondPulseLength: Int = 735,
                    var zeroBitPulseLength: Int = 855,
                    var oneBitPulseLength: Int = 1710,
                    var pilotPulseCount: Int = 8064,
                    var usedBitsLastByte: Int = 8,
                    var dataPulseCount: Int = 0,
                    var ticksPerBit: Int = 0,
                    var blockLength: Int = 0,
                    var sampleRate: Int = 0,
                    var compressionType: Int = 0,
                    var CSWPulseCount: Int = 0,
                    var relativeJump: Int = 0,
                    var numberRepetitions: Int = 0,
                    var description: String? = null) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TZXChunk

        if (id != other.id) return false
        if (pauseAfter != other.pauseAfter) return false
        if (dataSize != other.dataSize) return false
        if (!chunkData.contentEquals(other.chunkData)) return false
        if (pilotPulseLength != other.pilotPulseLength) return false
        if (syncFirstPulseLength != other.syncFirstPulseLength) return false
        if (syncSecondPulseLength != other.syncSecondPulseLength) return false
        if (zeroBitPulseLength != other.zeroBitPulseLength) return false
        if (oneBitPulseLength != other.oneBitPulseLength) return false
        if (pilotPulseCount != other.pilotPulseCount) return false
        if (usedBitsLastByte != other.usedBitsLastByte) return false
        if (dataPulseCount != other.dataPulseCount) return false
        if (ticksPerBit != other.ticksPerBit) return false
        if (blockLength != other.blockLength) return false
        if (sampleRate != other.sampleRate) return false
        if (compressionType != other.compressionType) return false
        if (CSWPulseCount != other.CSWPulseCount) return false
        if (relativeJump != other.relativeJump) return false
        if (numberRepetitions != other.numberRepetitions) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + pauseAfter
        result = 31 * result + dataSize
        result = 31 * result + chunkData.contentHashCode()
        result = 31 * result + pilotPulseLength
        result = 31 * result + syncFirstPulseLength
        result = 31 * result + syncSecondPulseLength
        result = 31 * result + zeroBitPulseLength
        result = 31 * result + oneBitPulseLength
        result = 31 * result + pilotPulseCount
        result = 31 * result + usedBitsLastByte
        result = 31 * result + dataPulseCount
        result = 31 * result + ticksPerBit
        result = 31 * result + blockLength
        result = 31 * result + sampleRate
        result = 31 * result + compressionType
        result = 31 * result + CSWPulseCount
        result = 31 * result + relativeJump
        result = 31 * result + numberRepetitions
        result = 31 * result + (description?.hashCode() ?: 0)
        return result
    }
}