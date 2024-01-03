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

package com.android.car.messenger.testing;

import static java.util.Comparator.comparingLong;

import androidx.core.app.Person;

import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.common.Conversation.Message;
import com.android.car.messenger.common.Conversation.Message.MessageStatus;
import com.android.car.messenger.common.Conversation.Message.MessageType;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Helper class for testing containing commonly used utility methods
 */
public class TestUtils {

    /** Returns a received message */
    public static Message createRecvMessage(
            String text, long timestamp, Person person, boolean isRead) {
        Message msg = new Conversation.Message(text, timestamp, person);
        msg.setMessageType(MessageType.MESSAGE_TYPE_INBOX);
        msg.setMessageStatus(isRead
                ? MessageStatus.MESSAGE_STATUS_READ
                : MessageStatus.MESSAGE_STATUS_UNREAD);
        return msg;
    }

    /** Returns a reply message */
    public static Message createReplyMessage(String text, long timestamp, Person person) {
        Message reply = new Conversation.Message(text, timestamp, person);
        reply.setMessageType(MessageType.MESSAGE_TYPE_SENT);
        reply.setMessageStatus(MessageStatus.MESSAGE_STATUS_NONE);
        return reply;
    }

    /** Returns a list of messages that is sorted from latest to oldest */
    public static ArrayList<Message> createMessageListDesc(Message... messages) {
        ArrayList<Message> msgList = new ArrayList<>(Arrays.asList(messages));
        msgList.sort(comparingLong(Message::getTimestamp).reversed());
        return msgList;
    }
}
