package com.example.totallynottrackingyou;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{
    /**********************************************************************************************/
    /** VARIABLES & DEFINITIONS */

    /* log tag */
    private static final String TAG = "MainActivity";

    public static final int REQUEST_ACCESS_startLocationUpdates = 0;
    public static final int REQUEST_ACCESS_onConnected = 1;
    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    private static final int TAKE_PICTURE = 1;

    /* widgets */
    private GoogleMap _map;
    private float _zoom;

    private FusedLocationProviderClient _location_client;
    private Location _last_location;
    private LocationRequest _location_request;
    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient _location_client_settings;
    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest _location_request_setting;
    /**
     * Callback for Location events.
     */
    private LocationCallback _location_callback;
    private boolean _requesting_location;

    private FloatingActionButton _camera_fab;

    /* variables */
    private ArrayMap<String, Bitmap> _marker_photos;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        _marker_photos = new ArrayMap<>();


        /* setting up the Map API */
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        _zoom = 18.0f;

        _location_client = LocationServices.getFusedLocationProviderClient(this);
        _location_client_settings = LocationServices.getSettingsClient(this);

        _camera_fab = findViewById(R.id.camera_fab);
        _camera_fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, TAKE_PICTURE);
            }
        });

        createLocationRequest();
        createLocationCallback();
        buildLocationSettingsRequest();
        Log.v(TAG, "starting");
        getLastLocation();
        startLocationUpdates();
    }

    /**********************************************************************************************/
    /** CAMERA */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        // if you know for a fact there will be a bundle, you can use  data.getExtras().get("Data");  but we don't know.
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        // if there was an image returned
                        if(bitmap != null)
                        {
                            // get the current time
                            Date currentTime = Calendar.getInstance().getTime();
                            String timeStamp = currentTime.toString().replace(" ", "-");

                            // save the image
                            String photo_name = "geophoto_" + timeStamp;
                            Log.w(TAG, photo_name);

                            _marker_photos.put(photo_name, bitmap);

                            // use the photo marker to create a map marker
                            _map.addMarker(new MarkerOptions()
                                    .position(new LatLng(_last_location.getLatitude(), _last_location.getLongitude()))
                                    .title(photo_name)
                                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                    .rotation(90.0f)
                                    .flat(false));
                        }

                    } else {
                        Toast.makeText(this, "No picture was returned", Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    /* workarounds ak "google magic"*/
    private BitmapDescriptor vectorToBitmap(@DrawableRes int id, @ColorInt int color) {
        Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(), id, null);
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        DrawableCompat.setTint(vectorDrawable, color);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**********************************************************************************************/
    /** LOCATION SERVICES */

    /* Builders */
    protected void createLocationRequest()
    {
        _location_request = new LocationRequest();
        _location_request.setInterval(10000);
        _location_request.setFastestInterval(5000);
        _location_request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest()
    {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(_location_request);
        _location_request_setting = builder.build();
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback()
    {
        _location_callback = new LocationCallback()
        {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                super.onLocationResult(locationResult);

                _last_location = locationResult.getLastLocation();
                _map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(_last_location.getLatitude(), _last_location.getLongitude()), _zoom));
            }
        };
    }


    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();

        startLocationUpdates();
    }

    protected void startLocationUpdates()
    {
        //first check to see if I have permissions (marshmallow) if I don't then ask, otherwise start up the demo.
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            //I'm on not explaining why, just asking for permission.
            Log.v(TAG, "asking for permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MapsActivity.REQUEST_ACCESS_startLocationUpdates);
            return;
        }

        // Begin by checking if the device has the necessary location settings.
        _location_client_settings.checkLocationSettings(_location_request_setting)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>()
                {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse)
                    {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        _location_client.requestLocationUpdates(_location_request, _location_callback, Looper.myLooper());

                    }
                })
                .addOnFailureListener(this, new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode)
                        {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try
                                {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie)
                                {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MapsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                _requesting_location = false;
                        }


                    }
                });


    }

    protected void stopLocationUpdates()
    {
        if (!_requesting_location)
        {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        _location_client.removeLocationUpdates(_location_callback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        _requesting_location = false;
                    }
                });
    }

    //This shows how to get a "one off" location.  instead of using the location updates
    //
    public void getLastLocation()
    {
        //first check to see if I have permissions (marshmallow) if I don't then ask, otherwise start up the demo.
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED))
        {
            //I'm on not explaining why, just asking for permission.
            Log.v(TAG, "asking for permissions");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    MapsActivity.REQUEST_ACCESS_onConnected);
            return;
        }
        _location_client.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>()
                {
                    @Override
                    public void onSuccess(Location location)
                    {
                        if (location == null)
                        {
                            Log.w(TAG, "onSuccess:null");
                            return;
                        }
                        _last_location = location;

                        Log.v(TAG, "getLastLocation");
                        if (_last_location != null)
                        {
                            _map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(_last_location.getLatitude(), _last_location.getLongitude()), _zoom));
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.w(TAG, "getLastLocation:onFailure", e);
                    }
                });

    }

    /**********************************************************************************************/
    /** MAP */

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        _map = googleMap;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        else
        {
            _map.setMyLocationEnabled(true);

            _map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
            {
                @Override
                public boolean onMarkerClick(Marker marker)
                {
                    // get the title of the clicked marker
                    String marker_title = marker.getTitle();
                    // if the map contains the key from the marker
                    if(_marker_photos.containsKey(marker_title))
                    {
                        // get the bitmap from the map
                        Bitmap bitmap = _marker_photos.get(marker_title);

                        // compress the bitmap to send it to the dialog fragment
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();

                        // create a bundle to send the dialog fragment
                        Bundle photo_dialog_bundle = new Bundle();
                        photo_dialog_bundle.putByteArray("photo", byteArray);

                        PhotoDialog photoDialog = new PhotoDialog();
                        photoDialog.setArguments(photo_dialog_bundle);
                        photoDialog.show(getSupportFragmentManager(), marker_title);
                    }
                    else
                    {
                        Toast toast = Toast.makeText(getApplicationContext(), "Marker Photo not found", Toast.LENGTH_SHORT);
                        toast.show();
                    }

                    return false;
                }
            });
        }
    }
    /**********************************************************************************************/
    /** PERMISSIONS */

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.v(TAG, "onRequest result called.");
        boolean coarse = false, fine = false;

        //received result for GPS access
        for (int i = 0; i < grantResults.length; i++) {
            if ((permissions[i].compareTo(Manifest.permission.ACCESS_COARSE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                coarse = true;
            else if ((permissions[i].compareTo(Manifest.permission.ACCESS_FINE_LOCATION) == 0) &&
                    (grantResults[i] == PackageManager.PERMISSION_GRANTED))
                fine = true;
        }
        Log.v(TAG, "Received response for gps permission request.");
        // If request is cancelled, the result arrays are empty.
        if (coarse && fine) {
            // permission was granted
            Log.v(TAG, permissions[0] + " permission has now been granted. Showing preview.");
            Toast.makeText(this, "GPS access granted",
                    Toast.LENGTH_SHORT).show();
            if (requestCode == REQUEST_ACCESS_startLocationUpdates) {
                startLocationUpdates();
            } else if (requestCode == REQUEST_ACCESS_onConnected) {
                getLastLocation();
            }

        } else {
            // permission denied,    Disable this feature or close the app.
            Log.v(TAG, "GPS permission was NOT granted.");
            Toast.makeText(this, "GPS access NOT granted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }



}
