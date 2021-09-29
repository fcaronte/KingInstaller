package com.example.kinginstaller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            checkManageExternalStoragePermission();
        } catch (Exception e) {
            TextView tv = findViewById(R.id.textViewError);
            tv.setText(e.toString());
        }
        Button btnSelect = findViewById(R.id.selectButton);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    showFileChooser();
                } catch (Exception e) {
                    TextView tv = findViewById(R.id.textViewError);
                    tv.setText(e.toString());
                }
            }
        });

        Button btnInstall = findViewById(R.id.installButton);
        btnInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    installAsKing();
                } catch (Exception e) {
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
        return true;
    }


    private void installAsKing() {
        try {
            EditText et = findViewById(R.id.pathTextEdit);
            String filepath = et.getText().toString();
            if (filepath.length() == 0) {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
                return;
            }
            File myFile = new File(filepath);
            if (!myFile.exists()) {
                Toast.makeText(this, "Error: file not exists", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent("android.intent.action.INSTALL_PACKAGE");
            Uri fileUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName()+".provider", myFile);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK+1);
            } else {
                fileUri = Uri.fromFile(myFile);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  // Constant Value: 268435456 (0x10000000)
            }
            intent.setData(fileUri);

            intent.putExtra("android.intent.extra.NOT_UNKNOWN_SOURCE", true);
            intent.putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", "com.android.vending");
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
}