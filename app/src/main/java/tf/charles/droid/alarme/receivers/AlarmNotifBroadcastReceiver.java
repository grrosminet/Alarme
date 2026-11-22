package tf.charles.droid.alarme.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.window.core.ExperimentalWindowApi;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.activities.AlarmActivity;

/**
 * Created by tc on 26/06/17.
 */

public class AlarmNotifBroadcastReceiver extends BroadcastReceiver {
    public static String ALARM_ID = "AlarmId";

    @OptIn(markerClass = ExperimentalWindowApi.class)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlmNotBdcst", "AlarmNotifBroadcastReceiver @ " + System.currentTimeMillis());

        AlarmApp.getInstance().showAlarmNotification();
    }
}
