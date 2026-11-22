package tf.charles.droid.alarme.data;

public class Calendrier {
    private int id;
    private String accountType;
    private String accountName;
    private String calendarName;
    private String calendarDisplayName;
    private int calendarColor;

    public Calendrier() {
    }

    public Calendrier(int id, String accountType, String accountName, String calendarName, String calendarDisplayName, int calendarColor) {
        this.id = id;
        this.accountType = accountType;
        this.accountName = accountName;
        this.calendarName = calendarName;
        this.calendarDisplayName = calendarDisplayName;
        this.calendarColor = calendarColor;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }

    public String getCalendarDisplayName() {
        return calendarDisplayName;
    }

    public void setCalendarDisplayName(String calendarDisplayName) {
        this.calendarDisplayName = calendarDisplayName;
    }

    public int getCalendarColor() {
        return calendarColor;
    }

    public void setCalendarColor(int calendarColor) {
        this.calendarColor = calendarColor;
    }
}