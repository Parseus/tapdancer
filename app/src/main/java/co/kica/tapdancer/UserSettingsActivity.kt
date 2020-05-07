package co.kica.tapdancer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import co.kica.fileutils.Storage

class UserSettingsActivity : AppCompatActivity(R.layout.activity_settings) {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            val storage = findPreference<ListPreference>("prefStorageInUse")

            // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
            setListPreferenceData(storage)
            storage!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                setListPreferenceData(storage)
                false
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)
        }

        private fun setListPreferenceData(lp: ListPreference?) {
            val loc = Storage.storageSet
            val entries = arrayOfNulls<CharSequence>(loc.size)
            val entryValues = arrayOfNulls<CharSequence>(loc.size)

            for ((idx, s) in loc.withIndex()) {
                val key = s!!.subSequence(0, s.length)
                val value = s.subSequence(0, s.length)
                entries[idx] = key
                entryValues[idx] = value
            }

            lp!!.entries = entries
            lp.setDefaultValue(entryValues[0])
            lp.entryValues = entryValues
        }
    }
}