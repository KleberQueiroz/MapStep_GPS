package android.lucas.com.mapstep.db;

import android.provider.BaseColumns;

/**
 * Created by cc on 17-6-23.
 */

public final class EntryContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private EntryContract() {}

    /* Inner class that defines the table contents */
    public static class PairEntryTable implements BaseColumns {
        public static final String TABLE_NAME = "pair_entry";
        public static final String COLUMN_NAME_KEY = "key";
        public static final String COLUMN_NAME_VALUE = "value";
    }
}