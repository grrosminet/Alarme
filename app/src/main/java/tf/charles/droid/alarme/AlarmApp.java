package tf.charles.droid.alarme;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import tf.charles.droid.alarme.activities.ListeAlarmesActivity;
import tf.charles.droid.alarme.alarms.AlarmInstance;
import tf.charles.droid.alarme.alarms.AlarmUtils;
import tf.charles.droid.alarme.data.Alarme;
import tf.charles.droid.alarme.misc.Jour;
import tf.charles.droid.alarme.receivers.AlarmBroadcastReceiver;
import tf.charles.droid.alarme.receivers.AlarmNotifBroadcastReceiver;
import tf.charles.droid.alarme.receivers.CancelAlarmBroadcastReceiver;

/**
 * Created by tc on 27/06/17.
 */

public class AlarmApp extends Application {
    public static final String NOTIF_CHANNEL_ALARME_SOON = "alarm_soon_channel_id";
    public static final String NOTIF_CHANNEL_ALARME_RINGING = "alarm_ringing_channel_id";

    public static final int NOTIF_ALARME_SOON_ID = 10001;
    public static final int NOTIF_ALARME_RINGING_ID = 10002;

    public static final String ALARM_ID = "AlarmId";

    private PendingIntent alarmNotifIntent = null;
    private PendingIntent alarmIntent = null;

    private Long noAlarmUntil = null;

    private static AlarmApp instance;

    public static AlarmApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        instance = this;

