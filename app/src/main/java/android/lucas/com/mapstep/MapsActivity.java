package android.lucas.com.mapstep;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.lucas.com.mapstep.db.DBHelper;
import android.lucas.com.mapstep.db.model.PairEntry;
import android.lucas.com.mapstep.dialog.DataDialog;
import android.lucas.com.mapstep.sensor.sensors.Orientation;
import android.lucas.com.mapstep.sensor.utils.OrientationSensorInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
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
        GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener, OrientationSensorInterface {

    //LogCat tag
    private static final String TAG = MapsActivity.class.getSimpleName();
    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 5 * 1000; // 1 sec
    private static int FATEST_INTERVAL = 5 * 1000; // 5 sec
    private final int REQUEST_PERMISSIONS_CODE_GPS = 1;
    int count = 0;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Point lastPoint;
    private double latitude = 0;
    private double longitude = 0;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor stepDetector;
    private TextView textView, textView2;
    private Switch button;

    private boolean moveCamera = true;

    private ArrayList<String> latLong;
    private ArrayList<String> directions;
    private ArrayList<String> direcs_stable;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        dbHelper        = new DBHelper(this);
        latLong         = new ArrayList<String>();
        directions      = new ArrayList<String>();
        direcs_stable   = new ArrayList<String>();

        mSensorManager  = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer   = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer    = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector    = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        textView        = (TextView) findViewById(R.id.textView);
        textView2       = (TextView) findViewById(R.id.textView2);
        button          = (Switch) findViewById(R.id.switch1);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setButtons();
        permissionRequest();
        useStableSensor();

        if (checkPlayServices()) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API).build();

            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        }

    }

    private Orientation orientationSensor;
    private void useStableSensor() {

        orientationSensor = new Orientation(this.getApplicationContext(), this);

        // set tolerance for any directions
        orientationSensor.init(1.0, 1.0, 1.0);

    }

    private void setButtons() {

        ((Button)findViewById(R.id.data)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDataDialog();
            }
        });

        ((Button)findViewById(R.id.btn2)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveData();
//                ArrayList<PairEntry> pairEntries = dbHelper.findAll();
//                Toast.makeText(getApplicationContext(), "Size: " + pairEntries.size(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void showDataDialog() {

        if(latLong.isEmpty() || directions.isEmpty()){
            Toast.makeText(getApplicationContext(), "Data is empty", Toast.LENGTH_SHORT).show();
        } else {

            mLog_i("latLong: " + latLong.size());
            DataDialog.getDialog(this, latLong, directions).show();

        }

    }

    private void saveData() {

        String data_directions = "", data_latlong = "";
        Long current_time = Calendar.getInstance().getTimeInMillis();

        if (latLong.isEmpty() || directions.isEmpty()) return;

        for (String tmp: directions) { data_directions += (tmp + "\n"); }
        for (String tmp: latLong) { data_latlong += (tmp + "\n"); }

        saveToDB(new PairEntry(FileManager.DIRECTIONS + current_time, data_directions));
        saveToDB(new PairEntry(FileManager.LATLONG + current_time, data_latlong));

        Toast.makeText(getApplicationContext(), "data is saved, " + current_time, Toast.LENGTH_SHORT).show();

        // clear data
        latLong.clear();
        directions.clear();
        mMap.clear();

    }

    private void saveToDB(PairEntry entry) {
        dbHelper.addPairEntry(entry);
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

        checkPlayServices();

        // set output speed and turn initialized sensor on
        // 0 Normal
        // 1 UI
        // 2 GAME
        // 3 FASTEST
        orientationSensor.on(0);
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
        orientationSensor.off();

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

        mLog_d("onMapReady()");

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(b);
            }
        });

        // 90 - norte, 270 - sul, 180 - oeste, 360 - leste
        // 0  - norte, +/-180 - sul, -90 - oeste, 90 - leste
    }

    public void updateCurrentLocation() {

        lastPoint = LastLocationUpdater.calcNextLocation(
                azimut,
                LastLocationUpdater.STEP_DISTANCE,
                lastPoint);

        mLog_d("Lat: " + lastPoint.lat + ", Lon: " + lastPoint.lon);

        addMarker(lastPoint, mMap);

    }

    public void addMarker(Point point, GoogleMap googleMap) {

        MarkerOptions marker = new MarkerOptions()
                .position(new LatLng(point.lat, point.lon))
                .title(Integer.toString(count++))
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.icon));
        googleMap.addMarker(marker);

    }

    public void updateCamera() {

        lastPoint = new Point(latitude, longitude);

        CameraPosition currentPlace = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .bearing(0).tilt(0).zoom(20).build();

        mMap.clear();
        addMarker(lastPoint, mMap);

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));

    }

    /**
     * *********************************************************************************************************
     * GoogleApiClient Methods                                                                                 *
     * *********************************************************************************************************
     */

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();

        int resultCode = googleApi.isGooglePlayServicesAvailable(getApplicationContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApi.isUserResolvableError(resultCode)) {
                googleApi.getErrorDialog(this,
                        resultCode, GOOGLE_PLAY_SERVICES_VERSION_CODE).show();
            } else {
                finish();
            }
            return false;
        }

        return true;
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        updateLocation();
        mLog_d("onConnected()");

        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mLog_d("onConnectionSuspended()");
        mGoogleApiClient.connect();
    }

    /**
     * *********************************************************************************************************
     * Location Methods                                                                                        *
     * *********************************************************************************************************
     */

    /**
     * Method to display the location on UI
     */
    private void updateLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {

            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            if (moveCamera) { updateCamera(); }

        } else {
            Toast.makeText(getApplicationContext(), "(Couldn't get the location. Make sure location is enabled on the device)", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {

        mLog_d("stopLocationUpdates()");

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, (LocationListener) this);
    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        updateLocation();

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        updateAzimut(sensorEvent);

        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

            moveCamera = false;
            updateCurrentLocation();
            latLong.add(latitude + "\n " + longitude);
            directions.add(String.valueOf(azimut));

        }

    }

    private boolean isReadyGravity;
    private boolean isReadyGeomagnetic;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimut;
    private void updateAzimut(SensorEvent sensorEvent) {

        float accelFilteringFactor = 0.1f;
        float magFilteringFactor = 0.01f;

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity[0] = sensorEvent.values[0] * accelFilteringFactor + mGravity[0] * (1.0f - accelFilteringFactor);
            mGravity[1] = sensorEvent.values[1] * accelFilteringFactor + mGravity[1] * (1.0f - accelFilteringFactor);
            mGravity[2] = sensorEvent.values[2] * accelFilteringFactor + mGravity[2] * (1.0f - accelFilteringFactor);
            isReadyGravity = true;
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic[0] = sensorEvent.values[0] * magFilteringFactor + mGeomagnetic[0] * (1.0f - magFilteringFactor);
            mGeomagnetic[1] = sensorEvent.values[1] * magFilteringFactor + mGeomagnetic[1] * (1.0f - magFilteringFactor);
            mGeomagnetic[2] = sensorEvent.values[2] * magFilteringFactor + mGeomagnetic[2] * (1.0f - magFilteringFactor);
            isReadyGeomagnetic = true;
        }

        if (isReadyGravity && isReadyGeomagnetic) {

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

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void permissionRequest() {

        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_CODE_GPS);
        }

    }

    private void mLog_d(String txt) {
        Log.d(TAG, txt);
    }

    private void mLog_i(String txt) {
        Log.i(TAG, txt);
    }

    private Double azimut_;
    @Override
    public void orientation(Double AZIMUTH, Double PITCH, Double ROLL) {

        if (AZIMUTH > 180) {
            azimut_ = AZIMUTH - 360;
        } else {
            azimut_ = AZIMUTH;
        }
        textView2.setText(String.valueOf(azimut_));

    }
}
