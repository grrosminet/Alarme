package tf.charles.droid.alarme.data;

import java.util.ArrayList;
import java.util.List;

import tf.charles.droid.alarme.misc.Jour;

public class Alarme {
    private long id;
    private String titre;
    private int heure = 12;
    private int minute = 0;
    private boolean [] jours = new boolean[7];
    private boolean active = true;
    private List<Restriction> restrictions = new ArrayList<>();

    public Alarme() {
        this(System.currentTimeMillis());
    }

    public Alarme(long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public String getTitre() {
        return titre;
    }

    public int getHeure() {
        return heure;
    }

    public int getMinute() {
        return minute;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(List<Restriction> restrictions) {
        this.restrictions = restrictions;
    }

    public boolean isJourActif(Jour jour) {
        return jours[jour.ordinal()];
    }

    public void setJourActif(Jour jour, boolean actif) {
        jours[jour.ordinal()] = actif;
    }

    public boolean isDimanche() {
        return isJourActif(Jour.Dimanche);
    }

    public void setDimanche(boolean dimanche) {
        this.setJourActif(Jour.Dimanche, dimanche);
    }

    public boolean isSamedi() {
        return isJourActif(Jour.Samedi);
    }

    public void setSamedi(boolean samedi) {
        this.setJourActif(Jour.Samedi, samedi);
    }

    public boolean isVendredi() {
        return isJourActif(Jour.Vendredi);
    }

    public void setVendredi(boolean vendredi) {
        this.setJourActif(Jour.Vendredi, vendredi);
    }

    public boolean isJeudi() {
        return isJourActif(Jour.Jeudi);
    }

    public void setJeudi(boolean jeudi) {
        this.setJourActif(Jour.Jeudi, jeudi);
    }

    public boolean isMercredi() {
        return isJourActif(Jour.Mercredi);
    }

    public void setMercredi(boolean mercredi) {
        this.setJourActif(Jour.Mercredi, mercredi);
    }

    public boolean isMardi() {
        return isJourActif(Jour.Mardi);
    }

    public void setMardi(boolean mardi) {
        this.setJourActif(Jour.Mardi, mardi);
    }

    public boolean isLundi() {
        return isJourActif(Jour.Lundi);
    }

    public void setLundi(boolean lundi) {
        this.setJourActif(Jour.Lundi, lundi);
    }

    public void setHeure(int heure) {
        this.heure = heure;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

}