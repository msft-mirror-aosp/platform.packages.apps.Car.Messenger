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

package com.android.car.messenger.ui.utils;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.messenger.R;
import com.android.car.messenger.ui.widgets.LetterTileDrawable;

import java.util.List;

/**
 * Avatar Utils for generating conversation and contact avatars
 *
 * <p>For historical context, AvatarUtil is derived from Android Messages implementation of group
 * avatars particularly from these sources:
 *
 * <p>AvatarGroupRequestDescriptor#generateDestRectArray:
 * packages/apps/Messaging/src/com/android/messaging/datamodel/media/AvatarGroupRequestDescriptor
 *
 * <p>CompositeImageRequest#loadMediaInternal:
 * packages/apps/Messaging/src/com/android/messaging/datamodel/media/CompositeImageRequest
 *
 * <p>ImageUtils#drawBitmapWithCircleOnCanvas:
 * packages/apps/Messaging/src/com/android/messaging/util/ImageUtils.java
 *
 * <p>Current implementation is close to reference. However, future iterations can diverge.
 */
public final class AvatarUtil {

    private static final boolean DBG = false;

    private AvatarUtil() {}

    private static final class GroupAvatarConfigs {
        int mWidth;
        int mHeight;
        int mMaximumGroupSize;
        int mBackgroundColor;
        int mStrokeColor;
        boolean mFillBackground;
        float mCornerRadius;
    }

    /**
     * Supports creating a group avatar: a minimum of 1 avatar and a maximum of four avatars are
     * supported. Any avatars beyond the 4th index is ignored.
     */
    @Nullable
    public static Bitmap createGroupAvatar(
            @NonNull Context context, @Nullable List<Bitmap> participantsIcon) {
        if (participantsIcon == null || participantsIcon.isEmpty()) {
            return null;
        }

        GroupAvatarConfigs groupAvatarConfigs = readGroupAvatarConfigs(context);

        if (participantsIcon.size() == 1 || groupAvatarConfigs.mMaximumGroupSize == 1) {
            return participantsIcon.get(0);
        }

        return createGroupAvatarBitmap(participantsIcon, groupAvatarConfigs);
    }

    /**
     * Resolves person avatar to either the provided bitmap clipped into a round rect or a letter
     * drawable.
     */
    @Nullable
    public static Bitmap resolvePersonAvatar(
            @NonNull Context context, @Nullable Bitmap bitmap, @Nullable CharSequence name) {
        if (bitmap != null) {
            return AvatarUtil.createClippedCircle(bitmap, readGroupAvatarConfigs(context));
        } else {
            return createLetterTile(context, name);
        }
    }

    private static GroupAvatarConfigs readGroupAvatarConfigs(Context context) {
        GroupAvatarConfigs groupAvatarConfigs = new GroupAvatarConfigs();
        Resources resources = context.getResources();
        int size = resources.getDimensionPixelSize(R.dimen.conversation_avatar_width);

        groupAvatarConfigs.mWidth = size;
        groupAvatarConfigs.mHeight = size;
        groupAvatarConfigs.mMaximumGroupSize =
                resources.getInteger(R.integer.group_avatar_max_group_size);
        groupAvatarConfigs.mBackgroundColor =
                resources.getColor(R.color.group_avatar_background_color, context.getTheme());
        groupAvatarConfigs.mStrokeColor =
                resources.getColor(R.color.group_avatar_stroke_color, context.getTheme());
        groupAvatarConfigs.mFillBackground =
                resources.getBoolean(R.bool.group_avatar_fill_background);
        groupAvatarConfigs.mCornerRadius =
                (float) Math.min(
                        100,
                        resources.getInteger(R.integer.group_avatar_images_corner_radius))
                            / 100F;

        return groupAvatarConfigs;
    }

