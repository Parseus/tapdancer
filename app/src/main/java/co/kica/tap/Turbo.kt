package co.kica.tap

object Turbo {
    /*
      Threshold : $0107 (263) clock cycles (TAP byte = $20).
      Bit 0 : $19
      Bit 1 : $28
      Endianess : MSbF
      Pilot byte : $63
      Sync : $64 up to $FF (156 bytes)

      Then...

      1 byte  : $01
      2 bytes : Start address (LSBF)
      2 bytes : End address (LSBF)
      6 bytes : Stored in zero Page
      n bytes : Data
      1 byte  : Checksum (0 XOR all data bytes)

      Notes:-

      There is usually also a sequence of "pre-pilot" bytes, their value is $20.
    */
    const val PrePilot = 0x20
    const val PrePilotLength = 600
    const val PilotByte = 0x63 // leader tone
    const val PilotLength = 1400
    val threshold = byteArrayOf(0x07.toByte(), 0x18.toByte(), 0xa0.toByte())
    val zeroBit = intArrayOf(0x1a, 0x1f, 0x27) // zero?
    val oneBit = intArrayOf(0x28, 0x31, 0x3d) // one?
    @JvmField val SixBytes = byteArrayOf(
            0xAE.toByte(), 0xA7.toByte(), 0, 0, 0, 0
    )
    val SixBytesMoreData = byteArrayOf(
            0xAE.toByte(), 0xA7.toByte(), 1, 0, 0, 0
    )
    @JvmField val SixBytesExec = byteArrayOf(
            0xAE.toByte(), 0xA7.toByte(), 0, 1, 0, 0
    )
    @JvmField val SyncTrain = byteArrayOf(
            0x64.toByte(), 0x65.toByte(), 0x66.toByte(), 0x67.toByte(),
            0x68.toByte(), 0x69.toByte(), 0x6a.toByte(), 0x6b.toByte(), 0x6c.toByte(), 0x6d.toByte(), 0x6e.toByte(), 0x6f.toByte(),
            0x70.toByte(), 0x71.toByte(), 0x72.toByte(), 0x73.toByte(), 0x74.toByte(), 0x75.toByte(), 0x76.toByte(), 0x77.toByte(),
            0x78.toByte(), 0x79.toByte(), 0x7a.toByte(), 0x7b.toByte(), 0x7c.toByte(), 0x7d.toByte(), 0x7e.toByte(), 0x7f.toByte(),
            0x80.toByte(), 0x81.toByte(), 0x82.toByte(), 0x83.toByte(), 0x84.toByte(), 0x85.toByte(), 0x86.toByte(), 0x87.toByte(),
            0x88.toByte(), 0x89.toByte(), 0x8a.toByte(), 0x8b.toByte(), 0x8c.toByte(), 0x8d.toByte(), 0x8e.toByte(), 0x8f.toByte(),
            0x90.toByte(), 0x91.toByte(), 0x92.toByte(), 0x93.toByte(), 0x94.toByte(), 0x95.toByte(), 0x96.toByte(), 0x97.toByte(),
            0x98.toByte(), 0x99.toByte(), 0x9a.toByte(), 0x9b.toByte(), 0x9c.toByte(), 0x9d.toByte(), 0x9e.toByte(), 0x9f.toByte(),
            0xa0.toByte(), 0xa1.toByte(), 0xa2.toByte(), 0xa3.toByte(), 0xa4.toByte(), 0xa5.toByte(), 0xa6.toByte(), 0xa7.toByte(),
            0xa8.toByte(), 0xa9.toByte(), 0xaa.toByte(), 0xab.toByte(), 0xac.toByte(), 0xad.toByte(), 0xae.toByte(), 0xaf.toByte(),
            0xb0.toByte(), 0xb1.toByte(), 0xb2.toByte(), 0xb3.toByte(), 0xb4.toByte(), 0xb5.toByte(), 0xb6.toByte(), 0xb7.toByte(),
            0xb8.toByte(), 0xb9.toByte(), 0xba.toByte(), 0xbb.toByte(), 0xbc.toByte(), 0xbd.toByte(), 0xbe.toByte(), 0xbf.toByte(),
            0xc0.toByte(), 0xc1.toByte(), 0xc2.toByte(), 0xc3.toByte(), 0xc4.toByte(), 0xc5.toByte(), 0xc6.toByte(), 0xc7.toByte(),
            0xc8.toByte(), 0xc9.toByte(), 0xca.toByte(), 0xcb.toByte(), 0xcc.toByte(), 0xcd.toByte(), 0xce.toByte(), 0xcf.toByte(),
            0xd0.toByte(), 0xd1.toByte(), 0xd2.toByte(), 0xd3.toByte(), 0xd4.toByte(), 0xd5.toByte(), 0xd6.toByte(), 0xd7.toByte(),
            0xd8.toByte(), 0xd9.toByte(), 0xda.toByte(), 0xdb.toByte(), 0xdc.toByte(), 0xdd.toByte(), 0xde.toByte(), 0xdf.toByte(),
            0xe0.toByte(), 0xe1.toByte(), 0xe2.toByte(), 0xe3.toByte(), 0xe4.toByte(), 0xe5.toByte(), 0xe6.toByte(), 0xe7.toByte(),
            0xe8.toByte(), 0xe9.toByte(), 0xea.toByte(), 0xeb.toByte(), 0xec.toByte(), 0xed.toByte(), 0xee.toByte(), 0xef.toByte(),
            0xf0.toByte(), 0xf1.toByte(), 0xf2.toByte(), 0xf3.toByte(), 0xf4.toByte(), 0xf5.toByte(), 0xf6.toByte(), 0xf7.toByte(),
            0xf8.toByte(), 0xf9.toByte(), 0xfa.toByte(), 0xfb.toByte(), 0xfc.toByte(), 0xfd.toByte(), 0xfe.toByte(), 0xff.toByte()
    )

