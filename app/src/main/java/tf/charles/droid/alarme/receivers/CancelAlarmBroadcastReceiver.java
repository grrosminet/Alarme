package tf.charles.droid.alarme.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.window.core.ExperimentalWindowApi;

import tf.charles.droid.alarme.AlarmApp;

/**
 * Created by tc on 26/06/17.
 */

public class CancelAlarmBroadcastReceiver extends BroadcastReceiver {
    public static String ALARM_ID = "AlarmId";

    @OptIn(markerClass = ExperimentalWindowApi.class)
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Annulation de l'alarme", Toast.LENGTH_LONG).show();
        AlarmApp.getInstance().cancelNextAlarm();
    }
}
