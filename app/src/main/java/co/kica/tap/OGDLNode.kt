package co.kica.tap

import java.io.BufferedWriter
import java.io.IOException

/**************************************************************************
 * OGDLNode
 * ---------------------------------------------------------------------------
 * This code was translated from ObjectPascal by Scruffy (The Janitor).
 */
class OGDLNode(Parent: OGDLNode?) {
    /* instance vars */
    private var fKey = "nuttin"
    private var fChildren: OGDLNodeList
    private var fParent: OGDLNode?
    private var fIndent: String
    fun Key(): String {

        /* vars */
        val result: String
        result = fKey

        /* enforce non void return */return result
    }

    fun setKey(v: String) {

        /* vars */
        fKey = v
    }

    fun Children(): OGDLNodeList {

        /* vars */
        val result: OGDLNodeList
        result = fChildren

        /* enforce non void return */return result
    }

    fun setChildren(v: OGDLNodeList) {

        /* vars */
        fChildren = v
    }

    fun Parent(): OGDLNode? {

        /* vars */
        val result: OGDLNode?
        result = fParent

        /* enforce non void return */return result
    }

    fun setParent(v: OGDLNode?) {

        /* vars */
        fParent = v
    }

    fun Indent(): String {

        /* vars */
        val result: String
        result = fIndent

        /* enforce non void return */return result
    }

    fun setIndent(v: String) {

        /* vars */
        fIndent = v
    }// ok

    /* enforce non void return */

    /* vars */
    val isBinary: Boolean
        get() {

            /* vars */
            var result: Boolean
            result = false
            for (ch1 in Key().toCharArray()) {
                if (ch1.toInt() >= 32 && ch1.toInt() <= 127 || ch1 == '\r' || ch1 == '\n' || ch1 == '\t') {
                    // ok
                } else {
                    result = true
                    return result
                }
            }

            /* enforce non void return */return result
        }

    /* vars */

    /* enforce non void return */
    val firstChild: OGDLNode?
        get() {

            /* vars */
            var result: OGDLNode?
            result = null
            if (childCount > 0) result = Children()[0]

            /* enforce non void return */return result
        }

    fun ChildByName(name: String): OGDLNode? {

        /* vars */
        var result: OGDLNode?
        var c: Int
        result = null
        c = 0
        while (c < Children().size) {

            //System.out.println("ChildByName() comparing requested ["+name+"] with item ["+this.Children().get(c).Key()+"]");
            if (Children()[c].Key() == name) {
                result = Children()[c]
                return result
            }
            c++
        }

        /* enforce non void return */return result
    }

    fun LastIndent(s: String): OGDLNode? {

        /* vars */
        var result: OGDLNode?
        result = this
        while (result!!.Parent() != null && result.Indent().length > s.length) result = result.Parent()

        /* enforce non void return */return result
    }//fl.Add('');

    //sl.Free;
    //fl.Free;

    /* enforce non void return */

    /* vars */
    val value: String
        get() {

            /* vars */
            var result: String
            var ch: Char
            var s: String
            val sl: TStringList
            val fl: TStringList
            var x: Int
            result = Key()
            if (Key().indexOf('\r') > -1 || Key().indexOf('\n') > -1) {
                s = ""
                x = 1
                while (x <= Level() - 1) {
                    s = "$s  "
                    x++
                }
                sl = TStringList()
                fl = TStringList()
                sl.setText(Key())
                fl.add(sl[0])
                x = 1
                while (x <= sl.size - 1) {
                    fl.add(s + sl[x])
                    x++
                }
                //fl.Add('');
                result = fl.Text()

                //sl.Free;
                //fl.Free;
            } else if (Key().indexOf(' ') > -1 || Key().indexOf('\t') > -1) {
                result = ""
                for (ch1 in Key().toCharArray()) {
                    result = if (ch1 == '\'') {
                        result + '\\' + ch1
                    } else {
                        result + ch1
                    }
                }
                result = "'$result'"
            }

            /* enforce non void return */return result
        }

    private fun Level(): Int {
        return levelsToRoot
    }

    /* vars */

    /* enforce non void return */
    val levelsToRoot: Int
        get() {

            /* vars */
            var result: Int
            var node: OGDLNode?
            result = 0
            node = this
            while (node!!.Parent() != null) {
                result++
                node = node.Parent()
            }

            /* enforce non void return */return result
        }

