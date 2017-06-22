package android.lucas.com.mapstep;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Calendar;

import static com.google.android.gms.common.zze.GOOGLE_PLAY_SERVICES_VERSION_CODE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {

    //LogCat tag
    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final float accelFilteringFactor = 0.1f;
    private static final float magFilteringFactor = 0.01f;
    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 5 * 1000; // 1 sec
    private static int FATEST_INTERVAL = 5 * 1000; // 5 sec
    private static int DISPLACEMENT = 1; // 2 meters
    private final int REQUEST_PERMISSIONS_CODE_GPS = 1;
    int count = 0;
    private GoogleMap mMap;
    private boolean GPS_PERMISSION_GRANTED = false;
    //Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Point lastPoint;
    private double latitude = 0;
    private double longitude = 0;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor stepDetector;
    private boolean isReadyGravity;
    private boolean isReadyGeomagnetic;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimut;
    private TextView textView;
    private Switch button;
    private Button data;

    private boolean moveCamera = true;

    private ArrayList<String> latLong;
    private ArrayList<String> directions;

    private View customAlert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        latLong = new ArrayList<String>();
        directions = new ArrayList<String>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mLog_i("onCreate()");

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        textView = (TextView) findViewById(R.id.textView);

        data = (Button)findViewById(R.id.data);
        data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(latLong.isEmpty() || directions.isEmpty())
                {
                   Toast.makeText(getApplicationContext(), "Data is empty", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    customAlert = getLayoutInflater().inflate(R.layout.data_dialog, null);

                    final AlertDialog dialog = new AlertDialog.Builder(MapsActivity.this).create();

                    final ListView lv_latlong = (ListView)customAlert.findViewById(R.id.latlong);

                    final ListView lv_directions = (ListView)customAlert.findViewById(R.id.direction);

                    ArrayAdapter<String> arrayAdapter_latlong =  new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, latLong);
                    ArrayAdapter<String> arrayAdapter_directions =  new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, directions);

                    lv_latlong.setAdapter(arrayAdapter_latlong);
                    lv_directions.setAdapter(arrayAdapter_directions);

                    dialog.setView(customAlert);
                    dialog.show();

                    saveData();

                }
            }
        });


        permissionRequest();

        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();

            createLocationRequest();
        }

        LocationManager locationManager = (LocationManager) this.getSystemService(getApplicationContext().LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS ON", Toast.LENGTH_SHORT).show();
        }

    }

    private void saveData() {

        String data_directions = "", data_latlong = "";
        Long current_time = Calendar.getInstance().getTimeInMillis();

        if (latLong.isEmpty() || directions.isEmpty()) return;

        for (String tmp: directions) {
            data_directions += (tmp + "\n");
        }

        for (String tmp: latLong) {
            data_latlong += (tmp + "\n");
        }

        FileManager fm = new FileManager(this);

        fm.salvar(data_directions   , fm.DIRECTIONS);
        fm.salvar(data_latlong      , fm.LATLONG);

        Toast.makeText(getApplicationContext(), "data is saved, " + current_time, Toast.LENGTH_SHORT).show();

        // clear data
        latLong = new ArrayList<String>();
        directions = new ArrayList<String>();

    }

    @Override
    protected void onStart() {
        super.onStart();

        mLog_i("onStart()");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mLog_i("onResume()");

        checkPlayServices();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_GAME);

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mLog_i("onStop()");

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mLog_i("onPause()");

        mSensorManager.unregisterListener(this);


        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    /**
     * *********************************************************************************************************
     * Map Methods                                                                                             *
     * *********************************************************************************************************
     */


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        button = (Switch) findViewById(R.id.switch1);

        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(b);
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLog_d("onMapReady()");

        // 90 - norte, 270 - sul, 180 - oeste, 360 - leste
        // 0  - norte, +/-180 - sul, -90 - oeste, 90 - leste
    }

    public void updateCurrentLocation()
    {
        lastPoint = new LocationBasedStepDetector()
                .calcFinalPosition(
                        azimut,
                        LocationBasedStepDetector.STEP_DISTANCE,
                        lastPoint);

        mLog_d("Lat: " + lastPoint.lat + ", Lon: " + lastPoint.lon);

        addMarker(lastPoint, mMap);
    }

    public void addMarker(Point point, GoogleMap googleMap)
    {
        mLog_d("addMarker");

        MarkerOptions marker = new MarkerOptions().position(new LatLng(point.lat, point.lon)).title(Integer.toString(count++));

        marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon));

        googleMap.addMarker(marker);
    }

    public void updateCamera() {

//        mLog_d("updateCamera()");

//        displayLocation();

        if (latitude != 0 && longitude != 0 && moveCamera)
        {

            lastPoint = new Point(latitude, longitude);

            mLog_d("lastPoint lat ->: "+lastPoint.lat+" long - > " + lastPoint.lon );

            CameraPosition currentPlace = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude))
                    .bearing(0).tilt(0).zoom(20).build();
            mMap.clear();

            MarkerOptions marker = new MarkerOptions().position(new LatLng(lastPoint.lat, lastPoint.lon)).title(Integer.toString(count++));

            marker.icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon));

            mMap.addMarker(marker);

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));


        }
