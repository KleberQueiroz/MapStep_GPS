package android.lucas.com.mapstep.dialog;

import android.app.Activity;
import android.lucas.com.mapstep.R;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by cc on 17-7-5.
 */

public class DataDialog {

    private static AlertDialog dialog = null;
    private static View customAlert = null;

    public static AlertDialog getDialog(Activity activity, ArrayList<String> latLong, ArrayList<String> directions) {

        if (dialog == null){

            dialog = new AlertDialog.Builder(activity).create();
            customAlert = activity.getLayoutInflater().inflate(R.layout.data_dialog, null);

        }

        ListView lv_latlong = (ListView)customAlert.findViewById(R.id.latlong);
        ListView lv_directions = (ListView)customAlert.findViewById(R.id.direction);

        ArrayAdapter<String> adapter_latlon =  new ArrayAdapter<String>(activity.getApplicationContext(), android.R.layout.simple_list_item_1, latLong);
        ArrayAdapter<String> adapter_direc =  new ArrayAdapter<String>(activity.getApplicationContext(), android.R.layout.simple_list_item_1, directions);

        lv_latlong.setAdapter(adapter_latlon);
        lv_directions.setAdapter(adapter_direc);

        dialog.setView(customAlert);

        return dialog;

    }

}
