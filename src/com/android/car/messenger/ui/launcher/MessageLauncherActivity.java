/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.messenger.ui.launcher;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.services.MessengerService;
import com.android.car.messenger.ui.conversationlist.ConversationListFragment;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;

/**
 * This is the launcher activity for the messaging app. This first routes to{@link
 * ConversationListFragment} or displays an error when no {@link UserAccount} are found.
 */
public class MessageLauncherActivity extends FragmentActivity implements InsetsChangedListener {
    private static final String TAG = "CM.MessageLauncherActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        L.d(TAG, "onCreate: MessageLauncher");
        super.onCreate(savedInstanceState);
        MessageLauncherViewModel viewModel =
                new ViewModelProvider(this).get(MessageLauncherViewModel.class);

        L.d(TAG, "Starting MessengerService");
        startService(new Intent(this, MessengerService.class));

        viewModel
                .getAccounts()
                .observe(
                        this,
                        accounts -> {
                            L.d(TAG, "Total number of accounts: %d", accounts.size());
                            // First version only takes one device until multi-account support is
                            // added
                            UserAccount primaryAccount =
                                    !accounts.isEmpty() ? accounts.get(0) : null;
                            String fragmentTag =
                                    ConversationListFragment.getFragmentTag(primaryAccount);
                            Fragment fragment =
                                    getSupportFragmentManager().findFragmentByTag(fragmentTag);
                            if (fragment == null) {
                                fragment = ConversationListFragment.newInstance(primaryAccount);
                            }
                            setContentFragment(fragment, fragmentTag);
                        });
    }

    private void setContentFragment(Fragment fragment, String fragmentTag) {
        getSupportFragmentManager().executePendingTransactions();
        while (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        pushContentFragment(fragment, fragmentTag);
    }

    @Override
    protected void onResume() {
        L.d(TAG, "onResumeL: Message Activity");
        AppFactory.get().getDataModel().refresh();
        super.onResume();
    }

    private void pushContentFragment(
            @NonNull Fragment topContentFragment, @NonNull String fragmentTag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, topContentFragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit();
    }

    @Override
    public void onCarUiInsetsChanged(Insets insets) {
        // Do nothing, this is just a marker that we will handle the insets in fragments.
        // This is only necessary because the fragments are not immediately added to the
        // activity when calling .commit()
    }

    @Override
    public void onBackPressed() {
        // By default, onBackPressed will pop all the fragments off the backstack and then finish
        // the activity. We want to finish the activity when there is only one fragment left.
        if (isBackNavigationAvailable()) {
            super.onBackPressed();
        } else {
            finishAfterTransition();
        }
    }

    private boolean isBackNavigationAvailable() {
        return getSupportFragmentManager().getBackStackEntryCount() > 1;
    }
}
