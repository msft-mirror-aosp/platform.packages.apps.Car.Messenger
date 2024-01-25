/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.car.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.interfaces.DataModel;
import com.android.car.messenger.messaging.TelephonyDataModel;
import com.android.car.messenger.util.CarStateListener;

/**
 * Test implementation of AppFactory to provide mocked Context, DataModel, SharedPreferences,
 * and CarStateListener.
 *
 * {@link #teardown()} must be run at the end of each test to ensure subsequent register calls go
 * through.
 */
public class AppFactoryTestImpl extends AppFactory {

    private Context mContext;
    private DataModel mDataModel;
    private SharedPreferences mSharedPreferences;

    public AppFactoryTestImpl(Context context,
            DataModel dataModel,
            SharedPreferences prefs,
            CarStateListener listener) {
        AppFactory.setInstance(this);
        sRegistered = true;
        sInitialized = true;

        mContext = context;
        mDataModel = dataModel;
        mSharedPreferences = prefs;
        mCarStateListener = listener;
    }

    public void teardown() {
        sRegistered = false;
        sInitialized = false;
        mContext = ApplicationProvider.getApplicationContext();
        mDataModel = new TelephonyDataModel();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public DataModel getDataModel() {
        return mDataModel;
    }

    @Override
    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }
}
