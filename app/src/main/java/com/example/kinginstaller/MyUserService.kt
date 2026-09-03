package com.example.kinginstaller

import android.os.IBinder
import android.util.Log
import kotlin.system.exitProcess

class MyUserService : IMyUserService.Stub() {

    init {
        Log.d("MyUserService", "UserService initialized with Shizuku privileges")
    }

    override fun setInstaller(packageName: String, installerPackageName: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("cmd", "package", "set-installer", packageName, installerPackageName))
            process.waitFor()
        } catch (e: Exception) {
            Log.e("MyUserService", "Failed to set installer", e)
        }
    }

    override fun execCommand(command: String) {
        try {
            // Esegue l'am start con i flag di spoofing originali come faceva il vecchio Shizuku
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            Log.d("MyUserService", "Exec command exited with code: $exitCode")
        } catch (e: Exception) {
            Log.e("MyUserService", "Failed to execute command via UserService", e)
        }
    }

    override fun destroy() {
        exitProcess(0)
    }
}