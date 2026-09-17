// SPDX-License-Identifier: Apache-2.0
package org.lineageos.settings.kiwi;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

/**
 * CollapsingToolbarBaseActivity is what gives the Android 13 large-title
 * header that the rest of Settings uses. It is an androidx FragmentActivity,
 * so the fragment goes in through getSupportFragmentManager().
 */
public class BypassChargingActivity extends CollapsingToolbarBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bypass_content);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.bypass_content, new BypassChargingFragment())
                    .commit();
        }
    }
}
