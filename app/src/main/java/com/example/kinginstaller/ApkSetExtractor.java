package com.example.kinginstaller;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class ApkSetExtractor {
    private static final String DEFAULT_FILE_NAME = "selected.apk";
    private static final String PARTS_DIR_NAME = "parts";
    private static final int BUFFER_SIZE = 64 * 1024;

    private ApkSetExtractor() {
    }

    static ApkSet fromUri(Context context, Uri uri, String tempDirName) throws IOException {
        String displayName = queryDisplayName(context, uri);
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = DEFAULT_FILE_NAME;
        }

        File tempRoot = new File(context.getFilesDir(), tempDirName);
        if (!tempRoot.exists() && !tempRoot.mkdirs()) {
            throw new IOException("Could not create temp directory");
        }

        String safeName = sanitizeFileName(displayName);
        File workDir = uniqueDirectory(tempRoot, stripExtension(safeName));
        if (!workDir.mkdirs()) {
            throw new IOException("Could not create package temp directory");
        }

        File copiedSource = new File(workDir, safeName);
        try {
            copyUriToFile(context, uri, copiedSource);

            List<File> apkFiles = new ArrayList<>();
            PackageInfo singleApkInfo = readArchiveInfo(context, copiedSource);
            if (singleApkInfo != null && singleApkInfo.packageName != null) {
                apkFiles.add(copiedSource);
            } else if (isZipFile(copiedSource)) {
                File partsDir = new File(workDir, PARTS_DIR_NAME);
                apkFiles.addAll(extractApksFromZip(copiedSource, partsDir));
            }

            if (apkFiles.isEmpty()) {
                throw new IOException("Selected file does not contain installable APK files");
            }

            sortApks(apkFiles);
            PackageDetails details = validateApks(context, apkFiles);
            return new ApkSet(
                    copiedSource,
                    workDir,
                    apkFiles,
                    details.appName,
                    details.packageName,
                    details.versionName,
                    details.minSdkVersion,
                    details.targetSdkVersion,
                    totalSize(apkFiles),
                    safeName
            );
        } catch (IOException | RuntimeException error) {
            deleteRecursive(workDir);
            throw error;
        }
    }

    private static void copyUriToFile(Context context, Uri uri, File output) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(output)) {
            if (inputStream == null) {
                throw new IOException("Could not open selected file");
            }
            copy(inputStream, outputStream);
        }
    }

    private static List<File> extractApksFromZip(File sourceFile, File outputDir) throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Could not create APK parts directory");
        }

        String outputRoot = outputDir.getCanonicalPath() + File.separator;
        List<File> apkFiles = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sourceFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zipInputStream.closeEntry();
                    continue;
                }

                String entryName = entry.getName().replace('\\', '/');
                if (!entryName.toLowerCase(Locale.US).endsWith(".apk")) {
                    zipInputStream.closeEntry();
                    continue;
                }

                int slash = entryName.lastIndexOf('/');
                String fileName = slash >= 0 ? entryName.substring(slash + 1) : entryName;
                File outputFile = uniqueFile(outputDir, sanitizeFileName(fileName));
                String outputPath = outputFile.getCanonicalPath();
                if (!outputPath.startsWith(outputRoot)) {
                    zipInputStream.closeEntry();
                    throw new IOException("Unsafe APK path in archive");
                }

                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    copy(zipInputStream, outputStream);
                }

                if (outputFile.length() > 0) {
                    apkFiles.add(outputFile);
                } else {
                    outputFile.delete();
                }
                zipInputStream.closeEntry();
            }
        }
        return apkFiles;
    }

    private static PackageDetails validateApks(Context context, List<File> apkFiles) throws IOException {
        String packageName = null;
        String versionName = null;
        String appName = null;
        int minSdkVersion = 0;
        int targetSdkVersion = 0;
        for (File apkFile : apkFiles) {
            if (!isApkArchive(apkFile)) {
                throw new IOException("Invalid APK inside bundle: " + apkFile.getName());
            }
            PackageInfo info = readArchiveInfo(context, apkFile);
            if (info == null || info.packageName == null) {
                continue;
            }
            if (packageName == null) {
                packageName = info.packageName;
                appName = readApplicationLabel(context, info, apkFile);
                if (info.applicationInfo != null) {
                    minSdkVersion = info.applicationInfo.minSdkVersion;
                    targetSdkVersion = info.applicationInfo.targetSdkVersion;
                }
            } else if (!packageName.equals(info.packageName)) {
                throw new IOException("Bundle contains APKs for multiple packages");
            }
            if (versionName == null && info.versionName != null) {
                versionName = info.versionName;
            }
        }
        if (packageName == null) {
            throw new IOException("Bundle does not contain a valid base APK");
        }
        return new PackageDetails(appName, packageName, versionName, minSdkVersion, targetSdkVersion);
    }

    private static String readApplicationLabel(Context context, PackageInfo info, File apkFile) {
        if (info.applicationInfo == null) return null;
        try {
            info.applicationInfo.sourceDir = apkFile.getAbsolutePath();
            info.applicationInfo.publicSourceDir = apkFile.getAbsolutePath();
            CharSequence label = context.getPackageManager().getApplicationLabel(info.applicationInfo);
            if (label == null) return null;
            String value = label.toString().trim();
            return value.isEmpty() ? null : value;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static PackageInfo readArchiveInfo(Context context, File file) {
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.getPackageArchiveInfo(
                    file.getAbsolutePath(),
                    PackageManager.PackageInfoFlags.of(0)
            );
        }
        return packageManager.getPackageArchiveInfo(file.getAbsolutePath(), 0);
    }

    private static boolean isZipFile(File file) {
        byte[] header = new byte[4];
        try (FileInputStream inputStream = new FileInputStream(file)) {
            int read = inputStream.read(header);
            if (read < 4) return false;
            return header[0] == 'P'
                    && header[1] == 'K'
                    && (header[2] == 3 || header[2] == 5 || header[2] == 7)
                    && (header[3] == 4 || header[3] == 6 || header[3] == 8);
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean isApkArchive(File file) {
        if (!isZipFile(file)) return false;
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory() && "AndroidManifest.xml".equals(entry.getName())) {
                    zipInputStream.closeEntry();
                    return true;
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private static void sortApks(List<File> apkFiles) {
        Collections.sort(apkFiles, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                int order = apkOrder(left) - apkOrder(right);
                if (order != 0) return order;
                return left.getName().compareToIgnoreCase(right.getName());
            }
        });
    }

    private static int apkOrder(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        if ("base.apk".equals(name) || name.startsWith("base.")) return 0;
        if (name.contains("base")) return 1;
        return 2;
    }

    private static long totalSize(List<File> files) {
        long total = 0;
        for (File file : files) {
            total += Math.max(0, file.length());
        }
        return total;
    }

    private static String queryDisplayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Throwable ignored) {
        }
        String path = uri.getLastPathSegment();
        if (path == null) return null;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static File uniqueDirectory(File parent, String name) {
        File output = new File(parent, name);
        if (!output.exists()) return output;

        int index = 1;
        do {
            output = new File(parent, name + "-" + index);
            index++;
        } while (output.exists());
        return output;
    }

    private static File uniqueFile(File dir, String name) {
        File output = new File(dir, name);
        if (!output.exists()) return output;

        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int index = 1;
        do {
            output = new File(dir, base + "-" + index + ext);
            index++;
        } while (output.exists());
        return output;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return base.trim().isEmpty() ? "selected" : base;
    }

    private static String sanitizeFileName(String name) {
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.trim().isEmpty() ? DEFAULT_FILE_NAME : sanitized;
    }

    static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    private static void copy(InputStream inputStream, FileOutputStream outputStream) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
    }

    private static class PackageDetails {
        final String appName;
        final String packageName;
        final String versionName;
        final int minSdkVersion;
        final int targetSdkVersion;

        PackageDetails(
                String appName,
                String packageName,
                String versionName,
                int minSdkVersion,
                int targetSdkVersion
        ) {
            this.appName = appName;
            this.packageName = packageName;
            this.versionName = versionName;
            this.minSdkVersion = minSdkVersion;
            this.targetSdkVersion = targetSdkVersion;
        }
    }
}
