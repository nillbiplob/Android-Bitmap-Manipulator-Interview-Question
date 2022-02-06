package com.devskiller.bitmapmanipulation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;

import com.aapbd.appbajarlib.notification.BusyDialog;

public final class MainActivity extends AppCompatActivity {

    private static final int GIVE_GALLERY_PERMISSIONS_REQUEST_CODE = 277;
    private static final int PICK_GALLERY_IMAGE_REQUEST_CODE = 9826;
    private static final String SAMPLE_NOTE = "Biplob note";

    // directory should be document to be saved for the EXIF value.
    private static final String OUTPUT_DIRECTORY_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + File.separator;


    @SuppressWarnings("FieldCanBeLocal")
    private static Location sLondonLocation;

    private static class OpenGalleryClickListener implements View.OnClickListener {

        OpenGalleryClickListener(@NonNull MainActivity parent) {
            mWeakParent = new WeakReference<>(parent);
        }

        private final WeakReference<MainActivity> mWeakParent;

        @Override
        public void onClick(View view) {
            final MainActivity parent = mWeakParent.get();
            if (parent != null) {
                parent.checkPermissionsAndOpenGallery();
            }
        }
    }

    private interface ProcessImageCallback {

        void onSuccess();

        void onFailure();
    }

    static {
        sLondonLocation = new Location(LocationManager.GPS_PROVIDER);
        sLondonLocation.setLatitude(51.5285582);
        sLondonLocation.setLongitude(-0.2416794);
    }

    private ImageProcessor mImageProcessor;

    private AppCompatImageView imageView;

     BusyDialog busyDialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.activity_main_select_image_button).setOnClickListener(new OpenGalleryClickListener(this));

        imageView=findViewById(R.id.activity_main_image_view);

        mImageProcessor = new ImageProcessor(getResources().getInteger(R.integer.image_max_dimension), getContentResolver());


    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults
    ) {
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            if (requestCode == GIVE_GALLERY_PERMISSIONS_REQUEST_CODE) {
                openGallery();
            }
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_GALLERY_IMAGE_REQUEST_CODE) {
                processImage(data);
            }
        }
    }

    private void checkPermissionsAndOpenGallery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    GIVE_GALLERY_PERMISSIONS_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        final Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
        pickImageIntent.setType("image/*");
        startActivityForResult(pickImageIntent, PICK_GALLERY_IMAGE_REQUEST_CODE);
    }

    @SuppressWarnings("ConstantConditions")
    private void processImage(Intent data) {

        busyDialog=new BusyDialog(MainActivity.this, true,"Processing");
        busyDialog.show();


        shareImage(data.getData(), OUTPUT_DIRECTORY_PATH, SAMPLE_NOTE, sLondonLocation, new ProcessImageCallback() {

            @Override
            public void onSuccess() {

                Log.e("shareImage", "onSuccess called");




                showMessage(R.string.share_image_success);
            }

            @Override
            public void onFailure() {
                showMessage(R.string.process_image_failure);

            }
         });
    }

    private void showMessage(@StringRes int messageResource) {
        Toast.makeText(this, messageResource, Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("SameParameterValue")
    private void shareImage(
        final @NonNull Uri imageUri,
        final @NonNull String outputDirectoryPath,
        final @NonNull String note,
        final @NonNull Location location,
        final @NonNull ProcessImageCallback callback
    ) {
        mImageProcessor.processImage(this,imageUri, outputDirectoryPath, note, location, new ImageProcessor.ProcessImageCallback() {



            @Override
            public void onSuccess(@NonNull String outputPath) {

                Log.e("processImage", "onSuccess called");

                if(busyDialog!=null)
                busyDialog.dismis();


                showTheImageIntoUI(outputPath);

                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/jpeg");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(outputPath));
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)));

                callback.onSuccess();
            }


            @Override
            public void onFailure() {
                callback.onFailure();
                if(busyDialog!=null)
                    busyDialog.dismis();

            }
        });
    }


    private void showTheImageIntoUI(String outputPath) {

        Log.e("showTheImageIntoUI", " path is "+outputPath);

        File imgFile = new File(outputPath);
        if(imgFile.exists())
        {
            Log.e("showTheImageIntoUI", " File is exists");

            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);
        }


    }
}
