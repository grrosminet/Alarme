package tf.charles.droid.alarme.alarms;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.VIBRATOR_SERVICE;

import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.data.Alarme;

public class AlarmInstance {
    private final Alarme alarme;
    private long alarmStartTS = 0;
    private volatile boolean alarmPlaying = false;
    private volatile MediaPlayer player = null;
    private volatile Vibrator vibrator = null;
    private volatile Handler handler;

    private int moveStopButton;
    private int mathematics;
    private boolean vibrate;
    private long delaiVibration;
    private long dureeAube;
    private String alarmUri;
    private long delaiSonnerie;
    private long dureeVolProg;

    public AlarmInstance(Alarme alarmData) {
        this.alarme = alarmData;

        SharedPreferences prefs = AlarmApp.getInstance().getSharedPreferences(PreferenceManager.getDefaultSharedPreferencesName(AlarmApp.getInstance().getApplicationContext()),
                MODE_PRIVATE);

        vibrate = prefs.getBoolean("alarm_vibration", false);
        delaiVibration = 0;
        if (vibrate)
            try {
                String delai = prefs.getString("alarm_vibration_delai", "60");
                delaiVibration = Long.parseLong(delai);
            } catch (Throwable t) {
                Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
                delaiVibration = 60;
            }

        dureeAube = 0;
        try {
            if (prefs.getBoolean("alarm_sunrise", true))
                dureeAube = Long.parseLong(prefs.getString("alarm_sunrise_duree", "300"));
        } catch (Throwable t) {
            Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
        }

        alarmUri = prefs.getString("alarm_tone", "");
        delaiSonnerie = 0;
        dureeVolProg = 0;
        if (alarmUri.isBlank() == false) {
            try {
                delaiSonnerie = Long.parseLong(prefs.getString("alarm_tone_delai", "0"));
            } catch (Throwable t) {
                Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
            }

            if (prefs.getBoolean("alarm_volume_progressif", true)) {
                try {
                    dureeVolProg = Long.parseLong(prefs.getString("alarm_volume_progressif_delai", "300"));
                } catch (Throwable t) {
                    Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
                    dureeVolProg = 300;
                }
            }
        }

        String movingButton = prefs.getString("moving_button", "0");
        try {
            moveStopButton = Integer.parseInt(movingButton);
        } catch (NumberFormatException e) {
            moveStopButton = 0;
        }

        String maths = prefs.getString("mathematics", "0");
        try {
            mathematics = Integer.parseInt(maths);
        } catch (NumberFormatException e) {
            mathematics = 0;
        }
    }

    public void start() {
        alarmStartTS = System.currentTimeMillis();
        alarmPlaying = true;

        handler = new Handler(AlarmApp.getInstance().getMainLooper());

        startVibration();
        startSonnerie();
    }

    public void stop() {
        synchronized (this) {
            alarmPlaying = false;
            if(handler != null)
                handler.removeMessages(0);
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
            if (player != null) {
                player.stop();
                player = null;
            }
            handler = null;
        }
    }

    private void startVibration() {
        if(vibrate == false)
            return;

        if (delaiVibration > 0) {
            handler.postDelayed(() -> {
                try {
                    if (alarmPlaying == false)
                        return;

                    doVibration();
                } catch (Throwable t) {
                    Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
                }
            }, (delaiVibration) * 1000);
        } else
            doVibration();
    }

    private void doVibration() {
        synchronized (this) {
            if (alarmPlaying) {
                if (vibrator == null)
                    vibrator = (Vibrator) AlarmApp.getInstance().getSystemService(VIBRATOR_SERVICE);

                vibrator.vibrate(new long[]{0, 200, 1800, 500, 1500, 1000, 1000, 1500, 500, 1500, 500, 1500, 500}, 7);
            }
        }
    }


    private void startSonnerie() {
        if(alarmUri == null || alarmUri.isBlank())
            return;

        if (delaiSonnerie > 0) {
            handler.postDelayed(() -> {
                try {
                    if (alarmPlaying == false)
                        return;

                    doSonnerie();
                } catch (Throwable t) {
                    Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
                }
            }, delaiSonnerie * 1000);
        } else
            doSonnerie();
    }

    private void doSonnerie() {
        Uri uri = Uri.parse(alarmUri);
        synchronized (this) {
            player = new MediaPlayer();
            if (dureeVolProg > 0)
                player.setVolume(0, 0);
            else
                player.setVolume(1, 1);
            try {
                player.setDataSource(AlarmApp.getInstance().getApplicationContext(), uri);
            } catch (IOException e) {
                Log.e(AlarmInstance.class.getSimpleName(), "Impossible de charger la sonnerie", e);
            }
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            try {
                player.prepare();
            } catch (IOException e) {
                Log.e(AlarmInstance.class.getSimpleName(), "Impossible de préparer la sonnerie", e);
            }
            player.start();
        }

        if (dureeVolProg > 0)
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (AlarmInstance.this) {
                            if (player == null || alarmPlaying == false)
                                return;

                            float progress = (System.currentTimeMillis() - alarmStartTS) / 1000;
                            progress /= dureeVolProg;

                            Log.i("Alarm", "volume progressif : " + progress);
                            player.setVolume(progress, progress);

                            if (progress < 1)
                                handler.postDelayed(this, 1000);
                        }
                    } catch (Throwable t) {
                        Log.e(AlarmInstance.class.getSimpleName(), t.getMessage(), t);
                    }
                }
            }, 1000);
    }

    public Alarme getAlarme() {
        return alarme;
    }

    public long getAlarmStartTS() {
        return alarmStartTS;
    }

    public boolean isAlarmPlaying() {
        return alarmPlaying;
    }

    public int getMoveStopButton() {
        return moveStopButton;
    }

    public int decrementMoveStopBtn() {
        return --moveStopButton;
    }

    public int getMathematics() {
        return mathematics;
    }

    public int decrementMathematics() {
        return --mathematics;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public long getDelaiVibration() {
        return delaiVibration;
    }

    public long getDureeAube() {
        return dureeAube;
    }

    public String getAlarmUri() {
        return alarmUri;
    }

    public long getDelaiSonnerie() {
        return delaiSonnerie;
    }

    public long getDureeVolProg() {
        return dureeVolProg;
    }

    public float getAubeProgress() {
        float progress = (System.currentTimeMillis() - alarmStartTS) / 1000;
        if(progress < 0)
            return 0;
        return progress / dureeAube;
    }
}
