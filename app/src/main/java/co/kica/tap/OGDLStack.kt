package co.kica.tap

/**************************************************************************
 * OGDLStack
 * ---------------------------------------------------------------------------
 * This code was translated from ObjectPascal by Scruffy (The Janitor).
 */
class OGDLStack {
    /* instance vars */
    private var S: OGDLNodeList? = null
    fun S(): OGDLNodeList? {

        /* vars */
        val result: OGDLNodeList? = S

        /* enforce non void return */return result
    }

    fun Nodes(): OGDLNodeList? {
        return S
    }

    fun setS(v: OGDLNodeList?) {

        /* vars */
        S = v
    }

    fun Empty() {

        /* vars */
        S = OGDLNodeList()
    }

    fun Peek(): OGDLNode? {

        /* vars */
        var result: OGDLNode?
        result = null
        if (S!!.size > 0) result = S!![S!!.size]

        /* enforce non void return */return result
    }

    fun Pop(): OGDLNode? {

        /* vars */
        var result: OGDLNode?
        result = null
        if (S!!.size > 0) {
            result = S!![S!!.size]
            /* don't need to shrink this: SetLength(S,length(S)-1); */
        }

        /* enforce non void return */return result
    }

    fun Push(value: OGDLNode) {

        /* vars */


        /* don't need to grow this: SetLength(S, length(S)+1); */
        S!!.add(value)
    }

    init {

        /* vars */
        Empty()
    }
}