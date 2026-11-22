package tf.charles.droid.alarme.activities.preferences;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.activities.AlarmActivity;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.alarms.AlarmService;

/**
 * This fragment shows notification preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
public class NotificationPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.pref_notification, rootKey);

        SettingsActivity.bindPreferenceSummaryToValue(findPreference("alarm_sunrise_duree"), true);
        SettingsActivity.bindPreferenceSummaryToValue(findPreference("alarm_vibration_delai"), true);
        ((SettingsActivity) this.requireActivity()).bindRingtonePreference(findPreference("alarm_tone"));
        SettingsActivity.bindPreferenceSummaryToValue(findPreference("alarm_tone_delai"), true);
        SettingsActivity.bindPreferenceSummaryToValue(findPreference("alarm_volume_progressif_delai"), true);
        SettingsActivity.bindPreferenceSummaryToValueExplanation(findPreference("moving_button"), R.string.pref_explain_bouton_mobile_0,
                R.string.pref_explain_bouton_mobile_1, R.string.pref_explain_bouton_mobile_n);
        SettingsActivity.bindPreferenceSummaryToValueExplanation(findPreference("mathematics"), R.string.pref_explain_calcul_0,
                R.string.pref_explain_calcul_1, R.string.pref_explain_calcul_n);
        Objects.<Preference>requireNonNull(findPreference("test")).setOnPreferenceClickListener(preference -> {
            Intent service = new Intent(this.getContext(), AlarmService.class);
            service.putExtra(AlarmApp.ALARM_ID, Long.MIN_VALUE);
            service.setAction(AlarmService.START_ALARM_ACTION);
            this.getContext().startService(service);
            return true;
        });
        Objects.<Preference>requireNonNull(findPreference("broadcastAction")).setOnPreferenceClickListener(preference -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Broadcast Action", "tf.charles.droid.alarme.PLAY"));
            Toast.makeText(getActivity(), "Action copiée dans le presse-papier", Toast.LENGTH_LONG).show();
            return true;
        });
        Objects.<Preference>requireNonNull(findPreference("activityClass")).setOnPreferenceClickListener(preference -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Alarm activity", AlarmActivity.class.getName()));
            Toast.makeText(getActivity(), "Classe d'activity copiée dans le presse-papier", Toast.LENGTH_LONG).show();
            return true;
        });
    }
}
