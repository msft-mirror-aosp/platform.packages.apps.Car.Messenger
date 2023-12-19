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

package com.android.car.messenger.core.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.common.Conversation.Message;
import com.android.car.messenger.common.Conversation.Message.MessageType;

/**
 * Conversation Util class for the {@link Conversation} DAO.
 *
 * Conversation messages should be ordered in ascending order, from oldest to latest.
 */
public class ConversationUtil {
    private ConversationUtil() {}

    /**
     * Get the last timestamp for the conversation. This could be a reply timestamp or last received
     * message timestamp, whichever is last.
     */
    public static long getConversationTimestamp(@Nullable Conversation conversation) {
        if (conversation == null) {
            return 0L;
        }
        Message msg = getLastMessage(conversation);
        if (msg == null) {
            return 0L;
        }
        return msg.getTimestamp();
    }

    /** Returns if the {@link Conversation} has been last responded to. */
    public static boolean isReplied(@Nullable Conversation conversation) {
        if (conversation == null) {
            return false;
        }
        Message msg = getLastMessage(conversation);
        if (msg == null) {
            return false;
        }
        return msg.getMessageType() == MessageType.MESSAGE_TYPE_SENT;
    }

    /** Returns the last message in the conversation, or null if {@link
     * Conversation#getMessages} is empty */
    @Nullable
    public static Message getLastMessage(@Nullable Conversation conversation) {
        if (conversation == null || conversation.getMessages().isEmpty()) {
            return null;
        }
        int size = conversation.getMessages().size();
        return conversation.getMessages().get(size - 1);
    }

    /**
     * Returns the last incoming message in the conversation, or null if {@link
     * Conversation#getMessages} is empty
     */
    @NonNull
    public static String getLastMessagePreview(@Nullable Conversation conversation) {
        Message lastMessage = getLastMessage(conversation);
        if (lastMessage == null) {
            return "";
        }
        return lastMessage.getText();
    }
}
