package com.example.kinginstaller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuProvider;
import rikka.sui.Sui;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "KingInstaller";
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";
    private static final String GOOGLE_PACKAGE_INSTALLER = "com.google.android.packageinstaller";
    private static final String TEMP_DIR_NAME = "apk";
    private static final int SHIZUKU_PERMISSION_REQUEST = 8201;
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String PREF_INSTALL_ROUTE = "install_route";
    private static final String PREF_INSTALLER_PACKAGE = "installer_package";
    private static final String PREF_SET_INSTALL_SOURCE = "set_install_source";
    private static final String PREF_INSTALL_REASON = "install_reason";
    private static final String PREF_PACKAGE_SOURCE = "package_source";
    private static final String PREF_TARGET_USER_ID = "target_user_id";
    private static final String PREF_ALLOW_TEST_ONLY = "allow_test_only";
    private static final String PREF_BYPASS_LOW_TARGET = "bypass_low_target";
    private static final String PREF_GRANT_ALL_PERMISSIONS = "grant_all_permissions";
    private static final String PREF_REQUEST_UPDATE_OWNERSHIP = "request_update_ownership";
    private static final String PREF_INSTALL_FOR_ALL_USERS = "install_for_all_users";
    private static final String PREF_ALLOW_DOWNGRADE = "allow_downgrade";
    private static final String PREF_ALLOW_RESTRICTED_PERMISSIONS = "allow_restricted_permissions";
    private static final String PREF_DISABLE_VERIFICATION = "disable_verification";
    private static final String PREF_DISABLE_ADB_VERIFY = "disable_adb_verify";
    private static final String PREF_ENABLE_ROLLBACK = "enable_rollback";
    private static final String PREF_FROM_ADB = "from_adb";
    private static final String PREF_BYPASS_PLAY_PROTECT = "bypass_play_protect";
    private static final String PREF_PRIVATE_SPACE_INSTALL = "private_space_install";
    private static final String STATE_SELECTED_URI = "selected_uri";
    private static final long ROOT_COMMAND_TIMEOUT_MS = 1500;
    private static final int PACKAGE_VISIBILITY_CHECK_ATTEMPTS = 8;
    private static final long PACKAGE_VISIBILITY_RETRY_DELAY_MS = 500;
    private static final int THEME_AUTO = 0;
    private static final int THEME_LIGHT = 1;
    private static final int THEME_DARK = 2;
    private static final int DEFAULT_INSTALL_REASON = PackageManager.INSTALL_REASON_USER;
    private static final int DEFAULT_PACKAGE_SOURCE = PackageInstaller.PACKAGE_SOURCE_STORE;
    private static final int ROUTE_NORMAL_NO_ROOT = 0;
    private static final int ROUTE_NORMAL_SHIZUKU = 1;
    private static final int ROUTE_NORMAL_ROOT = 2;
    private static final int ROUTE_OEM_NO_ROOT = 3;
    private static final int ROUTE_OEM_ROOT = 4;
    private static final int ROUTE_OEM_SHIZUKU = 5;
    private static final String[] ROOT_MANAGER_PACKAGES = {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk",
            "me.weishu.kernelsu",
            "io.github.rifsxd.ksunext",
            "me.bmax.apatch",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.kingroot.kinguser",
            "com.kingo.root"
    };
    private static final String[] ROOT_BINARY_PATHS = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/su/bin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/system_ext/bin/su",
            "/vendor/bin/su",
            "/product/bin/su",
            "/odm/bin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/debug_ramdisk/su"
    };
    private static final String[] ROOT_ARTIFACT_PATHS = {
            "/data/adb/magisk",
            "/data/adb/ksu",
            "/data/adb/ksud",
            "/data/adb/ap",
            "/data/adb/apd",
            "/data/adb/modules",
            "/metadata/adb/magisk",
            "/metadata/adb/ksu",
            "/metadata/adb/ap"
    };
    private static final String[] ROOT_PROPERTY_KEYS = {
            "ro.magisk.version",
            "ro.kernelsu.version",
            "ro.ksu.version",
            "ro.apatch.version",
            "init.svc.ksud",
            "init.svc.apd"
    };

    private EditText pathEdit;
    private TextView statusText;
    private TextView apkInfoText;
    private TextView deviceInfoText;
    private EditText installerSourceEdit;
    private EditText targetUserEdit;
    private MaterialAutoCompleteTextView installReasonDropdown;
    private MaterialAutoCompleteTextView packageSourceDropdown;
    private MaterialCheckBox setInstallSourceCheck;
    private MaterialCheckBox allowTestOnlyCheck;
    private MaterialCheckBox bypassLowTargetCheck;
    private MaterialCheckBox grantAllPermissionsCheck;
    private MaterialCheckBox requestUpdateOwnershipCheck;
    private MaterialCheckBox installForAllUsersCheck;
    private MaterialCheckBox allowDowngradeCheck;
    private MaterialCheckBox allowRestrictedPermissionsCheck;
    private MaterialCheckBox disableVerificationCheck;
    private MaterialCheckBox disableAdbVerifyCheck;
    private MaterialCheckBox enableRollbackCheck;
    private MaterialCheckBox fromAdbCheck;
    private MaterialCheckBox bypassPlayProtectCheck;
    private MaterialCheckBox privateSpaceInstallCheck;
    private RadioGroup methodGroup;
    private RadioGroup authorizerGroup;
    private MaterialRadioButton methodNormalMode;
    private MaterialRadioButton methodOemMode;
    private MaterialRadioButton authorizerNoRootMode;
    private MaterialRadioButton authorizerShizukuMode;
    private MaterialRadioButton authorizerRootMode;
    private Button installButton;
    private Button openButton;
    private Button ignoreBatteryButton;

    private ApkSet selectedApkSet;
    private Uri selectedSourceUri;
    private String selectedAppName;
    private String selectedPackageName;
    private String selectedLabel;
    private boolean forceRootEnabled;
    private boolean pendingInstallVerification;
    private boolean updatingMode;
    private int selectedInstallRoute = ROUTE_NORMAL_NO_ROOT;
    private int verificationSequence;
    private InstallOptions lastInstallOptions;
    private int lastInstallRoute = ROUTE_NORMAL_NO_ROOT;
    private String lastAdbVerifyStatus;
    private KingShizukuInstaller shizukuInstaller;

    private final ActivityResultLauncher<String[]> apkPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    persistSelectedUriPermission(uri);
                    handleSelectedApk(uri);
                }
            });

    private final Shizuku.OnRequestPermissionResultListener shizukuPermissionListener =
            (requestCode, grantResult) -> {
                if (requestCode != SHIZUKU_PERMISSION_REQUEST) return;
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    setInstallRoute(isOemRouteSelected() ? ROUTE_OEM_SHIZUKU : ROUTE_NORMAL_SHIZUKU);
                    setStatus(getString(R.string.shizuku_permission_requested));
                } else {
                    setInstallRoute(isOemRouteSelected() ? ROUTE_OEM_NO_ROOT : ROUTE_NORMAL_NO_ROOT);
                    setStatus(getString(R.string.shizuku_not_ready));
                }
                updateDeviceInfoStatus();
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        super.onCreate(savedInstanceState);
        enableHiddenApiAccess();
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        initViews();
        initAdvancedSettings();
        initShizuku();
        updateDeviceInfoStatus();
        bindControls();
        restoreModeState();
        updateGooglePackageInstallerStatus();
        if (restoreRetainedSelection()) {
            verifySelectedPackage();
        } else if (savedInstanceState != null && savedInstanceState.getString(STATE_SELECTED_URI) != null) {
            handleSelectedApk(Uri.parse(savedInstanceState.getString(STATE_SELECTED_URI)));
        } else {
            handleIncomingIntent(getIntent());
        }
    }

    private void enableHiddenApiAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;
        try {
            HiddenApiBypass.addHiddenApiExemptions("L");
        } catch (Throwable error) {
            Log.w(TAG, "Hidden API bypass initialization failed", error);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedSourceUri != null) {
            outState.putString(STATE_SELECTED_URI, selectedSourceUri.toString());
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        if (selectedApkSet == null) return null;
        return new RetainedSelection(
                selectedApkSet,
                selectedSourceUri,
                selectedAppName,
                selectedPackageName,
                selectedLabel
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBatteryButtonLabel();
        if (pendingInstallVerification) {
            verifySelectedPackageWithRetry();
        } else {
            verifySelectedPackage();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener);
        } catch (Throwable ignored) {
        }
        if (!isChangingConfigurations()) {
            selectedApkSet = null;
            clearTempFile();
        }
    }

    private void initViews() {
        pathEdit = findViewById(R.id.pathTextEdit);
        statusText = findViewById(R.id.textViewError);
        apkInfoText = findViewById(R.id.textViewApkInfo);
        deviceInfoText = findViewById(R.id.deviceInfoText);
        installerSourceEdit = findViewById(R.id.installerSourceEdit);
        targetUserEdit = findViewById(R.id.targetUserEdit);
        installReasonDropdown = findViewById(R.id.installReasonDropdown);
        packageSourceDropdown = findViewById(R.id.packageSourceDropdown);
        setInstallSourceCheck = findViewById(R.id.setInstallSourceCheck);
        allowTestOnlyCheck = findViewById(R.id.allowTestOnlyCheck);
        bypassLowTargetCheck = findViewById(R.id.bypassLowTargetCheck);
        grantAllPermissionsCheck = findViewById(R.id.grantAllPermissionsCheck);
        requestUpdateOwnershipCheck = findViewById(R.id.requestUpdateOwnershipCheck);
        installForAllUsersCheck = findViewById(R.id.installForAllUsersCheck);
        allowDowngradeCheck = findViewById(R.id.allowDowngradeCheck);
        allowRestrictedPermissionsCheck = findViewById(R.id.allowRestrictedPermissionsCheck);
        disableVerificationCheck = findViewById(R.id.disableVerificationCheck);
        disableAdbVerifyCheck = findViewById(R.id.disableAdbVerifyCheck);
        enableRollbackCheck = findViewById(R.id.enableRollbackCheck);
        fromAdbCheck = findViewById(R.id.fromAdbCheck);
        bypassPlayProtectCheck = findViewById(R.id.bypassPlayProtectCheck);
        privateSpaceInstallCheck = findViewById(R.id.privateSpaceInstallCheck);
        methodGroup = findViewById(R.id.methodGroup);
        authorizerGroup = findViewById(R.id.authorizerGroup);
        methodNormalMode = findViewById(R.id.radioMethodNormal);
        methodOemMode = findViewById(R.id.radioMethodOem);
        authorizerNoRootMode = findViewById(R.id.radioAuthNoRoot);
        authorizerShizukuMode = findViewById(R.id.radioAuthShizuku);
        authorizerRootMode = findViewById(R.id.radioAuthRoot);
        installButton = findViewById(R.id.installButton);
        openButton = findViewById(R.id.openButton);
        ignoreBatteryButton = findViewById(R.id.ignoreBatteryButton);
        updateOpenButton(false);
    }

    private void initShizuku() {
        try {
            boolean isSui = Sui.init(getPackageName());
            if (!isSui) {
                ShizukuProvider.requestBinderForNonProviderProcess(this);
            }
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener);
            shizukuInstaller = new KingShizukuInstaller(getApplication());
        } catch (Throwable error) {
            Log.w(TAG, "Shizuku initialization failed", error);
        }
    }

    private void initAdvancedSettings() {
        SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        String[] reasonLabels = installReasonLabels();
        String[] sourceLabels = packageSourceLabels();

        installReasonDropdown.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                reasonLabels
        ));
        packageSourceDropdown.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                sourceLabels
        ));

        installerSourceEdit.setText(prefs.getString(PREF_INSTALLER_PACKAGE, PLAY_STORE_PACKAGE));
        targetUserEdit.setText(String.valueOf(prefs.getInt(PREF_TARGET_USER_ID, 0)));
        setInstallSourceCheck.setChecked(prefs.getBoolean(PREF_SET_INSTALL_SOURCE, true));
        setDropdownSelection(
                installReasonDropdown,
                reasonLabels,
                prefs.getInt(PREF_INSTALL_REASON, DEFAULT_INSTALL_REASON)
        );
        setDropdownSelection(
                packageSourceDropdown,
                sourceLabels,
                prefs.getInt(PREF_PACKAGE_SOURCE, DEFAULT_PACKAGE_SOURCE)
        );
        allowTestOnlyCheck.setChecked(prefs.getBoolean(PREF_ALLOW_TEST_ONLY, true));
        bypassLowTargetCheck.setChecked(prefs.getBoolean(PREF_BYPASS_LOW_TARGET, false));
        grantAllPermissionsCheck.setChecked(prefs.getBoolean(PREF_GRANT_ALL_PERMISSIONS, false));
        requestUpdateOwnershipCheck.setChecked(prefs.getBoolean(PREF_REQUEST_UPDATE_OWNERSHIP, false));
        installForAllUsersCheck.setChecked(prefs.getBoolean(PREF_INSTALL_FOR_ALL_USERS, false));
        allowDowngradeCheck.setChecked(prefs.getBoolean(PREF_ALLOW_DOWNGRADE, false));
        allowRestrictedPermissionsCheck.setChecked(prefs.getBoolean(PREF_ALLOW_RESTRICTED_PERMISSIONS, false));
        disableVerificationCheck.setChecked(prefs.getBoolean(PREF_DISABLE_VERIFICATION, false));
        disableAdbVerifyCheck.setChecked(prefs.getBoolean(PREF_DISABLE_ADB_VERIFY, false));
        enableRollbackCheck.setChecked(prefs.getBoolean(PREF_ENABLE_ROLLBACK, false));
        fromAdbCheck.setChecked(prefs.getBoolean(PREF_FROM_ADB, false));
        bypassPlayProtectCheck.setChecked(prefs.getBoolean(PREF_BYPASS_PLAY_PROTECT, false));
        privateSpaceInstallCheck.setChecked(prefs.getBoolean(PREF_PRIVATE_SPACE_INSTALL, false));

        installerSourceEdit.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) saveAdvancedSettings();
        });
        targetUserEdit.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) saveAdvancedSettings();
        });
        installReasonDropdown.setOnItemClickListener((parent, view, position, id) -> saveAdvancedSettings());
        packageSourceDropdown.setOnItemClickListener((parent, view, position, id) -> saveAdvancedSettings());
        setInstallSourceCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAdvancedSettings();
            updateAdvancedControlState();
        });
        allowTestOnlyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        bypassLowTargetCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        grantAllPermissionsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        requestUpdateOwnershipCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        installForAllUsersCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        allowDowngradeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        allowRestrictedPermissionsCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        disableVerificationCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        disableAdbVerifyCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        enableRollbackCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        fromAdbCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        bypassPlayProtectCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        privateSpaceInstallCheck.setOnCheckedChangeListener((buttonView, isChecked) -> saveAdvancedSettings());
        updateAdvancedControlState();
    }

    private String[] installReasonLabels() {
        return new String[]{
                getString(R.string.install_reason_unknown),
                getString(R.string.install_reason_policy),
                getString(R.string.install_reason_restore),
                getString(R.string.install_reason_setup),
                getString(R.string.install_reason_user),
                getString(R.string.install_reason_rollback)
        };
    }

    private String[] packageSourceLabels() {
        return new String[]{
                getString(R.string.package_source_unspecified),
                getString(R.string.package_source_other),
                getString(R.string.package_source_store),
                getString(R.string.package_source_local),
                getString(R.string.package_source_downloaded)
        };
    }

    private void setDropdownSelection(MaterialAutoCompleteTextView view, String[] labels, int index) {
        int safeIndex = index >= 0 && index < labels.length ? index : 0;
        view.setText(labels[safeIndex], false);
    }

    private int selectedDropdownIndex(MaterialAutoCompleteTextView view, String[] labels, int fallback) {
        String value = view.getText() == null ? "" : view.getText().toString();
        for (int index = 0; index < labels.length; index++) {
            if (labels[index].equals(value)) return index;
        }
        return fallback;
    }

    private void saveAdvancedSettings() {
        getPreferences(Activity.MODE_PRIVATE).edit()
                .putString(PREF_INSTALLER_PACKAGE, getConfiguredInstallerPackageName())
                .putBoolean(PREF_SET_INSTALL_SOURCE, setInstallSourceCheck.isChecked())
                .putInt(PREF_INSTALL_REASON, selectedInstallReason())
                .putInt(PREF_PACKAGE_SOURCE, selectedPackageSource())
                .putInt(PREF_TARGET_USER_ID, selectedTargetUserId())
                .putBoolean(PREF_ALLOW_TEST_ONLY, allowTestOnlyCheck.isChecked())
                .putBoolean(PREF_BYPASS_LOW_TARGET, bypassLowTargetCheck.isChecked())
                .putBoolean(PREF_GRANT_ALL_PERMISSIONS, grantAllPermissionsCheck.isChecked())
                .putBoolean(PREF_REQUEST_UPDATE_OWNERSHIP, requestUpdateOwnershipCheck.isChecked())
                .putBoolean(PREF_INSTALL_FOR_ALL_USERS, installForAllUsersCheck.isChecked())
                .putBoolean(PREF_ALLOW_DOWNGRADE, allowDowngradeCheck.isChecked())
                .putBoolean(PREF_ALLOW_RESTRICTED_PERMISSIONS, allowRestrictedPermissionsCheck.isChecked())
                .putBoolean(PREF_DISABLE_VERIFICATION, disableVerificationCheck.isChecked())
                .putBoolean(PREF_DISABLE_ADB_VERIFY, disableAdbVerifyCheck.isChecked())
                .putBoolean(PREF_ENABLE_ROLLBACK, enableRollbackCheck.isChecked())
                .putBoolean(PREF_FROM_ADB, fromAdbCheck.isChecked())
                .putBoolean(PREF_BYPASS_PLAY_PROTECT, bypassPlayProtectCheck.isChecked())
                .putBoolean(PREF_PRIVATE_SPACE_INSTALL, privateSpaceInstallCheck.isChecked())
                .apply();
    }

    private InstallOptions createInstallOptions() {
        saveAdvancedSettings();
        return new InstallOptions(
                getConfiguredInstallerPackageName(),
                setInstallSourceCheck.isChecked(),
                selectedInstallReason(),
                selectedPackageSource(),
                selectedTargetUserId(),
                allowTestOnlyCheck.isChecked(),
                bypassLowTargetCheck.isChecked(),
                grantAllPermissionsCheck.isChecked(),
                requestUpdateOwnershipCheck.isChecked(),
                installForAllUsersCheck.isChecked(),
                allowDowngradeCheck.isChecked(),
                allowRestrictedPermissionsCheck.isChecked(),
                disableVerificationCheck.isChecked(),
                disableAdbVerifyCheck.isChecked(),
                enableRollbackCheck.isChecked(),
                fromAdbCheck.isChecked(),
                bypassPlayProtectCheck.isChecked(),
                privateSpaceInstallCheck.isChecked()
        );
    }

    private String getConfiguredInstallerPackageName() {
        String value = installerSourceEdit.getText() == null
                ? ""
                : installerSourceEdit.getText().toString().trim();
        return value.isEmpty() ? PLAY_STORE_PACKAGE : value;
    }

    private int selectedInstallReason() {
        return selectedDropdownIndex(installReasonDropdown, installReasonLabels(), DEFAULT_INSTALL_REASON);
    }

    private int selectedPackageSource() {
        return selectedDropdownIndex(packageSourceDropdown, packageSourceLabels(), DEFAULT_PACKAGE_SOURCE);
    }

    private int selectedTargetUserId() {
        String value = targetUserEdit.getText() == null ? "" : targetUserEdit.getText().toString().trim();
        if (value.isEmpty()) return 0;
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void bindControls() {
        findViewById(R.id.selectButton).setOnClickListener(v -> showFileChooser());

        findViewById(R.id.sourceButton).setOnClickListener(v -> openUrl("https://github.com/rushiranpise/KingInstaller-X"));

        methodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (updatingMode) return;
            handleRouteSelection(routeFromCurrentSelection());
        });

        authorizerGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (updatingMode) return;
            handleRouteSelection(routeFromCurrentSelection());
        });

        installButton.setOnClickListener(v -> {
            if (!ensureApkSelected()) return;
            if (isShizukuRouteSelected()) {
                installAsShizuku();
            } else if (isRootRouteSelected()) {
                installAsRoot();
            } else {
                installAsKing();
            }
        });

        openButton.setOnClickListener(v -> openSelectedPackage());

        findViewById(R.id.resetButton).setOnClickListener(v -> openGooglePackageInstallerSettings());
        ignoreBatteryButton.setOnClickListener(v -> requestIgnoreBatteryOptimization());
    }

    private void restoreModeState() {
        SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        int route = prefs.contains(PREF_INSTALL_ROUTE)
                ? prefs.getInt(PREF_INSTALL_ROUTE, ROUTE_NORMAL_NO_ROOT)
                : legacyRouteFromPrefs(prefs);
        if (isShizukuRoute(route) && !ensureShizukuReady(false)) {
            route = fallbackRouteFor(route);
        }
        if (isRootRoute(route) && !isDeviceRooted()) {
            route = fallbackRouteFor(route);
        }
        setInstallRoute(route);
    }

    private int legacyRouteFromPrefs(SharedPreferences prefs) {
        if (prefs.getBoolean("shizuku_trick_value", false)) {
            return ROUTE_NORMAL_SHIZUKU;
        }
        if (prefs.getBoolean("root_trick_value", false)) {
            return ROUTE_NORMAL_ROOT;
        }
        return prefs.getBoolean("oppo_trick_value", false)
                ? ROUTE_OEM_NO_ROOT
                : ROUTE_NORMAL_NO_ROOT;
    }

    private void handleRouteSelection(int route) {
        if (isShizukuRoute(route) && !ensureShizukuReady(true)) {
            setInstallRoute(selectedInstallRoute);
            return;
        }
        if (isRootRoute(route)) {
            if (!isDeviceRooted()) {
                Toast.makeText(getBaseContext(), R.string.device_not_rooted, Toast.LENGTH_SHORT).show();
                setInstallRoute(fallbackRouteFor(route));
                return;
            }
            if (isGooglePackageExist() && !forceRootEnabled) {
                setStatus(getString(R.string.root_method_warning));
                setInstallRoute(fallbackRouteFor(route));
                forceRootEnabled = true;
                return;
            }
            forceRootEnabled = true;
        }
        setInstallRoute(route);
    }

    private void setInstallRoute(int route) {
        selectedInstallRoute = normalizeInstallRoute(route);
        updatingMode = true;
        methodNormalMode.setChecked(!isOemRoute(selectedInstallRoute));
        methodOemMode.setChecked(isOemRoute(selectedInstallRoute));
        authorizerNoRootMode.setChecked(isNoRootRoute(selectedInstallRoute));
        authorizerShizukuMode.setChecked(isShizukuRoute(selectedInstallRoute));
        authorizerRootMode.setChecked(isRootRoute(selectedInstallRoute));
        updatingMode = false;
        updateOemAliasState();
        updateAdvancedControlState();
        saveModeState();
        updateInstallButtonLabel();
        Log.d(TAG, "install route=" + selectedInstallRoute);
    }

    private void updateAdvancedControlState() {
        boolean privileged = !isNoRootRoute(selectedInstallRoute);
        setAdvancedEnabled(installerSourceEdit, setInstallSourceCheck == null || setInstallSourceCheck.isChecked());
        setPrivilegedAdvancedEnabled(installReasonDropdown, privileged);
        setPrivilegedAdvancedEnabled(packageSourceDropdown, privileged);
        setPrivilegedAdvancedEnabled(targetUserEdit, privileged);
        setPrivilegedAdvancedEnabled(allowTestOnlyCheck, privileged);
        setPrivilegedAdvancedEnabled(bypassLowTargetCheck, privileged);
        setPrivilegedAdvancedEnabled(grantAllPermissionsCheck, privileged);
        setPrivilegedAdvancedEnabled(requestUpdateOwnershipCheck, privileged);
        setPrivilegedAdvancedEnabled(installForAllUsersCheck, privileged);
        setPrivilegedAdvancedEnabled(allowDowngradeCheck, privileged);
        setPrivilegedAdvancedEnabled(allowRestrictedPermissionsCheck, privileged);
        setPrivilegedAdvancedEnabled(disableVerificationCheck, privileged);
        setPrivilegedAdvancedEnabled(disableAdbVerifyCheck, privileged);
        setPrivilegedAdvancedEnabled(enableRollbackCheck, privileged);
        setPrivilegedAdvancedEnabled(fromAdbCheck, privileged);
        setPrivilegedAdvancedEnabled(bypassPlayProtectCheck, privileged);
        setPrivilegedAdvancedEnabled(privateSpaceInstallCheck, privileged);
    }

    private void setPrivilegedAdvancedEnabled(View view, boolean enabled) {
        setAdvancedEnabled(view, enabled);
    }

    private void setAdvancedEnabled(View view, boolean enabled) {
        if (view == null) return;
        view.setEnabled(enabled);
        view.setAlpha(enabled ? 1f : 0.45f);
    }

    private void updateInstallButtonLabel() {
        if (installButton == null) return;
        if (selectedInstallRoute == ROUTE_OEM_ROOT) {
            installButton.setText(R.string.install_with_oem_root);
        } else if (selectedInstallRoute == ROUTE_OEM_SHIZUKU) {
            installButton.setText(R.string.install_with_oem_shizuku);
        } else if (selectedInstallRoute == ROUTE_NORMAL_ROOT) {
            installButton.setText(R.string.install_with_root);
        } else if (selectedInstallRoute == ROUTE_NORMAL_SHIZUKU) {
            installButton.setText(R.string.install_with_shizuku);
        } else if (selectedInstallRoute == ROUTE_OEM_NO_ROOT) {
            installButton.setText(R.string.install_with_oem);
        } else {
            installButton.setText(R.string.install_with_normal);
        }
    }

    private void saveModeState() {
        getPreferences(Activity.MODE_PRIVATE).edit()
                .putInt(PREF_INSTALL_ROUTE, selectedInstallRoute)
                .putBoolean("oppo_trick_value", isOemRouteSelected())
                .putBoolean("root_trick_value", isRootRouteSelected())
                .putBoolean("shizuku_trick_value", isShizukuRouteSelected())
                .apply();
    }

    private int routeFromCurrentSelection() {
        boolean oem = methodOemMode != null && methodOemMode.isChecked();
        boolean shizuku = authorizerShizukuMode != null && authorizerShizukuMode.isChecked();
        boolean root = authorizerRootMode != null && authorizerRootMode.isChecked();
        if (oem && root) return ROUTE_OEM_ROOT;
        if (oem && shizuku) return ROUTE_OEM_SHIZUKU;
        if (oem) return ROUTE_OEM_NO_ROOT;
        if (root) return ROUTE_NORMAL_ROOT;
        if (shizuku) return ROUTE_NORMAL_SHIZUKU;
        return ROUTE_NORMAL_NO_ROOT;
    }

    private int normalizeInstallRoute(int route) {
        if (route >= ROUTE_NORMAL_NO_ROOT && route <= ROUTE_OEM_SHIZUKU) {
            return route;
        }
        return ROUTE_NORMAL_NO_ROOT;
    }

    private int fallbackRouteFor(int route) {
        return isOemRoute(route) ? ROUTE_OEM_NO_ROOT : ROUTE_NORMAL_NO_ROOT;
    }

    private boolean isNoRootRoute(int route) {
        return route == ROUTE_NORMAL_NO_ROOT || route == ROUTE_OEM_NO_ROOT;
    }

    private boolean isRootRoute(int route) {
        return route == ROUTE_NORMAL_ROOT || route == ROUTE_OEM_ROOT;
    }

    private boolean isShizukuRoute(int route) {
        return route == ROUTE_NORMAL_SHIZUKU || route == ROUTE_OEM_SHIZUKU;
    }

    private boolean isOemRoute(int route) {
        return route == ROUTE_OEM_NO_ROOT || route == ROUTE_OEM_SHIZUKU || route == ROUTE_OEM_ROOT;
    }

    private boolean isRootRouteSelected() {
        return isRootRoute(selectedInstallRoute);
    }

    private boolean isShizukuRouteSelected() {
        return isShizukuRoute(selectedInstallRoute);
    }

    private boolean isOemRouteSelected() {
        return isOemRoute(selectedInstallRoute);
    }

    private boolean isNoRootRouteSelected() {
        return isNoRootRoute(selectedInstallRoute);
    }

    private void updateDeviceInfoStatus() {
        if (deviceInfoText == null) return;
        deviceInfoText.setText(getString(
                R.string.device_info_status,
                capitalize(Build.MANUFACTURER),
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                getString(isDeviceRooted() ? R.string.status_yes : R.string.status_no),
                getString(isShizukuAuthorized() ? R.string.status_ready : R.string.status_not_ready)
        ));
    }

    private boolean isShizukuAuthorized() {
        try {
            return !Shizuku.isPreV11()
                    && Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private void updateGooglePackageInstallerStatus() {
        if (isGooglePackageExist()) {
            setStatus(getString(R.string.google_package_installer_is_installed));
        } else {
            setStatus(getString(R.string.missing_google_package_installer));
        }
    }

    public boolean isGooglePackageExist() {
        try {
            getPackageManager().getPackageInfo(GOOGLE_PACKAGE_INSTALLER, PackageManager.GET_META_DATA);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    public void updateOemAliasState() {
        ComponentName oppoTrickFlagged =
                new ComponentName(getPackageName(), getPackageName() + ".OppoTrick");
        getPackageManager().setComponentEnabledSetting(
                oppoTrickFlagged,
                isOemRouteSelected()
                        ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_info_menu, menu);
        updateThemeMenuItem(menu.findItem(R.id.action_theme));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            cycleThemeMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applySavedTheme() {
        AppCompatDelegate.setDefaultNightMode(appCompatNightModeFor(getThemeMode()));
    }

    private void cycleThemeMode() {
        int nextMode = (getThemeMode() + 1) % 3;
        getPreferences(Activity.MODE_PRIVATE).edit()
                .putInt(PREF_THEME_MODE, nextMode)
                .apply();
        AppCompatDelegate.setDefaultNightMode(appCompatNightModeFor(nextMode));
        invalidateOptionsMenu();
    }

    private int getThemeMode() {
        return getPreferences(Activity.MODE_PRIVATE).getInt(PREF_THEME_MODE, THEME_AUTO);
    }

    private int appCompatNightModeFor(int themeMode) {
        if (themeMode == THEME_LIGHT) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        }
        if (themeMode == THEME_DARK) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private void updateThemeMenuItem(MenuItem item) {
        if (item == null) return;
        int titleRes;
        int themeMode = getThemeMode();
        if (themeMode == THEME_LIGHT) {
            titleRes = R.string.theme_light;
        } else if (themeMode == THEME_DARK) {
            titleRes = R.string.theme_dark;
        } else {
            titleRes = R.string.theme_auto;
        }
        item.setTitle(titleRes);
        item.setContentDescription(getString(titleRes));
        Drawable icon = item.getIcon();
        if (icon != null) {
            icon.mutate().setTint(resolveThemeColor(com.google.android.material.R.attr.colorOnSurface));
        }
    }

    private int resolveThemeColor(int attr) {
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(attr, value, true);
        if (value.resourceId != 0) {
            return getColor(value.resourceId);
        }
        return value.data;
    }

    private void installAsRoot() {
        InstallOptions options = createInstallOptions();
        rememberInstallAttempt(options);
        markInstallVerificationPending();
        setInstallButtonsEnabled(false);
        new Thread(() -> {
            try {
                lastAdbVerifyStatus = applyRootPreInstallSettings(options);
                StreamLogs logs = runSuWithCmd(buildRootInstallCommand(options));
                runOnUiThread(() -> {
                    setInstallButtonsEnabled(true);
                    if (!logs.getErrorStreamLog().isEmpty()) {
                        pendingInstallVerification = false;
                        setStatus(logs.getStreamLogsWithLabels());
                    } else {
                        setStatus(logs.getStreamLogsWithLabels());
                        verifySelectedPackageWithRetry();
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    pendingInstallVerification = false;
                    setInstallButtonsEnabled(true);
                    setStatus(getString(R.string.install_failed, error.toString()));
                });
            }
        }).start();
    }

    private String buildRootInstallCommand(InstallOptions options) {
        StringBuilder command = new StringBuilder();
        if (selectedApkSet.isSingleApk()) {
            command.append("pm install");
            appendRootInstallOptions(command, options);
            command.append(" -r ")
                    .append(shellQuote(selectedApkSet.getApkFiles().get(0).getAbsolutePath()));
        } else {
            command.append("pm install-multiple");
            appendRootInstallOptions(command, options);
            command.append(" -r");
            for (File apkFile : selectedApkSet.getApkFiles()) {
                command.append(' ').append(shellQuote(apkFile.getAbsolutePath()));
            }
        }
        if (options.setInstallSource) {
            command.append(" && ")
                    .append(buildSetInstallerCommand(selectedPackageName, options.installerPackageName));
        }
        return command.toString();
    }

    private void appendRootInstallOptions(StringBuilder command, InstallOptions options) {
        if (options.installForAllUsers) {
            command.append(" --user all");
        } else {
            command.append(" --user ").append(options.targetUserId);
        }
        if (options.allowTestOnly) {
            command.append(" -t");
        }
        if (options.setInstallSource) {
            command.append(" -i ").append(shellQuote(options.installerPackageName));
        }
        if (options.allowDowngrade) {
            command.append(" -d");
        }
        if (options.grantAllPermissions) {
            command.append(" -g");
        }
        if (!options.allowRestrictedPermissions && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            command.append(" --restrict-permissions");
        }
        if ((options.disableVerification || options.disableAdbVerify || options.bypassPlayProtect)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            command.append(" --skip-verification");
        }
        if (options.enableRollback && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            command.append(" --enable-rollback");
        }
        if (options.bypassLowTargetSdk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            command.append(" --bypass-low-target-sdk-block");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            command.append(" --package-source ").append(options.packageSource);
        }
        if (options.requestUpdateOwnership && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            command.append(" --update-ownership");
        }
        command.append(" --install-reason ").append(options.installReason);
    }

    private String buildSetInstallerCommand(String packageName, String installerPackageName) {
        return "(cmd package set-installer " + shellQuote(packageName) + " " +
                shellQuote(installerPackageName) + " 2>&1 || " +
                "pm set-installer " + shellQuote(packageName) + " " +
                shellQuote(installerPackageName) + " 2>&1 || true)";
    }

    private void rememberInstallAttempt(InstallOptions options) {
        lastInstallOptions = options;
        lastInstallRoute = selectedInstallRoute;
        lastAdbVerifyStatus = null;
    }

    private String applyRootPreInstallSettings(InstallOptions options) {
        if (!shouldDisableAdbVerify(options)) return null;
        return verifyAdbSettingResult(runSuWithCmd(buildDisableAdbVerifyCommand()));
    }

    private String applyShizukuPreInstallSettings(InstallOptions options) {
        if (!shouldDisableAdbVerify(options)) return null;
        try {
            return verifyAdbSettingResult(runShizukuShellCommand(buildDisableAdbVerifyCommand()));
        } catch (Exception error) {
            String message = error.getMessage() == null ? error.toString() : error.getMessage();
            return getString(R.string.advanced_status_failed, message);
        }
    }

    private boolean shouldDisableAdbVerify(InstallOptions options) {
        return options.disableAdbVerify || options.bypassPlayProtect;
    }

    private String buildDisableAdbVerifyCommand() {
        return "settings put global verifier_verify_adb_installs 0 && " +
                "settings get global verifier_verify_adb_installs";
    }

    private String verifyAdbSettingResult(StreamLogs logs) {
        if (logs == null) {
            return getString(R.string.advanced_status_failed, getString(R.string.install_source_unknown));
        }
        String error = logs.getErrorStreamLog();
        if (!error.isEmpty()) {
            return getString(R.string.advanced_status_failed, error);
        }
        String output = logs.getInputStreamLog().trim();
        if ("0".equals(lastLine(output))) {
            return getString(R.string.advanced_status_disabled);
        }
        return getString(
                R.string.advanced_status_failed,
                output.isEmpty() ? getString(R.string.install_source_unknown) : output
        );
    }

    private static String lastLine(String value) {
        if (value == null || value.isEmpty()) return "";
        int index = value.lastIndexOf('\n');
        return index == -1 ? value.trim() : value.substring(index + 1).trim();
    }

    private static StreamLogs runShizukuShellCommand(String cmd) throws Exception {
        StreamLogs streamLogs = new StreamLogs();
        streamLogs.setOutputStreamLog(cmd);
        Method method = Shizuku.class.getDeclaredMethod(
                "newProcess",
                String[].class,
                String[].class,
                String.class
        );
        method.setAccessible(true);
        Process process = (Process) method.invoke(
                null,
                new Object[]{new String[]{"sh", "-c", cmd}, null, null}
        );
        try (InputStream inputStream = process.getInputStream();
             InputStream errorStream = process.getErrorStream()) {
            try {
                process.waitFor();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            streamLogs.setInputStreamLog(readStream(inputStream));
            streamLogs.setErrorStreamLog(readStream(errorStream));
        } finally {
            process.destroy();
        }
        return streamLogs;
    }

    private void installAsShizuku() {
        if (!ensureShizukuReady(true)) return;
        InstallOptions options = createInstallOptions();
        rememberInstallAttempt(options);
        markInstallVerificationPending();
        setInstallButtonsEnabled(false);
        new Thread(() -> {
            try {
                lastAdbVerifyStatus = applyShizukuPreInstallSettings(options);
                if (shizukuInstaller == null) {
                    shizukuInstaller = new KingShizukuInstaller(getApplication());
                }
                shizukuInstaller.install(selectedApkSet.getApkFiles(), selectedPackageName, options);
                runOnUiThread(() -> {
                    setInstallButtonsEnabled(true);
                    Toast.makeText(this, R.string.shizuku_install_success, Toast.LENGTH_SHORT).show();
                    verifySelectedPackageWithRetry();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    pendingInstallVerification = false;
                    setInstallButtonsEnabled(true);
                    setStatus(getString(R.string.install_failed, error.getMessage() == null ? error.toString() : error.getMessage()));
                });
            }
        }).start();
    }

    private void installAsKing() {
        InstallOptions options = createInstallOptions().withoutPrivilegedOptions();
        rememberInstallAttempt(options);
        if (!selectedApkSet.isSingleApk()) {
            installSplitSessionWithUserAction(options);
            return;
        }
        File apkFile = selectedApkSet.getApkFiles().get(0);
        try {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            Uri fileUri = FileProvider.getUriForFile(
                    getApplicationContext(),
                    getPackageName() + ".provider",
                    apkFile
            );
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setData(fileUri);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            if (options.setInstallSource) {
                intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, options.installerPackageName);
            }
            grantReadPermissionToInstallers(intent, fileUri);
            setStatus(getString(R.string.install_started_no_root));
            markInstallVerificationPending();
            if (isOemRouteSelected()) {
                Intent chooser = Intent.createChooser(intent, getString(R.string.choose_package_installer));
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                chooser.setClipData(ClipData.newRawUri("apk", fileUri));
                startActivity(chooser);
            } else {
                startActivity(intent);
            }
        } catch (Exception error) {
            pendingInstallVerification = false;
            setStatus(getString(R.string.install_failed, error.toString()));
        }
    }

    private void installSplitSessionWithUserAction(InstallOptions options) {
        markInstallVerificationPending();
        setInstallButtonsEnabled(false);
        new Thread(() -> {
            PackageInstaller packageInstaller = getPackageManager().getPackageInstaller();
            int sessionId = -1;
            PackageInstaller.Session session = null;
            boolean committed = false;
            try {
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                try {
                    params.setAppPackageName(selectedPackageName);
                } catch (Throwable ignored) {
                }
                applySessionOptions(params, options);

                sessionId = packageInstaller.createSession(params);
                session = packageInstaller.openSession(sessionId);
                ApkSessionWriter.writeApks(session, selectedApkSet.getApkFiles());

                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<SessionInstallResult> resultRef = new AtomicReference<>();
                IntentSender sender = createIntentSender(intent -> {
                    int status = intent.getIntExtra(
                            PackageInstaller.EXTRA_STATUS,
                            PackageInstaller.STATUS_FAILURE
                    );
                    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                        Object confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                        if (confirmation instanceof Intent) {
                            runOnUiThread(() -> {
                                Intent confirmationIntent = (Intent) confirmation;
                                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(confirmationIntent);
                            });
                        }
                        return;
                    }
                    String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                    resultRef.set(new SessionInstallResult(status, message));
                    latch.countDown();
                });

                session.commit(sender);
                committed = true;
                if (!latch.await(5, TimeUnit.MINUTES)) {
                    throw new IOException("Timed out waiting for package installer result");
                }

                SessionInstallResult result = resultRef.get();
                if (result == null) {
                    throw new IOException("No package installer result was returned");
                }
                if (result.status != PackageInstaller.STATUS_SUCCESS) {
                    throw new IOException(result.message == null ? "Package installer failed" : result.message);
                }
                runOnUiThread(() -> {
                    setInstallButtonsEnabled(true);
                    verifySelectedPackageWithRetry();
                });
            } catch (Exception error) {
                pendingInstallVerification = false;
                if (!committed && sessionId != -1) {
                    try {
                        packageInstaller.abandonSession(sessionId);
                    } catch (Throwable ignored) {
                    }
                }
                runOnUiThread(() -> {
                    setInstallButtonsEnabled(true);
                    setStatus(getString(
                            R.string.install_failed,
                            error.getMessage() == null ? error.toString() : error.getMessage()
                    ));
                });
            } finally {
                if (session != null) {
                    try {
                        session.close();
                    } catch (Throwable ignored) {
                    }
                }
            }
        }).start();
        setStatus(getString(R.string.split_install_started));
    }

    private void applySessionOptions(PackageInstaller.SessionParams params, InstallOptions options) {
        try {
            params.setInstallReason(options.installReason);
        } catch (Throwable ignored) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                params.setPackageSource(options.packageSource);
            } catch (Throwable ignored) {
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (options.setInstallSource) {
                try {
                    params.setInstallerPackageName(options.installerPackageName);
                } catch (Throwable ignored) {
                }
            }
            if (options.requestUpdateOwnership) {
                try {
                    params.setRequestUpdateOwnership(true);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private void grantReadPermissionToInstallers(Intent intent, Uri apkUri) {
        List<ResolveInfo> installers = getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo installer : installers) {
            if (installer.activityInfo == null || installer.activityInfo.packageName == null) continue;
            try {
                grantUriPermission(
                        installer.activityInfo.packageName,
                        apkUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (SecurityException ignored) {
            }
        }
    }

    private void showFileChooser() {
        try {
            apkPicker.launch(new String[]{
                    "application/vnd.android.package-archive",
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream",
                    "*/*"
            });
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        Uri uri = null;
        if (ApkReceiverActivity.ACTION_OPEN_APK.equals(intent.getAction())) {
            Object extra = intent.getParcelableExtra(ApkReceiverActivity.EXTRA_APK_URI);
            if (extra instanceof Uri) {
                uri = (Uri) extra;
            }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())
                || Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())) {
            uri = intent.getData();
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Object stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (stream instanceof Uri) {
                uri = (Uri) stream;
            }
        }
        if (uri != null) {
            persistSelectedUriPermission(uri);
            handleSelectedApk(uri);
        }
    }

    private boolean restoreRetainedSelection() {
        Object retained = getLastCustomNonConfigurationInstance();
        if (!(retained instanceof RetainedSelection)) {
            return false;
        }
        RetainedSelection selection = (RetainedSelection) retained;
        selectedApkSet = selection.apkSet;
        selectedSourceUri = selection.sourceUri;
        selectedAppName = selection.appName;
        selectedPackageName = selection.packageName;
        selectedLabel = selection.label;
        renderSelectedApkInfo();
        return true;
    }

    private void handleSelectedApk(Uri uri) {
        try {
            selectedApkSet = null;
            clearTempFile();
            selectedApkSet = ApkSetExtractor.fromUri(this, uri, TEMP_DIR_NAME);
            selectedSourceUri = uri;
            selectedAppName = selectedApkSet.getAppName();
            selectedPackageName = selectedApkSet.getPackageName();
            selectedLabel = selectedApkSet.getLabel();
            renderSelectedApkInfo();
            verifySelectedPackage();
        } catch (Exception error) {
            selectedApkSet = null;
            selectedSourceUri = null;
            selectedAppName = null;
            selectedPackageName = null;
            selectedLabel = null;
            pathEdit.setText("");
            apkInfoText.setText(R.string.no_apk_selected);
            updateOpenButton(false);
            setStatus(getString(
                    R.string.install_failed,
                    error.getMessage() == null ? error.toString() : error.getMessage()
            ));
        }
    }

    private void renderSelectedApkInfo() {
        if (selectedApkSet == null) return;
        pathEdit.setText(selectedLabel);
        updateOpenButton(false);
        String appName = selectedAppName == null ? selectedLabel : selectedAppName;
        String version = selectedApkSet.getVersionName() == null
                ? getString(R.string.install_source_unknown)
                : selectedApkSet.getVersionName();
        String minSdk = sdkLabel(selectedApkSet.getMinSdkVersion());
        String targetSdk = sdkLabel(selectedApkSet.getTargetSdkVersion());
        String size = formatBytes(selectedApkSet.getTotalSizeBytes());
        int apkCount = selectedApkSet.getApkCount();
        apkInfoText.setText(apkCount > 1
                ? getString(R.string.apk_info_bundle, appName, selectedPackageName, version, minSdk, targetSdk, size, apkCount)
                : getString(R.string.apk_info_single, appName, selectedPackageName, version, minSdk, targetSdk, size));
    }

    private String sdkLabel(int sdk) {
        return sdk > 0 ? String.valueOf(sdk) : getString(R.string.install_source_unknown);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) return getString(R.string.install_source_unknown);
        final long unit = 1024;
        if (bytes < unit) return bytes + " B";
        double value = bytes;
        String[] units = {"KB", "MB", "GB"};
        int unitIndex = -1;
        do {
            value /= unit;
            unitIndex++;
        } while (value >= unit && unitIndex < units.length - 1);
        return String.format(Locale.US, value >= 10 ? "%.0f %s" : "%.1f %s", value, units[unitIndex]);
    }

    private void persistSelectedUriPermission(Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return;
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException | IllegalArgumentException ignored) {
        }
    }

    private boolean ensureApkSelected() {
        if (selectedApkSet == null
                || selectedApkSet.getApkFiles().isEmpty()
                || selectedPackageName == null) {
            Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void verifySelectedPackage() {
        if (selectedPackageName == null) {
            updateOpenButton(false);
            return;
        }
        verifyInstallSource(selectedPackageName);
    }

    private void verifySelectedPackageWithRetry() {
        if (selectedPackageName == null) return;
        String packageName = selectedPackageName;
        int sequence = ++verificationSequence;
        new Thread(() -> {
            for (int attempt = 0; attempt < PACKAGE_VISIBILITY_CHECK_ATTEMPTS; attempt++) {
                if (isPackageVisible(packageName)) {
                    break;
                }
                try {
                    Thread.sleep(PACKAGE_VISIBILITY_RETRY_DELAY_MS);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            runOnUiThread(() -> {
                if (sequence != verificationSequence || !packageName.equals(selectedPackageName)) {
                    return;
                }
                pendingInstallVerification = false;
                verifyInstallSource(packageName);
            });
        }).start();
    }

    private boolean isPackageVisible(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private void markInstallVerificationPending() {
        pendingInstallVerification = true;
        updateOpenButton(false);
    }

    private void verifyInstallSource(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            pm.getPackageInfo(packageName, 0);
            updateOpenButton(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo info = pm.getInstallSourceInfo(packageName);
                String installerPackage = info.getInstallingPackageName();
                setStatus(withAdvancedStatus(
                        packageName,
                        installerPackage,
                        getString(
                        R.string.install_source_status,
                        packageName,
                        displaySource(installerPackage)
                )));
            } else {
                String installerPackage = pm.getInstallerPackageName(packageName);
                setStatus(withAdvancedStatus(
                        packageName,
                        installerPackage,
                        getString(
                        R.string.install_source_status_legacy,
                        packageName,
                        displaySource(installerPackage)
                )));
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            updateOpenButton(false);
            setStatus(withAdvancedStatus(
                    packageName,
                    null,
                    getString(R.string.install_source_not_installed, packageName)
            ));
        } catch (Exception error) {
            updateOpenButton(false);
            setStatus(error.toString());
        }
    }

    private String withAdvancedStatus(String packageName, String reportedInstallerPackage, String baseStatus) {
        String advancedStatus = buildAdvancedStatus(packageName, reportedInstallerPackage);
        if (advancedStatus.isEmpty()) return baseStatus;
        return baseStatus + "\n\n" + advancedStatus;
    }

    private String buildAdvancedStatus(String packageName, String reportedInstallerPackage) {
        InstallOptions options = lastInstallOptions;
        if (options == null || selectedPackageName == null || !packageName.equals(selectedPackageName)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(getString(R.string.advanced_status_header));
        if (options.setInstallSource) {
            String status = options.installerPackageName.equals(reportedInstallerPackage)
                    ? getString(R.string.advanced_status_set)
                    : getString(R.string.advanced_status_failed_expected, displaySource(reportedInstallerPackage));
            appendAdvancedLine(builder, getString(R.string.install_source_package), status);
        }
        if (options.installReason != DEFAULT_INSTALL_REASON) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.install_reason),
                    optionRouteStatus(true, 0)
            );
        }
        if (options.packageSource != DEFAULT_PACKAGE_SOURCE) {
            int minSdk = isRootRoute(lastInstallRoute)
                    ? Build.VERSION_CODES.VANILLA_ICE_CREAM
                    : Build.VERSION_CODES.TIRAMISU;
            appendAdvancedLine(
                    builder,
                    getString(R.string.package_source),
                    optionRouteStatus(true, minSdk)
            );
        }
        if (options.targetUserId != 0 && !options.installForAllUsers) {
            appendAdvancedLine(builder, getString(R.string.target_user), optionRouteStatus(true, 0));
        }
        if (options.allowTestOnly) {
            appendAdvancedLine(builder, getString(R.string.allow_test_apks), optionRouteStatus(true, 0));
        }
        if (options.installForAllUsers) {
            appendAdvancedLine(builder, getString(R.string.install_for_all_users), optionRouteStatus(true, 0));
        }
        if (options.allowDowngrade) {
            appendAdvancedLine(builder, getString(R.string.allow_downgrade), optionRouteStatus(true, 0));
        }
        if (options.bypassLowTargetSdk) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.bypass_low_target_sdk),
                    optionRouteStatus(true, Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            );
        }
        if (options.grantAllPermissions) {
            appendAdvancedLine(builder, getString(R.string.grant_all_permissions), optionRouteStatus(true, 0));
        }
        if (options.allowRestrictedPermissions) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.allow_restricted_permissions),
                    optionRouteStatus(true, Build.VERSION_CODES.S)
            );
        }
        if (options.disableVerification) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.disable_verification),
                    optionRouteStatus(true, Build.VERSION_CODES.Q)
            );
        }
        if (options.disableAdbVerify || options.bypassPlayProtect) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.disable_adb_verify),
                    lastAdbVerifyStatus == null
                            ? optionRouteStatus(true, 0)
                            : lastAdbVerifyStatus
            );
        }
        if (options.enableRollback) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.enable_rollback),
                    optionRouteStatus(true, Build.VERSION_CODES.Q)
            );
        }
        if (options.fromAdb) {
            appendAdvancedLine(builder, getString(R.string.from_adb), optionRouteStatus(true, 0));
        }
        if (options.requestUpdateOwnership) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.request_update_ownership),
                    optionRouteStatus(true, Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            );
        }
        if (options.bypassPlayProtect) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.bypass_play_protect),
                    optionRouteStatus(true, Build.VERSION_CODES.Q)
            );
        }
        if (options.privateSpaceInstall) {
            appendAdvancedLine(
                    builder,
                    getString(R.string.private_space_install),
                    optionRouteStatus(isShizukuRoute(lastInstallRoute), Build.VERSION_CODES.VANILLA_ICE_CREAM)
            );
        }
        return builder.toString().equals(getString(R.string.advanced_status_header)) ? "" : builder.toString();
    }

    private String optionRouteStatus(boolean supportedByRoute, int minSdk) {
        if (isNoRootRoute(lastInstallRoute)) {
            return getString(R.string.advanced_status_skipped_no_root);
        }
        if (!supportedByRoute) {
            return getString(R.string.advanced_status_skipped_method);
        }
        if (minSdk > 0 && Build.VERSION.SDK_INT < minSdk) {
            return getString(R.string.advanced_status_skipped_sdk, minSdk);
        }
        return getString(R.string.advanced_status_submitted);
    }

    private void appendAdvancedLine(StringBuilder builder, String label, String status) {
        builder.append('\n')
                .append(getString(R.string.advanced_status_line, label, status));
    }

    private void openSelectedPackage() {
        if (selectedPackageName == null) {
            Toast.makeText(this, R.string.select_a_file, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(selectedPackageName);
            if (launchIntent == null) {
                if (!isPackageVisible(selectedPackageName)) {
                    updateOpenButton(false);
                    setStatus(getString(R.string.install_source_not_installed, selectedPackageName));
                } else {
                    setStatus(getString(R.string.open_installed_app_unavailable));
                }
                return;
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } catch (Exception error) {
            setStatus(error.toString());
        }
    }

    private void updateOpenButton(boolean enabled) {
        if (openButton != null) {
            openButton.setEnabled(enabled);
        }
    }

    private String displaySource(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return getString(R.string.install_source_unknown);
        }
        if (PLAY_STORE_PACKAGE.equals(packageName)) {
            return "Google Play Store (" + packageName + ")";
        }
        if ("com.android.vending".equals(packageName)) {
            return "Google Play Store (" + packageName + ")";
        }
        if ("com.android.shell".equals(packageName)) {
            return "Android shell (" + packageName + ")";
        }
        return packageName;
    }

    private boolean ensureShizukuReady(boolean requestPermission) {
        try {
            if (Shizuku.isPreV11()) {
                setStatus(getString(R.string.shizuku_not_ready));
                return false;
            }
            if (!Shizuku.pingBinder()) {
                setStatus(getString(R.string.shizuku_not_ready));
                return false;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
            if (requestPermission && !Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST);
                setStatus(getString(R.string.shizuku_permission_requested));
            } else {
                setStatus(getString(R.string.shizuku_not_ready));
            }
            return false;
        } catch (Throwable error) {
            setStatus(getString(R.string.shizuku_not_ready));
            return false;
        }
    }

    private void openGooglePackageInstallerSettings() {
        if (!isGooglePackageExist()) {
            setStatus(getString(R.string.missing_google_package_installer));
            return;
        }
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + GOOGLE_PACKAGE_INSTALLER));
            startActivity(intent);
        } catch (Exception error) {
            setStatus(error.toString());
        }
    }

    @SuppressLint("BatteryLife")
    private void requestIgnoreBatteryOptimization() {
        if (isIgnoringBatteryOptimizations()) {
            setStatus(getString(R.string.battery_optimization_ignored));
            updateBatteryButtonLabel();
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            setStatus(getString(R.string.battery_optimization_requested));
        } catch (Exception error) {
            setStatus(error.toString());
        }
    }

    private void updateBatteryButtonLabel() {
        if (ignoreBatteryButton == null) return;
        ignoreBatteryButton.setText(isIgnoringBatteryOptimizations()
                ? R.string.ignore_battery_optimization_done
                : R.string.ignore_battery_optimization);
    }

    private boolean isIgnoringBatteryOptimizations() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public final void clearTempFile() {
        File dir = new File(getFilesDir(), TEMP_DIR_NAME);
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (selectedApkSet != null && selectedApkSet.getStorageRoot().equals(file)) continue;
            ApkSetExtractor.deleteRecursive(file);
        }
    }

    private void setInstallButtonsEnabled(boolean enabled) {
        installButton.setEnabled(enabled);
    }

    private void setStatus(String message) {
        statusText.setText(message == null ? "" : message);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception error) {
            setStatus(error.toString());
        }
    }

    private static class RetainedSelection {
        final ApkSet apkSet;
        final Uri sourceUri;
        final String appName;
        final String packageName;
        final String label;

        RetainedSelection(
                ApkSet apkSet,
                Uri sourceUri,
                String appName,
                String packageName,
                String label
        ) {
            this.apkSet = apkSet;
            this.sourceUri = sourceUri;
            this.appName = appName;
            this.packageName = packageName;
            this.label = label;
        }
    }

    private interface IntentCallback {
        void onIntent(Intent intent);
    }

    private static IntentSender createIntentSender(IntentCallback callback) {
        android.content.IIntentSender.Stub binder = new android.content.IIntentSender.Stub() {
            @Override
            public int send(
                    int code,
                    Intent intent,
                    String resolvedType,
                    android.content.IIntentReceiver finishedReceiver,
                    String requiredPermission,
                    Bundle options
            ) {
                if (intent != null) callback.onIntent(intent);
                return 0;
            }

            @Override
            public void send(
                    int code,
                    Intent intent,
                    String resolvedType,
                    IBinder whitelistToken,
                    android.content.IIntentReceiver finishedReceiver,
                    String requiredPermission,
                    Bundle options
            ) {
                if (intent != null) callback.onIntent(intent);
            }
        };

        try {
            java.lang.reflect.Constructor<IntentSender> ctor =
                    IntentSender.class.getDeclaredConstructor(android.content.IIntentSender.class);
            ctor.setAccessible(true);
            return ctor.newInstance(binder);
        } catch (ReflectiveOperationException error) {
            throw new RuntimeException(error);
        }
    }

    private static class SessionInstallResult {
        final int status;
        final String message;

        SessionInstallResult(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public static StreamLogs runSuWithCmd(String cmd) {
        StreamLogs streamLogs = new StreamLogs();
        streamLogs.setOutputStreamLog(cmd);

        try {
            Process su = Runtime.getRuntime().exec("su");
            try (DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                 InputStream inputStream = su.getInputStream();
                 InputStream errorStream = su.getErrorStream()) {
                outputStream.writeBytes(cmd + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n");
                outputStream.flush();

                try {
                    su.waitFor();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                }
                streamLogs.setInputStreamLog(readStream(inputStream));
                streamLogs.setErrorStreamLog(readStream(errorStream));
            }
        } catch (IOException error) {
            streamLogs.setErrorStreamLog(error.toString());
        }

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

    private boolean isDeviceRooted() {
        return hasRootBuildTags()
                || hasKnownRootManagerPackage()
                || hasRootBinary()
                || hasRootArtifact()
                || hasRootSystemProperty()
                || canFindSu();
    }

    private static boolean hasRootBuildTags() {
        String buildTags = Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private boolean hasKnownRootManagerPackage() {
        PackageManager packageManager = getPackageManager();
        for (String packageName : ROOT_MANAGER_PACKAGES) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
                } else {
                    packageManager.getPackageInfo(packageName, 0);
                }
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            } catch (Throwable error) {
                Log.d(TAG, "Root manager package check failed for " + packageName, error);
            }
        }
        return false;
    }

    private static boolean hasRootBinary() {
        for (String path : ROOT_BINARY_PATHS) {
            File file = new File(path);
            if (file.exists() && (file.canExecute() || path.endsWith(".apk") || path.endsWith("/su"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRootArtifact() {
        for (String path : ROOT_ARTIFACT_PATHS) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRootSystemProperty() {
        StringBuilder command = new StringBuilder();
        for (String key : ROOT_PROPERTY_KEYS) {
            command.append("getprop ").append(key).append("; ");
        }
        String output = runCommandForOutput(new String[]{"sh", "-c", command.toString()}, 1000);
        for (String value : output.split("\\R")) {
            value = value.trim();
            if (!value.isEmpty() && !"stopped".equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canFindSu() {
        return commandSucceeds(new String[]{
                "sh",
                "-c",
                "command -v su >/dev/null 2>&1 || which su >/dev/null 2>&1 || su -v >/dev/null 2>&1 || su -V >/dev/null 2>&1"
        });
    }

    private static boolean commandSucceeds(String[] command) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(ROOT_COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static String runCommandForOutput(String[] command, long timeoutMs) {
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0) {
                return "";
            }
            return readStream(process.getInputStream());
        } catch (Throwable ignored) {
            return "";
        } finally {
            if (process != null) process.destroy();
        }
    }
}