    /**
     * Create a {@link Bitmap} for the given name.
     *
     * @param name will decide the color for the drawable. If null, a default color will be used.
     */
    @Nullable
    private static Bitmap createLetterTile(@NonNull Context context, @Nullable CharSequence name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        char firstInitial = name.charAt(0);
        String letters = Character.isLetter(firstInitial) ? Character.toString(firstInitial) : null;
        LetterTileDrawable drawable =
                new LetterTileDrawable(context.getResources(), letters, name.toString());
        int size = context.getResources().getDimensionPixelSize(R.dimen.conversation_avatar_width);
        return drawable.toBitmap(size);
    }

    /** Returns a clipped bitmap by the corner radius factor. */
    @NonNull
    private static Bitmap createClippedCircle(
            Bitmap bitmap, GroupAvatarConfigs groupAvatarConfigs) {
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();
        final Bitmap outputBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        float cornerRadius = groupAvatarConfigs.mCornerRadius;

        final Path path = new Path();
        float widthHeight = (float) min(width, (height / 2));
        float x = (float) (width / 2);
        float y = (float) (height / 2);
        path.addRoundRect(
                x,
                y,
                x + widthHeight,
                y + widthHeight,
                cornerRadius * widthHeight / 2f,
                cornerRadius * widthHeight / 2f,
                Path.Direction.CCW);

        final Canvas canvas = new Canvas(outputBitmap);
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return outputBitmap;
    }

    /** Creates a group avatar bitmap. */
    @NonNull
    private static Bitmap createGroupAvatarBitmap(
            @NonNull List<Bitmap> participantsIcon, GroupAvatarConfigs groupAvatarConfigs) {
        int width = groupAvatarConfigs.mWidth;
        int height = groupAvatarConfigs.mHeight;
        Bitmap bitmap = createOrReuseBitmap(width, height, Color.TRANSPARENT);
        Canvas canvas = new Canvas(bitmap);
        RectF[] rect =
                generateDestRectArray(
                        width,
                        height,
                        min(participantsIcon.size(), groupAvatarConfigs.mMaximumGroupSize),
                        groupAvatarConfigs.mCornerRadius);

        drawDebugBackground(canvas, width, height, groupAvatarConfigs.mCornerRadius);

        for (int i = 0; i < rect.length; i++) {
            RectF avatarDestOnGroup = rect[i];
            // Draw the bitmap into a smaller size with a circle mask.
            Bitmap resourceBitmap = participantsIcon.get(i);
            RectF resourceRect =
                    new RectF(
                            /* left= */ 0,
                            /* top= */ 0,
                            resourceBitmap.getWidth(),
                            resourceBitmap.getHeight());
            Bitmap smallCircleBitmap =
                    createOrReuseBitmap(
                            Math.round(avatarDestOnGroup.width()),
                            Math.round(avatarDestOnGroup.height()),
                            Color.TRANSPARENT);
            RectF smallCircleRect =
                    new RectF(
                            /* left= */ 0,
                            /* top= */ 0,
                            smallCircleBitmap.getWidth(),
                            smallCircleBitmap.getHeight());
            Canvas smallCircleCanvas = new Canvas(smallCircleBitmap);
            drawBitmapWithCircleOnCanvas(
                    resourceBitmap,
                    smallCircleCanvas,
                    resourceRect,
                    smallCircleRect,
                    groupAvatarConfigs.mFillBackground,
                    groupAvatarConfigs.mBackgroundColor,
                    groupAvatarConfigs.mStrokeColor,
                    groupAvatarConfigs.mCornerRadius);
            Matrix matrix = new Matrix();
            matrix.setRectToRect(smallCircleRect, avatarDestOnGroup, Matrix.ScaleToFit.FILL);
            canvas.drawBitmap(smallCircleBitmap, matrix, new Paint(Paint.ANTI_ALIAS_FLAG));
        }

        return bitmap;
    }

