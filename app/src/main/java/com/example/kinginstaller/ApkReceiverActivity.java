package com.example.kinginstaller;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ApkReceiverActivity extends Activity {
    static final String ACTION_OPEN_APK = "com.example.kinginstaller.action.OPEN_APK";
    static final String EXTRA_APK_URI = "com.example.kinginstaller.extra.APK_URI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forwardToMainActivity(getIntent());
    }

    private void forwardToMainActivity(Intent incoming) {
        Uri uri = extractApkUri(incoming);
        if (uri != null) {
            Intent target = new Intent(this, MainActivity.class);
            target.setAction(ACTION_OPEN_APK);
            target.putExtra(EXTRA_APK_URI, uri);
            target.setClipData(ClipData.newRawUri("apk", uri));
            target.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(target);
        }
        finish();
    }

    private Uri extractApkUri(Intent intent) {
        if (intent == null) return null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())
                || Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())) {
            return intent.getData();
        }
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Object stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream instanceof Uri) {
                return (Uri) stream;
            }
        }
        return null;
    }
}
