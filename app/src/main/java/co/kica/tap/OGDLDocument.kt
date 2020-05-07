package co.kica.tap

import java.io.*

/**************************************************************************
 * OGDLDocument
 * ---------------------------------------------------------------------------
 * This code was translated from ObjectPascal by Scruffy (The Janitor).
 */
class OGDLDocument {
    /* instance vars */
    private var fRoot: OGDLNode? = null
    private var doc = ""
    private var index = 0
    fun Root(): OGDLNode? {

        /* vars */
        val result: OGDLNode?
        result = fRoot

        /* enforce non void return */return result
    }

    fun setRoot(v: OGDLNode?) {

        /* vars */
        fRoot = v
    }

    fun FindNode(nodepath: String, mkdir: Boolean): OGDLNode? {

        /* vars */
        var result: OGDLNode?
        var n: OGDLNode?
        var c: OGDLNode?
        var chunk: String
        result = null
        if (Root() == null) {
            setRoot(OGDLNode(null))
            Root()!!.setIndent("")
            Root()!!.setKey("{root}")
        }
        n = Root()
        chunk = ""
        val parts: Array<String> = nodepath.split("[.\\\\]+").toTypedArray()
        var i: Int = 0
        while (i < parts.size) {
            chunk = parts[i]
            //L.d("Looking for child by name = "+chunk);
            c = n!!.ChildByName(chunk)
            if (c == null) {
                //L.d( "NOT FOUND" );
            }
            if (c == null) {
                if (mkdir) {
                    c = n.AddChild()
                    c.setKey(chunk)
                } else return result
            }
            n = c
            i++
        }
        result = n

        /* enforce non void return */return result
    }

    fun getValue(index: String): String {

        /* vars */
        var result: String
        val n: OGDLNode?
        result = ""
        n = FindNode(index, false)
        if (n != null) {
            if (n.childCount == 1) {
                result = n.Children()[0].Key()
            }
        }

        /* enforce non void return */return result
    }

    fun setValue(index: String, value: String?) {

        /* vars */
        var n: OGDLNode?
        n = FindNode(index, true)
        if (n != null) {
            if (n.childCount == 1) {
                n.Children()[0].setKey(value!!)
            } else if (n.childCount == 0) {
                n = n.AddChild()
                n.setKey(value!!)
            }
        }
    }

    fun NextToken(): String {

        /* vars */
        var result: String
        var qq: Boolean
        var enq: Boolean
        val ch: Char
        qq = false
        result = ""
        ch = doc[index]

        /* meta */if (ch == ',' || ch == '(' || ch == ')' || ch == '\\') {
            result = "" + ch
            index++
            return result
        }

        /* line comment */if (ch == '#') {
            index++
            while (index < doc.length && doc[index] != '\r' && doc[index] != '\n') index++
            if (index < doc.length && (doc[index + 1] == '\r' || doc[index + 1] == '\n')) index++
            result = "\n"
            //L.d( "--- Ignored comment until EOL" );
            return result
        }
        if (ch == '\r' || ch == '\n') {
            result = "\n"
            index++
            if (index < doc.length && (doc[index + 1] == '\r' || doc[index + 1] == '\n')) index++
            //L.d( "--- EOL" );
            return result
        }

        /* chunk starting with double quote */if (ch == '"') {
            index++
            qq = true
            enq = false
            while (index < doc.length && qq) {
                when (doc[index]) {
                    '"' -> if (enq) {
                        result += doc[index]
                        enq = false
                    } else {
                        qq = false
                    }
                    '\\' -> enq = true
                    else -> {
                        if (enq) result += '\\'
                        result += doc[index]
                        enq = false
                    }
                }
                index++
            }
            //L.d( "--- Got chunk: ["+result+"]" );
            return result
        }

        /* chunk starting with double quote */if (ch == '\'') {
            index++
            qq = true
            enq = false
            while (index < doc.length && qq) {
                when (doc[index]) {
                    '\'' -> if (enq) {
                        result += doc[index]
                        enq = false
                    } else {
                        qq = false
                    }
                    '\\' -> enq = true
                    else -> {
                        if (enq) result += '\\'
                        result += doc[index]
                        enq = false
                    }
                }
                index += 1
            }
            //L.d( "--- Got chunk: ["+result+"]" );
            return result
        }

        /* break */if (doc[index] == ' ' || doc[index] == '\t') {
            while (index < doc.length && (doc[index] == ' ' || doc[index] == '\t')) {
                result += doc[index]
                index++
            }
            result = UnTab(result)
            //L.d( "--- Got break: ["+result+"]" );
            //result = ' ';
            return result
        }

        /* word */while (index < doc.length && doc[index] != ' ' && doc[index] != '\t' && doc[index] != '\r' && doc[index] != '\n' && doc[index] != ',' && doc[index] != '(' && doc[index] != ')') {
            result += doc[index]
            index++
        }

        //L.d( "--- Got chunk: ["+result+"]" );
        index++

        /* enforce non void return */return result
    }

