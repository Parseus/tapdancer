package co.kica.tapdancer

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

class FileArrayAdapter(private val c: Context, private val id: Int,
                       private val items: List<Option>) : ArrayAdapter<Option>(c, id, items) {

    override fun getItem(i: Int): Option {
        return items[i]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var v = convertView
        if (v == null) {
            val vi = c.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            v = vi.inflate(id, null)
        }
        val o = items[position]
        val t1 = v!!.findViewById<View>(R.id.TextView01) as TextView
        t1.setTextColor(Color.WHITE)
        t1.typeface = ResourcesCompat.getFont(c, R.font.atarcc)
        val t2 = v.findViewById<View>(R.id.TextView02) as TextView
        t2.setTextColor(Color.LTGRAY)
        t2.typeface = ResourcesCompat.getFont(c, R.font.atarcc)
        t1.text = o.name
        t2.text = o.data
        v.setBackgroundColor(-0xffbd01)
        return v
    }

}