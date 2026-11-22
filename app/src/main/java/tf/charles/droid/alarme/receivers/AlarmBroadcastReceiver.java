package tf.charles.droid.alarme.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.window.core.ExperimentalWindowApi;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.alarms.AlarmService;

/**
 * Created by tc on 26/06/17.
 */

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    @OptIn(markerClass = ExperimentalWindowApi.class)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AlmBdcst", "AlarmBroadcastReceiver @ " + System.currentTimeMillis());

        long alarmId = intent.getLongExtra(AlarmApp.ALARM_ID, 0);

        Intent service = new Intent(context, AlarmService.class);
        service.putExtra(AlarmApp.ALARM_ID, alarmId);
        service.setAction(AlarmService.START_ALARM_ACTION);
        context.startService(service);
    }
}
