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

package com.android.car.messenger.core.service;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.impl.AppFactoryTestImpl;
import com.android.car.messenger.impl.datamodels.TelephonyDataModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class OnBootReceiverTest {

    private Context mContext;
    private AppFactoryTestImpl mAppFactory;
    @Captor
    private ArgumentCaptor<Intent> mCaptor;
    @Mock
    private TelephonyDataModel mDataModel;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(null).when(mContext).startService(any());
    }

    @Test
    public void testOnReceive_bootCompleted() {
        OnBootReceiver onBootReceiver = new OnBootReceiver();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_BOOT_COMPLETED);

        onBootReceiver.onReceive(mContext, intent);
        verify(mContext).startService(mCaptor.capture());
        assertThat(mCaptor.getValue().getComponent().getClassName())
                .isEqualTo(MessengerService.class.getName());
    }

    @Test
    public void testOnReceive_packageReplaced() {
        OnBootReceiver onBootReceiver = new OnBootReceiver();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MY_PACKAGE_REPLACED);

        onBootReceiver.onReceive(mContext, intent);
        verify(mContext).startService(mCaptor.capture());
        assertThat(mCaptor.getValue().getComponent().getClassName())
                .isEqualTo(MessengerService.class.getName());
    }
}
