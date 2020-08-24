package jp.anzx.stsclient;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Perms {

    private static final String TAG = "Perms";

    //manifest permissions
    public final static String[] PERMS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE
    };

    //manifest permission codes
    public final static int WRITE_EXTERNAL_STORAGE = 0;
    public final static int READ_EXTERNAL_STORAGE = 1;
    public final static int FOREGROUND_SERVICE = 2;

    //permission codes
    public final static int OVERLAY_PERMISSION = 133;
    public final static int MEDIA_PROJECTION = 136;

    private Activity activity;

    //media projection data
    public static Integer mediaProjectionResultCode;
    public static Intent mediaProjectionData;

    public Perms(Activity act){
        activity = act;
    }

    private boolean hasPermission(int permCode) {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, PERMS[permCode]));
    }

    public void requestManifestPermission(int permCode){

        String permName = PERMS[permCode];

        if (ContextCompat.checkSelfPermission(
                activity, permName) ==
                PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            Log.i(TAG, permName + "already gained");
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            ActivityCompat.requestPermissions(activity, new String[]{permName}, permCode);
        }

    }

    public void requestOverlay(){
        //Overlay
        if (!Settings.canDrawOverlays(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, OVERLAY_PERMISSION);
        }else{
            Log.i(TAG, "already has overlay permission");
        }
    }

    public void requestMediaProjection(){

        if (Perms.mediaProjectionResultCode == null || Perms.mediaProjectionData == null ){

            MediaProjectionManager mProjectionManager = (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            activity.startActivityForResult(mProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION);

        }else{
            Log.i(TAG, "already has screencapture permission");
        }
    }

    // activity -> onActivityResult
    //

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(activity)) {
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Permission overlay gained");
                }
                else{
                    Log.d(TAG, "Permission overlay denied");
                }
            }
        }

        if (requestCode == MEDIA_PROJECTION){
            if (resultCode == Activity.RESULT_OK) {

                Perms.mediaProjectionResultCode = resultCode;
                Perms.mediaProjectionData = data;

                Log.i(TAG, "Permission screencapture gained");
            }
            else{
                Log.d(TAG, "Permission screencapture denied");
            }
        }

    }

    // activity -> onRequestPermissionsResult
    //
    public boolean IsManifestPermissionGot(int requestCode, String[] permissions, int[] grantResults) {

        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            // permission was granted, yay! Do the
            // contacts-related task you need to do.
            Log.i(TAG, "Permission " + PERMS[requestCode] + " granted");
            return true;
        } else {

            // permission denied, boo! Disable the
            // functionality that depends on this permission.
            //Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Permission " + PERMS[requestCode] + " denied");
            return false;
        }
    }

}
