package tf.charles.droid.alarme.activities;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tf.charles.droid.alarme.adapters.CalendrierTreeAdapter;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.adapters.RestrictionsAdapter;
import tf.charles.droid.alarme.alarms.AlarmService;
import tf.charles.droid.alarme.alarms.AlarmUtils;
import tf.charles.droid.alarme.data.Alarme;
import tf.charles.droid.alarme.data.Calendrier;
import tf.charles.droid.alarme.data.CursorEx;
import tf.charles.droid.alarme.data.Restriction;

public class ParametresAlarmeActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText textTitre;
    private TextView textHeure;
    private TextView textLundi, textMardi, textMercredi, textJeudi, textVendredi, textSamedi, textDimanche;
    private RecyclerView recyclerViewExclusions;
    private Button buttonAjouterExclusion;
    private RestrictionsAdapter exclusionsAdapter;
    private RecyclerView recyclerViewInclusions;
    private Button buttonAjouterInclusion;
    private RestrictionsAdapter inclusionsAdapter;
    private int textStdColor = Color.BLACK;
    private Boolean calendarAllowed = null;

    private Alarme currentAlarm = new Alarme();
    private List<Restriction> include = new ArrayList<>();
    private List<Restriction> exclude = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametres_alarme);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialisation des vues
        textTitre = findViewById(R.id.textTitre);
        textHeure = findViewById(R.id.textHeure);
        textLundi = findViewById(R.id.textLundi);
        textMardi = findViewById(R.id.textMardi);
        textMercredi = findViewById(R.id.textMercredi);
        textJeudi = findViewById(R.id.textJeudi);
        textVendredi = findViewById(R.id.textVendredi);
        textSamedi = findViewById(R.id.textSamedi);
        textDimanche = findViewById(R.id.textDimanche);
        if (Calendar.getInstance().getFirstDayOfWeek() == Calendar.SUNDAY) {
            LinearLayout daysLayout = (LinearLayout) textDimanche.getParent();
            daysLayout.removeView(textDimanche);
            daysLayout.addView(textDimanche, 0);
        }

        textStdColor = textLundi.getCurrentTextColor();

        recyclerViewExclusions = findViewById(R.id.recyclerViewExclusions);
        buttonAjouterExclusion = findViewById(R.id.buttonAjouterExclusion);

        recyclerViewInclusions = findViewById(R.id.recyclerViewInclusions);
        buttonAjouterInclusion = findViewById(R.id.buttonAjouterInclusion);

        // Configurer le RecyclerView pour les restrictions
        exclusionsAdapter = new RestrictionsAdapter();
        recyclerViewExclusions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewExclusions.setAdapter(exclusionsAdapter);

        inclusionsAdapter = new RestrictionsAdapter();
        recyclerViewInclusions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewInclusions.setAdapter(inclusionsAdapter);

        // Gestion du clic sur l'heure
        textHeure.setOnClickListener(v -> showTimePickerDialog());

        // Gestion des restrictions (calendriers)
        buttonAjouterExclusion.setOnClickListener(v -> btnAjouterExclusionClicked());
        buttonAjouterInclusion.setOnClickListener(v -> btnAjouterInclusionClicked());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parametres_alarme_toolbar, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(AlarmService.getCurrentAlarm() != null && AlarmService.getCurrentAlarm().isAlarmPlaying()) {
            startActivity(AlarmActivity.createLaunchIntent(this));
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_CALENDAR}, 100);
        }

        if (calendarAllowed == null) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CALENDAR}, 100);
            } else
                calendarAllowed = true;
        }

        long idToLoad = getIntent().getLongExtra("Id", Long.MIN_VALUE);
        if (idToLoad > 0) {
            SharedPreferences prefs = AlarmUtils.getAlarmesPreferences(this);

            //noinspection unchecked,rawtypes
            String alarmeJson = prefs.getString(String.valueOf(idToLoad), null);
            if (alarmeJson != null)
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Alarme alarme = objectMapper.readValue(alarmeJson, Alarme.class);
                    this.currentAlarm = alarme;
                } catch (JsonProcessingException e) {
                    //noinspection CallToPrintStackTrace
                    e.printStackTrace();
                    Toast.makeText(this, R.string.erreur_chargement_alarme, Toast.LENGTH_LONG).show();
                }
        }

        textTitre.setText(currentAlarm.getTitre());

        // Gestion des jours de répétition
        setupJoursRepetition();

        textHeure.setText(String.format(Locale.getDefault(), "%02d:%02d", currentAlarm.getHeure(), currentAlarm.getMinute()));

        include.clear();
        exclude.clear();
        for (Restriction r : currentAlarm.getRestrictions()) {
            if (r.isEnCours())
                include.add(r);
            else
                exclude.add(r);
        }

        this.exclusionsAdapter.setRestrictionsList(exclude);
        this.inclusionsAdapter.setRestrictionsList(include);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_CALENDAR.equals(permissions[i])) {
                    if (grantResults.length > 0 && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        calendarAllowed = true;
                    } else {
                        calendarAllowed = false;
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Gérer le clic sur le bouton des paramètres
        if (item.getItemId() == R.id.action_valider) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void save() {
        currentAlarm.setTitre(this.textTitre.getText().toString());
        this.currentAlarm.getRestrictions().clear();
        this.currentAlarm.getRestrictions().addAll(include);
        this.currentAlarm.getRestrictions().addAll(exclude);
        AlarmUtils.storeAlarm(this, this.currentAlarm);
        finish();
    }

    private void showTimePickerDialog() {
        int currentHour = currentAlarm.getHeure();
        int currentMinute = currentAlarm.getMinute();

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            String heureAlarme = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            textHeure.setText(heureAlarme);
            currentAlarm.setHeure(selectedHour);
            currentAlarm.setMinute(selectedMinute);
        }, currentHour, currentMinute, true);
        timePickerDialog.show();
    }

    private void setupJoursRepetition() {
        View.OnClickListener jourClickListener = v -> {
            TextView textView = (TextView) v;
            boolean isSelected = textView.getCurrentTextColor() == textStdColor;
            textView.setTextColor(isSelected ? getResources().getColor(android.R.color.darker_gray, getTheme()) : textStdColor);
            isSelected = textView.getCurrentTextColor() == textStdColor;
            switch (textView.getId()) {
                case R.id.textLundi:
                    currentAlarm.setLundi(isSelected);
                    break;
                case R.id.textMardi:
                    currentAlarm.setMardi(isSelected);
                    break;
                case R.id.textMercredi:
                    currentAlarm.setMercredi(isSelected);
                    break;
                case R.id.textJeudi:
                    currentAlarm.setJeudi(isSelected);
                    break;
                case R.id.textVendredi:
                    currentAlarm.setVendredi(isSelected);
                    break;
                case R.id.textSamedi:
                    currentAlarm.setSamedi(isSelected);
                    break;
                case R.id.textDimanche:
                    currentAlarm.setDimanche(isSelected);
                    break;
            }
        };

        setupJour(textLundi, jourClickListener, currentAlarm.isLundi());
        setupJour(textMardi, jourClickListener, currentAlarm.isMardi());
        setupJour(textMercredi, jourClickListener, currentAlarm.isMercredi());
        setupJour(textJeudi, jourClickListener, currentAlarm.isJeudi());
        setupJour(textVendredi, jourClickListener, currentAlarm.isVendredi());
        setupJour(textSamedi, jourClickListener, currentAlarm.isSamedi());
        setupJour(textDimanche, jourClickListener, currentAlarm.isDimanche());
    }

    private void setupJour(TextView view, View.OnClickListener listener, boolean isSelected) {
        view.setOnClickListener(listener);
        view.setTextColor(isSelected ? textStdColor : getResources().getColor(android.R.color.darker_gray, getTheme()));
    }

    private void btnAjouterInclusionClicked() {
        btnAjouterRestriction(true);
    }

    private void btnAjouterExclusionClicked() {
        btnAjouterRestriction(false);
    }

    private void btnAjouterRestriction(boolean inclusion) {
        Map<String, List<Calendrier>> calendriersParCompte = getCalendriersParCompte();

        if (calendriersParCompte.isEmpty()) {
            if (calendarAllowed) {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.dlg_cal_aucun_title).setMessage(R.string.dlg_cal_aucun_msg).setPositiveButton(R.string.dlg_cal_aucun_ok, null).show();
            } else {
                new MaterialAlertDialogBuilder(this).setTitle(R.string.dlg_cal_refus_title).setMessage(R.string.dlg_cal_refus_msg).setPositiveButton(R.string.dlg_cal_refus_ok, null).show();
            }
            return;
        }

        // Créer un RecyclerView pour l'arborescence
        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this).setTitle(R.string.dlg_cal_select_title).setView(recyclerView).setNegativeButton(R.string.dlg_cal_select_annuler, null).create();

        CalendrierTreeAdapter adapter = new CalendrierTreeAdapter(calendriersParCompte, (calendrier) -> {
            // Gérer le clic sur un calendrier
            (inclusion ? include : exclude).add(new Restriction(calendrier, inclusion));
            (inclusion ? inclusionsAdapter : exclusionsAdapter).notifyDataSetChanged();
            dialog.dismiss();
        });
        recyclerView.setAdapter(adapter);

        // Afficher le dialogue
        dialog.show();
    }

    private Map<String, List<Calendrier>> getCalendriersParCompte() {
        Map<String, List<Calendrier>> calendriersParCompte = new HashMap<>();
        List<Calendrier> calendriers = getCalendriers();

        for (Calendrier calendrier : calendriers) {
            String compte = calendrier.getAccountType() + "[" + calendrier.getAccountName() + "]";
            if (!calendriersParCompte.containsKey(compte)) {
                calendriersParCompte.put(compte, new ArrayList<>());
            }
            calendriersParCompte.get(compte).add(calendrier);
        }

        return calendriersParCompte;
    }

    private List<Calendrier> getCalendriers() {
        List<Calendrier> calendriers = new ArrayList<>();
        String[] projection = new String[]{CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars._ID,
                CalendarContract.Calendars.NAME, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.CALENDAR_COLOR // Ajout de la
                // couleur
        };

        // Filtre pour ne sélectionner que les calendriers actifs
        String selection = CalendarContract.Calendars.VISIBLE + " = ?";
        String[] selectionArgs = new String[]{"1"}; // 1 = actif

        // Clause ORDER BY pour trier par compte puis par nom
        String sortOrder =
                CalendarContract.Calendars.ACCOUNT_TYPE + " ASC, " + CalendarContract.Calendars.ACCOUNT_NAME + " ASC, " + CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC";

        // URI pour interroger les calendriers
        Uri uri = CalendarContract.Calendars.CONTENT_URI;

        // Exécuter la requête
        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                CursorEx cursorEx = new CursorEx(cursor);
                while (cursor.moveToNext()) {
                    int id = cursorEx.getInt(CalendarContract.Calendars._ID);
                    String name = cursorEx.getString(CalendarContract.Calendars.NAME);
                    String displayName = cursorEx.getString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
                    String accountType = cursorEx.getString(CalendarContract.Calendars.ACCOUNT_TYPE);
                    String accountName = cursorEx.getString(CalendarContract.Calendars.ACCOUNT_NAME);
                    int color = cursorEx.getInt(CalendarContract.Calendars.CALENDAR_COLOR);
                    calendriers.add(new Calendrier(id, accountType, accountName, name, displayName, color));
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            // Gérer l'erreur de permission
        }

        return calendriers;
    }
}