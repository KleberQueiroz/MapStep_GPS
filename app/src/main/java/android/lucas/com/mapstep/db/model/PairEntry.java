package android.lucas.com.mapstep.db.model;

/**
 * Created by cc on 17-6-23.
 */

public class PairEntry {
    private String key;
    private String value;

    public PairEntry() {
    }

    public PairEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String name) {
        this.value = value;
    }
}
