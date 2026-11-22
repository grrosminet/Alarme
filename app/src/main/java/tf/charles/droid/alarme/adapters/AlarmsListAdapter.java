package tf.charles.droid.alarme.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.activities.ParametresAlarmeActivity;
import tf.charles.droid.alarme.alarms.AlarmUtils;
import tf.charles.droid.alarme.data.Alarme;
import tf.charles.droid.alarme.misc.Jour;

public class AlarmsListAdapter extends RecyclerView.Adapter<AlarmsListAdapter.AlarmeViewHolder> {

    private final Context context;
    private List<Alarme> alarmesList = new ArrayList<>();

    public AlarmsListAdapter(Context context) {
        this.context = context;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadAlarms() {
        this.alarmesList = AlarmUtils.loadAllAlarms(this.context);
        notifyDataSetChanged();
    }

    public void removeAlarme(long id) {
        SharedPreferences prefs = AlarmUtils.getAlarmesPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(String.valueOf(id));
        editor.apply();
    }

    public void storeAlarme(Alarme alarme) {
        AlarmUtils.storeAlarm(context, alarme);
    }

    @NonNull
    @Override
    public AlarmeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarme, parent, false);
        return new AlarmeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmeViewHolder holder, int position) {
        Alarme alarme = alarmesList.get(position);
        holder.bind(alarme, position);
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_contextuel_alarme, popupMenu.getMenu());

        // Gestion du clic sur une option du menu
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_supprimer) {
                // Supprimer l'élément de la liste
                Alarme alarme = alarmesList.remove(position);
                removeAlarme(alarme.getId());
                notifyItemRemoved(position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        return alarmesList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setAlarmesList(List<Alarme> alarmesList) {
        this.alarmesList = alarmesList;
        this.notifyDataSetChanged();
    }

    public class AlarmeViewHolder extends RecyclerView.ViewHolder {
        TextView titre;
        TextView nextOccurence;
        TextView heure;
        TextView [] jours = new TextView[7];
        SwitchMaterial boutonActivation;
        TextView restrictions;

        public AlarmeViewHolder(@NonNull View itemView) {
            super(itemView);
            titre = itemView.findViewById(R.id.titre);
            nextOccurence = itemView.findViewById(R.id.nextOccurence);
            heure = itemView.findViewById(R.id.heure);
            jours[0] = itemView.findViewById(R.id.jourLundi);
            jours[1] = itemView.findViewById(R.id.jourMardi);
            jours[2] = itemView.findViewById(R.id.jourMercredi);
            jours[3] = itemView.findViewById(R.id.jourJeudi);
            jours[4] = itemView.findViewById(R.id.jourVendredi);
            jours[5] = itemView.findViewById(R.id.jourSamedi);
            jours[6] = itemView.findViewById(R.id.jourDimanche);
            if(Calendar.getInstance().getFirstDayOfWeek() == Calendar.SUNDAY) {
                LinearLayout daysLayout = (LinearLayout) jours[6].getParent();
                daysLayout.removeView(jours[6]);
                daysLayout.addView(jours[6], daysLayout.indexOfChild(jours[0]));
            }

            boutonActivation = itemView.findViewById(R.id.bouton_activation);
            restrictions = itemView.findViewById(R.id.restrictions);
        }

        private void bindNextOccurence(Alarme alarme) {
            if(alarme.isActive()) {
                Calendar nextOcc = AlarmUtils.getNextOccurence(context, alarme);
                ((View) nextOccurence.getParent()).setVisibility(View.VISIBLE);
                if (nextOcc == null) {
                    nextOccurence.setText(R.string.item_alarme_occurence);
                    nextOccurence.setTextColor(Color.GRAY);
                }
                else {
                    try (TypedArray attrData = nextOccurence.getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorOnSurface})) {
                        nextOccurence.setTextColor(attrData.getColor(0, 0));
                    }
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
                    nextOccurence.setText(dateFormat.format(nextOcc.getTime()));
                }
            }
            else
                ((View) nextOccurence.getParent()).setVisibility(View.GONE);
        }

        public void bind(Alarme alarme, int position) {
            titre.setText(alarme.getTitre() != null ? alarme.getTitre() : "");
            titre.setVisibility(alarme.getTitre() != null && (alarme.getTitre().isBlank() == false) ? View.VISIBLE : View.GONE);

            bindNextOccurence(alarme);

            heure.setText(String.format(Locale.getDefault(), "%02d:%02d", alarme.getHeure(), alarme.getMinute()));

            int cnt = 0;
            for(Jour jour : Jour.values()) {
                boolean actif = alarme.isJourActif(jour);
                jours[jour.ordinal()].setTextColor(actif ? heure.getCurrentTextColor() : Color.GRAY);
                if(actif)
                    cnt++;
            }
            for(TextView jour : jours)
                jour.setVisibility(cnt == 0 ? View.GONE : View.VISIBLE);

            boutonActivation.setChecked(alarme.isActive());

            boolean hasDayRepeat = false;
            for(Jour j : Jour.values()) {
                hasDayRepeat = alarme.isJourActif(j);
                if(hasDayRepeat)
                    break;
            }
            restrictions.setVisibility(alarme.getRestrictions().size() > 0 || (!hasDayRepeat) ? View.VISIBLE : View.GONE);
            String expl = "";
            if(!hasDayRepeat)
                expl = context.getString(R.string.restr_sonnera_une_fois);
            if(alarme.getRestrictions().size() > 0) {
                if(!expl.isBlank())
                    expl += "\n";
                expl += AlarmUtils.restrictionsToString(this.itemView.getContext(), alarme.getRestrictions());
            }
            restrictions.setText(expl);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ParametresAlarmeActivity.class);
                intent.putExtra("Id", alarme.getId());
                v.getContext().startActivity(intent);
            });

            itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, position);
                return true;
            });

            boutonActivation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                alarme.setActive(isChecked);
                storeAlarme(alarme);
                bindNextOccurence(alarme);
            });
        }
    }
}