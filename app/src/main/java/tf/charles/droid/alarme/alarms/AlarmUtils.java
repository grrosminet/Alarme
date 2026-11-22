package tf.charles.droid.alarme.alarms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import tf.charles.droid.alarme.AlarmApp;
import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.data.Alarme;
import tf.charles.droid.alarme.data.Calendrier;
import tf.charles.droid.alarme.data.Restriction;
import tf.charles.droid.alarme.misc.Jour;
import tf.charles.droid.alarme.misc.Tools;

public class AlarmUtils {
    public static String restrictionsToString(Context ctx, List<Restriction> restrictionList) {
        if(restrictionList.isEmpty())
            return ctx.getString(R.string.restr_aucune);

        StringBuilder sb = new StringBuilder();
        sb.append(ctx.getString(R.string.restr_sonner_uniquement));
        int cnt = 0;
        for(Restriction r : restrictionList) {
            if(r.isEnCours()) {
                if(cnt > 0)
                    sb.append(' ').append(ctx.getString(R.string.restr_et)).append(' ');
                else
                    sb.append(' ').append(ctx.getString(R.string.restr_evenement_prevu));
                sb.append(' ').append(r.getCalendrier().getCalendarDisplayName());
                cnt++;
            }
        }
        int cnt2 = 0;
        for(Restriction r : restrictionList) {
            if(r.isEnCours() == false) {
                if(cnt2 > 0)
                    sb.append(ctx.getString(R.string.restr_ni_dans));
                else {
                    if (cnt > 0)
                        sb.append(' ').append(ctx.getString(R.string.restr_et));
                    sb.append(' ').append(ctx.getString(R.string.restr_aucun_evenement_prevu));
                }
                sb.append(' ').append(r.getCalendrier().getCalendarDisplayName());
                cnt2++;
            }
        }
        return sb.toString();
    }

    public static Calendar getNextOccurence(Context context, Alarme alarme) {
        if(alarme.isActive() == false)
            return null;

        int nbJours = 0;
        for(Jour j : Jour.values()) {
            if(alarme.isJourActif(j))
                nbJours++;
        }


        Calendar occurence = Calendar.getInstance();
        occurence.set(Calendar.HOUR_OF_DAY, alarme.getHeure());
        occurence.set(Calendar.MINUTE, alarme.getMinute());
        occurence.set(Calendar.SECOND, 0);
        occurence.set(Calendar.MILLISECOND, 0);

        if(occurence.getTimeInMillis() < System.currentTimeMillis())
            occurence.add(Calendar.DAY_OF_MONTH, 1);
        while(occurence.getTimeInMillis() <= AlarmApp.getInstance().getNoAlarmUntil())
            occurence.add(Calendar.DAY_OF_MONTH, 1);

        int loops = 0;
        do {
            boolean valide = true;

            if(nbJours > 0 && nbJours < 7) {
                int dow = occurence.get(Calendar.DAY_OF_WEEK);
                Jour j = Jour.values()[ (dow + 5) % 7 ];
                valide = alarme.isJourActif(j);
            }

            if(valide && alarme.getRestrictions().size() > 0) {
                for(Restriction restriction : alarme.getRestrictions()) {
                    Calendrier cal = restriction.getCalendrier();
                    boolean isOnEvent = Tools.isOnCalendarEvent(context, occurence.getTimeInMillis(), cal);
                    valide = isOnEvent == restriction.isEnCours();
                    if(valide == false)
                        break;
                }
            }

            if(valide)
                return occurence;

            occurence.add(Calendar.DAY_OF_MONTH, 1);
        }
        while (++loops < 366);

        return null;
    }

    public static SharedPreferences getAlarmesPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences("Alarmes", Context.MODE_PRIVATE);
    }

    public static List<Alarme> loadAllAlarms(Context context) {
        SharedPreferences prefs = getAlarmesPreferences(context);
        SharedPreferences.Editor editor = null;

        List<Alarme> alarmes = new ArrayList<>();

        //noinspection unchecked,rawtypes
        Map<String, String> listeAlarmesJSON = (Map)prefs.getAll();
        ObjectMapper objectMapper = new ObjectMapper();
        for(Map.Entry<String,String> alarmeJson : listeAlarmesJSON.entrySet()) {
            try {
                Alarme alarme = objectMapper.readValue(alarmeJson.getValue(), Alarme.class);
                alarmes.add(alarme);
            } catch (JsonProcessingException e) {
                Log.e(Tools.class.getSimpleName(), e.getMessage(), e);
                if(editor == null)
                    editor = prefs.edit();
                editor.remove(alarmeJson.getKey());
            }
        }

        if (editor != null)
            editor.apply();

        return alarmes;
    }

    public static Alarme loadAlarm(Context context, long alarmId) {
        SharedPreferences prefs = getAlarmesPreferences(context);
        SharedPreferences.Editor editor = null;

        //noinspection unchecked,rawtypes
        String alarmJson = prefs.getString(String.valueOf(alarmId), null);
        Log.i("AlmUtl", "Loading alarm " + alarmId + " from json " + alarmJson);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(alarmJson, Alarme.class);
        } catch (JsonProcessingException e) {
            Log.e(Tools.class.getSimpleName(), e.getMessage(), e);
            if(editor == null)
                editor = prefs.edit();
            editor.remove(String.valueOf(alarmId));
        }

        if (editor != null)
            editor.apply();

        return null;
    }

    public static void storeAlarm(Context context, Alarme alarme) {
        String content;
        try {
            ObjectMapper mapper = new ObjectMapper();
            content = mapper.writeValueAsString(alarme);
        }
        catch (Throwable t) {
            t.printStackTrace();
            return;
        }

        SharedPreferences preferences = getAlarmesPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(String.valueOf(alarme.getId()), content);
        editor.apply();

        AlarmApp.getInstance().updateAlarm();
    }
}
