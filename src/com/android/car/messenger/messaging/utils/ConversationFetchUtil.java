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

package com.android.car.messenger.messaging.utils;

import static java.lang.Math.min;
import static java.util.Comparator.comparingLong;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.android.car.apps.common.log.L;
import com.android.car.messenger.MessageConstants;
import com.android.car.messenger.R;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.interfaces.AppFactory;
import com.android.car.messenger.ui.utils.AvatarUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/** Utility class for retrieving and setting conversation items. */
public class ConversationFetchUtil {
    private static final String TAG = "CM.ConversationFetchUtil";

    private static final String COMMA_DELIMITER = ", ";
    private static final int MAX_TITLE_NAMES = 3;

    private ConversationFetchUtil() {}

    /**
     * Fetches a complete conversation based on a provided conversation id
     *
     * Messages are ordered in ascending order, from oldest to latest
     */
    public static Conversation fetchCompleteConversation(@NonNull String conversationId) {
        L.d(TAG, "Fetching complete conversation " + conversationId);
        Conversation.Builder conversationBuilder = initConversationBuilder(conversationId);
        Cursor mmsCursor = getMmsCursor(conversationId);
        Cursor smsCursor = getSmsCursor(conversationId);
        Context context = AppFactory.get().getContext();
        int messageLimit = context.getResources().getInteger(R.integer.conversation_size_limit);

        // message list sorted by date desc (latest to oldest)
        List<Conversation.Message> messages =
                MessageUtils.getMessages(messageLimit, mmsCursor, smsCursor);

        List<Conversation.Message> messagesToRead = MessageUtils.getUnreadMessages(messages);
        int unreadCount = messagesToRead.size();

        // sort ascending
        messages.sort(comparingLong(Conversation.Message::getTimestamp));
        conversationBuilder.setMessages(messages).setUnreadCount(unreadCount);
        return conversationBuilder.build();
    }

    @NonNull
    private static Conversation.Builder initConversationBuilder(@NonNull String conversationId) {
        Context context = AppFactory.get().getContext();
        String userName = ContactUtils.DRIVER_NAME;
        Conversation.Builder builder =
                new Conversation.Builder(
                        new Person.Builder().setName(userName).build(), conversationId);
        List<Person> participants =
                fetchParticipants(
                        conversationId,
                        (names, icons) -> {
                            builder.setConversationTitle(formatConversationTitle(names));
                            Bitmap bitmap = AvatarUtil.createGroupAvatar(context, icons);
                            if (bitmap != null) {
                                builder.setConversationIcon(IconCompat.createWithBitmap(bitmap));
                            }
                        });
        builder.setParticipants(participants);
        builder.setMuted(loadMutedList().contains(conversationId));
        return builder;
    }

    private static String formatConversationTitle(List<CharSequence> names) {
        Context context = AppFactory.get().getContext();
        String title =
                TextUtils.join(
                        COMMA_DELIMITER, names.subList(0, min(MAX_TITLE_NAMES, names.size())));
        if (names.size() > MAX_TITLE_NAMES) {
            title +=
                    context.getString(
                            R.string.participant_overflow_text, names.size() - MAX_TITLE_NAMES);
        }
        return title;
    }

    /**
     * Fetches participants and allows caller to process names and icons before returning.
     *
     * <p>For context, a conversation often holds multiple messages, which holds multiple
     * participant contacts, which each in turn could hold an avatar.
     *
     * <p>This leads to a very heavy conversation class and leads to problems down the road when
     * sending this conversation as a bundle.
     *
     * <p>To mitigate this, {@link Person} classes do not hold an avatar. Instead, each contact
     * avatar is channeled up to the caller during a fetch to make one avatar for the entire
     * conversation.
     *
     * @param conversationId, id for conversation to fetch participants information
     * @param processNamesAndIcons the method to process the names and icons of the participants
     * @return list of participants as {@link Person}. For performance reasons, the objects do not
     *     contain an avatar, and a functional interface is needed in order to process the various
     *     participant icons nto one conversation icon.
     */
    private static List<Person> fetchParticipants(
            @NonNull String conversationId,
            @NonNull BiConsumer<List<CharSequence>, List<Bitmap>> processNamesAndIcons) {
        List<CharSequence> participantNames = new ArrayList<>();
        List<Bitmap> participantIcons = new ArrayList<>();
        List<Person> participants =
                ContactUtils.getRecipients(
                        conversationId,
                        (name, bitmap) -> {
                            participantNames.add(name);
                            participantIcons.add(bitmap);
                        });
        processNamesAndIcons.accept(participantNames, participantIcons);
        return participants;
    }

    /** Returns a set of muted conversation items */
    @NonNull
    public static Set<String> loadMutedList() {
        SharedPreferences sharedPreferences = AppFactory.get().getSharedPreferences();
        return sharedPreferences.getStringSet(
                MessageConstants.KEY_MUTED_CONVERSATIONS, new HashSet<>());
    }

    private static Cursor getMmsCursor(@NonNull String conversationId) {
        Context context = AppFactory.get().getContext();
        int messageLimit = context.getResources().getInteger(R.integer.conversation_size_limit);
        return CursorUtils.getMessagesCursor(
                conversationId, messageLimit, CursorUtils.ContentType.MMS);
    }

    private static Cursor getSmsCursor(@NonNull String conversationId) {
        Context context = AppFactory.get().getContext();
        int messageLimit = context.getResources().getInteger(R.integer.conversation_size_limit);
        return CursorUtils.getMessagesCursor(
                conversationId, messageLimit, CursorUtils.ContentType.SMS);
    }
}