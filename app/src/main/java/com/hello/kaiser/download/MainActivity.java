package com.hello.kaiser.download;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button mButton;
    private ImageView mImageView;
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutProgress;
    private TextView mTxtProgress;

    private DownloadManager DM;
    private DownloadManager.Request request;
    private long downloadID;
    private DownloadManager.Query query;
    BroadcastReceiver receiver;
    static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initSet();
        initListener();
    }

    private void initView() {
        mButton = (Button) findViewById(R.id.bt_button);
        mImageView = (ImageView) findViewById(R.id.iv_image);
        mProgressBar = (ProgressBar) findViewById(R.id.pg_progress);
        mLayoutProgress = (LinearLayout) findViewById(R.id.lm_layout_progress);
        mTxtProgress = (TextView) findViewById(R.id.tv_txtprogress);
    }

    private void initSet() {
        DM = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    }

    private void initListener() {
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    //確認版本後進入
                    checkPermission();
                } else {
                    downloadStuff();
                }
            }
        });
    }

    private void downloadStuff() {


        mLayoutProgress.setVisibility(View.VISIBLE);
        String URL = "https://dl.dropboxusercontent.com/s/kkvs6zw8k50uc8m/android-nougat.png";
        request = new DownloadManager.Request(Uri.parse(URL));
        //創建目錄
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdir();
        //設定APK儲存位置
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DG_App.apk");

        request.setTitle("ImageDownload");
        request.setDescription("please wait for download...");
        downloadID = DM.enqueue(request);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    query = new DownloadManager.Query();
                    query.setFilterById(downloadID);
                    query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
                    Cursor cur = DM.query(query);
                    if (cur != null) {
                        if (cur.moveToFirst()) {
                            String url = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            if (!TextUtils.isEmpty(url)) {
                                mImageView.setImageURI(Uri.parse(url));
                                mButton.setVisibility(View.GONE);
                                Toast.makeText(context, "download Success", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        DownloadObserver download = new DownloadObserver(null);
        getContentResolver().registerContentObserver(CONTENT_URI, true, download);
    }

    private void checkPermission() {
        int readPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (readPermission != PackageManager.PERMISSION_GRANTED && writePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1999);
        } else {
            downloadStuff();
        }
    }

    class DownloadObserver extends ContentObserver {
        public DownloadObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadID);
            Cursor cursor = DM.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                final int totalColum = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                final int currentColumn = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                Log.d(TAG, "totalColum = " + totalColum);
                Log.d(TAG, "currentColumn = " + currentColumn);
                int totalSize = cursor.getInt(totalColum);
                int currentSize = cursor.getInt(currentColumn);
                Log.d(TAG, "totalSize = " + totalSize);
                Log.d(TAG, "currentSize = " + currentSize);
                float percent = (float) currentSize / (float) totalSize;
                Log.d(TAG, "percent = " + percent);

                final int progress = Math.round(percent * 100);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mTxtProgress.setText("" + progress + "%");
                        mProgressBar.setProgress(progress);
                    }
                });

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
    }
}











