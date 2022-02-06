package com.devskiller.bitmapmanipulation;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.aapbd.appbajarlib.image.BitmapUtils;
import com.arasthel.asyncjob.AsyncJob;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

final class ImageProcessor {

    private static final int COMPRESS_QUALITY_HINT = 100;

    interface ProcessImageCallback {

        void onSuccess(@NonNull String outputPath);

        void onFailure();
    }

    private final ExecutorService mBackgroundExecutorService = Executors.newSingleThreadExecutor();
    @SuppressWarnings("FieldCanBeLocal")
    private final int mMaxDimension;
    @SuppressWarnings("FieldCanBeLocal")
    private final ContentResolver mContentResolver;

    ImageProcessor(int maxDimension, @NonNull ContentResolver contentResolver) {
        mMaxDimension = maxDimension;
        mContentResolver = contentResolver;
    }

    @SuppressWarnings("ConstantConditions")
    void processImage(
            final Context mainActivity, final @NonNull Uri streamSource,
            final @NonNull String outputDirectoryPath,
            final @NonNull String note,
            final @NonNull Location location,
            final @NonNull ProcessImageCallback callback
    ) {
        AsyncJob.doInBackground(new AsyncJob.OnBackgroundJob() {

            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void doOnBackground() {
                final String fileName = "bitmapmanipulator_" + System.currentTimeMillis() + ".jpeg";
                new File(outputDirectoryPath).mkdirs();
                final String outputPath = (outputDirectoryPath + fileName);

                Log.e("Output file path", outputPath);

                Bitmap bitmap=getContactBitmapFromURI(mainActivity,streamSource);

                Log.e("Bitmap found after", "getContactBitmapFromURI");


                bitmap=BitmapUtils.getResizedBitmap(bitmap, mMaxDimension);

                Log.e("Bitmap found after", "getResizedBitmap");


                // inverse bitmap color

                bitmap=  inverseBitmapColors(bitmap);

                Log.e("Bitmap found after", "inverseBitmapColors");


                File file= saveBitmapIntoSDCardImage(mainActivity, outputPath,bitmap);

                Log.e("Bitmap File found after", "saveBitmapIntoSDCardImage");



                if(file != null)
                {
                    try {
                        addExtraData(outputPath,note,location);

                        Log.e("Bitmap File found after", "addExtraData");


                        callback.onSuccess(outputPath);

                    } catch (IOException e) {
                        e.printStackTrace();

                        callback.onFailure();
                    }


                }else
                {

                    Log.e("Bitmap File ", "Not Found");

                    callback.onFailure();
                }


                //START CHANGES
                //END CHANGES
            }
        }, mBackgroundExecutorService);
    }

    private int calculateInSampleSize(
        final @NonNull BitmapFactory.Options options,
        int maxDimension
    ) {
        //START CHANGES
        return 0;
        //END CHANGES
    }

    private void addExtraData(
        final @NonNull String bitmapPath,
        final @NonNull String note,
        final @NonNull Location location
    ) throws IOException {
        //START CHANGES
        //END CHANGES


        ExifInterface exif = new ExifInterface(bitmapPath);
        exif.setAttribute("description", note);
        exif.saveAttributes();

        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                String.valueOf(location.getLatitude()));

        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                String.valueOf(location.getLongitude()));

        exif.saveAttributes();

        Log.d("exif value","added");
    }

    private void callCallbackOnMainThread(@NonNull AsyncJob.OnMainThreadJob job) {
        AsyncJob.doOnMainThread(job);
    }

    public Bitmap getContactBitmapFromURI(Context context, Uri uri) {
        try {

            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) {
                return null;
            }
            return BitmapFactory.decodeStream(input);
        }
        catch (FileNotFoundException e)
        {

        }
        return null;

    }

    public  File saveBitmapIntoSDCardImage(Context context, String outputPath, Bitmap finalBitmap) {


        File file = new File (outputPath);

        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }


    public Bitmap inverseBitmapColors(Bitmap src)
    {
        int height = src.getHeight();
        int width = src.getWidth();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();

        ColorMatrix matrixGrayscale = new ColorMatrix();
        matrixGrayscale.setSaturation(0);

        ColorMatrix matrixInvert = new ColorMatrix();
        matrixInvert.set(new float[]
                {
                        -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                        0.0f, 0.0f, 0.0f, 1.0f, 0.0f
                });
        matrixInvert.preConcat(matrixGrayscale);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrixInvert);
        paint.setColorFilter(filter);

        canvas.drawBitmap(src, 0, 0, paint);
        return bitmap;
    }

}