        super.onCreate();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            registerCalendarsListener();
        }

        createNotificationChannel();

        updateAlarm();
    }

    private void createNotificationChannel() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        {
            NotificationChannelCompat channel =
                    new NotificationChannelCompat.Builder(NOTIF_CHANNEL_ALARME_SOON, NotificationManagerCompat.IMPORTANCE_HIGH).setName("Alarmes à venir").setDescription("Notifications d'alarmes à venir").setVibrationEnabled(false).setSound(null, null).build();
            notificationManager.createNotificationChannel(channel);
        }
        {
            NotificationChannelCompat channel = new NotificationChannelCompat.Builder(NOTIF_CHANNEL_ALARME_RINGING,
                    NotificationManagerCompat.IMPORTANCE_HIGH).setName("Alarme en cours").setDescription("Notifications d'alarme").setVibrationEnabled(false).setSound(null, null).build();
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void registerCalendarsListener() {
        Uri calendarUri = Uri.parse("content://com.android.calendar/events");
        ContentObserver calendarObserver = new ContentObserver(new Handler(getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                updateAlarm();
            }
        };
        getContentResolver().registerContentObserver(calendarUri, true, calendarObserver);
    }

    public Alarme getNextAlarm() {
        List<Alarme> alarmeList = AlarmUtils.loadAllAlarms(this);

        Alarme nextAlarm = null;
        Calendar nextAlarmCal = null;
        for (Alarme alarme : alarmeList) {
            if (alarme.isActive()) {
                Calendar nextOccurence = AlarmUtils.getNextOccurence(this, alarme);
                if (nextOccurence != null) {
                    if (nextAlarmCal == null || nextAlarmCal.after(nextOccurence)) {
                        nextAlarm = alarme;
                        nextAlarmCal = nextOccurence;
                    }
                }
            }
        }

        return nextAlarm;
    }


    public void updateAlarm() {
        Alarme nextAlarm = getNextAlarm();

        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmIntent != null) {
            mgr.cancel(alarmNotifIntent);
            alarmNotifIntent = null;
            mgr.cancel(alarmIntent);
            alarmIntent = null;
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancel(NOTIF_ALARME_SOON_ID);
        }

        Calendar nextAlarmCal = nextAlarm == null ? null : AlarmUtils.getNextOccurence(this, nextAlarm);
        if (nextAlarm != null && nextAlarmCal != null) {
            Intent intent = new Intent(this, AlarmBroadcastReceiver.class);
            intent.putExtra(ALARM_ID, nextAlarm.getId());
            intent.setData(Uri.parse("alarm://" + nextAlarm.getId()));
            alarmIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Log.i("AlmApp", "Programmation de l'alarme " + nextAlarm.getId() + " pour " + nextAlarmCal.getTime() + " @ " + System.currentTimeMillis());

            intent = new Intent(this, ListeAlarmesActivity.class);
            intent.putExtra(ALARM_ID, nextAlarm.getId());
            intent.setData(Uri.parse("alarm://" + nextAlarm.getId()));
            PendingIntent appIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(nextAlarmCal.getTimeInMillis(), appIntent);
            mgr.setAlarmClock(info, alarmIntent);

            intent = new Intent(this, AlarmNotifBroadcastReceiver.class);
            intent.putExtra(ALARM_ID, nextAlarm.getId());
            intent.setData(Uri.parse("alarm://" + nextAlarm.getId()));
            alarmNotifIntent = PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            long notifTime = nextAlarmCal.getTimeInMillis() - 30 * 60_000;
            if (notifTime < System.currentTimeMillis() + 10_000)
                showAlarmNotification();
            else
                mgr.setAndAllowWhileIdle(AlarmManager.RTC, notifTime, alarmNotifIntent);
        } else {
            Log.i("AlmApp", "Aucune alarme programmée");
        }
    }

    public void showAlarmNotification() {
        Alarme nextAlarm = getNextAlarm();

        // Intent pour l'action "Annuler"
        Intent cancelIntent = new Intent(this, CancelAlarmBroadcastReceiver.class);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Créer la notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL_ALARME_SOON).setSmallIcon(R.drawable.ic_reveil).setContentTitle(getString(R.string.notif_reveil_imminent)) // Titre
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Notification haute priorité
                .setAutoCancel(true) // Se ferme au clic
                .addAction(R.drawable.ic_cancel, getString(R.string.notif_annuler), cancelPendingIntent); // Bouton d'annulation

        String time = String.format(Locale.getDefault(), "%02d:%02d", nextAlarm.getHeure(), nextAlarm.getMinute());
        if (nextAlarm.getTitre() == null || nextAlarm.getTitre().isBlank())
            builder.setContentText(time);
        else
            builder.setContentText(nextAlarm.getTitre() + " (" + time + ")");

        // Afficher la notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIF_ALARME_SOON_ID, builder.build());
        }
    }

    public long getNoAlarmUntil() {
        if (noAlarmUntil == null) {
            SharedPreferences prefs = getSharedPreferences(PreferenceManager.getDefaultSharedPreferencesName(this), MODE_PRIVATE);
            noAlarmUntil = prefs.getLong("NoAlarmUntil", System.currentTimeMillis() - 86_400_000);
        }
        return noAlarmUntil;
    }

    public void setNoAlarmUntil(long ts) {
        noAlarmUntil = ts;
        SharedPreferences prefs = getSharedPreferences(PreferenceManager.getDefaultSharedPreferencesName(this), MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("NoAlarmUntil", noAlarmUntil);
        editor.apply();
    }

    public void cancelNextAlarm() {
        Alarme nextAlarm = getNextAlarm();
        Calendar nextAlarmCal = nextAlarm == null ? null : AlarmUtils.getNextOccurence(this, nextAlarm);
        if (nextAlarm == null || nextAlarmCal == null)
            return;
        setNoAlarmUntil(nextAlarmCal.getTimeInMillis() + 59_999);

        updateAlarm();

        boolean repeat = false;
        for (Jour j : Jour.values()) {
            if (nextAlarm.isJourActif(j)) {
                Log.i("AlmApp", "repeat on " + j);
                repeat = true;
                break;
            }
        }
        if (!repeat) {
            Log.i("AlmApp", "no-repeat => disable");
            nextAlarm.setActive(false);
            AlarmUtils.storeAlarm(this, nextAlarm);
        }
    }
}
