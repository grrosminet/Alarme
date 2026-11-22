package tf.charles.droid.alarme.alarms;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Calendar;
import java.util.Locale;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.activities.AlarmActivity;
import tf.charles.droid.alarme.data.Alarme;
import tf.charles.droid.alarme.misc.Jour;

public class AlarmService extends Service {
    public static final String START_ALARM_ACTION = "START_ALARM_ACTION";
    public static final String RENEW_ALARM_ACTION = "RENEW_ALARM_ACTION";
    public static final String STOP_ALARM_ACTION = "STOP_ALARM_ACTION";

    private static AlarmInstance currentAlarm = null;
    private static Notification currentAlarmNotification = null;

    private final IBinder binder = new Binder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public static AlarmInstance getCurrentAlarm() {
        return currentAlarm;
    }

    private static synchronized void startAlarm(Service service, Alarme instance) {
        if (currentAlarm != null) {
            stopCurrentAlarm(service);
        }

        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "Alarme:LaunchAlarmLock");
        wakeLock.acquire(60_000); // Garde l'écran allumé pendant 60 secondes

        currentAlarm = new AlarmInstance(instance);

        if(instance.getId() != Long.MIN_VALUE) {
            boolean repeat = false;
            for (Jour j : Jour.values()) {
                if (currentAlarm.getAlarme().isJourActif(j)) {
                    String content;
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        content = mapper.writeValueAsString(currentAlarm.getAlarme());
                    }
                    catch (Throwable t) {
                        content = t.getMessage();
                    }
                    Log.i("AlmSvc", "Alarm content : " + content);
                    Log.i("AlmSvc", "repeat on " + j);
                    repeat = true;
                    break;
                }
            }
            if (!repeat) {
                Log.i("AlmSvc", "no-repeat => disable");
                currentAlarm.getAlarme().setActive(false);
                AlarmUtils.storeAlarm(service, currentAlarm.getAlarme());
            }
        }
        else {
            instance.setTitre(service.getString(R.string.pref_title_tester));
            instance.setHeure(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
            instance.setMinute(Calendar.getInstance().get(Calendar.MINUTE));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(service, AlarmApp.NOTIF_CHANNEL_ALARME_RINGING)
                .setSmallIcon(R.drawable.ic_reveil)
                .setContentTitle(service.getString(R.string.title_activity_alarm)) // Titre
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Notification haute priorité
                .setAutoCancel(false)
                .setOngoing(true)
                .setWhen(0)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setLocalOnly(true); // Se ferme au clic

        if(currentAlarm.getAlarme().getTitre() != null && !currentAlarm.getAlarme().getTitre().isBlank())
            builder.setContentText(currentAlarm.getAlarme().getTitre() + " " + String.format(Locale.getDefault(), "%02d:%02d",
                    currentAlarm.getAlarme().getHeure(),
                    currentAlarm.getAlarme().getMinute()));
        else
            builder.setContentText(String.format(Locale.getDefault(), "%02d:%02d", currentAlarm.getAlarme().getHeure(), currentAlarm.getAlarme().getMinute()));

        builder.setContentIntent(PendingIntent.getActivity(service,
                1, AlarmActivity.createLaunchIntent(service), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        builder.setFullScreenIntent(PendingIntent.getActivity(service,
                        1, AlarmActivity.createLaunchIntent(service), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT),
                true);

        Intent renewIntent = new Intent(service, AlarmService.class);
        renewIntent.setAction(AlarmService.RENEW_ALARM_ACTION);
        builder.setDeleteIntent(PendingIntent.getService(service, 1, renewIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        currentAlarmNotification = builder.build();

        currentAlarm.start();
        
        if (ActivityCompat.checkSelfPermission(service, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            service.startForeground(AlarmApp.NOTIF_ALARME_RINGING_ID, currentAlarmNotification);
        }

        try {
            service.startActivity(AlarmActivity.createLaunchIntent(service));
        }
        catch (Throwable t) {
            // ignore
        }
    }

    private static synchronized void stopCurrentAlarm(Service service) {
        if (currentAlarm == null) {
            Log.v("AlmSvc", "There is no current alarm to stop");
            return;
        }

        Log.v("AlmSvc","AlarmService.stop");

        currentAlarm.stop();
        service.stopForeground(true /* removeNotification */);
        currentAlarm = null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("AlmSvc", "AlarmService.onStartCommand() with " + intent);
        if (intent == null || intent.getAction() == null) {
            return Service.START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case START_ALARM_ACTION:
                long alarmId = intent.getLongExtra(AlarmApp.ALARM_ID, 0);
                if(alarmId == 0)
                    break;
                Alarme alarme = (alarmId == Long.MIN_VALUE) ? new Alarme(Long.MIN_VALUE) : AlarmUtils.loadAlarm(this, alarmId);
                if(alarme == null)
                    break;
                startAlarm(this, alarme);
                break;
            case RENEW_ALARM_ACTION:
                if(currentAlarmNotification != null)
                    startForeground(AlarmApp.NOTIF_ALARME_RINGING_ID, currentAlarmNotification);
                break;
            case STOP_ALARM_ACTION:
                stopCurrentAlarm(this);
                stopSelf();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (currentAlarm != null) {
            stopCurrentAlarm(this);
        }
    }
}
