package tf.charles.droid.alarme.data;

public class Restriction {
    private Calendrier calendrier;
    private boolean enCours;

    public Restriction() {
    }

    public Restriction(Calendrier calendrier, boolean enCours) {
        this.calendrier = calendrier;
        this.enCours = enCours;
    }

    public Calendrier getCalendrier() {
        return calendrier;
    }

    public void setCalendrier(Calendrier calendrier) {
        this.calendrier = calendrier;
    }

    public boolean isEnCours() {
        return enCours;
    }

    public void setEnCours(boolean enCours) {
        this.enCours = enCours;
    }
}