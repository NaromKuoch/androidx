/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.mediarouter.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Provides an utility method to show the system's output switcher dialog.
 *
 * @see <a href="https://developer.android.com/guide/topics/media/media-routing">Media Routing</a>
 */
public final class SystemOutputSwitcherDialogController {

    /** System ui service package name. */
    private static final String PACKAGE_NAME_SYSTEM_UI =
            "com.android.systemui";

    /** Output switcher dialog intent action in Android S. **/
    private static final String OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S =
            "com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG";

    /** Output switcher dialog intent action in Android R. **/
    private static final String OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R =
            "com.android.settings.panel.action.MEDIA_OUTPUT";

    /** A package name key for output switcher intent in Android S. */
    private static final String OUTPUT_SWITCHER_INTENT_KEY_PACKAGE_NAME_ANDROID_S =
            "package_name";

    /** A package name key for output switcher intent in Android R. */
    private static final String OUTPUT_SWITCHER_INTENT_KEY_PACKAGE_NAME_ANDROID_R =
            "com.android.settings.panel.extra.PACKAGE_NAME";

    private SystemOutputSwitcherDialogController() {
        // Private to prevent new instances.
    }

    /**
     * Shows the system output switcher dialog.
     *
     * @param context Android context
     * @return {@code true} if the dialog was shown successfully and {@code false} otherwise
     */
    public static boolean showDialog(@NonNull Context context) {
        boolean result = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            result = showDialogForAndroidSAndAbove(context)
                    // The intent action and related string constants are changed in S,
                    // however they are not public API yet. Try opening the output switcher with the
                    // old constants for devices that have prior version of the constants.
                    || showDialogForAndroidR(context);
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            result = showDialogForAndroidR(context);
        }

        if (result) {
            return true;
        }

        if (isRunningOnWear(context) && showBluetoothSettingsFragment(context)) {
            return true;
        }

        return false;
    }

    private static boolean showDialogForAndroidSAndAbove(@NonNull Context context) {
        Intent intent = new Intent()
                .setAction(OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_S)
                .setPackage(PACKAGE_NAME_SYSTEM_UI)
                .putExtra(OUTPUT_SWITCHER_INTENT_KEY_PACKAGE_NAME_ANDROID_S,
                        context.getPackageName());

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryBroadcastReceivers(intent,
                0 /* flags */);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.applicationInfo == null) {
                continue;
            }
            ApplicationInfo appInfo = activityInfo.applicationInfo;
            if (((ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                    & appInfo.flags) != 0) {
                context.sendBroadcast(intent);
                return true;
            }
        }

        return false;
    }

    private static boolean showDialogForAndroidR(@NonNull Context context) {
        Intent intent = new Intent()
                // Context can be either activity's or application's context,
                // therefore we need to start a new task.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(OUTPUT_SWITCHER_INTENT_ACTION_ANDROID_R)
                .putExtra(OUTPUT_SWITCHER_INTENT_KEY_PACKAGE_NAME_ANDROID_R,
                        context.getPackageName());

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,
                0 /* flags */);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.applicationInfo == null) {
                continue;
            }
            ApplicationInfo appInfo = activityInfo.applicationInfo;
            if (((ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                    & appInfo.flags) != 0) {
                context.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private static boolean showBluetoothSettingsFragment(@NonNull Context context) {
        // Wear OS specific intent. This is a default behaviour
        // for devices without the output switcher dialog.
        // See https://developer.android.com/training/wearables/overlays/audio.
        Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("EXTRA_CONNECTION_ONLY", true)
                .putExtra("EXTRA_CLOSE_ON_CONNECT", true)
                .putExtra("android.bluetooth.devicepicker.extra.FILTER_TYPE", 1);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,
                0 /* flags */);
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.applicationInfo == null) {
                continue;
            }
            ApplicationInfo appInfo = activityInfo.applicationInfo;
            if (((ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)
                    & appInfo.flags) != 0) {
                context.startActivity(intent);
                return true;
            }
        }
        return false;
    }

    private static boolean isRunningOnWear(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
    }
}
