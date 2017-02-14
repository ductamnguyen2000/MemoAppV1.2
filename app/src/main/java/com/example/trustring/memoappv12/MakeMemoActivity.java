package com.example.trustring.memoappv12;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MakeMemoActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    //GOOGLE API ---------------
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyBYzU5on1xa4H41-RSr_QJcLuBKqrpSM_c";
    private static final String SHA1 = "1f8c0943c968922f883b83a316fd0e66b7109187";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = MainActivity.class.getSimpleName();
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR, DriveScopes.DRIVE
    };
    //END
    public static final String IMAGE_TYPE = "image/*";
    String path = null;
    String imageContent = "";
    String labelContent = "";
    boolean checkFirstTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String TAG = "ABCD";
        Log.d(TAG, "onCreate: chay vao on Create");
        checkFirstTime = true;

        super.onCreate(savedInstanceState);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google API ...");

        //setContentView(R.layout.activity_make_memo);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        getResultsFromApi();
        Log.d(TAG, "onCreate: chay dc on Create");
    }

    @Override
    protected void onStart() {


        super.onStart();
    }


    @Override
    protected void onResume() {
        super.onResume();
//finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ABC", "onDestroy: ");
    }




    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("ABCDEF", "onActivityResult " + requestCode + " result" + resultCode);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();//
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;

        }

    }

    //GOOGLE API

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            Log.d("ABCD", "getResultsFromApi: Xin lenh");
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            Log.d("ABCD", "getResultsFromApi: chon account");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            // mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */


    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     *
     * @param requestCode The request code associated with the requested
     *                    permission
     * @param list        The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MakeMemoActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Drive mServicedrive = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Android memoappv12")
                    .build();
            mServicedrive = new Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Android memoappv12")
                    .build();
            Log.d("ABC", "MakeRequestTask ");
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                Log.d("ABC", "List<String> getDataFromApi() ");

                Intent getputIntent = getIntent();
                path = getputIntent.getStringExtra("path");
                googleCloudAPI(Uri.fromFile(new java.io.File(path)));
                return null;
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         *
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            Intent getputIntent = getIntent();
            path = getputIntent.getStringExtra("path");
            // Upload file capture to Google Drive
            File fileMetadata = new File();
            File file = new File();
            String urlFile = null;
            if (path != null) {

                fileMetadata.setName("Capture -" + System.currentTimeMillis());
                fileMetadata.setMimeType("image/png");
                //fileMetadata.setParents(Collections.singletonList("0BwwA4oUTeiV1TGRPeTVjaWRDY1E"));
                Log.d("ABC", "getDataFromApi: " + path);
                java.io.File filePath = new java.io.File(path);
                FileContent mediaContent = new FileContent("image/png", filePath);
                file = mServicedrive.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                System.out.println("File ID: " + file.getId());
                urlFile="https://drive.google.com/file/d/" + file.getId() + "/view?usp=drive_web";
            }else return null;
            //End upload
            Log.d("ABC", "getDataFromApi: Image " + path);
            Log.d("ABCD", "getDataFromApi: Bat dau ");
            Event event = new Event()
                    .setSummary("SCHEPPA " + labelContent)
                    .setDescription("【写真の文字】\n【START】"+imageContent +"【END】\n This photo was taken at " + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(Calendar.getInstance().getTime())+'\n'+urlFile );

            DateTime startDateTime = new DateTime(new Date());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            event.setStart(start);

            DateTime endDateTime = new DateTime(new Date());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            String calendarId = "primary";
            // Google Drive UP
            if (path != null) {
                List<EventAttachment> attachments = new ArrayList<EventAttachment>();

                attachments.add(new EventAttachment()
                        .setFileUrl("https://drive.google.com/file/d/" + file.getId() + "/view?usp=drive_web")
                        .setFileId(file.getId())
                        .setMimeType(file.getMimeType())
                        .setTitle(file.getName()));
                event.setAttachments(attachments);
                event = mService.events().insert(calendarId, event).setSupportsAttachments(true).execute();
            } else {
                Log.d("ABCD", "getDataFromApi: khong co anh ");
                event = mService.events().insert(calendarId, event).execute();
            }
            System.out.printf("Event created: %s\n", event.getHtmlLink());
            //finish();
            getputIntent.removeExtra("path");
            return null;
        }

        private void googleCloudAPI(final Uri uri ) throws IOException {
            try {
                HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                VisionRequestInitializer requestInitializer =
                        new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                            /**
                             * We override this so we can inject important identifying fields into the HTTP
                             * headers. This enables use of a restricted cloud platform API key.
                             */
                            @Override
                            protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                    throws IOException {
                                super.initializeVisionRequest(visionRequest);

                                String packageName = getPackageName();
                                visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                //String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, SHA1);
                            }
                        };

                Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                builder.setVisionRequestInitializer(requestInitializer);

                Vision vision = builder.build();

                BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                        new BatchAnnotateImagesRequest();
                batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                    // Add the image
                    Image base64EncodedImage = new Image();
                    // Convert the bitmap to a JPEG
                    // Just in case it's a format that Android understands but Cloud Vision
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    scaleBitmapDown(
                            MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                            1200).compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                    byte[] imageBytes = byteArrayOutputStream.toByteArray();

                    // Base64 encode the JPEG
                    base64EncodedImage.encodeContent(imageBytes);
                    annotateImageRequest.setImage(base64EncodedImage);

                    // add the features we want
                    annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                        Feature labelDetection = new Feature();
                        labelDetection.setType("LABEL_DETECTION");
                        labelDetection.setMaxResults(3);

                        Feature textDetection = new Feature();
                        textDetection.setType("TEXT_DETECTION");
                        add(labelDetection);
                        add(textDetection);
                    }});

                    // Add the list of one thing to the request
                    add(annotateImageRequest);
                }});

                Vision.Images.Annotate annotateRequest =
                        vision.images().annotate(batchAnnotateImagesRequest);
                // Due to a bug: requests to Vision API containing large images fail when GZipped.
                annotateRequest.setDisableGZipContent(true);
                Log.d("ABC", "created Cloud Vision request object, sending request");

                BatchAnnotateImagesResponse response = annotateRequest.execute();
                imageContent = convertResponseToString(response);
                getDataFromApi();

            } catch (GoogleJsonResponseException e) {
                Log.d("ABC", "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d("ABC", "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            Log.d("ABC", "googleCloudAPI: Cloud Vision API request failed. Check logs for details.");
        }

        @Override
        protected void onPreExecute() {
            Log.d("ABC", "onPreExecute: ");
            // mOutputText.setText("");
            mProgress.show();
            //mProgress.hide();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                //  mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Google Calendar API:");
                // mOutputText.setText(TextUtils.join("\n", output));
            }
            //mProgress.dismiss();
            Log.d("ABC", "onPostExecute: ");


            finish();
            finishAffinity();
            //System.exit(0);
            //android.os.Process.killProcess(android.os.Process.myPid());
        }

        @Override
        protected void onCancelled() {
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MakeMemoActivity.REQUEST_AUTHORIZATION);
                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
                //  mOutputText.setText("Request cancelled.");
            }
        }
    }

    public String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }
    public Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message = ""; //"I found these things:\n\n";

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {

            for (EntityAnnotation label : labels) {
                labelContent += " - "+label.getDescription();
            }
        } else {
            message += "nothing";
        }
        List<EntityAnnotation> textcont = response.getResponses().get(0).getTextAnnotations();
        message += "\n"+textcont.get(0).getDescription();
        return message;
    }
}
