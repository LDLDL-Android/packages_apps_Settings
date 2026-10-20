/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.pluskey;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.settings.core.BasePreferenceController;

/** Shows the Plus Key homepage entry only on devices that ship the PlusKey app. */
public class TopLevelPlusKeyPreferenceController extends BasePreferenceController {

    private static final String PLUSKEY_PACKAGE = "com.oplus.pluskey";

    public TopLevelPlusKeyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        try {
            final ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(
                    PLUSKEY_PACKAGE, PackageManager.MATCH_SYSTEM_ONLY);
            return info.enabled ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } catch (PackageManager.NameNotFoundException e) {
            return UNSUPPORTED_ON_DEVICE;
        }
    }
}
