package com.example.kinginstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ApkSet {
    private final File sourceFile;
    private final File storageRoot;
    private final List<File> apkFiles;
    private final String appName;
    private final String packageName;
    private final String versionName;
    private final int minSdkVersion;
    private final int targetSdkVersion;
    private final long totalSizeBytes;
    private final String label;

    ApkSet(
            File sourceFile,
            File storageRoot,
            List<File> apkFiles,
            String appName,
            String packageName,
            String versionName,
            int minSdkVersion,
            int targetSdkVersion,
            long totalSizeBytes,
            String label
    ) {
        this.sourceFile = sourceFile;
        this.storageRoot = storageRoot;
        this.apkFiles = Collections.unmodifiableList(new ArrayList<>(apkFiles));
        this.appName = appName;
        this.packageName = packageName;
        this.versionName = versionName;
        this.minSdkVersion = minSdkVersion;
        this.targetSdkVersion = targetSdkVersion;
        this.totalSizeBytes = totalSizeBytes;
        this.label = label;
    }

    File getSourceFile() {
        return sourceFile;
    }

    File getStorageRoot() {
        return storageRoot;
    }

    List<File> getApkFiles() {
        return apkFiles;
    }

    String getAppName() {
        return appName;
    }

    String getPackageName() {
        return packageName;
    }

    String getVersionName() {
        return versionName;
    }

    int getMinSdkVersion() {
        return minSdkVersion;
    }

    int getTargetSdkVersion() {
        return targetSdkVersion;
    }

    long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    String getLabel() {
        return label;
    }

    int getApkCount() {
        return apkFiles.size();
    }

    boolean isSingleApk() {
        return apkFiles.size() == 1;
    }
}