    private static void drawDebugBackground(
            Canvas canvas, int width, int height, float cornerRadius) {
        if (!DBG) {
            return;
        }
        final Paint stroke = new Paint();
        stroke.setAntiAlias(true);
        stroke.setColor(Color.LTGRAY);
        stroke.setStyle(Paint.Style.FILL);
        final float strokeWidth = 6f;
        stroke.setStrokeWidth(strokeWidth);

        float intersect = stroke.getStrokeWidth() / 2f;
        RectF strokeDest = new RectF(0, 0, width, height);
        strokeDest.inset(intersect, intersect);
        canvas.drawRoundRect(
                strokeDest,
                cornerRadius * strokeDest.width() / 2f,
                cornerRadius * strokeDest.height() / 2f,
                stroke);
    }

    /**
     * Given the source bitmap and a canvas, draws the bitmap through a circular mask. Only draws a
     * circle with diameter equal to the destination width.
     *
     * @param bitmap The source bitmap to draw.
     * @param canvas The canvas to draw it on.
     * @param source The source bound of the bitmap.
     * @param dest The destination bound on the canvas.
     * @param fillBackground when set, fill the circle with backgroundColor
     * @param strokeColor draw a border outside the circle with strokeColor
     * @param cornerRadius roundness factor of the edge of the rectangle to render into
     */
    private static void drawBitmapWithCircleOnCanvas(
            @NonNull Bitmap bitmap,
            @NonNull Canvas canvas,
            @NonNull RectF source,
            @NonNull RectF dest,
            boolean fillBackground,
            int backgroundColor,
            int strokeColor,
            float cornerRadius) {
        // Draw bitmap through shader first.
        final BitmapShader shader = new BitmapShader(bitmap, TileMode.CLAMP, TileMode.CLAMP);
        final Matrix matrix = new Matrix();

        // Fit bitmap to bounds.
        matrix.setRectToRect(source, dest, Matrix.ScaleToFit.CENTER);

        shader.setLocalMatrix(matrix);
        Paint bitmapPaint = new Paint();

        bitmapPaint.setAntiAlias(true);
        if (fillBackground) {
            bitmapPaint.setColor(backgroundColor);
            canvas.drawRoundRect(
                    dest,
                    cornerRadius * dest.width() / 2f,
                    cornerRadius * dest.height() / 2f,
                    bitmapPaint);
        }

        bitmapPaint.setShader(shader);
        canvas.drawRoundRect(
                dest,
                cornerRadius * dest.width() / 2f,
                cornerRadius * dest.height() / 2f,
                bitmapPaint);
        bitmapPaint.setShader(null);

        if (strokeColor != Color.TRANSPARENT) {
            final Paint stroke = new Paint();
            stroke.setAntiAlias(true);
            stroke.setColor(strokeColor);
            stroke.setStyle(Paint.Style.STROKE);
            final float strokeWidth = 6f;
            stroke.setStrokeWidth(strokeWidth);

            float intersect = stroke.getStrokeWidth() / 2f;
            RectF strokeDest = new RectF(dest);
            strokeDest.inset(intersect, intersect);
            canvas.drawRoundRect(
                    strokeDest,
                    cornerRadius * strokeDest.width() / 2f,
                    cornerRadius * strokeDest.height() / 2f,
                    stroke);
        }
    }

