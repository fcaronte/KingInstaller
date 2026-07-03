package com.example.kinginstaller;

import android.content.pm.PackageInstaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ApkSessionWriter {
    private static final int BUFFER_SIZE = 64 * 1024;

    private ApkSessionWriter() {
    }

    static void writeApks(PackageInstaller.Session session, List<File> apkFiles) throws IOException {
        Set<String> usedNames = new HashSet<>();
        for (int index = 0; index < apkFiles.size(); index++) {
            File apkFile = apkFiles.get(index);
            String sessionName = sessionFileName(apkFile, index, usedNames);
            try (FileInputStream input = new FileInputStream(apkFile);
                 OutputStream output = session.openWrite(sessionName, 0, apkFile.length())) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                session.fsync(output);
            }
        }
    }

    private static String sessionFileName(File file, int index, Set<String> usedNames) {
        String name = file.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!name.toLowerCase(Locale.US).endsWith(".apk")) {
            name += ".apk";
        }
        if (name.trim().isEmpty()) {
            name = "split-" + index + ".apk";
        }

        String candidate = name;
        if (usedNames.contains(candidate)) {
            candidate = index + "-" + name;
        }
        usedNames.add(candidate);
        return candidate;
    }
}
