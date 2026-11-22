package tf.charles.droid.alarme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import tf.charles.droid.alarme.R;
import tf.charles.droid.alarme.data.Restriction;

public class RestrictionsAdapter extends RecyclerView.Adapter<RestrictionsAdapter.RestrictionViewHolder> {

    private List<Restriction> restrictionsList = new ArrayList<>();

    public RestrictionsAdapter() {
    }

    @NonNull
    @Override
    public RestrictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_restriction, parent, false);
        return new RestrictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RestrictionViewHolder holder, int position) {
        Restriction content;
        if(position == 0 && restrictionsList.isEmpty())
            content = null;
        else
            content = restrictionsList.get(position);

        if(content == null) {
            holder.textNomCalendrier.setText(R.string.restr_aucune);
        }
        else {
            holder.textNomCalendrier.setText(content.getCalendrier().getCalendarDisplayName());

            // popupmenu
            holder.itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, position);
                return true;
            });
        }
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_contextuel_restriction, popupMenu.getMenu());

        // Gestion du clic sur une option du menu
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_supprimer) {
                // Supprimer l'élément de la liste
                restrictionsList.remove(position);
                notifyItemRemoved(position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    @Override
    public int getItemCount() {
        if(restrictionsList.isEmpty())
            return 1;
        return restrictionsList.size();
    }

    public void setRestrictionsList(List<Restriction> restrictionsList) {
        this.restrictionsList = restrictionsList == null ? new ArrayList<>() : restrictionsList;
        notifyDataSetChanged();
    }

    public static class RestrictionViewHolder extends RecyclerView.ViewHolder {
        TextView textNomCalendrier;

        public RestrictionViewHolder(@NonNull View itemView) {
            super(itemView);
            textNomCalendrier = itemView.findViewById(R.id.textNomCalendrier);
        }
    }
}