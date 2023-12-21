/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.car.messenger.services;

import static android.app.Notification.EXTRA_TEXT;
import static android.app.Notification.EXTRA_TITLE;

import static com.android.car.messenger.services.NotificationHandler.LAST_REPLY_TIMESTAMP;
import static com.android.car.messenger.services.NotificationHandler.TIME_DESYNC_NOTIFICATION_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.app.Person;
import androidx.preference.PreferenceManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.car.messenger.AppFactoryTestImpl;
import com.android.car.messenger.R;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.messaging.TelephonyDataModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class NotificationHandlerTest {

    private AppFactoryTestImpl mAppFactory;
    private Context mContext;
    @Mock
    private TelephonyDataModel mDataModel;
    private SharedPreferences mSharedPrefs;

    @Captor
    private ArgumentCaptor<Notification> mNotifCaptor;

    @Before
    public void setup() throws TimeoutException {
        MockitoAnnotations.initMocks(this);

        mContext = spy(InstrumentationRegistry.getInstrumentation().getTargetContext());
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAppFactory = new AppFactoryTestImpl(mContext, mDataModel, mSharedPrefs, null);
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    public void postTimestampDesyncNotificationTest_postNotification() {
        mSharedPrefs.edit().putLong(LAST_REPLY_TIMESTAMP, 0).apply();

        Person user = new Person.Builder().setName("user").build();
        List<Conversation.Message> messages = new ArrayList<>();
        Conversation.Message msg = new Conversation.Message("msg", Long.MAX_VALUE, user);
        messages.add(msg);
        Conversation conversation = new Conversation.Builder(user, "0")
                .setMessages(messages)
                .build();

        NotificationManager mockNotifManager = mock(NotificationManager.class);
        when(mContext.getSystemService(NotificationManager.class)).thenReturn(mockNotifManager);

        NotificationHandler.postTimestampDesyncNotification(conversation);

        verify(mockNotifManager).notify(eq(TIME_DESYNC_NOTIFICATION_ID), mNotifCaptor.capture());
        assertThat(mNotifCaptor.getValue().extras.getString(EXTRA_TITLE))
                .isEqualTo(mContext.getString(R.string.time_desync_error_title));
        assertThat(mNotifCaptor.getValue().extras.getString(EXTRA_TEXT))
                .isEqualTo(mContext.getString(R.string.time_desync_error_text));
    }

    @Test
    public void postTimestampDesyncNotificationTest_postTooSoon() {
        mSharedPrefs.edit().putLong(LAST_REPLY_TIMESTAMP, 0).apply();

        Person user = new Person.Builder().setName("user").build();
        List<Conversation.Message> messages = new ArrayList<>();
        Conversation.Message msg = new Conversation.Message("msg", 1, user);
        messages.add(msg);
        Conversation conversation = new Conversation.Builder(user, "0")
                .setMessages(messages)
                .build();

        NotificationManager mockNotifManager = mock(NotificationManager.class);
        when(mContext.getSystemService(NotificationManager.class)).thenReturn(mockNotifManager);

        NotificationHandler.postTimestampDesyncNotification(conversation);
        verify(mockNotifManager, times(0)).notify(anyInt(), any(Notification.class));
    }
}