    @NonNull
    private static Bitmap createOrReuseBitmap(int width, int height, @ColorInt int background) {
        Bitmap bitmap =
                Bitmap.createBitmap(width, height, /* Bitmap.Config= */ Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(background);
        return bitmap;
    }

    /**
     * Generates an array of {@link RectF} which represents where each of the individual avatar
     * should be located in the final group avatar image. The location of each avatar depends on the
     * size of the group and the size of the overall group avatar size.
     */
    public static RectF[] generateDestRectArray(
            int desiredWidth,
            int desiredHeight,
            int groupSize,
            float cornerRadius) {
        float halfWidth = desiredWidth / 2F;
        float halfHeight = desiredHeight / 2F;

        RectF[] destArray = new RectF[groupSize];
        float outerRadius = Math.min(desiredWidth, desiredHeight) / 2F;
        float imageSize = outerRadius;
        switch (groupSize) {
            case 1:
                destArray[0] = new RectF(0F, 0F, desiredWidth, desiredHeight);
                break;
            case 2:
                /*
                * +-------+
                * | 0 |   |
                * +-------+
                * |   | 1 |
                * +-------+
                * We want two circles which touches in the center. To get this we know that
                * the diagonal
                * of the overall group avatar is squareRoot(2) * w We also know that the two
                * circles
                * touches the at the center of the overall group avatar and the distance from
                * the center of
                * the circle to the corner of the group avatar is radius * squareRoot(2).
                * Therefore, the
                * following emerges.
                */
                float image2inset = cornerRadius * (float) Math.sqrt(0.5F) * outerRadius / 4F;
                float sqrt2 = (float) Math.sqrt(2F);
                destArray[0] =
                        new RectF(
                                image2inset,
                                image2inset,
                                imageSize + image2inset / sqrt2,
                                imageSize + image2inset / sqrt2);
                destArray[1] =
                        new RectF(
                                imageSize - image2inset / sqrt2,
                                imageSize - image2inset / sqrt2,
                                desiredWidth - image2inset,
                                desiredHeight - image2inset);
                break;
            case 3:
                /*
                * +-------+
                * | | 0 | |
                * +-------+
                * | 1 | 2 |
                * +-------+
                * Note:Radius is meant as the half width/height of the surrounding rect of the
                * area where the image can be drawn.
                *
                * R is the radius of the circle which fits into the desired size.
                * r is the radius of the image.
                * r=[2*sqrt(3)-3]*R
                * The radius of the image areas (circle to rect) can be between [r, R]. We are
                * approximating is linearly with the corner radius factor.
                */
                // rFactor in [0.5 .. (2F * (float) Math.sqrt(3F) - 3F)]
                // -0.03589835f = (2F * (float) Math.sqrt(3F) - 3F) - 0.5F
                float rFactor = -0.03589835f * cornerRadius + 0.5F;
                imageSize = rFactor * outerRadius;

                float left1 = desiredWidth / 2F - imageSize;
                float right1 = desiredWidth / 2F + imageSize;
                float sqrt3 = (float) Math.sqrt(3F);
                float top2 = ((sqrt3 - 1F) * cornerRadius + 1F) * imageSize
                        + (1F - cornerRadius) * imageSize;
                float bottom2 = top2 + 2F * imageSize;

                destArray[0] = new RectF(left1, 0F, right1, 2F * imageSize);
                destArray[1] = new RectF(left1 - imageSize, top2, left1 + imageSize, bottom2);
                destArray[2] = new RectF(
                        right1 - imageSize, top2, right1 + imageSize, bottom2);
                break;
            default:
                /*
                * +-------+
                * | 0 | 1 |
                * +-------+
                * | 2 | 3 |
                * +-------+
                *
                * image4insest is a good approximation which scales between the radius and the
                * hypotenuse of the image rect, which is "sqrt(0.5) * outerRadius". The factor
                * 4.f is used to scale it a bit down.
                */
                float image4insest = cornerRadius * (float) Math.sqrt(0.5F) * outerRadius / 4F;
                destArray[0] = new RectF(image4insest, image4insest, halfWidth, halfHeight);
                destArray[1] =
                        new RectF(
                                halfWidth,
                                image4insest,
                                desiredWidth - image4insest,
                                halfHeight);
                destArray[2] =
                        new RectF(
                                image4insest,
                                halfHeight,
                                halfWidth,
                                desiredHeight - image4insest);
                destArray[3] =
                        new RectF(
                                halfWidth,
                                halfHeight,
                                desiredWidth - image4insest,
                                desiredHeight - image4insest);
                break;
        }
        return destArray;
    }
}
