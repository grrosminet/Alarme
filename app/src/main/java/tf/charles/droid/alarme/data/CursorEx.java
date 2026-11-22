package tf.charles.droid.alarme.data;

import android.database.Cursor;

public class CursorEx {
    private Cursor m_cursor;

    public CursorEx(Cursor c) {
        m_cursor = c;
    }

    public Integer getInt(String colName) {
        int colIdx = m_cursor.getColumnIndex(colName);
        if(colIdx < 0)
            return null;
        return m_cursor.getInt(colIdx);
    }

    public String getString(String colName) {
        int colIdx = m_cursor.getColumnIndex(colName);
        if(colIdx < 0)
            return null;
        return m_cursor.getString(colIdx);
    }
}