    fun getChildIndex(child: OGDLNode): Int {

        /* vars */
        var result: Int
        var x: Int
        result = -1
        x = 0
        while (x <= fChildren.size) {
            if (fChildren[x] === child) {
                result = x
                break
            }
            x++
        }

        /* enforce non void return */return result
    }

    /* vars */
    val nextSibling: OGDLNode
        get() {

            /* vars */
            val result: OGDLNode
            val x: Int
            x = Parent()!!.getChildIndex(this)
            result = Parent()!!.Children()[x + 1]

            /* enforce non void return */return result
        }

    /* vars */
    val prevSibling: OGDLNode
        get() {

            /* vars */
            val result: OGDLNode
            val x: Int
            x = Parent()!!.getChildIndex(this)
            result = Parent()!!.Children()[x - 1]

            /* enforce non void return */return result
        }

    /* vars */
    val childCount:

            /* enforce non void return */Int
        get() {

            /* vars */
            val result: Int
            result = fChildren.size

            /* enforce non void return */return result
        }

    fun getChildNode(index: Int): OGDLNode? {

        /* vars */
        val result: OGDLNode?
        result = if (index >= 0 && index <= fChildren.size) {
            fChildren[index]
        } else {
            null
        }

        /* enforce non void return */return result
    }

    fun setChildNode(index: Int, value: OGDLNode) {

        /* vars */
        var index = index
        var c: Int
        if (index < 0) {
            /* don't need to grow this: SetLength(fChildren, length(fChildren)+1); */
            index = 0
            c = fChildren.size
            while (c >= 0 + 1) {
                fChildren[c] = fChildren[c - 1]
                c--
            }
            //if (fChildren.size() > index)
            //value.getNextSibling() = fChildren.get(0+1);
        } else if (index > fChildren.size) {
            /* don't need to grow this: SetLength(fChildren, length(fChildren)+1); */
            index = fChildren.size
            //if (0 < index)
            //value.PrevSibling = fChildren.get(fChildren.size()-1);
        }
        fChildren[index] = value
        value.setParent(this)
    }

    fun AddChild(): OGDLNode {

        /* vars */
        val result: OGDLNode
        result = OGDLNode(this)
        //this.Children().add(result);

        /* enforce non void return */return result
    }

    fun AddSibling(): OGDLNode {

        /* vars */
        val result: OGDLNode
        result = OGDLNode(Parent())
        //this.Parent().Children().add(result);

        /* enforce non void return */return result
    }

    fun Dump() {

        /* vars */
        var x: Int
        var s: String

        //System.out.println("DEBUG: Dump() called on node with KEY = "+this.Key()+", LEVEL = "+this.Level());
        s = ""
        x = 1
        while (x <= Level() - 1) {
            s = "$s  "
            x++
        }
        if (Level() > 0) {
            //System.out.println("Level = "+this.Level());
            //System.out.println("Key = "+this.Key());
            if (hasBlock()) {
                println(s + value + " \\")
            } else {
                println(s + value)
            }
        }
        x = 0
        while (x < fChildren.size) {
            fChildren[x].Dump()
            x++
        }
    }

    private fun hasBlock(): Boolean {
        var has_block: Boolean
        has_block = false
        if (childCount == 1) {
            if (Children()[0].Key().indexOf(10.toChar()) > -1 || Children()[0].Key().indexOf(13.toChar()) > -1) {
                has_block = true
            }
        }
        return has_block
    }

    fun DumpFile(dos: BufferedWriter) {

        /* vars */
        var x: Int
        var s: String
        s = ""
        x = 1
        while (x <= Level() - 1) {
            s = "$s  "
            x++
        }
        if (Level() > 0) {
            if (hasBlock()) {
                try {
                    dos.write("""$s${value} \
""")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                try {
                    dos.write("""
    $s${value}

    """.trimIndent())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        x = 0
        while (x < fChildren.size) {
            fChildren[x].DumpFile(dos)
            x++
        }
    }

    init {

        /* vars */
        fChildren = OGDLNodeList()
        fParent = Parent
        fIndent = ""
        if (Parent() != null && !Parent()!!.Children().contains(this)) {
            // bind our node into the child list
            Parent()!!.Children().add(this)
        }
    }
}