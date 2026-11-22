package tf.charles.droid.alarme.misc;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import tf.charles.droid.alarme.data.Calendrier;

public class Tools {


    public static boolean isOnCalendarEvent(Context context, long timestamp, Calendrier cal) {
        // Récupérer le ContentResolver
        ContentResolver contentResolver = context.getContentResolver();

        // URI de base pour les événements du calendrier
        Uri eventsUri = CalendarContract.Events.CONTENT_URI;

        // Colonnes à récupérer
        String[] projection = new String[]{
                CalendarContract.Events._ID,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
        };

        // Filtres pour la requête
        String selection = CalendarContract.Events.ACCOUNT_TYPE + " = ? AND " +
                CalendarContract.Events.ACCOUNT_NAME + " = ? AND " +
                CalendarContract.Events.CALENDAR_DISPLAY_NAME + " = ? AND " +
                CalendarContract.Events.DTSTART + " <= ? AND " +
                CalendarContract.Events.DTEND + " >= ?";

        String[] selectionArgs = new String[]{
                cal.getAccountType(),
                cal.getAccountName(),
                cal.getCalendarDisplayName(),
                String.valueOf(timestamp),
                String.valueOf(timestamp)
        };

        // Exécuter la requête
        Cursor cursor = contentResolver.query(eventsUri, projection, selection, selectionArgs, null);

        // Vérifier si un événement est en cours
        if (cursor != null) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    // Un événement est en cours
                    return true;
                }
            }
        }

        // Aucun événement trouvé
        return false;
    }

}