    /* This is data we put in the CBM header block */
    var threshIndex = 2
    private val HeaderCode_033c = byteArrayOf(
            0x78.toByte(), 0xa9.toByte(), 0x07.toByte(), 0x8d.toByte(), 0x06.toByte(), 0xdd.toByte(), 0xa2.toByte(), 0x01.toByte(),
            0x20.toByte(), 0xd4.toByte(), 0x02.toByte(), 0x26.toByte(), 0xf7.toByte(), 0xa5.toByte(), 0xf7.toByte(), 0xc9.toByte(),
            0x63.toByte(), 0xd0.toByte(), 0xf5.toByte(), 0xa0.toByte(), 0x64.toByte(), 0x20.toByte(), 0xe7.toByte(), 0x03.toByte(),
            0xc9.toByte(), 0x63.toByte(), 0xf0.toByte(), 0xf9.toByte(), 0xc4.toByte(), 0xf7.toByte(), 0xd0.toByte(), 0xe8.toByte(),
            0x20.toByte(), 0xe7.toByte(), 0x03.toByte(), 0xc8.toByte(), 0xd0.toByte(), 0xf6.toByte(), 0xc9.toByte(), 0x00.toByte(),
            0xf0.toByte(), 0xd6.toByte(), 0x20.toByte(), 0xe7.toByte(), 0x03.toByte(), 0x99.toByte(), 0x2b.toByte(), 0x00.toByte(),
            0x99.toByte(), 0xf9.toByte(), 0x00.toByte(), 0xc8.toByte(), 0xc0.toByte(), 0x0a.toByte(), 0xd0.toByte(), 0xf2.toByte(),
            0xa0.toByte(), 0x00.toByte(), 0x84.toByte(), 0x90.toByte(), 0x84.toByte(), 0x02.toByte(), 0x20.toByte(), 0xe7.toByte(),
            0x03.toByte(), 0x91.toByte(), 0xf9.toByte(), 0x45.toByte(), 0x02.toByte(), 0x85.toByte(), 0x02.toByte(), 0xe6.toByte(),
            0xf9.toByte(), 0xd0.toByte(), 0x02.toByte(), 0xe6.toByte(), 0xfa.toByte(), 0xa5.toByte(), 0xf9.toByte(), 0xc5.toByte(),
            0x2d.toByte(), 0xa5.toByte(), 0xfa.toByte(), 0xe5.toByte(), 0x2e.toByte(), 0x90.toByte(), 0xe7.toByte(), 0x20.toByte(),
            0xe7.toByte(), 0x03.toByte(), 0xc8.toByte(), 0x84.toByte(), 0xc0.toByte(), 0x58.toByte(), 0x18.toByte(), 0xa9.toByte(),
            0x00.toByte(), 0x8d.toByte(), 0xa0.toByte(), 0x02.toByte(), 0x20.toByte(), 0x93.toByte(), 0xfc.toByte(), 0x20.toByte(),
            0x53.toByte(), 0xe4.toByte(), 0xa5.toByte(), 0xf7.toByte(), 0x45.toByte(), 0x02.toByte(), 0x05.toByte(), 0x90.toByte(),
            0xf0.toByte(), 0x03.toByte(), 0x4c.toByte(), 0xe2.toByte(), 0xfc.toByte(), 0xa5.toByte(), 0x31.toByte(), 0xf0.toByte(),
            0x03.toByte(), 0x4c.toByte(), 0xb9.toByte(), 0x02.toByte(), 0xa5.toByte(), 0x32.toByte(), 0xf0.toByte(), 0x03.toByte(),
            0x6c.toByte(), 0x2f.toByte(), 0x00.toByte(), 0x20.toByte(), 0x33.toByte(), 0xa5.toByte(), 0xa2.toByte(), 0x03.toByte(),
            0x86.toByte(), 0xc6.toByte(), 0xbd.toByte(), 0xf3.toByte(), 0x02.toByte(), 0x9d.toByte(), 0x76.toByte(), 0x02.toByte(),
            0xca.toByte(), 0xd0.toByte(), 0xf7.toByte(), 0x4c.toByte(), 0xe9.toByte(), 0x02.toByte(), 0xa9.toByte(), 0x07.toByte(),
            0x85.toByte(), 0xf8.toByte(), 0x20.toByte(), 0xd4.toByte(), 0x02.toByte(), 0x26.toByte(), 0xf7.toByte(), 0xce.toByte(),
            0x20.toByte(), 0xd0.toByte(), 0xc6.toByte(), 0xf8.toByte(), 0x10.toByte(), 0xf4.toByte(), 0xa5.toByte(), 0xf7.toByte(),
            0x60.toByte()
    )
    @JvmField val CBMDataBlock_02a7 = byteArrayOf(
            0xa9.toByte(), 0x80.toByte(), 0x05.toByte(), 0x91.toByte(), 0x4c.toByte(), 0xef.toByte(), 0xf6.toByte(), 0xa9.toByte(),
            0xa7.toByte(), 0x78.toByte(), 0x8d.toByte(), 0x28.toByte(), 0x03.toByte(), 0xa9.toByte(), 0x02.toByte(), 0x8d.toByte(),
            0x29.toByte(), 0x03.toByte(), 0x58.toByte(), 0xa0.toByte(), 0x00.toByte(), 0x84.toByte(), 0xc6.toByte(), 0x84.toByte(),
            0xc0.toByte(), 0x84.toByte(), 0x02.toByte(), 0xad.toByte(), 0x11.toByte(), 0xd0.toByte(), 0x29.toByte(), 0xef.toByte(),
            0x8d.toByte(), 0x11.toByte(), 0xd0.toByte(), 0xca.toByte(), 0xd0.toByte(), 0xfd.toByte(), 0x88.toByte(), 0xd0.toByte(),
            0xfa.toByte(), 0x78.toByte(), 0x4c.toByte(), 0x51.toByte(), 0x03.toByte(), 0xad.toByte(), 0x0d.toByte(), 0xdc.toByte(),
            0x29.toByte(), 0x10.toByte(), 0xf0.toByte(), 0xf9.toByte(), 0xad.toByte(), 0x0d.toByte(), 0xdd.toByte(), 0x8e.toByte(),
            0x07.toByte(), 0xdd.toByte(), 0x4a.toByte(), 0x4a.toByte(), 0xa9.toByte(), 0x19.toByte(), 0x8d.toByte(), 0x0f.toByte(),
            0xdd.toByte(), 0x60.toByte(), 0x20.toByte(), 0x8e.toByte(), 0xa6.toByte(), 0xa9.toByte(), 0x00.toByte(), 0xa8.toByte(),
            0x91.toByte(), 0x7a.toByte(), 0x4c.toByte(), 0x74.toByte(), 0xa4.toByte(), 0x52.toByte(), 0xd5.toByte(), 0x0d.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x8b.toByte(), 0xe3.toByte(), 0xae.toByte(), 0x02.toByte()
    )
    const val CBMDataLoadAddress = 0x02A7
    @JvmField val CBMDataEndAddress = CBMDataLoadAddress + CBMDataBlock_02a7.size
    @JvmStatic fun zeroVal(mode: Int): Int {
        return zeroBit[mode]
    }

    @JvmStatic fun oneVal(mode: Int): Int {
        return oneBit[mode]
    }

    @JvmStatic fun getHeaderBlock(mode: Int): ByteArray {
        HeaderCode_033c[threshIndex] = threshold[mode]
        return HeaderCode_033c
    }
}