//        else
//        {
//            Toast.makeText(getApplicationContext(), "Cannot get location", Toast.LENGTH_SHORT).show();
//        }
    }

    /**
     * *********************************************************************************************************
     * *********************************************************************************************************
     */

    /**
     * *********************************************************************************************************
     * GoogleApiClient Methods                                                                                 *
     * *********************************************************************************************************
     */

    public boolean checkPlayServices()
    {

        mLog_d("checkPlayServices()");

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();

        int resultCode = googleApi.isGooglePlayServicesAvailable(getApplicationContext());

        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (googleApi.isUserResolvableError(resultCode))
            {
                googleApi.getErrorDialog(this,
                        resultCode, GOOGLE_PLAY_SERVICES_VERSION_CODE).show();
            }
            else
            {
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }

        Toast.makeText(getApplicationContext(), "Play service avaiable", Toast.LENGTH_SHORT).show();

        return true;
    }

    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient()
    {
        mLog_d("buildGoogleApiClient()");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0)
    {

//        // Once connected with google api, get the location
        displayLocation();
//
        mLog_d("onConnected()");
        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int arg0)
    {
        mLog_d("onConnectionSuspended()");
        mGoogleApiClient.connect();
    }


    /**
     * *********************************************************************************************************
     * *********************************************************************************************************
     */

    /**
     * *********************************************************************************************************
     * Location Methods                                                                                        *
     * *********************************************************************************************************
     */

    /**
     * Creating location request object
     */
    protected void createLocationRequest()
    {
        mLog_d("createLocationRequest()");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
//        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to display the location on UI
     */
    private void displayLocation()
    {

//        mLog_d("displayLocation()");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null)
        {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

//            Log.i(TAG, latitude + ", " + longitude);

            updateCamera();

        }
        else
        {
            Toast.makeText(getApplicationContext(), "(Couldn't get the location. Make sure location is enabled on the device)", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates()
    {
        mLog_d("startLocationUpdates()");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates()
    {

        mLog_d("stopLocationUpdates()");

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, (LocationListener) this);
    }


    @Override
    public void onLocationChanged(Location location) {

        mLog_d("onLocationChanged()");
        // Assign the new location
        mLastLocation = location;

        // Displaying the new location on UI
        displayLocation();
    }


    /**
     * *********************************************************************************************************
     * *********************************************************************************************************
     */

    /**
     * *********************************************************************************************************
     * Permission Methods                                                                                      *
     * *********************************************************************************************************
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            mGravity[0] = sensorEvent.values[0] * accelFilteringFactor + mGravity[0] * (1.0f - accelFilteringFactor);
            mGravity[1] = sensorEvent.values[1] * accelFilteringFactor + mGravity[1] * (1.0f - accelFilteringFactor);
            mGravity[2] = sensorEvent.values[2] * accelFilteringFactor + mGravity[2] * (1.0f - accelFilteringFactor);
            isReadyGravity = true;
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
        {
            mGeomagnetic[0] = sensorEvent.values[0] * magFilteringFactor + mGeomagnetic[0] * (1.0f - magFilteringFactor);
            mGeomagnetic[1] = sensorEvent.values[1] * magFilteringFactor + mGeomagnetic[1] * (1.0f - magFilteringFactor);
            mGeomagnetic[2] = sensorEvent.values[2] * magFilteringFactor + mGeomagnetic[2] * (1.0f - magFilteringFactor);
            isReadyGeomagnetic = true;
        }
        if (mGravity != null && mGeomagnetic != null && isReadyGravity && isReadyGeomagnetic) {

            isReadyGeomagnetic = isReadyGravity = false;

            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                double x180pi = 180.0 / Math.PI;
                azimut = (float)(orientation[0] * x180pi);
                textView.setText(String.valueOf(azimut));


            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {

            moveCamera = false;
            updateCurrentLocation();
            latLong.add(latitude + "\n " + longitude);
            directions.add(azimut + "\n");

//            Log.i(TAG, latitude + ", " + longitude);
//            Log.i(TAG, "Azimut: " + azimut);

//            stopLocationUpdates();

        }

//        Log.i(TAG, "Azimut: " + (float)(azimut));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    /**
     * *********************************************************************************************************
     * *********************************************************************************************************
     */

    /**
     * *********************************************************************************************************
     * Permission Methods                                                                                      *
     * *********************************************************************************************************
     */

    private boolean permissionRequest() {

        mLog_d("permissionRequest()");

        if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_CODE_GPS);

            } else {

                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_CODE_GPS);
            }
        } else {
            return true;
        }

        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE_GPS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    GPS_PERMISSION_GRANTED = true;

                    Toast.makeText(this, "Permissão de GPS -> CONCEDIDA", Toast.LENGTH_SHORT).show();


                } else {

                    Toast.makeText(this, "Permissão de GPS -> NEGADA", Toast.LENGTH_SHORT).show();

                }
                return;
            }

            default:
                break;
        }
    }


    /**
     * *********************************************************************************************************
     * *********************************************************************************************************
     */

    private void mLog_d(String txt) {
        Log.d(TAG, txt);
    }

    private void mLog_i(String txt) {
        Log.i(TAG, txt);
    }


}
