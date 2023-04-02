package com.example.kinginstaller;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    boolean oppoTrickEnabled, rootTrickEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try { checkManageExternalStoragePermission(); }
        catch (Exception e) {
            TextView tv = findViewById(R.id.textViewError);
            tv.setText(e.toString());
        }
        Button btnSelect = findViewById(R.id.selectButton);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try { showFileChooser(); }
                catch (Exception e) {
                    TextView tv = findViewById(R.id.textViewError);
                    tv.setText(e.toString());
                }
            }
        });

        TextView siteAnnexhack = findViewById(R.id.site_annexhack);
        siteAnnexhack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String url = "https://inceptive.ru";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (Exception e) {
                    TextView tv = findViewById(R.id.textViewError);
                    tv.setText(e.toString());
                }
            }
        });

        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        SharedPreferences oppoTrickStatus = getSharedPreferences("oppo_trick_value", Activity.MODE_PRIVATE);
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value",false);
        CheckBox oppoTrick = (CheckBox) findViewById(R.id.checkBox1);
        oppoTrick.setChecked(oppoTrickEnabled);
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        SharedPreferences rootTrickStatus = getSharedPreferences("root_trick_value", Activity.MODE_PRIVATE);
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value",false);
        CheckBox rootTrick = (CheckBox) findViewById(R.id.checkBox2);
        rootTrick.setChecked(rootTrickEnabled);
        oppoTrick();

        oppoTrick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                oppoTrickEnabled = !oppoTrickEnabled;
                SharedPreferences.Editor oppoEditor = oppoTrickStatus.edit();
                oppoEditor.putBoolean("oppo_trick_value", oppoTrickEnabled);
                oppoEditor.apply();
                oppoTrick.setChecked(oppoTrickEnabled);
                //Switch off root flags
                SharedPreferences.Editor rootEditor = rootTrickStatus.edit();
                rootEditor.putBoolean("root_trick_value", false);
                rootEditor.apply();
                rootTrick.setChecked(false);
                oppoTrick();

                Log.d("oppo button", "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false));
                Log.d("root button", "root value is " + rootTrickStatus.getBoolean("root_trick_value", false));
            }
        });

        rootTrick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDeviceRooted();
                if (!isDeviceRooted()) {
                    Toast.makeText(getBaseContext(), R.string.device_not_rooted, Toast.LENGTH_SHORT).show();
                    //Switch off root flags
                    SharedPreferences.Editor rootEditor = rootTrickStatus.edit();
                    rootEditor.putBoolean("root_trick_value", false);
                    rootEditor.apply();
                    rootTrick.setChecked(false);
                } else {
                    rootTrickEnabled = !rootTrickEnabled;
                    SharedPreferences.Editor rootEditor = rootTrickStatus.edit();
                    rootEditor.putBoolean("root_trick_value", rootTrickEnabled);
                    rootEditor.apply();
                    rootTrick.setChecked(rootTrickEnabled);
                    //Switch off oppo flags
                    SharedPreferences.Editor oppoEditor = oppoTrickStatus.edit();
                    oppoEditor.putBoolean("oppo_trick_value", false);
                    oppoEditor.apply();
                    oppoTrick.setChecked(false);
                    oppoTrick();

                }
                Log.d("root check", "is phone rooted " + isDeviceRooted());
                Log.d("oppo button", "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false));
                Log.d("root button", "root value is " + rootTrickStatus.getBoolean("root_trick_value", false));
            }
        });

        Button btnInstall = findViewById(R.id.installButton);
        btnInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences oppoTrickStatus = getSharedPreferences("oppo_trick_value", Activity.MODE_PRIVATE);
                oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value",false);
                SharedPreferences rootTrickStatus = getSharedPreferences("root_trick_value", Activity.MODE_PRIVATE);
                rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value",false);
                try {
                    if (rootTrickEnabled) { installAsRoot(); }
                    else installAsKing();
                }
                catch (Exception e) {
                    TextView tv = findViewById(R.id.textViewError);
                    tv.setText(e.toString());
                }
            }
        });

        //RESET BUTTON TO OPEN DEFAULT PACKAGE INSTALLER TO CAN CLEAR AS DEFAULT SETTING
        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + "com.google.android.packageinstaller"));
                    startActivity(intent);
                } catch (Exception e) {
                    TextView tv = findViewById(R.id.textViewError);
                    tv.setText(e.toString());
                }
            }
        });
    }

    public void oppoTrick() {
        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        SharedPreferences oppoTrickStatus = getSharedPreferences("oppo_trick_value", Activity.MODE_PRIVATE);
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value",false);
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        SharedPreferences rootTrickStatus = getSharedPreferences("root_trick_value", Activity.MODE_PRIVATE);
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value",false);
        PackageManager pm = getApplicationContext().getPackageManager();
        if (oppoTrickEnabled) {
            ComponentName oppoTrickFlagged =
                    new ComponentName(getPackageName(), getPackageName() + ".OppoTrick");
            pm.setComponentEnabledSetting(
                    oppoTrickFlagged,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            ComponentName oppoTrickFlagged =
                    new ComponentName(getPackageName(), getPackageName() + ".OppoTrick");
            pm.setComponentEnabledSetting(
                    oppoTrickFlagged,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        SharedPreferences.Editor oppoEditor = oppoTrickStatus.edit();
        oppoEditor.putBoolean("oppo_trick_value", oppoTrickEnabled);
        oppoEditor.apply();
        SharedPreferences.Editor rootEditor = rootTrickStatus.edit();
        rootEditor.putBoolean("root_trick_value", rootTrickEnabled);
        rootEditor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_info_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_search) {
            String url = "https://gitlab.com/annexhack/king-installer";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        if(item.getItemId() == R.id.action_search2) {
            String url = "https://github.com/fcaronte/KingInstaller";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        if(item.getItemId() == R.id.action_search3) {
            String url = "https://github.com/Rikj000/KingInstaller";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }
        return true;
    }

    private void installAsRoot() {
        try {
            EditText et = findViewById(R.id.pathTextEdit);
            String filepath = et.getText().toString();
            runSuWithCmd("pm install -t -i \"com.android.vending\" -r " + filepath);
            et.setText("");
            TextView tv = findViewById(R.id.textViewError);
            tv.setText("");
        } catch (Exception e) {
            TextView tv = findViewById(R.id.textViewError);
            tv.setText(e.toString());
        }
    }

    private void installAsKing() {
        try {
            EditText et = findViewById(R.id.pathTextEdit);
            String filepath = et.getText().toString();
            if (filepath.length() == 0) {
                Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show();
                return;
            }
            File myFile = new File(filepath);
            if (!myFile.exists()) {
                Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            Uri fileUri;
            fileUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", myFile);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(fileUri);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending");
            et.setText("");
            TextView tv = findViewById(R.id.textViewError);
            tv.setText("");
            startActivity(intent);
        } catch (Exception e) {
            TextView tv = findViewById(R.id.textViewError);
            tv.setText(e.toString());
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        requestPermissions();
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select APK"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {

                    Uri uri = data.getData();
                    String path = copyFileToInternalStorage(uri, "apk");

                    EditText et = findViewById(R.id.pathTextEdit);
                    et.setText(path);
                }
                break;
            case PERMISSION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestPermissions();
                    }
                }
        }
    }

    public final void clearTempFile() {
        File[] listFiles;
        Context applicationContext = getApplicationContext();
        File file = new File(applicationContext.getFilesDir() + "/apk");
        if (!file.exists() || !file.isDirectory() || (listFiles = file.listFiles()) == null) {
            return;
        }
        for (File file2 : listFiles) {
            file2.delete();
        }
    }
    public void onDestroy() {
        super.onDestroy();
        try {
            clearTempFile();
        } catch (Throwable ignored) {
        }
    }

    private void checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // android 11 has new readFiles request permission
            if (Environment.isExternalStorageManager()) {
                return;
            } else {
                if (Environment.isExternalStorageLegacy()) {
                    return;
                }
                try {
                    Intent intent = new Intent();
                    intent.setAction(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:"+getApplicationContext().getPackageName()));
                    startActivityForResult(intent, RESULT_OK); //result code is just an int
                    return;
                } catch (Exception e) {
                    return;
                }
            }
        } else { // android 10 and lower - classic request
            requestPermissions();
        }
    }

    private String copyFileToInternalStorage(Uri uri, String newDirName) {
        Uri returnUri = uri;

        Context mContext = getApplicationContext();
        Cursor returnCursor = mContext.getContentResolver().query(returnUri, new String[]{ OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
        }, null, null, null);


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));

        File output;
        if (!newDirName.equals("")) {
            File dir = new File(mContext.getFilesDir() + "/" + newDirName);
            if (!dir.exists()) {
                dir.mkdir();
            }
            output = new File(mContext.getFilesDir() + "/" + newDirName + "/" + name);
        } else {
            output = new File(mContext.getFilesDir() + "/" + name);
        }
        try {
            InputStream inputStream = mContext.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(output);
            int read = 0;
            int bufferSize = 1024;
            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {

//            L.e("Exception", e.getMessage());
        }

        return output.getPath();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE); //permission request code is just an int
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE); //permisison request code is just an int
        }
    }

    /**
     * https://github.com/shmykelsa/AA-Tweaker/blob/4d03205f14b2938f96bf04e198dd067cd6fe0967/app/src/main/java/sksa/aa/tweaker/MainActivity.java#L3964
     * @param cmd
     * @return
     */
    public static StreamLogs runSuWithCmd(String cmd) {
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        InputStream errorStream = null;

        StreamLogs streamLogs = new StreamLogs();
        streamLogs.setOutputStreamLog(cmd);

        try {
            Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            inputStream = su.getInputStream();
            errorStream = su.getErrorStream();

            outputStream.writeBytes(cmd + "\n");
            outputStream.flush();
            outputStream.writeBytes("exit\n");
            outputStream.flush();

            try { su.waitFor(); }
            catch (InterruptedException e) { e.printStackTrace(); }
            streamLogs.setInputStreamLog(readStream(inputStream));
            streamLogs.setErrorStreamLog(readStream(errorStream));
        } catch (IOException e) { e.printStackTrace(); }

        return streamLogs;
    }

    public static String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, length);
        }
        return byteArrayOutputStream.toString("UTF-8");
    }
    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() ||
                checkRootMethod3();
    }
    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }
    private static boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] {
                    "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
