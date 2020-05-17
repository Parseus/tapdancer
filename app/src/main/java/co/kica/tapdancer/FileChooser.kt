package co.kica.tapdancer

import android.Manifest
import android.app.AlertDialog
import android.app.ListActivity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.WindowManager
import android.widget.ListView
import androidx.preference.PreferenceManager
import co.kica.fileutils.SmartFile
import co.kica.tap.T64Format
import co.kica.tap.T64Format.DirEntry
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.*

class FileChooser : ListActivity() {
    private var currentDir: SmartFile? = null
    private var adapter: FileArrayAdapter? = null
    private var prefExtDir: String? = ""
    private var t64option: Option? = null

    private fun initFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefExtDir = prefs.getString("prefStorageInUse", Environment.getExternalStorageDirectory().absolutePath)
        var lastPath = prefs.getString("lastdir", prefExtDir)

        /* if the user has changed their preferred path we want to note that
		 * if they havent then prefExtDir will be contained in lastPath */lastPath = lastPath!!.replaceFirst("[/]$".toRegex(), "") + "/"
        val testExtDir = prefExtDir!!.replaceFirst("[/]$".toRegex(), "") + "/"
        if (!lastPath.startsWith(testExtDir)) {
            lastPath = prefExtDir
        }
        val lp = SmartFile(lastPath!!)
        currentDir = if (lp.exists()) {
            lp
        } else {
            SmartFile(Environment.getExternalStorageDirectory().absolutePath)
        }
    }

    private fun saveToPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = prefs.edit()
        edit.putString("lastdir", currentDir!!.absolutePath)
        edit.commit()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFromPreferences()
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        fill()

        // after this...
        this.window.setBackgroundDrawableResource(R.drawable.tapdancer_background)
        //.setBackgroundResource(R.drawable.tapdancer_background);
    }

    override fun onNewIntent(intent: Intent) {
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        initFromPreferences()
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        fill()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun kb(f: SmartFile): Long {
        return f.length() / 1024
    }

    @AfterPermissionGranted(RC_EXTERNAL_STORAGE_PERMISSION)
    private fun fill() {
        val permission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (EasyPermissions.hasPermissions(this, *permission)) {
            val dirs = currentDir!!.listFiles()
            this.title = "Current Dir: " + currentDir!!.name
            val dir: MutableList<Option> = ArrayList()
            val fls: MutableList<Option> = ArrayList()
            try {
                for (ff in dirs!!) {
                    if (ff.isDirectory) {
                        if (ff.name[0] != '.') dir.add(Option(ff.name, "Folder", ff.absolutePath))
                    } else {
                        if (ff.name.endsWith(".TAP") || ff.name.endsWith(".tap") ||
                                ff.name.endsWith(".CAS") || ff.name.endsWith(".cas") ||
                                ff.name.endsWith(".UEF") || ff.name.endsWith(".uef") ||
                                ff.name.endsWith(".PRG") || ff.name.endsWith(".prg") ||
                                ff.name.endsWith(".P00") || ff.name.endsWith(".p00") ||
                                ff.name.endsWith(".T64") || ff.name.endsWith(".t64") ||
                                ff.name.endsWith(".TZX") || ff.name.endsWith(".tzx") ||
                                ff.name.endsWith(".CDT") || ff.name.endsWith(".cdt")) {
                            if (ff.name[0] != '.') fls.add(Option(ff.name, "File Size: " + kb(ff) + "Kb", ff.absolutePath))
                        }
                    }
                }
            } catch (e: Exception) {
            }
            dir.sort()
            fls.sort()
            dir.addAll(fls)
            if (currentDir!!.absolutePath != prefExtDir) dir.add(0, Option("..", "Parent Directory", currentDir!!.parent))
            adapter = FileArrayAdapter(this@FileChooser, R.layout.file_view, dir)
            this.listAdapter = adapter
        } else {
            EasyPermissions.requestPermissions(this,
                    getString(R.string.external_storage_permission_explanation),
                    RC_EXTERNAL_STORAGE_PERMISSION, *permission)
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        // TODO Auto-generated method stub
        super.onListItemClick(l, v, position, id)
        val o = adapter!!.getItem(position)
        if (o.data.equals("folder", ignoreCase = true) || o.data.equals("parent directory", ignoreCase = true)) {
            currentDir = SmartFile(o.path)
            fill()
        } else {
            onFileClick(o)
        }
    }

    private fun onFileClick(o: Option?) {
        //Toast.makeText(this, "File Clicked: "+o.getPath(), Toast.LENGTH_SHORT).show();
        // create an intent so that we can process the render...

        // create an intent so that we can display the render...
        if (o!!.path.toUpperCase().endsWith(".T64")) {
            val t64 = T64Format(o.path, true)
            if (!t64.hasValidHeader()) {
                val intent = Intent(this, RenderActivity::class.java)
                intent.putExtra(PICKED_MESSAGE, o.path)
                startActivity(intent)
                return
            }
            // valid file
            val dir = t64.dir
            if (dir.size == 1) {
                val intent = Intent(this, RenderActivity::class.java)
                intent.putExtra(PICKED_MESSAGE, o.path)
                startActivity(intent)
                return
            }
            // present chooser..
            chooseT64Item(o, dir)
        } else {
            val intent = Intent(this, RenderActivity::class.java)
            intent.putExtra(PICKED_MESSAGE, o.path)
            startActivity(intent)
        }
    }

    private fun chooseT64Item(o: Option?, dir: ArrayList<DirEntry>) {
        // TODO Auto-generated method stub
        val alertDialog = AlertDialog.Builder(this@FileChooser)
        t64option = o

        // Setting Dialog Title
        alertDialog.setTitle("Select T64 Subfile")

        // Setting Icon to Dialog
        alertDialog.setIcon(R.mipmap.ic_launcher)
        val storageList = arrayOfNulls<String>(dir.size)
        var i = 0
        for (d in dir) {
            storageList[i++] = d.filename
        }
        alertDialog.setItems(storageList) { dialog: DialogInterface?, which: Int ->
            // TODO Auto-generated method stub
            val intent = Intent(this@FileChooser, RenderActivity::class.java)
            intent.putExtra(PICKED_MESSAGE, t64option!!.path)
            intent.putExtra(PICKED_SUBITEM, which.toString())
            startActivity(intent)
        }

        // Showing Alert Message
        alertDialog.show()
    }

    public override fun onPause() {
        super.onPause()
        saveToPreferences()
    }

    public override fun onStop() {
        super.onStop()
        saveToPreferences()
    }

    companion object {
        const val PICKED_MESSAGE = "co.kica.tapdancer.PICKED_MESSAGE"
        const val PICKED_SUBITEM = "co.kica.tapdancer.PICKED_SUBITEM"
        private const val RC_EXTERNAL_STORAGE_PERMISSION = 123
    }
}