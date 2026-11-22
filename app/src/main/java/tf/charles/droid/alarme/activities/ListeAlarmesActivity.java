package tf.charles.droid.alarme.activities;

import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.activities.preferences.SettingsActivity;
import tf.charles.droid.alarme.adapters.AlarmsListAdapter;
import tf.charles.droid.alarme.alarms.AlarmService;

public class ListeAlarmesActivity extends AppCompatActivity {

    private static final String STATE_BATTERY_WARNING_SHOWN = "battery_warning_shown";
    private static final String PLAY_STORE_NOTICE_PREFERENCES = "play_store_notice";
    private static final String PREF_PLAY_STORE_NOTICE_COLLAPSED = "collapsed";
    private boolean batteryWarningShown;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private AlarmsListAdapter alarmsListAdapter;
    private FloatingActionButton fabAjouterAlarme;
    private FloatingActionButton fabVacances;

    private BottomSheetDialog periodeVacancesDlg;
    private Calendar noAlarmUntil = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        batteryWarningShown = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_BATTERY_WARNING_SHOWN);
        setContentView(R.layout.activity_liste_alarmes);

        // Configurer la Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View playStoreNotice = findViewById(R.id.play_store_notice);
        playStoreNotice.setVisibility(isInstalledFromPlayStore() ? View.VISIBLE : View.GONE);
        findViewById(R.id.play_store_notice_github).setOnClickListener(v -> openProjectPage());
        updatePlayStoreNotice(getSharedPreferences(PLAY_STORE_NOTICE_PREFERENCES, MODE_PRIVATE)
                .getBoolean(PREF_PLAY_STORE_NOTICE_COLLAPSED, false));
        findViewById(R.id.play_store_notice_toggle).setOnClickListener(v -> {
            boolean collapsed = findViewById(R.id.play_store_notice_content).getVisibility() == View.VISIBLE;
            updatePlayStoreNotice(collapsed);
            getSharedPreferences(PLAY_STORE_NOTICE_PREFERENCES, MODE_PRIVATE).edit()
                    .putBoolean(PREF_PLAY_STORE_NOTICE_COLLAPSED, collapsed).apply();
        });

        // Configurer le RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Configurer l'adapteur
        alarmsListAdapter = new AlarmsListAdapter(this);
        recyclerView.setAdapter(alarmsListAdapter);

        fabAjouterAlarme = findViewById(R.id.fab_ajouter_alarme);
        fabAjouterAlarme.setOnClickListener(v -> {
            // Ouvrir une activité ou un dialogue pour ajouter une alarme
            Intent intent = new Intent(this, ParametresAlarmeActivity.class);
            startActivity(intent);
        });

        fabVacances = findViewById(R.id.fab_vacances);
        fabVacances.setOnClickListener(v -> {
            showPeriodeVacancesDlg();
        });

        AlarmApp.getInstance().updateAlarm();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.liste_alarmes_toolbar, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(AlarmService.getCurrentAlarm() != null && AlarmService.getCurrentAlarm().isAlarmPlaying()) {
            startActivity(AlarmActivity.createLaunchIntent(this));
            return;
        }
        this.alarmsListAdapter.loadAlarms();
        this.updateFabVacances();
        if (!needsUnrestrictedBattery()) {
            batteryWarningShown = false;
        } else if (!batteryWarningShown) {
            showBatteryOptimizationDialog();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_BATTERY_WARNING_SHOWN, batteryWarningShown);
        super.onSaveInstanceState(outState);
    }

    private boolean isBackgroundRestricted() {
        ActivityManager activityManager = getSystemService(ActivityManager.class);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && activityManager != null && activityManager.isBackgroundRestricted();
    }

    private boolean needsUnrestrictedBattery() {
        PowerManager powerManager = getSystemService(PowerManager.class);
        return isBackgroundRestricted() || (powerManager != null
                && !powerManager.isIgnoringBatteryOptimizations(getPackageName()));
    }

    private void showBatteryOptimizationDialog() {
        batteryWarningShown = true;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.battery_optimization_title)
                .setMessage(R.string.battery_optimization_message)
                .setPositiveButton(R.string.battery_optimization_settings,
                        (dialog, which) -> requestUnrestrictedBattery())
                .setNegativeButton(R.string.battery_optimization_later, null)
                .show();
    }

    private void requestUnrestrictedBattery() {
        if (!needsUnrestrictedBattery()) {
            Toast.makeText(this, R.string.battery_optimization_disabled, Toast.LENGTH_LONG).show();
            return;
        }
        // Background restriction must be removed in the app's system settings.
        if (isBackgroundRestricted()) {
            if (openBatterySettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())))) {
                return;
            }
        } else if (openBatterySettings(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName())))) {
            return;
        }
        if (openBatterySettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) {
            return;
        }
        if (!openBatterySettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())))) {
            Toast.makeText(this, R.string.battery_settings_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private boolean openBatterySettings(Intent intent) {
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gérer le clic sur le bouton des paramètres
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == R.id.action_battery) {
            if (needsUnrestrictedBattery()) {
                showBatteryOptimizationDialog();
            } else {
                Toast.makeText(this, R.string.battery_optimization_disabled, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        if (item.getItemId() == R.id.action_github) {
            openProjectPage();
            return true;
        }
        if (item.getItemId() == R.id.action_license) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.license_title)
                    .setMessage(R.string.project_license_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePlayStoreNotice(boolean collapsed) {
        findViewById(R.id.play_store_notice_content).setVisibility(collapsed ? View.GONE : View.VISIBLE);
        View toggle = findViewById(R.id.play_store_notice_toggle);
        toggle.setRotation(collapsed ? 0f : 180f);
        toggle.setContentDescription(getString(collapsed
                ? R.string.play_store_notice_expand : R.string.play_store_notice_collapse));
    }

    private boolean isInstalledFromPlayStore() {
        try {
            String installerPackageName;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                installerPackageName = getPackageManager()
                        .getInstallSourceInfo(getPackageName()).getInstallingPackageName();
            } else {
                installerPackageName = getPackageManager().getInstallerPackageName(getPackageName());
            }
            return "com.android.vending".equals(installerPackageName);
        } catch (PackageManager.NameNotFoundException | IllegalArgumentException | SecurityException e) {
            return false;
        }
    }

    private void openProjectPage() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/grrosminet/Alarme"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser_available, Toast.LENGTH_LONG).show();
        }
    }

    private void showPeriodeVacancesDlg() {
        periodeVacancesDlg = new BottomSheetDialog(this, R.style.BottomSheetDialog);
        View view = getLayoutInflater().inflate(R.layout.periode_vacances_picker, null);
        periodeVacancesDlg.setContentView(view);

        TextView tvDate = view.findViewById(R.id.tv_date);
        TextView tvTime = view.findViewById(R.id.tv_time);
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        TextView btnOk = view.findViewById(R.id.btn_ok);

        // Gestion du clic sur la date
        tvDate.setOnClickListener(v -> showDatePicker());

        // Gestion du clic sur l'heure
        tvTime.setOnClickListener(v -> showTimePicker());

        // Bouton Effacer
        btnCancel.setOnClickListener(v -> {
            noAlarmUntil = Calendar.getInstance();
            noAlarmUntil.set(Calendar.HOUR_OF_DAY, 0);
            noAlarmUntil.set(Calendar.MINUTE, 0);
            noAlarmUntil.set(Calendar.SECOND, 0);
            noAlarmUntil.set(Calendar.MILLISECOND, 0);
            updateDateTimeText(tvDate, tvTime);
        });

        // Bouton OK
        btnOk.setOnClickListener(v -> {
            // Sauvegardez la date sélectionnée ici
            AlarmApp.getInstance().setNoAlarmUntil(noAlarmUntil.getTimeInMillis());
            periodeVacancesDlg.dismiss();
        });

        periodeVacancesDlg.setOnDismissListener(dialog -> {
            AlarmApp.getInstance().updateAlarm();
            alarmsListAdapter.notifyDataSetChanged();
            updateFabVacances();
        });

        updateDateTimeText(tvDate, tvTime);
        periodeVacancesDlg.show();
    }

    private void updateFabVacances() {
        noAlarmUntil.setTimeInMillis(AlarmApp.getInstance().getNoAlarmUntil());
        if(noAlarmUntil.getTimeInMillis() < System.currentTimeMillis()) {
            fabVacances.setBackgroundTintList(ColorStateList.valueOf(MaterialColors.getColor(fabVacances, androidx.appcompat.R.attr.colorPrimary)));
        }
        else {
            fabVacances.setBackgroundTintList(ColorStateList.valueOf(
                    Color.rgb(0,127,0)));
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    noAlarmUntil.set(year, month, dayOfMonth);
                    updateDateTimeText();
                },
                noAlarmUntil.get(Calendar.YEAR),
                noAlarmUntil.get(Calendar.MONTH),
                noAlarmUntil.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    noAlarmUntil.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    noAlarmUntil.set(Calendar.MINUTE, minute);
                    updateDateTimeText();
                },
                noAlarmUntil.get(Calendar.HOUR_OF_DAY),
                noAlarmUntil.get(Calendar.MINUTE),
                true
        );
        timePicker.show();
    }

    private void updateDateTimeText() {
        View view = periodeVacancesDlg.findViewById(R.id.bottom_sheet);
        if (view != null) {
            TextView tvDate = view.findViewById(R.id.tv_date);
            TextView tvTime = view.findViewById(R.id.tv_time);
            updateDateTimeText(tvDate, tvTime);
        }
    }

    private void updateDateTimeText(TextView tvDate, TextView tvTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        tvDate.setText(dateFormat.format(noAlarmUntil.getTime()));
        tvTime.setText(timeFormat.format(noAlarmUntil.getTime()));

        if(noAlarmUntil.getTimeInMillis() < System.currentTimeMillis()) {
            tvDate.setTextColor(Color.GRAY);
            tvTime.setTextColor(Color.GRAY);
        }
        else {
            int color = Color.rgb(0,127, 0);
            tvDate.setTextColor(color);
            tvTime.setTextColor(color);
        }
    }
}