/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.car.messenger.core.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.android.car.apps.common.log.L;

/**
 * Receiver that listens for on boot completed broadcast intent and starts {@link MessengerService}.
 */
public class OnBootReceiver extends BroadcastReceiver {
    private static final String TAG = "CM.OnBootReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        L.d(TAG, "BootReceiver received " + intent.getAction());
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startService(new Intent(context, MessengerService.class));
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            context.startService(new Intent(context, MessengerService.class));
        }
    }
}
