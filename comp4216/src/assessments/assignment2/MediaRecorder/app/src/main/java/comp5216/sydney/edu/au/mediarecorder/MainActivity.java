package comp5216.sydney.edu.au.mediarecorder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * Based on code from Kshitiz Bhargava, Tutorial 6: Media Access
 * Adapted by Tanaka Chitete
 * Accessed 19/09/2023
 */
public class MainActivity extends Activity {
    private static class FileEntity {
        private final Uri uri;
        private final Location lastKnownLocation;
        FileEntity(Uri uri, Location lastKnownLocation) {
            this.uri = uri;
            this.lastKnownLocation = lastKnownLocation;
        }

        public Uri getUri() {
            return uri;
        }

        public Location getLastKnownLocation() {
            return lastKnownLocation;
        }
    }
    private static final int MY_PERMISSIONS_REQUEST_OPEN_CAMERA = 101;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHOTOS = 102;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_VIDEO = 103;
    private static final int MY_PERMISSIONS_REQUEST_READ_VIDEOS = 104;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int BATCH_SIZE = 3;
    public final String APP_TAG = "MediaRecorder";
    public final String CLASS_TAG = "MainActivity";
    public String photoFileName = "photo.jpg";
    public String videoFileName = "video.mp4";
    private static final double DEFAULT_LATITUDE = -33.865143;
    private static final double DEFAULT_LONGITUDE = 151.209900;

