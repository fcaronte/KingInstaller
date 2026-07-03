package com.example.kinginstaller;

import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;

class InstallOptions {
    final String installerPackageName;
    final boolean setInstallSource;
    final int installReason;
    final int packageSource;
    final int targetUserId;
    final boolean allowTestOnly;
    final boolean bypassLowTargetSdk;
    final boolean grantAllPermissions;
    final boolean requestUpdateOwnership;
    final boolean installForAllUsers;
    final boolean allowDowngrade;
    final boolean allowRestrictedPermissions;
    final boolean disableVerification;
    final boolean disableAdbVerify;
    final boolean enableRollback;
    final boolean fromAdb;
    final boolean bypassPlayProtect;
    final boolean privateSpaceInstall;

    InstallOptions(
            String installerPackageName,
            boolean setInstallSource,
            int installReason,
            int packageSource,
            int targetUserId,
            boolean allowTestOnly,
            boolean bypassLowTargetSdk,
            boolean grantAllPermissions,
            boolean requestUpdateOwnership,
            boolean installForAllUsers,
            boolean allowDowngrade,
            boolean allowRestrictedPermissions,
            boolean disableVerification,
            boolean disableAdbVerify,
            boolean enableRollback,
            boolean fromAdb,
            boolean bypassPlayProtect,
            boolean privateSpaceInstall
    ) {
        this.installerPackageName = installerPackageName;
        this.setInstallSource = setInstallSource;
        this.installReason = installReason;
        this.packageSource = packageSource;
        this.targetUserId = targetUserId;
        this.allowTestOnly = allowTestOnly;
        this.bypassLowTargetSdk = bypassLowTargetSdk;
        this.grantAllPermissions = grantAllPermissions;
        this.requestUpdateOwnership = requestUpdateOwnership;
        this.installForAllUsers = installForAllUsers;
        this.allowDowngrade = allowDowngrade;
        this.allowRestrictedPermissions = allowRestrictedPermissions;
        this.disableVerification = disableVerification;
        this.disableAdbVerify = disableAdbVerify;
        this.enableRollback = enableRollback;
        this.fromAdb = fromAdb;
        this.bypassPlayProtect = bypassPlayProtect;
        this.privateSpaceInstall = privateSpaceInstall;
    }

    InstallOptions withoutPrivilegedOptions() {
        return new InstallOptions(
                installerPackageName,
                setInstallSource,
                PackageManager.INSTALL_REASON_USER,
                PackageInstaller.PACKAGE_SOURCE_STORE,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
    }
}
