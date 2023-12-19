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

package com.android.car.messenger.core.util;

import static com.google.common.truth.Truth.assertThat;

import androidx.core.app.Person;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.common.Conversation.Message;
import com.android.car.messenger.testing.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(AndroidJUnit4.class)
public class ConversationUtilTest {

    private Person mPerson;
    private static final String CONV_ID = "0";

    @Before
    public void setup() {
        mPerson = new Person.Builder()
                .setName("self")
                .build();
    }

    @Test
    public void getLastMessage_noMessages() {
        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .build();
        Message msg = ConversationUtil.getLastMessage(conv);
        assertThat(msg).isNull();
    }

    @Test
    public void getLastMessage_hasMessages() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(TestUtils.createRecvMessage(
                "msg", /* timestamp= */ 0, mPerson, /* isRead= */ true));
        messages.add(TestUtils.createRecvMessage(
                "msg2", /* timestamp= */ 1, mPerson, /* isRead= */ false));

        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .setMessages(messages)
                .build();
        Message msg = ConversationUtil.getLastMessage(conv);

        assertThat(msg).isEqualTo(messages.get(1));
    }

    @Test
    public void getLastMessagePreview() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(TestUtils.createRecvMessage(
                "msg", /* timestamp= */ 0, mPerson, /* isRead= */ true));
        messages.add(TestUtils.createReplyMessage("reply", /* timestamp= */ 1, mPerson));

        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .setMessages(messages)
                .build();
        String msg = ConversationUtil.getLastMessagePreview(conv);

        assertThat(msg).isEqualTo("reply");
    }

    @Test
    public void getConversationTimestamp() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(TestUtils.createRecvMessage(
                "msg", /* timestamp= */ 0, mPerson, /* isRead= */ true));
        messages.add(TestUtils.createRecvMessage(
                "msg2", /* timestamp= */ 1, mPerson, /* isRead= */ false));

        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .setMessages(messages)
                .build();
        long timestamp = ConversationUtil.getConversationTimestamp(conv);

        assertThat(timestamp).isEqualTo(1L);
    }

    @Test
    public void isReplied_notReplied() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(TestUtils.createRecvMessage(
                "msg", /* timestamp= */ 0, mPerson, /* isRead= */ true));
        messages.add(TestUtils.createRecvMessage(
                "msg2", /* timestamp= */ 1, mPerson, /* isRead= */ false));

        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .setMessages(messages)
                .build();
        boolean isReplied = ConversationUtil.isReplied(conv);

        assertThat(isReplied).isEqualTo(false);
    }

    @Test
    public void isReplied_hasReplied() {
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(TestUtils.createRecvMessage(
                "msg", /* timestamp= */ 0, mPerson, /* isRead= */ true));
        messages.add(TestUtils.createReplyMessage("reply", /* timestamp= */ 1, mPerson));

        Conversation conv = new Conversation.Builder(mPerson, CONV_ID)
                .setMessages(messages)
                .build();
        boolean isReplied = ConversationUtil.isReplied(conv);

        assertThat(isReplied).isEqualTo(true);
    }
}