    private final Queue<FileEntity> mediaQueue = new LinkedList<>();
    private boolean locationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationProviderClient;
    MarshmallowPermission marshmallowPermission = new MarshmallowPermission(this);
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Construct a FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Prompt the user for permission
        getLocationPermission();
    }

    public void onTakePhotoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to take a picture and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            photoFileName = "IMG_" + timeStamp + ".jpg";

            // Create a photo file reference
            Uri file_uri = getFileUri(photoFileName, 0);

            // Add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // If you call startActivityForResult() using an intent that no app can handle, your app
            // will crash. So as long as the result is not null, it's safe to use the intent.
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Start the image capture intent to take photo
                startActivityForResult(intent, MY_PERMISSIONS_REQUEST_OPEN_CAMERA);
            }
        }
    }

    public void onLoadPhotoClick(View view) {
        // Create intent for picking a photo from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a photo
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_PHOTOS);
    }

    public void onRecordVideoClick(View v) {
        // Check permissions
        if (!marshmallowPermission.checkPermissionForCamera()) {
            marshmallowPermission.requestPermissionForCamera();
        } else {
            // create Intent to capture a video and return control to the calling application
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

            // set file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(new Date());
            videoFileName = "VIDEO_" + timeStamp + ".mp4";

            // Create a video file reference
            Uri file_uri = getFileUri(videoFileName, 1);

            // add extended data to the intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file_uri);

            // Start the video record intent to capture video
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_RECORD_VIDEO);
        }
    }

    public void onLoadVideoClick(View view) {
        // Create intent for picking a video from the gallery
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        // Bring up gallery to select a video
        startActivityForResult(intent, MY_PERMISSIONS_REQUEST_READ_VIDEOS);
    }

    public void onSynchroniseClick(View v) {
        synchroniseFiles();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final VideoView mVideoView = findViewById(R.id.videoview);
        ImageView ivPreview = findViewById(R.id.photopreview);

        mVideoView.setVisibility(View.GONE);
        ivPreview.setVisibility(View.GONE);

        if (requestCode == MY_PERMISSIONS_REQUEST_OPEN_CAMERA) {
            if (resultCode == RESULT_OK) {
                // by this point we have the camera photo on disk
                Bitmap takenImage = BitmapFactory.decodeFile(file.getAbsolutePath());
                // Scan the file so it is visible in the gallery
                scanFile(file.getAbsolutePath(), "photos");
                ivPreview.setImageBitmap(takenImage);
                ivPreview.setVisibility(View.VISIBLE);
            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!",
                        Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHOTOS) {
            if (resultCode == RESULT_OK) {
                Uri photoUri = data.getData();
                // Do something with the photo based on Uri
                Bitmap selectedImage;
                try {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),
                            photoUri);

                    // Load the selected image into a preview
                    ivPreview.setImageBitmap(selectedImage);
                    ivPreview.setVisibility(View.VISIBLE);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_VIDEOS) {
            if (resultCode == RESULT_OK) {
                Uri videoUri = data.getData();
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(videoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_RECORD_VIDEO) {
            //if you are running on emulator remove the if statement
            if (resultCode != RESULT_OK) {
                Uri takenVideoUri = getFileUri(videoFileName, 1);
                // Scan the file so it is visible in the gallery
                scanFile(file.getAbsolutePath(), "videos");
                mVideoView.setVisibility(View.VISIBLE);
                mVideoView.setVideoURI(takenVideoUri);
                mVideoView.requestFocus();
                mVideoView.setOnPreparedListener(new OnPreparedListener() {
                    // Close the progress bar and play the video
                    public void onPrepared(MediaPlayer mp) {
                        mVideoView.start();
                    }
                });
            }
        }
    }

    // Returns the Uri for a photo/media stored on disk given the fileName and type
    public Uri getFileUri(String fileName, int type) {
        Uri fileUri = null;
        try {
            File mediaStorageDir = new File(getExternalMediaDirs()[0], APP_TAG);

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
                Log.d(CLASS_TAG, "Failed to create directory");
            }

            // Create the file target for the media based on filename
            file = new File(mediaStorageDir, fileName);


            fileUri = FileProvider.getUriForFile(this.getApplicationContext(),
                    "comp5216.sydney.edu.au.mediarecorder.fileProvider", file);
        } catch (Exception ex) {
            Log.e(CLASS_TAG, ex.getStackTrace().toString());
        }
        return fileUri;
    }


    /**
     * Obtained from Kshitiz Bhargava, Tutorial 5: Location Access
     * Adapted by Tanaka Chitete
     * Accessed 19/09/2023
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Based on code obtained from Kshitiz Bhargava, Tutorial 5: Location Access
     * Adapted by Tanaka Chitete
     * Accessed 19/09/2023
     */
    @SuppressLint("MissingPermission")
    private void getDeviceLocation(Uri uri) {
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this,
                        new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location lastKnownLocation;
                        if (task.isSuccessful()) {
                            // Retrieve the current location of the device
                            lastKnownLocation = task.getResult();

                            Log.d(CLASS_TAG, "Successfully retrieved location: " +
                                    lastKnownLocation.getLatitude() + ", " +
                                    lastKnownLocation.getLongitude());
                        } else {
                            Log.d(CLASS_TAG, "Failed to retrieve location; using default");

                            // Set the current location to the default
                            lastKnownLocation= new Location("");
                            lastKnownLocation.setLatitude(DEFAULT_LATITUDE);
                            lastKnownLocation.setLongitude(DEFAULT_LONGITUDE);
                        }

                        // TODO: Check for WiFi connection
                        mediaQueue.add(new FileEntity(uri, lastKnownLocation));
                        if (mediaQueue.size() == BATCH_SIZE) {
                            ConnectivityManager connectivityManager = (ConnectivityManager)
                                    getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(
                                    ConnectivityManager.TYPE_WIFI);
                            if (!networkInfo.isConnected()) {
                                Toast.makeText(MainActivity.this,
                                        "Connect to WiFi to synchronise",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Connected to WiFi", Toast.LENGTH_SHORT).show();
                                synchroniseFiles();
                            }
                        }
                    }
                });
            } else {
                Log.d(CLASS_TAG, "Failed to retrieve location; using default");
                // Set the current location to the default
                Location defaultLocation = new Location("");
                defaultLocation.setLatitude(DEFAULT_LATITUDE);
                defaultLocation.setLongitude(DEFAULT_LONGITUDE);
                // TODO: Check for WiFi connection
                mediaQueue.add(new FileEntity(uri, defaultLocation));
                if (mediaQueue.size() == BATCH_SIZE) {
                    ConnectivityManager connectivityManager = (ConnectivityManager)
                            getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo =
                            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (!networkInfo.isConnected()) {
                        Toast.makeText(MainActivity.this,
                                "Connect to WiFi to synchronise",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Connected to WiFi", Toast.LENGTH_SHORT).show();
                        synchroniseFiles();
                    }
                }
            }
        } catch (SecurityException securityException)  {
            Log.e(CLASS_TAG, securityException.getMessage());
        }
    }

    public String getCity(Location lastKnownLocation) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lastKnownLocation.getLatitude(),
                    lastKnownLocation.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getLocality(); // This gives the city name
            }
        } catch (IOException ioException) {
            Log.e(CLASS_TAG, ioException.getMessage());
        }
        return "";
    }

    private void scanFile(String path, String type) {
        MediaScannerConnection.scanFile(MainActivity.this,
                new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i(CLASS_TAG, "Successfully scanned file: " + path + " to " + uri);
                        getDeviceLocation(uri);
                    }
                });
    }

    private void synchroniseFiles() {
        while (!mediaQueue.isEmpty()) {
            try {
                FileEntity fileEntity = mediaQueue.remove();
                String encodedFile = encodeFile(this, fileEntity.getUri());
                if (encodedFile == null) {
                    Log.w(CLASS_TAG, "Failed to synchronise null");
                } else {
                    Map<String, Object> contents = new HashMap<>();

                    contents.put("encoded_file", encodedFile);
                    contents.put("latitude", fileEntity.getLastKnownLocation().getLatitude());
                    contents.put("longitude", fileEntity.getLastKnownLocation().getLongitude());

                    if (fileEntity.getUri().getLastPathSegment() != null) {
                        db.collection(getCity(fileEntity.getLastKnownLocation()))
                                .document(fileEntity.getUri().getLastPathSegment())
                                .set(contents)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(CLASS_TAG, "Successfully synchronised file: " +
                                                fileEntity.getUri().getLastPathSegment());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w(CLASS_TAG, "Failed to synchronise file: " +
                                                fileEntity.getUri().getLastPathSegment());
                                    }
                                });
                    } else {
                        Log.w(CLASS_TAG, "Failed to synchronise null");
                    }
                }
            } catch (IOException ioException) {
                Log.w(CLASS_TAG, "Failed to synchronise null");
            }
        }
    }

    private String encodeFile(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                // Read the data into a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                // Convert the byte array to Base64
                byte[] data = byteArrayOutputStream.toByteArray();
                return Base64.getEncoder().encodeToString(data);
            }
        }

        return null;
    }
}
