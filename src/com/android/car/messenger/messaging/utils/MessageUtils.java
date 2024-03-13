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

package com.android.car.messenger.messaging.utils;

import static com.android.car.messenger.common.Conversation.Message.MessageStatus.MESSAGE_STATUS_NONE;
import static com.android.car.messenger.common.Conversation.Message.MessageStatus.MESSAGE_STATUS_READ;
import static com.android.car.messenger.common.Conversation.Message.MessageStatus.MESSAGE_STATUS_UNREAD;

import static java.lang.Math.min;
import static java.util.Comparator.comparingLong;

import android.content.Context;
import android.database.Cursor;
import android.provider.Telephony.TextBasedSmsColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.Person;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.common.Conversation.Message;
import com.android.car.messenger.common.Conversation.Message.MessageStatus;
import com.android.car.messenger.common.Conversation.Message.MessageType;
import com.android.car.messenger.interfaces.AppFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Message Parser that provides useful static methods to parse 1-1 and Group MMS messages. */
public final class MessageUtils {
    private static final String TAG = "CM.MessageUtils";

    private MessageUtils() {}

    /**
     * Returns all messages in the given cursors in descending order.
     *
     * @param limit The maximum number of messages
     * @param messageCursors The messageCursors of messages in descending order
     */
    @NonNull
    public static List<Message> getMessages(int limit, @Nullable Cursor... messageCursors) {
        List<Message> messages = new ArrayList<>();
        for (Cursor cursor : messageCursors) {
            MessageUtils.forEachDesc(
                    cursor,
                    message -> {
                        messages.add(message);
                        return true;
                    });
        }
        messages.sort(comparingLong(Message::getTimestamp).reversed());
        return messages.subList(0, min(limit, messages.size()));
    }

    /**
     * Returns unread messages from a conversation in descending order.
     *
     * @param messages The messages in descending order
     */
    @NonNull
    public static List<Message> getUnreadMessages(@NonNull List<Message> messages) {
        messages.sort(comparingLong(Conversation.Message::getTimestamp).reversed());
        int i = 0;
        for (Conversation.Message message : messages) {
            if (message.getMessageStatus() != MessageStatus.MESSAGE_STATUS_UNREAD) {
                break;
            }
            i++;
        }
        List<Message> unreadMessages = messages.subList(0, i);
        return unreadMessages;
    }

    /**
     * Parses each message in the cursor and returns the item for further processing
     *
     * @param messageCursor The message cursor to be parsed for SMS and MMS messages
     * @param processor A consumer that takes in the {@link Message} and returns true for the method
     *     to continue parsing the cursor or false to return.
     */
    private static void forEachDesc(
            @Nullable Cursor messageCursor, @NonNull Function<Message, Boolean> processor) {
        if (messageCursor == null || !messageCursor.moveToFirst()) {
            return;
        }
        Context context = AppFactory.get().getContext();
        boolean moveToNext = true;
        boolean hasBeenRepliedTo = false;
        do {
            Message message;
            try {
                message = parseMessageAtPoint(context, messageCursor, hasBeenRepliedTo);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                L.d(TAG, "Message was not able to be parsed. Skipping.");
                continue;
            }
            if (message.getText().trim().isEmpty()) {
                // There are occasions where a user may send
                // a text message plus an image or audio and
                // bluetooth will post two messages to the database (b/182834412),
                // one with a text and one blank
                // This leads to boomerang notifications, one with text and one that is empty.
                // Validating or removing messages when blank is a mitigation on our end.
                L.d(TAG, "Message is blank. Skipped. ");
                continue;
            }
            if (message.getMessageType() == MessageType.MESSAGE_TYPE_SENT) {
                hasBeenRepliedTo = true;
            }
            moveToNext = processor.apply(message);
        } while (messageCursor.moveToNext() && moveToNext);
    }

    /**
     * Parses message at the point in cursor.
     *
     * @throws IllegalArgumentException if desired columns are missing.
     * @see CursorUtils#CONTENT_CONVERSATION_PROJECTION
     */
    @NonNull
    private static Conversation.Message parseMessageAtPoint(
            @NonNull Context context, @NonNull Cursor cursor, boolean userHasReplied) {
        MmsSmsMessage msg =
                MmsUtils.isMms(cursor)
                        ? MmsUtils.parseMms(context, cursor)
                        : SmsUtils.parseSms(cursor);
        Person person =
                ContactUtils.getPerson(context, msg.mPhoneNumber, /* processParticipant= */ null);
        Conversation.Message message =
                new Conversation.Message(msg.mBody, msg.mDate.toEpochMilli(), person);
        if (msg.mType == TextBasedSmsColumns.MESSAGE_TYPE_SENT) {
            message.setMessageType(MessageType.MESSAGE_TYPE_SENT);
            message.setMessageStatus(MESSAGE_STATUS_NONE);
        } else {
            int status =
                    (msg.mRead || userHasReplied) ? MESSAGE_STATUS_READ : MESSAGE_STATUS_UNREAD;
            message.setMessageType(MessageType.MESSAGE_TYPE_INBOX);
            message.setMessageStatus(status);
        }
        return message;
    }
}