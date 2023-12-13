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

package com.android.car.messenger.core.shared;

import static com.android.car.messenger.core.shared.MessageConstants.EXTRA_ACCOUNT_ID;
import static com.android.car.messenger.core.shared.MessageConstants.LAST_REPLY_TIMESTAMP_EXTRA;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.car.apps.common.log.L;
import com.android.car.assist.payloadhandlers.ConversationPayloadHandler;
import com.android.car.messenger.R;
import com.android.car.messenger.common.Conversation;
import com.android.car.messenger.core.interfaces.AppFactory;
import com.android.car.messenger.core.service.MessengerService;
import com.android.car.messenger.core.ui.launcher.MessageLauncherActivity;
import com.android.car.messenger.core.util.ConversationUtil;
import com.android.car.messenger.core.util.VoiceUtil;

import java.util.Calendar;

/** Useful notification handler for posting messages */
public class NotificationHandler {
    private static final String TAG = "CM.NotificationHandler";

    @NonNull
    private static final String GROUP_TAP_TO_READ_NOTIFICATION =
            "com.android.car.messenger.TAP_TO_READ";

    private static final int TAP_TO_READ_SBN_ATTEMPT_LIMIT = 3;

    @VisibleForTesting
    static final int TIME_DESYNC_NOTIFICATION_ID = 1337;
    private static final String DATE_SETTINGS_INTENT_ACTION = "android.settings.DATE_SETTINGS";
    private static final long TIME_SYNC_MARGIN_MILLIS = 60 * 1000;

    private NotificationHandler() {}

    /** Posts or updates a notification based on a conversation */
    public static void postNotification(Conversation conversation) {
        int userAccountId = conversation.getExtras().getInt(EXTRA_ACCOUNT_ID, 0);
        if (userAccountId == 0) {
            L.w(TAG,
                    "posting Notification with null user account id. "
                            + "Note, reply would likely fail if user account id is not set.");
        }
        L.d(TAG, "posting notification with id: " + conversation.getId());
        Conversation tapToReadConversation =
                VoiceUtil.createTapToReadConversation(conversation, userAccountId);
        Context context = AppFactory.get().getContext();
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        String channelId =
                conversation.isMuted()
                        ? MessengerService.SILENT_MESSAGE_CHANNEL_ID
                        : MessengerService.MESSAGE_CHANNEL_ID;
        Notification notification =
                ConversationPayloadHandler.createNotificationFromConversation(
                        context, channelId, tapToReadConversation, R.drawable.ic_message, null);
        notification.contentIntent = createContentIntent();
        notificationManager.notify(tapToReadConversation.getId().hashCode(), notification);
    }

    /**
     * Notifies the user that there is a time desync between the car and paired phone.
     *
     * The incoming message's timestamp is considered truth as it comes from the phone. We can
     * compare it to the local time to see if we are in the future or past.
     *
     * Replies from the car will always use the (possibly incorrect) local time, resulting in
     * misordering of messages in the conversation. See b/288895550.
     */
    public static void postTimestampDesyncNotification(Conversation conversation) {
        Context context = AppFactory.get().getContext();
        Resources res = context.getResources();

        if (!res.getBoolean(R.bool.enable_time_desync_reminder)) {
            L.d(TAG, "desync notification disabled");
            return;
        }

        SharedPreferences prefs = AppFactory.get().getSharedPreferences();

        long localTimestamp = Calendar.getInstance().getTimeInMillis();
        long incomingTimestamp = ConversationUtil.getConversationTimestamp(conversation);
        long storedTimestamp = prefs.getLong(LAST_REPLY_TIMESTAMP_EXTRA, 0);
        long reminderIntervalMillis =
                res.getInteger(R.integer.time_desync_reminder_interval_mins) * 60000L;

        boolean isDesynced = Math.abs(localTimestamp - incomingTimestamp) > TIME_SYNC_MARGIN_MILLIS;

        // Occasionally post the notification so we don't spam the user.
        if (isDesynced && incomingTimestamp - storedTimestamp > reminderIntervalMillis) {
            prefs.edit().putLong(LAST_REPLY_TIMESTAMP_EXTRA, incomingTimestamp).apply();

            L.d(TAG, "posting desync notification");

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            Intent launchIntent = new Intent();
            launchIntent.setAction(DATE_SETTINGS_INTENT_ACTION);
            launchIntent.addCategory(Intent.CATEGORY_DEFAULT);
            PendingIntent settingsIntent = PendingIntent.getActivity(
                    context, 0, launchIntent, PendingIntent.FLAG_IMMUTABLE);
            Notification notification =
                    new Notification.Builder(context, MessengerService.ERROR_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_message)
                            .setContentTitle(context.getString(R.string.time_desync_error_title))
                            .setContentText(context.getString(R.string.time_desync_error_text))
                            .setContentIntent(settingsIntent)
                            .setAutoCancel(true)
                            .build();
            notificationManager.notify(TIME_DESYNC_NOTIFICATION_ID, notification);
        }
    }

