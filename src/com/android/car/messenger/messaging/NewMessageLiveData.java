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

package com.android.car.messenger.messaging;

import static com.android.car.messenger.messaging.utils.ConversationFetchUtil.fetchCompleteConversation;
import static com.android.car.messenger.messaging.utils.CursorUtils.DEFAULT_SORT_ORDER;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.MessageConstants;
import com.android.car.messenger.bluetooth.UserAccount;
import com.android.car.messenger.bluetooth.UserAccountLiveData;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.interfaces.DataModel;
import com.android.car.messenger.messaging.utils.CursorUtils;
import com.android.car.messenger.util.CarStateListener;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Publishes a stream of {@link Conversation} with unread messages that was received on the user
 * device after the car's connection to the{@link UserAccount}.
 */
public class NewMessageLiveData extends ContentProviderLiveData<Conversation> {
    private static final String TAG = "CM.NewMessageLiveData";

    private final DataModel mDataModel;
    @NonNull
    private final UserAccountLiveData mUserAccountLiveData = UserAccountLiveData.getInstance();

    @VisibleForTesting
    @NonNull
    Collection<UserAccount> mUserAccounts = new ArrayList<>();

    @NonNull
    private static final String MESSAGE_QUERY =
            Telephony.TextBasedSmsColumns.SUBSCRIPTION_ID
                    + " = ? AND "
                    + Telephony.TextBasedSmsColumns.SEEN
                    + " = 0";

    @NonNull
    private final CarStateListener mCarStateListener = AppFactory.get().getCarStateListener();

    NewMessageLiveData() {
        super(Telephony.MmsSms.CONTENT_URI);
        mDataModel = AppFactory.get().getDataModel();
    }

    @Override
    protected void onActive() {
        super.onActive();
        addSource(mUserAccountLiveData, it -> mUserAccounts = it.getAccounts());
        if (getValue() == null) {
            onDataChange();
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        removeSource(mUserAccountLiveData);
        mUserAccounts.clear();
    }

    @Override
    public void onDataChange() {
        L.d(TAG, "NewMessageLiveData: telephony database changed");
        for (UserAccount userAccount : mUserAccounts) {
            if (hasProjectionInForeground(userAccount)) {
                continue;
            }
            Cursor mmsCursor = getMmsCursor(userAccount);
            boolean foundNewMms = postNewMessageIfFound(mmsCursor, userAccount);
            if (foundNewMms) {
                L.d(TAG, "found new MMS");
                String messageId =
                        String.valueOf(mmsCursor.getInt(mmsCursor.getColumnIndex(BaseColumns._ID)));
                mDataModel.markAsSeen(messageId, CursorUtils.ContentType.MMS);
                break;
            }

            Cursor smsCursor = getSmsCursor(userAccount);
            boolean foundNewSms = postNewMessageIfFound(smsCursor, userAccount);
            if (foundNewSms) {
                L.d(TAG, "found new SMS");
                String messageId =
                        String.valueOf(smsCursor.getInt(smsCursor.getColumnIndex(BaseColumns._ID)));
                mDataModel.markAsSeen(messageId, CursorUtils.ContentType.SMS);
                break;
            }
        }
    }

    /** Post a new message if one is found, and returns true if so, false otherwise */
    private boolean postNewMessageIfFound(
            @Nullable Cursor cursor, @NonNull UserAccount userAccount) {
        if (cursor == null || !cursor.moveToFirst()) {
            return false;
        }
        String conversationId =
                cursor.getString(cursor.getColumnIndex(Telephony.TextBasedSmsColumns.THREAD_ID));

        Conversation conversation;
        try {
            conversation = fetchCompleteConversation(conversationId);
        } catch (CursorIndexOutOfBoundsException e) {
            L.w(TAG, "Error occurred fetching conversation Id: %s", conversationId);
            return false;
        }
        conversation.getExtras().putInt(MessageConstants.EXTRA_ACCOUNT_ID, userAccount.getId());
        postValue(conversation);
        return true;
    }

    /** Get the last message cursor, taking into account the last message posted */
    @Nullable
    @VisibleForTesting
    Cursor getMmsCursor(@NonNull UserAccount userAccount) {
        return getCursor(Telephony.Mms.Inbox.CONTENT_URI, userAccount);
    }

    /** Get the last message cursor, taking into account the last message posted */
    @Nullable
    @VisibleForTesting
    Cursor getSmsCursor(@NonNull UserAccount userAccount) {
        return getCursor(Telephony.Sms.Inbox.CONTENT_URI, userAccount);
    }

    /** Get the last message cursor of the subscription id */
    @Nullable
    private Cursor getCursor(Uri uri, @NonNull UserAccount userAccount) {
        Context context = AppFactory.get().getContext();
        return context.getContentResolver()
                .query(
                        uri,
                        new String[] {BaseColumns._ID, Telephony.TextBasedSmsColumns.THREAD_ID},
                        MESSAGE_QUERY,
                        new String[] {String.valueOf(userAccount.getId())},
                        DEFAULT_SORT_ORDER + " LIMIT 1");
    }

    private boolean hasProjectionInForeground(@NonNull UserAccount userAccount) {
        return mCarStateListener.isProjectionInActiveForeground(userAccount.getIccId());
    }
}
