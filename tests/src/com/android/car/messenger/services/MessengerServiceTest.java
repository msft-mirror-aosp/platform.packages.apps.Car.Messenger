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

package com.android.car.messenger.services;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.verify;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Service;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.MutableLiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import com.android.car.messenger.AppFactoryTestImpl;
import com.android.car.messenger.MessageConstants;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.messaging.TelephonyDataModel;
import com.android.car.messenger.util.VoiceUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class MessengerServiceTest {

    private MessengerService mMessengerService;
    private AppFactoryTestImpl mAppFactory;
    private Context mContext;
    @Rule
    public final ServiceTestRule mServiceTestRule = new ServiceTestRule();
    @Captor
    private ArgumentCaptor<Intent> mCaptor;
    @Mock
    private TelephonyDataModel mDataModel;
    @Mock
    private MutableLiveData<Conversation> mUnseenLiveData;
    @Mock
    private MutableLiveData<String> mRemovedLiveData;

    @Before
    public void setup() throws TimeoutException {
        MockitoAnnotations.initMocks(this);

        when(mDataModel.getUnseenMessages()).thenReturn(mUnseenLiveData);
        when(mDataModel.onConversationRemoved()).thenReturn(mRemovedLiveData);

        mContext = spy(InstrumentationRegistry.getInstrumentation().getTargetContext());
        mAppFactory = new AppFactoryTestImpl(mContext, mDataModel, null, null);

        mMessengerService = new MessengerService();
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    public void testOnStartCommand_nullAction() {
        int result = mMessengerService.onStartCommand(null, 0, 0);
        assertThat(result).isEqualTo(Service.START_STICKY);

        result = mMessengerService.onStartCommand(new Intent(), 0, 0);
        assertThat(result).isEqualTo(Service.START_STICKY);
    }

    @Test
    public void testOnStartCommand_actionReply() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(VoiceUtil.class)
                .startMocking();
        try {
            Intent intent = new Intent();
            intent.setAction(MessageConstants.ACTION_REPLY);
            int result = mMessengerService.onStartCommand(intent, 0, 0);

            assertThat(result).isEqualTo(Service.START_STICKY);
            verify(() -> VoiceUtil.voiceReply(mCaptor.capture()));
            assertThat(mCaptor.getValue()).isEqualTo(intent);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testOnStartCommand_actionMute() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(VoiceUtil.class)
                .startMocking();
        try {
            Intent intent = new Intent();
            intent.setAction(MessageConstants.ACTION_MUTE);
            int result = mMessengerService.onStartCommand(intent, 0, 0);

            assertThat(result).isEqualTo(Service.START_STICKY);
            verify(() -> VoiceUtil.mute(mCaptor.capture()));
            assertThat(mCaptor.getValue()).isEqualTo(intent);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testOnStartCommand_actionMarkRead() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(VoiceUtil.class)
                .startMocking();
        try {
            Intent intent = new Intent();
            intent.setAction(MessageConstants.ACTION_MARK_AS_READ);
            int result = mMessengerService.onStartCommand(intent, 0, 0);

            assertThat(result).isEqualTo(Service.START_STICKY);
            verify(() -> VoiceUtil.markAsRead(mCaptor.capture()));
            assertThat(mCaptor.getValue()).isEqualTo(intent);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testOnStartCommand_actionDirectSend() {
        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(VoiceUtil.class)
                .startMocking();
        try {
            Intent intent = new Intent();
            intent.setAction(MessageConstants.ACTION_DIRECT_SEND);
            int result = mMessengerService.onStartCommand(intent, 0, 0);

            assertThat(result).isEqualTo(Service.START_STICKY);
            verify(() -> VoiceUtil.directSend(mCaptor.capture()));
            assertThat(mCaptor.getValue()).isEqualTo(intent);
        } finally {
            session.finishMocking();
        }
    }
}