    private static PendingIntent createContentIntent() {
        Context context = AppFactory.get().getContext();
        Intent intent =
                new Intent(context, MessageLauncherActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                context,
                /* requestCode= */ 0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Posts a notification in the foreground for Tap To Read
     *
     * <p>This is useful as legacy digital assistant implementations of Tap To Read require a {@link
     * StatusBarNotification} in order to fulfill a tap to read request.
     *
     * <p>This notification is invisible to the user but accessible by digital assistants.
     *
     * @return the StatusBarNotification posted by the system for this notification, or null if not
     *     found after a limited attempt at retrieval
     */
    @Nullable
    public static StatusBarNotification postNotificationForLegacyTapToRead(
            @NonNull Conversation tapToReadConversation) {
        L.d(TAG, "Posting legacy notification: " + tapToReadConversation.getId());
        Context context = AppFactory.get().getContext();
        // cancel any other notifications within group.
        // There should be only notification in group at a time.
        cancelAllTapToReadNotifications(context);
        // Post as a foreground service:
        // Foreground notifications by system apps with low priority
        // are hidden from user view, which is desired
        Notification notification =
                ConversationPayloadHandler.createNotificationFromConversation(
                        context,
                        MessengerService.APP_RUNNING_CHANNEL_ID,
                        tapToReadConversation,
                        context.getApplicationInfo().icon,
                        GROUP_TAP_TO_READ_NOTIFICATION);
        int id = (GROUP_TAP_TO_READ_NOTIFICATION + tapToReadConversation.getId()).hashCode();
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.notify(id, notification);

        // attempt to retrieve the status bar notification based on the notification
        // limit attempts
        int tries = 0;
        StatusBarNotification sbn;
        do {
            sbn = findSBN(notificationManager, id);
            tries++;
        } while (sbn == null && tries < TAP_TO_READ_SBN_ATTEMPT_LIMIT);
        return sbn;
    }

    /** Cancels all Tap To Read Notifications */
    public static void cancelAllTapToReadNotifications(@NonNull Context context) {
        L.d(TAG, "Cancelling all TTR notifications");
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
            if (GROUP_TAP_TO_READ_NOTIFICATION.equals(sbn.getNotification().getGroup())) {
                notificationManager.cancel(sbn.getId());
            }
        }
    }

    /** Returns the {@link StatusBarNotification} with desired id, or null if none found */
    private static StatusBarNotification findSBN(
            @NonNull NotificationManager notificationManager, int id) {
        for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
            if (sbn.getId() == id) {
                return sbn;
            }
        }
        return null;
    }

    /** Removes a notification based on a conversation */
    public static void removeNotification(@NonNull String conversationId) {
        Context context = AppFactory.get().getContext();
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        notificationManager.cancel(conversationId.hashCode());
    }
}
