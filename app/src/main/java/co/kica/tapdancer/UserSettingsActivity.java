package co.kica.tapdancer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.HashSet;

import co.kica.fileutils.Storage;

public class UserSettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);

            final ListPreference storage = this.findPreference("prefStorageInUse");

            // THIS IS REQUIRED IF YOU DON'T HAVE 'entries' and 'entryValues' in your XML
            setListPreferenceData(storage);

            storage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    setListPreferenceData(storage);
                    return false;
                }
            });
        }

        private static void setListPreferenceData(ListPreference lp) {
            HashSet<String> loc = Storage.getStorageSet();

            CharSequence[] entries = new CharSequence[loc.size()];
            CharSequence[] entryValues = new CharSequence[loc.size()];

            int idx = 0;
            for (String s: loc) {
                CharSequence key = s.subSequence(0, s.length());
                CharSequence value = s.subSequence(0, s.length());
                entries[idx] = key;
                entryValues[idx] = value;
                idx++;
            }

            lp.setEntries(entries);
            lp.setDefaultValue(entryValues[0]);
            lp.setEntryValues(entryValues);
        }
    }
}
