/*
 * SPDX-FileCopyrightText: The Evolution X Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.settings.display;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

/**
 * Selects the touch report rate mode on devices that define several. Replaces the
 * high touch polling rate switch: mode 0 disables it, any other mode enables it
 * and is written by the touch HAL instead of its default enable value.
 */
public class TouchReportRatePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY = "touch_report_rate_mode";
    private static final String KEY_HIGH_TOUCH_POLLING_RATE = "high_touch_polling_rate_enable";
    private static final String PROP_REPORT_RATE_MODE = "persist.sys.oplus.touch.report_rate_mode";
    private static final String MODE_DEFAULT = "0";

    private final String[] mEntries;
    private final String[] mValues;

    public TouchReportRatePreferenceController(Context context) {
        super(context, KEY);
        mEntries = context.getResources().getStringArray(R.array.config_touchReportRateEntries);
        mValues = context.getResources().getStringArray(R.array.config_touchReportRateValues);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mValues.length < 2 || mValues.length != mEntries.length) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return LineageHardwareManager.getInstance(mContext).isSupported(
                LineageHardwareManager.FEATURE_HIGH_TOUCH_POLLING_RATE)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        // The display settings XML may be replaced (e.g. by SettingsGoogle), so put the list
        // next to the high touch polling rate switch it replaces instead of declaring it there.
        final Preference toggle = screen.findPreference(KEY_HIGH_TOUCH_POLLING_RATE);
        ListPreference preference = screen.findPreference(getPreferenceKey());
        if (preference == null && isAvailable() && toggle != null && toggle.getParent() != null) {
            preference = new ListPreference(toggle.getContext());
            preference.setKey(getPreferenceKey());
            preference.setTitle(R.string.touch_report_rate_title);
            preference.setOrder(toggle.getOrder());
            preference.setPersistent(false);
            preference.setIconSpaceReserved(toggle.isIconSpaceReserved());
            toggle.getParent().addPreference(preference);
        }
        super.displayPreference(screen);
        if (preference == null || !isAvailable()) {
            return;
        }
        if (toggle != null) {
            toggle.setVisible(false);
        }
        preference.setEntries(mEntries);
        preference.setEntryValues(mValues);
        preference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setValue(getCurrentMode());
        listPreference.setSummary(listPreference.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String mode = String.valueOf(newValue);
        SystemProperties.set(PROP_REPORT_RATE_MODE, mode);
        // Toggle the switch so the touch HAL rewrites the newly selected mode.
        LineageSettings.System.putInt(mContext.getContentResolver(),
                LineageSettings.System.HIGH_TOUCH_POLLING_RATE_ENABLE, 0);
        if (!MODE_DEFAULT.equals(mode)) {
            LineageSettings.System.putInt(mContext.getContentResolver(),
                    LineageSettings.System.HIGH_TOUCH_POLLING_RATE_ENABLE, 1);
        }
        updateState(preference);
        return true;
    }

    private String getCurrentMode() {
        final boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.HIGH_TOUCH_POLLING_RATE_ENABLE, 0) == 1;
        if (!enabled) {
            return MODE_DEFAULT;
        }
        final String mode = SystemProperties.get(PROP_REPORT_RATE_MODE, "");
        for (String value : mValues) {
            if (!MODE_DEFAULT.equals(value) && value.equals(mode)) {
                return value;
            }
        }
        // Enabled through the plain switch: the HAL default enable value is the last mode.
        return mValues[mValues.length - 1];
    }
}
