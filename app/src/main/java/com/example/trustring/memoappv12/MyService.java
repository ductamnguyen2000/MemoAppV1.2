package com.example.trustring.memoappv12;


import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;


public class MyService extends Service {
    private ContentResolver contentResolver = null;
    HandlerThread handlerThread = new HandlerThread("content_observer");
    Handler handler = new Handler();
    String TAG = "ABC";
    private static final String EXTERNAL_CONTENT_URI_MATCHER =
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
    private static final String[] PROJECTION = new String[]{
            MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED
    };
    private static final String SORT_ORDER = MediaStore.Images.Media.DATE_ADDED + " DESC";

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("content_observer");
        handlerThread.start();
        // Thread start ============================================================
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
        final ContentResolver contentResolver = getApplicationContext().getContentResolver();
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                new ContentObserver(handler) {
                    @Override
                    public boolean deliverSelfNotifications() {
                        Log.d(TAG, "deliverSelfNotifications");
                        return super.deliverSelfNotifications();
                    }

                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                    }

                    @Override
                    public void onChange(boolean selfChange, Uri uri) {

                        Log.d(TAG, "onChange: " + selfChange + ", " + uri.toString());
                        if (uri.toString().startsWith(EXTERNAL_CONTENT_URI_MATCHER)) {
                            Cursor cursor = null;
                            try {
                                cursor = contentResolver.query(uri, PROJECTION, null, null,
                                        SORT_ORDER);
                                Log.d(TAG, "try: 1");
                                if (cursor != null && cursor.moveToFirst()) {
                                    String path = cursor.getString(
                                            cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                                    long dateAdded = cursor.getLong(cursor.getColumnIndex(
                                            MediaStore.Images.Media.DATE_ADDED));
                                    long currentTime = System.currentTimeMillis() / 1000;
                                    Log.d(TAG, "path: " + path + ", dateAdded: " + dateAdded +
                                            ", currentTime: " + currentTime);
                                    if (dateAdded == currentTime) {
                                        Intent dialogIntent = new Intent(MyService.this, MakeMemoActivity.class);
                                        dialogIntent.putExtra("path", path);
                                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                       // dialogIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(dialogIntent);
                                                                           }


                                    Log.d(TAG, "Main Activiy IF outside");
                                }
                            } catch (Exception e) {
                                Log.d(TAG, "open cursor fail" + e.toString());
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                        super.onChange(selfChange, uri);
                    }
                }
        );
        // End thread

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }


}