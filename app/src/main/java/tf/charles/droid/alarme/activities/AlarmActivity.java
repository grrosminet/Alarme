package tf.charles.droid.alarme.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.window.area.WindowAreaController;
import androidx.window.area.WindowAreaInfo;
import androidx.window.area.WindowAreaPresentationSessionCallback;
import androidx.window.area.WindowAreaSessionPresenter;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.java.area.WindowAreaControllerCallbackAdapter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.alarms.AlarmInstance;
import tf.charles.droid.alarme.alarms.AlarmService;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * @noinspection FieldMayBeFinal
 */
@ExperimentalWindowApi
public class AlarmActivity extends AppCompatActivity implements WindowAreaPresentationSessionCallback {
    public static Intent createLaunchIntent(Context context) {
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        return alarmIntent;
    }

    private List<View> m_rootLayout = new ArrayList<>();
    private List<TextView> m_stopButton = new ArrayList<>();

    private Handler m_handler = null;

    private WindowAreaControllerCallbackAdapter windowAreaController = null;
    private Executor displayExecutor = null;
    private WindowAreaSessionPresenter rearScreenWindowAreaSession = null;
    private WindowAreaInfo rearScreenWindowAreaInfo = null;

    private AlarmInstance instance = null;

    private void startSimulationAube() {
        applySimulationAube();
        if(instance.isAlarmPlaying())
            m_handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(instance.isAlarmPlaying() == false)
                        return;

                    applySimulationAube();
                    if(instance.isAlarmPlaying() && instance.getAubeProgress() < 1)
                        m_handler.postDelayed(this, 500);
                }
            }, 500);
        else
            applySimulationAube();
    }

    private void applySimulationAube() {
        final int[] couleurDebut = {254, 64, 0};
        final int[] couleurFin = {254, 254, 160};
        float fSol = instance.getAubeProgress();
        try {
            final WindowManager.LayoutParams lp = getWindow().getAttributes();
            if(fSol < 0.5) {
                lp.screenBrightness = 0.01f;
            }
            else {
                float fLumi = Math.min(0.495f, fSol - 0.5f);
                lp.screenBrightness = Math.min(0.01f + fLumi * 2, 1);
            }
            getWindow().setAttributes(lp);
        } catch (Throwable t) {
            Log.e(AlarmActivity.class.getSimpleName(), t.getMessage(), t);
        }

        int[] couleur;
        if(fSol < 0.5) {
            couleur = new int[3];
            for(int i = 0 ; i < couleur.length ; i++)
                couleur[i] = (int) (((couleurFin[i] - couleurDebut[i]) * fSol * 2 + couleurDebut[i]) * fSol * 2);
        }
        else {
            couleur = couleurFin;
        }
        int bgColor = Color.rgb(couleur[0], couleur[1], couleur[2]);
        for(View v : m_rootLayout)
            v.setBackgroundColor(bgColor);

        int txtColor = (fSol < 0.2f) ? Color.WHITE : Color.BLACK;
        for(TextView tv : m_stopButton)
            tv.setTextColor(txtColor);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        this.m_rootLayout.add(this.findViewById(R.id.root_layout));
        this.m_stopButton.add(this.findViewById(R.id.stop_button));

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            this.setTurnScreenOn(true);
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        instance = AlarmService.getCurrentAlarm();

        // init de la vue multi-écrans
        displayExecutor = ContextCompat.getMainExecutor(this);
        windowAreaController = new WindowAreaControllerCallbackAdapter(WindowAreaController.getOrCreate());
        windowAreaController.addWindowAreaInfoListListener(displayExecutor,
                windowAreaInfos -> {
                    for(WindowAreaInfo newInfo : windowAreaInfos) {
                        if(newInfo.getType().equals(WindowAreaInfo.Type.TYPE_REAR_FACING)){
                            rearScreenWindowAreaInfo = newInfo;
                            break;
                        }
                    }
                });

        m_handler = new Handler(getMainLooper());
        m_handler.postDelayed(new Runnable() {
            int cnt = 1;
            @Override
            public void run() {
                if (rearScreenWindowAreaInfo == null) {
                    if(cnt > 5)
                        return;
                    m_handler.postDelayed(this, 500L * (++cnt));
                }
                else
                    displayOnRearScreen();
            }
        }, 500);

        if (instance.getDureeAube() > 0)
            startSimulationAube();

        if (instance.getMoveStopButton() > 0) {
            randomizeStopButtonLocation();
        }
        else {
            centerStopButton();
        }
    }

    @Override
    protected void onPause() {
        if(m_handler != null)
            m_handler.removeMessages(0);
        AlarmApp.getInstance().updateAlarm();
        super.onPause();
    }

    private void displayOnRearScreen() {
        if(rearScreenWindowAreaSession == null) {
            try {
                Binder token = rearScreenWindowAreaInfo.getToken();
                windowAreaController.presentContentOnWindowArea(token, this, displayExecutor, this);
            } catch (Exception e) {
                Log.e(AlarmActivity.class.getSimpleName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void onSessionStarted(@NonNull WindowAreaSessionPresenter session) {
        rearScreenWindowAreaSession = session;

        @SuppressLint("InflateParams") View rearView = getLayoutInflater().inflate(R.layout.activity_alarm, null);
        m_rootLayout.add(rearView.findViewById(R.id.root_layout));
        TextView btn = rearView.findViewById(R.id.stop_button);
        btn.setOnClickListener(v -> stopAlarm(btn));
        m_stopButton.add(btn);
        session.setContentView(rearView);
    }

    @Override
    public void onSessionEnded(@Nullable Throwable t) {
        Log.e("xx", "end of session");
        if (t != null) {
            Log.e("xx", "Something was broken: " + t.getMessage());
            Log.e(AlarmActivity.class.getSimpleName(), t.getMessage(), t);
        }
        rearScreenWindowAreaSession = null;
    }

    @Override
    public void onContainerVisibilityChanged(boolean isVisible) {
        Log.e("xx", "visibility changed");
    }

    public void randomizeStopButtonLocation() {
        instance.decrementMoveStopBtn();
        for(TextView stopButton : m_stopButton)
            randomizeStopButtonLocation(stopButton);
    }

    public void randomizeStopButtonLocation(View btn) {
        int padding = (btn.getPaddingTop() + btn.getPaddingLeft() + btn.getPaddingBottom() + btn.getPaddingRight()) / 4;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)btn.getLayoutParams();
        for(int r = 0 ; r < params.getRules().length ; r++)
            params.removeRule(r);

        int paddingLeft;
        int paddingRight;

        double rH = Math.random();
        if(rH < 0.333) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            paddingLeft = 0;
            paddingRight = padding*2;
        }
        else if(rH < 0.667) {
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            paddingLeft = padding;
            paddingRight = padding;
        }
        else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            paddingLeft = padding*2;
            paddingRight = 0;
        }

        int paddingTop;
        int paddingBottom;
        double rV = Math.random();
        if(rV < 0.333) {
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            paddingTop = 0;
            paddingBottom = padding*2;
        }
        else if(rV < 0.667) {
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            paddingTop = padding;
            paddingBottom = padding;
        }
        else {
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            paddingTop = padding*2;
            paddingBottom = 0;
        }

        btn.setLayoutParams(params);
        btn.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
    }

    public void centerStopButton() {
        for(TextView stopButton : m_stopButton)
            centerStopButton(stopButton);
    }

    public void centerStopButton(View btn) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)btn.getLayoutParams();
        for(int r = 0 ; r < params.getRules().length ; r++)
            params.removeRule(r);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        btn.setLayoutParams(params);
    }

    public void stopAlarm(View v) {
        if(instance.getMoveStopButton() > 0) {
            randomizeStopButtonLocation();
            return;
        }

        if(instance.getMathematics() > 0) {
            askMath();
            return;
        }

        try {
            Intent service = new Intent(this, AlarmService.class);
            service.putExtra(AlarmApp.ALARM_ID, instance.getAlarme().getId());
            service.setAction(AlarmService.STOP_ALARM_ACTION);
            startService(service);
        }
        catch (Throwable t) {
            Log.e("Alarm", "Impossible d'arreter l'alarme par le service", t);
            instance.stop();
        }

        if(rearScreenWindowAreaSession != null) {
            try {
                rearScreenWindowAreaSession.close();
                rearScreenWindowAreaSession = null;
            } catch (Exception e) {
                Log.e(AlarmActivity.class.getSimpleName(), e.getMessage(), e);
            }
        }

        if(instance.getAlarme().getId() != Long.MIN_VALUE)
            finishAffinity();
        else
            finish();
    }

    @SuppressLint("DefaultLocale")
    private void askMath() {
        // 1. Générer une opération aléatoire
        Random random = new Random();
        int operand1; // Nombre entre 0 et 9
        int operand2; // Nombre entre 0 et 9

        String operator;
        int correctAnswer;
        do {
            operand1 = random.nextInt(10); // Nombre entre 0 et 9
            operand2 = random.nextInt(10); // Nombre entre 0 et 9
            switch (random.nextInt(4)) { // +, -, *, /
                case 0:
                    operator = "+";
                    break;
                case 1:
                    operator = "-";
                    break;
                case 2:
                    operator = "×";
                    break;
                default:
                    operator = "÷";
                    if (operand2 == 0)
                        operand2++;
                    break;
            }

            // 2. Calculer la réponse correcte
            switch (operator) {
                case "+":
                    correctAnswer = operand1 + operand2;
                    break;
                case "-":
                    correctAnswer = operand1 - operand2;
                    break;
                case "×":
                    correctAnswer = operand1 * operand2;
                    break;
                case "÷":
                    correctAnswer = operand1 / operand2;
                    break;
                default:
                    correctAnswer = -1;
                    break;
            }
        }
        while (correctAnswer < 0);

        // 3. Créer la boîte de dialogue
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_math_input, null);

        EditText editTextAnswer = dialogView.findViewById(R.id.editTextAnswer);
        TextView textViewQuestion = dialogView.findViewById(R.id.textViewQuestion);

        textViewQuestion.setText(String.format("%d %s %d = ?", operand1, operator, operand2));

        int finalCorrectAnswer = correctAnswer;
        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog).setTitle(R.string.dlg_maths_title).setView(dialogView).setPositiveButton(R.string.dlg_maths_btn_ok,
                (dialog,
                                                                                                                                                  which) -> {
            // 4. Vérifier la réponse
            int userAnswer;
            try {
                userAnswer = Integer.parseInt(editTextAnswer.getText().toString());
            } catch (NumberFormatException e) {
                userAnswer = -1;
            }

            if (userAnswer == finalCorrectAnswer)
                instance.decrementMathematics();
            else
                Toast.makeText(this, R.string.dlg_maths_faux, Toast.LENGTH_LONG).show();
            stopAlarm(null);
        }).show();
    }
}
