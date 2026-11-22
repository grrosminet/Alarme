package tf.charles.droid.alarme.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.activities.preferences.SettingsActivity;
import tf.charles.droid.alarme.adapters.AlarmsListAdapter;
import tf.charles.droid.alarme.alarms.AlarmService;

public class ListeAlarmesActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_liste_alarmes);

        // Configurer la Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gérer le clic sur le bouton des paramètres
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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