    private fun UnTab(result: String): String {
        return result.replace("\t", "  ")
    }

    fun Parse(fulldoc: String) {

        /* vars */
        var token: String
        var cnode: OGDLNode?
        var enode: OGDLNode
        var snode: OGDLNode?
        var nn: Int
        var indent: String
        val S: OGDLStack
        val B: OGDLStack
        val sl: TStringList
        var lineCount: Int
        var abortLine: Boolean
        doc = fulldoc
        setRoot(OGDLNode(null))
        Root()!!.setKey("{root}")
        Root()!!.setIndent("")
        cnode = Root()
        snode = cnode
        index = 1
        token = ""
        indent = ""
        cnode = Root()
        S = OGDLStack()
        S.Push(Root()!!)
        B = OGDLStack()
        sl = TStringList()
        sl.setText(fulldoc)
        lineCount = 0
        while (lineCount < sl.size) {
            doc = TrimRight(sl[lineCount])
            lineCount++
            //L.d( 'Got line: [', doc, ']' );
            index = 0
            indent = ""
            if (Trim(doc) !== "") {

                /* seed the start of line indentation */
                token = NextToken()
                if (token[0] == ' ') {
                    indent = token
                } else {
                    index = 0
                }
                //L.d( 'Indent == ['+indent+'], start index == ', index );

                /* next thing is the first node */token = NextToken()
                //L.d( 'First token is '+token );

                /* find a parent */nn = S.Nodes()!!.size - 1
                while (S.Nodes()!![nn].Indent().length >= indent.length && nn > 0) nn--

                /* create that start node */snode = S.Nodes()!![nn].AddChild()
                snode.setKey(token)
                snode.setIndent(indent)

                /* place this node on the stack too */S.Push(snode)
                B.Push(snode.Parent()!!)
                cnode = snode
                abortLine = index >= doc.length
                while (index < doc.length && !abortLine) {
                    token = NextToken()
                    //L.d('Token: ['+token+']' );
                    if (token[0] == ' ') {
                        //L.d( 'SKIP BLANK' );
                    } else if (token[0] == '(') {
                        B.Push(cnode!!)
                        //L.d( 'ENTER LIST' );
                    } else if (token[0] == ')') {
                        cnode = B.Pop()
                        //L.d( 'EXIT LIST' );
                    } else if (token[0] == ',') {
                        cnode = B.Peek()
                    } else if (token[0] == '\\') {
                        abortLine = true
                        index = doc.length + 1
                        doc = ""
                        while (lineCount < sl.Count() && Trim(sl[lineCount]) !== "") {
                            doc = if (doc === "") {
                                Trim(sl[lineCount])
                            } else {
                                """
     $doc
     ${Trim(sl[lineCount])}
     """.trimIndent()
                            }
                            lineCount++
                        }
                        lineCount++
                        enode = cnode!!.AddChild()
                        enode.setKey(doc)
                        enode.setIndent(indent)
                    } else {
                        /* word */
                        enode = cnode!!.AddChild()
                        enode.setKey(token)
                        enode.setIndent(indent)
                        cnode = enode
                    }

                    //L.d('loop');
                }
                B.Empty()
            }
        }

        //L.d;
        //this.Root.Dump;
    }

    private fun TrimRight(string: String): String {
        return string.replaceFirst("[ \t\r\n]*$".toRegex(), "")
    }

    private fun TrimLeft(string: String): String {
        return string.replaceFirst("^[ \t\r\n]*".toRegex(), "")
    }

    private fun Trim(string: String): String {
        return TrimLeft(TrimRight(string))
    }

    companion object {
        @JvmStatic
        fun ReadOGDLFile(filename: String?): OGDLDocument? {
            val contents = StringBuilder()
            var reader: BufferedReader? = null
            try {
                reader = BufferedReader(FileReader(File(filename)))
                var text: String? = null

                // repeat until all lines is read
                while (reader.readLine().also { text = it } != null) {
                    contents.append(text)
                            .append(System.getProperty(
                                    "line.separator"))
                }
                reader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    reader?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            val s = contents.toString()
            if (s == "") {
                return null
            }
            val doc = OGDLDocument()
            doc.Parse(s)
            return doc
        }

        @JvmStatic
        fun WriteOGDLFile(filename: String?, doc: OGDLDocument) {
            try {
                val bos = BufferedWriter(FileWriter(filename))
                doc.Root()!!.DumpFile(bos)
                bos.close()
            } catch (e: FileNotFoundException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        fun ReadXMLAsOGDL(filename: String?, doc: OGDLDocument?, tagAttributes: Boolean) {
            // this is a cheat method which employs reading an XML document as an OGDL tree
        }
    }

    init {

        /* vars */
        setRoot(OGDLNode(null))
        Root()!!.setKey("{root}")
    }
}