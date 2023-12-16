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

package com.android.car.messenger.impl.datamodels;

import static com.android.dx.mockito.inline.extended.ExtendedMockito.doReturn;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.mockitoSession;
import static com.android.dx.mockito.inline.extended.ExtendedMockito.when;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import androidx.core.app.Person;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.apps.common.testutils.InstantTaskExecutorRule;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.core.models.UserAccount;
import com.android.car.messenger.core.ui.livedata.UserAccountLiveData;
import com.android.car.messenger.core.util.CarStateListener;
import com.android.car.messenger.impl.AppFactoryTestImpl;
import com.android.car.messenger.impl.datamodels.util.ConversationFetchUtil;
import com.android.car.messenger.impl.datamodels.util.CursorUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class NewMessageLiveDataTest {

    private static final int USER_ACCOUNT_ID = 0;

    private NewMessageLiveData mNewMessageLiveData;
    private AppFactoryTestImpl mAppFactory;

    /** Used to execute livedata.postValue() synchronously */
    @Rule
    public TestRule rule = new InstantTaskExecutorRule();

    private LifecycleRegistry mLifecycleRegistry;
    @Mock
    private LifecycleOwner mMockLifecycleOwner;
    @Mock
    private Observer<Conversation> mMockObserver;
    private Context mContext;
    @Mock
    private ContentResolver mMockContentResolver;
    @Mock
    private Cursor mMockCursor;
    @Mock
    private UserAccount mMockUserAccount;
    @Mock
    private UserAccountLiveData mMockUserAccountLiveData;
    @Mock
    private CarStateListener mMockCarStateListener;
    @Mock
    private TelephonyDataModel mDataModel;
    private ArrayList<UserAccount> mUserAccountList;
    @Captor
    private ArgumentCaptor<Uri> mUriCaptor;

    private static final int MESSAGE_ID = 123;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mAppFactory = new AppFactoryTestImpl(mContext, mDataModel, null, mMockCarStateListener);

        when(mMockUserAccount.getId()).thenReturn(USER_ACCOUNT_ID);
        when(mMockUserAccount.getConnectionTime()).thenReturn(Instant.ofEpochMilli(0));

        mLifecycleRegistry = new LifecycleRegistry(mMockLifecycleOwner);
        when(mMockLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);

        mUserAccountList = new ArrayList<>();
        mUserAccountList.add(mMockUserAccount);

        // Sets up default values for stubbed objects related to database queries.
        when(mContext.getContentResolver()).thenReturn(mMockContentResolver);
        when(mMockContentResolver.query(any(), any(), any(), any(), any()))
                .thenReturn(mMockCursor);
        when(mMockCursor.getColumnIndex(any())).thenReturn(0);
        when(mMockCursor.getString(anyInt())).thenReturn("0");
    }

    @After
    public void teardown() {
        mAppFactory.teardown();
    }

    @Test
    public void testUri() {
        doNothing().when(mMockContentResolver).registerContentObserver(any(), eq(true), any());

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(UserAccountLiveData.class)
                .startMocking();
        try {
            doReturn(mMockUserAccountLiveData).when(() -> UserAccountLiveData.getInstance());

            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.observe(mMockLifecycleOwner,
                    (value) -> mMockObserver.onChanged(value));
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            verify(mMockContentResolver).registerContentObserver(
                    mUriCaptor.capture(), eq(true), any());
            assertThat(mUriCaptor.getValue()).isEqualTo(Telephony.MmsSms.CONTENT_URI);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    @UiThreadTest
    public void testOnDataChanged_mms() {
        // First check is for MMS, second is for SMS
        when(mMockCursor.moveToFirst()).thenReturn(true, false);
        // Mock retrieval of message id
        when(mMockCursor.getInt(anyInt())).thenReturn(MESSAGE_ID);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(ConversationFetchUtil.class)
                .spyStatic(UserAccountLiveData.class)
                .startMocking();
        try {
            Conversation conversation = new Conversation.Builder(
                    new Person.Builder().build(), /* conversationId= */ "0").build();
            doReturn(conversation).when(
                    () -> ConversationFetchUtil.fetchSummarizedConversation(any()));
            doReturn(mMockUserAccountLiveData).when(() -> UserAccountLiveData.getInstance());

            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.mUserAccounts = mUserAccountList;
            mNewMessageLiveData.observe(mMockLifecycleOwner,
                    (value) -> mMockObserver.onChanged(value));
            assertThat(mNewMessageLiveData.getValue()).isNull();
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            assertThat(mNewMessageLiveData.getValue()).isEqualTo(conversation);

            verify(mDataModel, never())
                    .markAsSeen(String.valueOf(MESSAGE_ID), CursorUtils.ContentType.SMS);
            verify(mDataModel).markAsSeen(String.valueOf(MESSAGE_ID), CursorUtils.ContentType.MMS);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    @UiThreadTest
    public void testOnDataChanged_sms() {
        // First check is for MMS, second is for SMS
        when(mMockCursor.moveToFirst()).thenReturn(false, true);
        // Mock retrieval of message id
        when(mMockCursor.getInt(anyInt())).thenReturn(MESSAGE_ID);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(ConversationFetchUtil.class)
                .spyStatic(UserAccountLiveData.class)
                .startMocking();
        try {
            Conversation conversation = new Conversation.Builder(
                    new Person.Builder().build(), /* conversationId= */ "0").build();
            doReturn(conversation).when(
                    () -> ConversationFetchUtil.fetchSummarizedConversation(any()));
            doReturn(mMockUserAccountLiveData).when(() -> UserAccountLiveData.getInstance());

            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.mUserAccounts = mUserAccountList;
            mNewMessageLiveData.observe(mMockLifecycleOwner,
                    (value) -> mMockObserver.onChanged(value));
            assertThat(mNewMessageLiveData.getValue()).isNull();
            mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
            assertThat(mNewMessageLiveData.getValue()).isEqualTo(conversation);

            verify(mDataModel, never())
                    .markAsSeen(String.valueOf(MESSAGE_ID), CursorUtils.ContentType.MMS);
            verify(mDataModel).markAsSeen(String.valueOf(MESSAGE_ID), CursorUtils.ContentType.SMS);
        } finally {
            session.finishMocking();
        }
    }

    @Test
    public void testOnDataChanged_hasProjection() {
        when(mMockCarStateListener.isProjectionInActiveForeground(any())).thenReturn(true);

        MockitoSession session = mockitoSession().strictness(Strictness.LENIENT)
                .spyStatic(UserAccountLiveData.class)
                .startMocking();
        try {
            doReturn(mMockUserAccountLiveData).when(() -> UserAccountLiveData.getInstance());

            mNewMessageLiveData = new NewMessageLiveData();
            mNewMessageLiveData.mUserAccounts = mUserAccountList;
            assertThat(mNewMessageLiveData.getValue()).isNull();
            mNewMessageLiveData.onDataChange();
            assertThat(mNewMessageLiveData.getValue()).isNull();
        } finally {
            session.finishMocking();
        }
    }
}
