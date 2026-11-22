package tf.charles.droid.alarme.adapters;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.data.Calendrier;

public class CalendrierTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CALENDRIER = 1;

    private Map<String, List<Calendrier>> calendriersParCompte;
    private OnCalendrierClickListener listener;

    public CalendrierTreeAdapter(Map<String, List<Calendrier>> calendriersParCompte, OnCalendrierClickListener listener) {
        this.calendriersParCompte = calendriersParCompte;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendrier_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendrier, parent, false);
            return new CalendrierViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int currentPosition = 0;
        for (Map.Entry<String, List<Calendrier>> entry : calendriersParCompte.entrySet()) {
            if (position == currentPosition) {
                ((HeaderViewHolder) holder).bind(entry.getKey());
                return;
            }
            currentPosition++;

            for (Calendrier calendrier : entry.getValue()) {
                if (position == currentPosition) {
                    ((CalendrierViewHolder) holder).bind(calendrier);
                    return;
                }
                currentPosition++;
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for (List<Calendrier> calendriers : calendriersParCompte.values()) {
            count += calendriers.size() + 1; // +1 pour l'en-tête
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        int currentPosition = 0;
        for (Map.Entry<String, List<Calendrier>> entry : calendriersParCompte.entrySet()) {
            if (position == currentPosition) {
                return TYPE_HEADER;
            }
            currentPosition++;

            for (Calendrier calendrier : entry.getValue()) {
                if (position == currentPosition) {
                    return TYPE_CALENDRIER;
                }
                currentPosition++;
            }
        }
        return TYPE_HEADER;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageCompte;
        TextView textCompte;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageCompte = itemView.findViewById(R.id.imageCompte);
            textCompte = itemView.findViewById(R.id.textCompte);
        }

        public void bind(String compte) {
            String accountType = compte.replaceAll("(.*)\\[.*\\].*", "$1");
            AccountManager am = AccountManager.get(imageCompte.getContext());
            AuthenticatorDescription[] authenticatorTypes = am.getAuthenticatorTypes();

            for (AuthenticatorDescription desc : authenticatorTypes) {
                if (desc.type.equals(accountType)) {
                    // Charger l'icône à partir du package du fournisseur de compte
                    PackageManager pm = imageCompte.getContext().getPackageManager();
                    Drawable icon = pm.getDrawable(desc.packageName, desc.iconId, null);
                    imageCompte.setImageDrawable(icon);
                    break;
                }
            }

            textCompte.setText(compte.replaceAll(".*\\[(.*)\\].*", "$1"));
        }
    }

    class CalendrierViewHolder extends RecyclerView.ViewHolder {
        View viewCouleur;
        TextView textViewCalendrier;

        public CalendrierViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCouleur = itemView.findViewById(R.id.viewCouleur);
            textViewCalendrier = itemView.findViewById(R.id.textViewCalendrier);
        }

        public void bind(Calendrier calendrier) {
            textViewCalendrier.setText(calendrier.getCalendarDisplayName());
            viewCouleur.setBackgroundColor(calendrier.getCalendarColor());
            itemView.setOnClickListener(v -> listener.onCalendrierClick(calendrier));
        }
    }

    public interface OnCalendrierClickListener {
        void onCalendrierClick(Calendrier calendrier);
    }
}