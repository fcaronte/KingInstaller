package com.example.kinginstaller

import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

class MyUserService : IMyUserService.Stub {

    constructor() : super()

    constructor(context: Context) : super() {
        Log.d("MyUserService", "MyUserService inizializzato con context: ${context.packageName}")
    }

    override fun execCommand(command: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            val out = process.inputStream.bufferedReader().readText().trim()
            val err = process.errorStream.bufferedReader().readText().trim()
            Log.d("MyUserService", "Exec command '$command' terminato con exitCode:$exitCode, out: '$out', err: '$err'")
        } catch (e: Exception) {
            Log.e("MyUserService", "Esecuzione comando fallita: $command", e)
        } catch (t: Throwable) {
            Log.e("MyUserService", "Throwable in execCommand", t)
        }
    }

    override fun setInstaller(packageName: String, installerPackageName: String) {
        val target = if (installerPackageName.isNotBlank()) installerPackageName else "com.android.vending"

        val commands = listOf(
            "cmd package set-installer-package --user 0 $packageName $target",
            "cmd package set-installer --user 0 $packageName $target",
            "pm set-installer $packageName $target"
        )

        var executed = false
        for (cmd in commands) {
            try {
                Log.d("MyUserService", "Tentativo esecuzione comando installer: $cmd")
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
                val exitCode = process.waitFor()
                val out = process.inputStream.bufferedReader().readText().trim()
                val err = process.errorStream.bufferedReader().readText().trim()
                Log.d("MyUserService", "Comando [$cmd] -> Exit:$exitCode, Out: '$out', Err: '$err'")
                if (exitCode == 0) {
                    executed = true
                    break
                }
            } catch (e: Exception) {
                Log.w("MyUserService", "Tentativo fallito per [$cmd]: ${e.message}")
            }
        }

        if (!executed) {
            Log.e("MyUserService", "Tutti i tentativi di impostare l'installer per $packageName sono falliti")
        }
    }

    override fun destroy() {
        Log.d("MyUserService", "destroy richiesto")
        try {
            exitProcess(0)
        } catch (_: Throwable) {}
    }
}
