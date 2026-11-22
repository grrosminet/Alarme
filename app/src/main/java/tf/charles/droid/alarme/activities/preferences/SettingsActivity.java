package tf.charles.droid.alarme.activities.preferences;


import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.InputType;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import java.util.Objects;

import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.activities.AlarmActivity;
import tf.charles.droid.alarme.alarms.AlarmService;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {
    private Preference ringtonePreference;
    private final ActivityResultLauncher<Intent> ringtonePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
        Intent data = result.getData();
        if (data != null) {
            Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String ringtone = ringtoneUri == null ? "" : ringtoneUri.toString();
            SharedPreferences.Editor editor = Objects.requireNonNull(ringtonePreference.getSharedPreferences()).edit();
            editor.putString(ringtonePreference.getKey(), ringtone);
            editor.apply();
            updateRingtoneSummary(ringtonePreference, ringtone);
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Afficher le fragment de préférences
        getSupportFragmentManager().beginTransaction().replace(R.id.settings_container, new NotificationPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(AlarmService.getCurrentAlarm() != null && AlarmService.getCurrentAlarm().isAlarmPlaying()) {
            startActivity(AlarmActivity.createLaunchIntent(this));
            return;
        }
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    public static void bindPreferenceSummaryToValue(Preference preference, boolean numeric) {
        if (preference == null)
            return;

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener((pref, value) -> {
            updatePreferenceSummary(pref, value, numeric);
            return true;
        });

        if(preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setOnBindEditTextListener(editText -> {
                editText.setSelectAllOnFocus(true);
                editText.selectAll();
                editText.setMaxLines(1);
                if(numeric)
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            });
        }

        // Trigger the listener immediately with the preference's current value.
        updatePreferenceSummary(preference, Objects.requireNonNull(preference.getSharedPreferences()).getString(preference.getKey(), ""), numeric);
    }

    public static void bindPreferenceSummaryToValueExplanation(Preference preference, int stringId_0, int stringId_1, int stringId_n) {
        if (preference == null)
            return;

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener((pref, value) -> {
            updatePreferenceSummaryExplanation(pref, value, stringId_0, stringId_1, stringId_n);
            return true;
        });

        if(preference instanceof EditTextPreference) {
            ((EditTextPreference) preference).setOnBindEditTextListener(editText -> {
                editText.setSelectAllOnFocus(true);
                editText.selectAll();
                editText.setMaxLines(1);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            });
        }

        // Trigger the listener immediately with the preference's current value.
        updatePreferenceSummaryExplanation(preference, Objects.requireNonNull(preference.getSharedPreferences()).getString(preference.getKey(), ""), stringId_0, stringId_1, stringId_n);
    }

    public void bindRingtonePreference(Preference preference) {
        if (preference == null)
            return;

        preference.setOnPreferenceClickListener(clickedPreference -> {
            ringtonePreference = clickedPreference;

            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Objects.requireNonNull(clickedPreference.getSharedPreferences()).getString(clickedPreference.getKey(), ""));

            ringtonePickerLauncher.launch(intent);
            return true;
        });

        updateRingtoneSummary(preference, Objects.requireNonNull(preference.getSharedPreferences()).getString(preference.getKey(), ""));
    }

    public static void updatePreferenceSummary(Preference preference, Object value, boolean numeric) {
        String stringValue = value.toString();
        if(numeric) {
            try {
                Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                preference.setSummary(preference.getContext().getString(R.string.pref_explain_valeur_non_entiere).replace("%1", stringValue));
                return;
            }
        }

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            preference.setSummary(stringValue);
        }
    }

    public static void updatePreferenceSummaryExplanation(Preference preference, Object value, int stringId_0, int stringId_1, int stringId_n) {
        String stringValue = value.toString();
        int intValue;
        try {
            intValue = Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            preference.setSummary(preference.getContext().getString(R.string.pref_explain_valeur_non_entiere).replace("%1", stringValue));
            return;
        }

        int resId = intValue == 0 ? stringId_0 : intValue == 1 ? stringId_1 : stringId_n;
        preference.setSummary(preference.getContext().getString(resId).replace("%1", stringValue));
    }

    public static void updateRingtoneSummary(Preference preference, String stringValue) {
        if (stringValue.isBlank()) {
            // Empty values correspond to 'silent' (no ringtone).
            preference.setSummary(R.string.pref_ringtone_silent);
            return;
        }

        // For ringtone preferences, look up the correct display value
        // using RingtoneManager.
        Ringtone ringtone = RingtoneManager.getRingtone(preference.getContext(), Uri.parse(stringValue));

        if (ringtone == null) {
            // Clear the summary if there was a lookup error.
            preference.setSummary(null);
        } else {
            // Set the summary to reflect the new ringtone display
            // name.
            String name = ringtone.getTitle(preference.getContext());
            preference.setSummary(name);
        }
    }

}
