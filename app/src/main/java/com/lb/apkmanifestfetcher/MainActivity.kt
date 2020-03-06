package com.lb.apkmanifestfetcher

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        thread {
            val problematicApkFiles = HashMap<ApplicationInfo, HashSet<String>>()
            val installedApplications = packageManager.getInstalledPackages(0)
            val startTime = System.currentTimeMillis()
            for ((index, packageInfo) in installedApplications.withIndex()) {
                val applicationInfo = packageInfo.applicationInfo
                val packageName = packageInfo.packageName
//                Log.d("AppLog", "$index/${installedApplications.size} parsing app $packageName ${packageInfo.versionCode}...")
                val mainApkFilePath = applicationInfo.publicSourceDir
                val parsedManifestOfMainApkFile =
                        try {
                            val parsedManifest = ManifestParser.parse(mainApkFilePath)
                            if (parsedManifest?.isSplitApk != false)
                                Log.e("AppLog", "$packageName - parsed normal APK, but failed to identify it as such")
                            parsedManifest?.manifestAttributes
                        } catch (e: Exception) {
                            Log.e("AppLog", e.toString())
                            null
                        }
                if (parsedManifestOfMainApkFile == null) {
                    problematicApkFiles.getOrPut(applicationInfo, { HashSet() }).add(mainApkFilePath)
                    Log.e("AppLog", "$packageName - failed to parse main APK file $mainApkFilePath")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    applicationInfo.splitPublicSourceDirs?.forEach {
                        val parsedManifestOfSplitApkFile =
                                try {
                                    val parsedManifest = ManifestParser.parse(it)
                                    if (parsedManifest?.isSplitApk != true)
                                        Log.e("AppLog", "$packageName - parsed split APK, but failed to identify it as such")
                                    parsedManifest?.manifestAttributes
                                } catch (e: Exception) {
                                    Log.e("AppLog", e.toString())
                                    null
                                }
                        if (parsedManifestOfSplitApkFile == null) {
                            Log.e("AppLog", "$packageName - failed to parse main APK file $it")
                            problematicApkFiles.getOrPut(applicationInfo, { HashSet() }).add(it)
                        }
                    }
            }
            val endTime = System.currentTimeMillis()
            Log.d("AppLog", "done parsing. number of files we failed to parse:${problematicApkFiles.size} time taken:${endTime - startTime} ms")
            if (problematicApkFiles.isNotEmpty()) {
                Log.d("AppLog", "list of files that we failed to get their manifest:")
                for (entry in problematicApkFiles) {
                    Log.d("AppLog", "packageName:${entry.key.packageName} , files:${entry.value}")
                }
            }
        }
    }
}
