package tf.charles.droid.alarme.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import tf.charles.droid.alarme.AlarmApp;

/**
 * Created by tc on 27/06/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BootAlrm", "onReceive");
        AlarmApp.getInstance().updateAlarm();
    }
}
