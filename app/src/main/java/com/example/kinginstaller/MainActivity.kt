package com.example.kinginstaller

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.color.DynamicColors
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import androidx.core.net.toUri
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    var oppoTrickEnabled: Boolean = false
    var rootTrickEnabled: Boolean = false
    var forceRootEnabled: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            checkManageExternalStoragePermission()
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = e.toString()
        }
        if (isGooglePackageExist) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.setText(R.string.google_package_installer_is_installed)
        } else {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.setText(R.string.missing_google_package_installer)
        }
        val btnSelect = findViewById<Button>(R.id.selectButton)
        btnSelect.setOnClickListener {
            try {
                showFileChooser()
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = e.toString()
            }
        }

        val siteAnnexhack = findViewById<TextView>(R.id.site_annexhack)
        siteAnnexhack.setOnClickListener {
            try {
                val url = "https://inceptive.ru"
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(url.toUri())
                startActivity(i)
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = e.toString()
            }
        }

        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        val oppoTrickStatus = getSharedPreferences("oppo_trick_value", MODE_PRIVATE)
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value", false)
        val oppoTrick = findViewById<View?>(R.id.checkBox1) as CheckBox
        oppoTrick.setChecked(oppoTrickEnabled)
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        val rootTrickStatus = getSharedPreferences("root_trick_value", MODE_PRIVATE)
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value", false)
        val rootTrick = findViewById<View?>(R.id.checkBox2) as CheckBox
        rootTrick.setChecked(rootTrickEnabled)
        oppoTrick()

        oppoTrick.setOnClickListener {
            oppoTrickEnabled = !oppoTrickEnabled
            oppoTrickStatus.edit {
                putBoolean("oppo_trick_value", oppoTrickEnabled)
            }
            oppoTrick.setChecked(oppoTrickEnabled)
            //Switch off root flags
            rootTrickStatus.edit {
                putBoolean("root_trick_value", false)
            }
            rootTrick.setChecked(false)
            oppoTrick()

            Log.d(
                "oppo button",
                "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false)
            )
            Log.d(
                "root button",
                "root value is " + rootTrickStatus.getBoolean("root_trick_value", false)
            )
        }

        rootTrick.setOnClickListener {
            isDeviceRooted
            val tv = findViewById<TextView>(R.id.textViewError)
            if (!isDeviceRooted) {
                Toast.makeText(baseContext, R.string.device_not_rooted, Toast.LENGTH_SHORT)
                    .show()
                //Switch off root flags
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", false)
                }
                rootTrick.setChecked(false)
            } else if (isGooglePackageExist && !forceRootEnabled) {
                tv.setText(R.string.root_method_warning)
                //Switch off root flags
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", false)
                }
                rootTrick.setChecked(false)
                forceRootEnabled = true
            } else {
                tv.text = ""
                forceRootEnabled = !rootTrickEnabled
                rootTrickEnabled = !rootTrickEnabled
                rootTrickStatus.edit {
                    putBoolean("root_trick_value", rootTrickEnabled)
                }
                rootTrick.setChecked(rootTrickEnabled)
                //Switch off oppo flags
                oppoTrickStatus.edit {
                    putBoolean("oppo_trick_value", false)
                }
                oppoTrick.setChecked(false)
                oppoTrick()
            }
            Log.d("root check", "is phone rooted $isDeviceRooted")
            Log.d(
                "oppo button",
                "oppo value is " + oppoTrickStatus.getBoolean("oppo_trick_value", false)
            )
            Log.d(
                "root button",
                "root value is " + rootTrickStatus.getBoolean("root_trick_value", false)
            )
        }

        val btnInstall = findViewById<Button>(R.id.installButton)
        btnInstall.setOnClickListener {
            val oppoTrickStatus = getSharedPreferences("oppo_trick_value", MODE_PRIVATE)
            oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value", false)
            val rootTrickStatus = getSharedPreferences("root_trick_value", MODE_PRIVATE)
            rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value", false)
            try {
                if (rootTrickEnabled) {
                    installAsRoot()
                } else installAsKing()
            } catch (e: Exception) {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.text = e.toString()
            }
        }

        //RESET BUTTON TO OPEN DEFAULT PACKAGE INSTALLER TO CAN CLEAR AS DEFAULT SETTING
        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.setOnClickListener {
            if (isGooglePackageExist) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.setData(("package:" + "com.google.android.packageinstaller").toUri())
                    startActivity(intent)
                } catch (e: Exception) {
                    val tv = findViewById<TextView>(R.id.textViewError)
                    tv.text = e.toString()
                }
            } else {
                val tv = findViewById<TextView>(R.id.textViewError)
                tv.setText(R.string.missing_google_package_installer)
            }
        }
    }

    val isGooglePackageExist: Boolean
        //CHECK IF GOOGLE PACKAGE INSTALLER EXIST ON YOUR DEVICE
        get() {
            val pm = packageManager
            try {
                val info = pm.getPackageInfo(
                    "com.google.android.packageinstaller",
                    PackageManager.GET_META_DATA
                )
            } catch (e: PackageManager.NameNotFoundException) {
                return false
            }
            return true
        }

    fun oppoTrick() {
        //MAKE OPPO TRICK DISABLED AS DEFAULT AND AVOID HAVE AN UNUSEFUL FAKE INSTALLER
        val oppoTrickStatus = getSharedPreferences("oppo_trick_value", MODE_PRIVATE)
        oppoTrickEnabled = oppoTrickStatus.getBoolean("oppo_trick_value", false)
        //MAKE ROOT TRICK DISABLED AS DEFAULT
        val rootTrickStatus = getSharedPreferences("root_trick_value", MODE_PRIVATE)
        rootTrickEnabled = rootTrickStatus.getBoolean("root_trick_value", false)
        val pm = applicationContext.packageManager
        if (oppoTrickEnabled) {
            val oppoTrickFlagged =
                ComponentName(packageName, "$packageName.OppoTrick")
            pm.setComponentEnabledSetting(
                oppoTrickFlagged,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            val oppoTrickFlagged =
                ComponentName(packageName, "$packageName.OppoTrick")
            pm.setComponentEnabledSetting(
                oppoTrickFlagged,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        oppoTrickStatus.edit {
            putBoolean("oppo_trick_value", oppoTrickEnabled)
        }
        rootTrickStatus.edit {
            putBoolean("root_trick_value", rootTrickEnabled)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_info_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            val url = "https://gitlab.com/annexhack/king-installer"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.itemId == R.id.action_search2) {
            val url = "https://github.com/fcaronte/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        if (item.itemId == R.id.action_search3) {
            val url = "https://github.com/Rikj000/KingInstaller"
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(url.toUri())
            startActivity(i)
        }
        return true
    }

    private fun installAsRoot() {
        try {
            val et = findViewById<EditText>(R.id.pathTextEdit)
            val filepath = et.getText().toString()
            runSuWithCmd("pm install -t -i \"com.android.vending\" -r $filepath")
            et.setText("")
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = ""
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = e.toString()
        }
    }

    private fun installAsKing() {
        try {
            val et = findViewById<EditText>(R.id.pathTextEdit)
            val filepath = et.getText().toString()
            if (filepath.isEmpty()) {
                Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show()
                return
            }
            val myFile = File(filepath)
            if (!myFile.exists()) {
                Toast.makeText(this, R.string.file_error, Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            val fileUri: Uri? = FileProvider.getUriForFile(
                applicationContext,
                "$packageName.provider",
                myFile
            )
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setData(fileUri)
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, "com.android.vending")
            et.setText("")
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = ""
            startActivity(intent)
        } catch (e: Exception) {
            val tv = findViewById<TextView>(R.id.textViewError)
            tv.text = e.toString()
        }
    }

    private fun showFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        requestPermissions()
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select APK"), FILE_SELECT_CODE
            )
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_SELECT_CODE -> if (resultCode == RESULT_OK) {
                val uri = data?.data
                val path = copyFileToInternalStorage(uri!!, "apk")

                val et = findViewById<EditText>(R.id.pathTextEdit)
                et.setText(path)
            }

            PERMISSION_REQUEST_CODE -> if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    requestPermissions()
                }
            }
        }
    }

    fun clearTempFile() {
        val file = File(applicationContext.filesDir.toString() + "/apk")
        val listFiles = file.listFiles()
        if (listFiles == null || !file.isDirectory) {
            return
        }
        for (file2 in listFiles) {
            file2.delete()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            clearTempFile()
        } catch (ignored: Throwable) {
        }
    }

    private fun checkManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // android 11 has new readFiles request permission
            if (Environment.isExternalStorageManager()) {
                return
            } else {
                if (Environment.isExternalStorageLegacy()) {
                    return
                }
                try {
                    val intent = Intent()
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.setData(("package:" + applicationContext.packageName).toUri())
                    startActivityForResult(intent, RESULT_OK) //result code is just an int
                    return
                } catch (e: Exception) {
                    return
                }
            }
        } else { // android 10 and lower - classic request
            requestPermissions()
        }
    }

    private fun copyFileToInternalStorage(uri: Uri, newDirName: String): String {

        val mContext = applicationContext
        val returnCursor = mContext.contentResolver.query(
            uri, arrayOf<String>(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )


        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = (returnCursor.getString(nameIndex))
        val size = (returnCursor.getLong(sizeIndex).toString())

        val output: File?
        if (newDirName != "") {
            val dir = File(mContext.filesDir.toString() + "/" + newDirName)
            if (!dir.exists()) {
                dir.mkdir()
            }
            output = File(mContext.filesDir.toString() + "/" + newDirName + "/" + name)
        } else {
            output = File(mContext.filesDir.toString() + "/" + name)
        }
        try {
            val inputStream = mContext.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(output)
            var read = 0
            val bufferSize = 1024
            val buffers = ByteArray(bufferSize)
            while ((inputStream!!.read(buffers).also { read = it }) != -1) {
                outputStream.write(buffers, 0, read)
            }

            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            //            L.e("Exception", e.getMessage());
        }

        return output.path
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            ) //permission request code is just an int
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                PERMISSION_REQUEST_CODE
            ) //permisison request code is just an int
        }
    }

    companion object {
        private const val FILE_SELECT_CODE = 1
        private const val PERMISSION_REQUEST_CODE = 2

        /**
         * https://github.com/shmykelsa/AA-Tweaker/blob/4d03205f14b2938f96bf04e198dd067cd6fe0967/app/src/main/java/sksa/aa/tweaker/MainActivity.java#L3964
         * @param cmd
         * @return
         */
        fun runSuWithCmd(cmd: String?): StreamLogs {
            var outputStream: DataOutputStream? = null
            var inputStream: InputStream? = null
            var errorStream: InputStream? = null

            val streamLogs = StreamLogs()
            streamLogs.setOutputStreamLog(cmd)

            try {
                val su = Runtime.getRuntime().exec("su")
                outputStream = DataOutputStream(su.outputStream)
                inputStream = su.inputStream
                errorStream = su.errorStream

                outputStream.writeBytes(cmd + "\n")
                outputStream.flush()
                outputStream.writeBytes("exit\n")
                outputStream.flush()

                try {
                    su.waitFor()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                streamLogs.setInputStreamLog(readStream(inputStream))
                streamLogs.setErrorStreamLog(readStream(errorStream))
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return streamLogs
        }

        @Throws(IOException::class)
        fun readStream(`is`: InputStream): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while ((`is`.read(buffer).also { length = it }) != -1) {
                byteArrayOutputStream.write(buffer, 0, length)
            }
            return byteArrayOutputStream.toString("UTF-8")
        }

        val isDeviceRooted: Boolean
            get() = checkRootMethod1() || checkRootMethod2() ||
                    checkRootMethod3() || checkRootMethod4()

        private fun checkRootMethod1(): Boolean {
            val buildTags = Build.TAGS
            return buildTags != null && buildTags.contains("test-keys")
        }

        private fun checkRootMethod2(): Boolean {
            val paths = arrayOf<String?>(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su",
                "/su/bin/su",
                "/magisk/.core/bin/su",
                "/data/adb/magisk/su",
                "/data/adb/ksu/bin/su"
            )
            for (path in paths) {
                if (File(path).exists()) return true
            }
            return false
        }

        private fun checkRootMethod3(): Boolean {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf<String>("/system/xbin/which", "su"))
                val `in` = BufferedReader(InputStreamReader(process.inputStream))
                if (`in`.readLine() != null) return true
                return false
            } catch (t: Throwable) {
                return false
            } finally {
                process?.destroy()
            }
        }

        private fun checkRootMethod4(): Boolean {
            var process: Process? = null
            return try {
                process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                os.writeBytes("exit\n")
                os.flush()
                process.waitFor()
                true
            } catch (e: Exception) {
                false
            } finally {
                process?.destroy()
            }
        }
    }
}
