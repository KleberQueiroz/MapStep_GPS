package android.lucas.com.mapstep.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.lucas.com.mapstep.db.model.PairEntry;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by cc on 17-6-23.
 *
 * Usage:
 *          DBHelper dbHelper = new DBHelper(<context>);
 *          dbHelper.findAll();                                 // get all data as ArrayList
 *          dbHelper.findByKey(key);                            // find data by key (name)
 *          dbHelper.addPairEntry(new PairEntry(key, value));   // add new data
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String LOG_NAME = "DBHelper";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + EntryContract.PairEntryTable.TABLE_NAME + " (" +
                    EntryContract.PairEntryTable._ID + " INTEGER PRIMARY KEY," +
                    EntryContract.PairEntryTable.COLUMN_NAME_KEY + TEXT_TYPE + COMMA_SEP +
                    EntryContract.PairEntryTable.COLUMN_NAME_VALUE + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + EntryContract.PairEntryTable.TABLE_NAME;

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "location_log.db";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
        onCreate(sqLiteDatabase);

    }

    public long addPairEntry(PairEntry pairEntry) {

        Log.i(LOG_NAME, "Saving entry with key: " + pairEntry.getKey() +
                ", value: " + pairEntry.getValue());

        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(EntryContract.PairEntryTable.COLUMN_NAME_KEY, pairEntry.getKey());
        values.put(EntryContract.PairEntryTable.COLUMN_NAME_VALUE, pairEntry.getValue());

        // Insert the new row, returning the primary key value of the new row
        return db.insert(EntryContract.PairEntryTable.TABLE_NAME, null, values);

    }

    public PairEntry findByKey(String key) {

        Log.i(LOG_NAME, "Finding entry by key: " + key);

        SQLiteDatabase db = this.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                EntryContract.PairEntryTable._ID,
                EntryContract.PairEntryTable.COLUMN_NAME_KEY,
                EntryContract.PairEntryTable.COLUMN_NAME_VALUE,
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = EntryContract.PairEntryTable.COLUMN_NAME_KEY + " = ?";
        String[] selectionArgs = { key };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                EntryContract.PairEntryTable.COLUMN_NAME_KEY + " DESC";

        Cursor c = db.query(
                EntryContract.PairEntryTable.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        c.moveToFirst();

        String entry_key = c.getString(
                c.getColumnIndexOrThrow(EntryContract.PairEntryTable.COLUMN_NAME_KEY)
        );

        String entry_value = c.getString(
                c.getColumnIndexOrThrow(EntryContract.PairEntryTable.COLUMN_NAME_VALUE)
        );

        c.close();

        Log.i(LOG_NAME, "Found entry with value: " + entry_value);

        return new PairEntry(entry_key, entry_value);

    }

    public ArrayList<PairEntry> findAll() {

        Log.i(LOG_NAME, "Finding all entries.");

        ArrayList<PairEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                EntryContract.PairEntryTable.COLUMN_NAME_KEY + " DESC";

        Cursor c = db.query(
                EntryContract.PairEntryTable.TABLE_NAME,
                null, null, null, null, null, sortOrder
        );

        if (c.moveToFirst()) {

            do {

                String entry_key = c.getString(
                        c.getColumnIndexOrThrow(EntryContract.PairEntryTable.COLUMN_NAME_KEY)
                );

                String entry_value = c.getString(
                        c.getColumnIndexOrThrow(EntryContract.PairEntryTable.COLUMN_NAME_VALUE)
                );

                Log.i(LOG_NAME, "Found entry with key: " + entry_key +
                        ", value: " + entry_value);

                entries.add(new PairEntry(entry_key, entry_value));

            } while (c.moveToNext());

        }

        c.close();

        return entries;

    }

    public void deleteByKey(String key) {

        // Gets the data repository in write mode
        SQLiteDatabase db = this.getWritableDatabase();

        // Define 'where' part of query.
        String selection = EntryContract.PairEntryTable.COLUMN_NAME_KEY + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { key };
        // Issue SQL statement.
        db.delete(EntryContract.PairEntryTable.TABLE_NAME, selection, selectionArgs);

    }
}
