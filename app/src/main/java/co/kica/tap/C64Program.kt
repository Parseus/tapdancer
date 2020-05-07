package co.kica.tap

class C64Program : C64Tape() {

    var loadModel = 1
    var idx = 0

    override fun load(fn: String) {
        val prg = PRGFormat(fn, idx)
        if (loadModel == -1) {
            data = prg.generate()!!
        } else {
            prg.turboMode = loadModel
            data = prg.generateWithTurboTape()
        }
        isValid = true
        status = tapeStatusOk
    }

    override fun writeAudioStreamData(path: String, base: String) {
        super.writeAudioStreamData(path, base)
        val w = IntermediateBlockRepresentation(path, base)
        w.loaderType = loadModel
        w.commit()
    }

}