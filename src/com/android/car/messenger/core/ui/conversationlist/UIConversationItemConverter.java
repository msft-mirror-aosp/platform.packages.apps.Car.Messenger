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

package com.android.car.messenger.core.ui.conversationlist;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.android.car.messenger.R;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.core.interfaces.AppFactory;
import com.android.car.messenger.core.util.ConversationUtil;

import java.util.Objects;

/** Util class that converts Conversation Item to UIConversationItem */
public class UIConversationItemConverter {

    // See ConversationFetchUtil#MESSAGE_LIMIT
    public static final int MAX_UNREAD_COUNT = 10;

    private UIConversationItemConverter() {}

    /** Converts Conversation Item to UIConversationItem */
    public static UIConversationItem convertToUIConversationItem(
            Conversation conversation, CarUxRestrictions carUxRestrictions) {
        Context context = AppFactory.get().getContext();
        boolean isReplied = ConversationUtil.isReplied(conversation);

        Drawable subtitleIcon =
                isReplied
                        ? context.getDrawable(R.drawable.car_ui_icon_reply)
                        : context.getDrawable(R.drawable.ic_subtitle_play);

        boolean showTextPreview =
                (carUxRestrictions.getActiveRestrictions()
                                & CarUxRestrictions.UX_RESTRICTIONS_NO_TEXT_MESSAGE)
                        == 0;
        String textPreview = "";
        int unreadCount = conversation.getUnreadCount();
        boolean isUnread = unreadCount > 0;

        // show a preview if UXR allows
        if (showTextPreview) {
            textPreview = ConversationUtil.getLastMessagePreview(conversation);
        } else if (isUnread) {
            // in place of text preview, we show "tap to read aloud" when unread
            textPreview = context.getString(R.string.tap_to_read_aloud);
        } else if (isReplied) {
            textPreview = context.getString(R.string.replied);
        }

        String unreadCountText = Integer.toString(unreadCount);
        if (unreadCount > MAX_UNREAD_COUNT) {
            unreadCountText = context.getString(R.string.message_overflow, MAX_UNREAD_COUNT);
        }

        long timestamp = ConversationUtil.getConversationTimestamp(conversation);
        return new UIConversationItem(
                conversation.getId(),
                Objects.requireNonNull(conversation.getConversationTitle()),
                textPreview,
                subtitleIcon,
                unreadCountText,
                timestamp,
                getConversationAvatar(context, conversation),
                /* showMuteIcon= */ false,
                /* showReplyIcon= */ true,
                /* showPlayIcon= */ false,
                isUnread,
                conversation.isMuted(),
                conversation);
    }

    private static Drawable getConversationAvatar(
            @NonNull Context context, @NonNull Conversation conversation) {
        return (conversation.getConversationIcon() != null)
                ? conversation.getConversationIcon().loadDrawable(context)
                : null;
    }
}
