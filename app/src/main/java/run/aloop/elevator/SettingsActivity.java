package run.aloop.elevator;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private PreferenceCategory connCate;
        private EditTextPreference localTSAP;
        private EditTextPreference remote_TSAP;
        private EditTextPreference rack;
        private EditTextPreference slot;
        private ListPreference mode;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            connCate = findPreference("connection");
            localTSAP = findPreference("local_TSAP");
            remote_TSAP = findPreference("remote_TSAP");
            rack = findPreference("rack");
            slot = findPreference("slot");
            mode = findPreference("mode");
            if (mode == null || localTSAP == null || remote_TSAP == null || rack == null || slot == null)
                return;
            updateModeSettings(mode.getValue());
            mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String val = newValue.toString();
                    updateModeSettings(val);
                    return true;
                }
            });
        }

        private void updateModeSettings(String val) {
            switch (val) {
                case "TSAP":
                    connCate.removePreference(rack);
                    connCate.removePreference(slot);
                    connCate.addPreference(localTSAP);
                    connCate.addPreference(remote_TSAP);
                    break;
                case "Rack/Slot":
                    connCate.removePreference(localTSAP);
                    connCate.removePreference(remote_TSAP);
                    connCate.addPreference(rack);
                    connCate.addPreference(slot);
                    break;
            }
        }
    }
}