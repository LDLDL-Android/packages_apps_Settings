/*
 * Copyright (C) 2020 The LineageOS Project
 * Copyright (C) 2022-2024 Nameless-AOSP
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

package org.evolution.settings.display.refreshrate;

import static android.provider.Settings.System.EXTREME_REFRESH_RATE;

import android.content.Context;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.evolution.DisplayRefreshRateHelper;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class MinRefreshRatePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private static final int LTPO_LOW_REFRESH_RATE = 30;
    private static final String OPLUS_LTPO_MIN_FPS_PROPERTY = "persist.sys.oplus_ltpo_min_fps";

    private final boolean mSupportsOplusLtpo;
    private final ArrayList<Integer> mSupportedList;
    private final DisplayRefreshRateHelper mHelper;

    private ListPreference mListPreference;

    public MinRefreshRatePreferenceController(Context context, String key) {
        super(context, key);

        mHelper = DisplayRefreshRateHelper.getInstance(context);
        mSupportsOplusLtpo = context.getResources().getBoolean(R.bool.config_supportsOplusLtpo);
        final TreeSet<Integer> rates = new TreeSet<>(mHelper.getSupportedRefreshRateList());
        if (mSupportsOplusLtpo) {
            // Zero releases the forced floor so stock ADFR can idle down to 1 Hz.
            rates.add(0);
            rates.add(LTPO_LOW_REFRESH_RATE);
        }
        mSupportedList = new ArrayList<>(rates);
    }

    @Override
    public int getAvailabilityStatus() {
        return mSupportedList.size() > 1 ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mListPreference = screen.findPreference(getPreferenceKey());

        final List<String> entries = new ArrayList<>();
        final List<String> values = new ArrayList<>();

        for (int i = 0; i < mSupportedList.size(); ++i) {
            final String refreshRate = String.valueOf(mSupportedList.get(i));
            entries.add((mSupportsOplusLtpo && mSupportedList.get(i) == 0
                    ? "1" : refreshRate) + " Hz");
            values.add(refreshRate);
        }

        mListPreference.setEntries(entries.toArray(new String[entries.size()]));
        mListPreference.setEntryValues(values.toArray(new String[values.size()]));
    }

    @Override
    public void updateState(Preference preference) {
        final String refreshRate = String.valueOf(mHelper.getMinimumRefreshRate());
        final int index = mListPreference.findIndexOfValue(refreshRate);
        if (index != -1) {
            mListPreference.setValueIndex(index);
            mListPreference.setSummary(mListPreference.getEntries()[index]);
        } else {
            mListPreference.setSummary(refreshRate + " Hz");
        }

        final int minRefreshRate = Integer.parseInt(refreshRate);
        final int peakRefreshRate = mHelper.getPeakRefreshRate();
        if (peakRefreshRate < minRefreshRate) {
            mHelper.setPeakRefreshRate(minRefreshRate);
        }

        final boolean extremeMode = Settings.System.getIntForUser(
                mContext.getContentResolver(), EXTREME_REFRESH_RATE,
                0, UserHandle.USER_CURRENT) != 0;
        mListPreference.setEnabled(!extremeMode);
        if (mSupportsOplusLtpo) {
            final String value = String.valueOf(minRefreshRate >= LTPO_LOW_REFRESH_RATE
                    ? minRefreshRate : 0);
            if (!value.equals(SystemProperties.get(OPLUS_LTPO_MIN_FPS_PROPERTY))) {
                SystemProperties.set(OPLUS_LTPO_MIN_FPS_PROPERTY, value);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int refreshRate;
        try {
            refreshRate = Integer.parseInt(String.valueOf(newValue));
        } catch (NumberFormatException e) {
            return false;
        }
        if (!mSupportedList.contains(refreshRate)) {
            return false;
        }
        mHelper.setMinimumRefreshRate(refreshRate);
        updateState(preference);
        return true;
    }
}
