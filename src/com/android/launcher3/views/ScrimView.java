/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 * Modifications copyright 2021, Lawnchair
 */
package com.android.launcher3.views;

import static android.content.Context.ACCESSIBILITY_SERVICE;

import static androidx.core.graphics.ColorUtils.compositeColors;

import static com.android.launcher3.anim.Interpolators.ACCEL;
import static com.android.launcher3.anim.Interpolators.LINEAR;
import static com.android.launcher3.anim.Interpolators.clampToProgress;
import static com.android.launcher3.icons.GraphicsUtils.setColorAlphaBound;
import static com.android.launcher3.util.SystemUiController.UI_STATE_SCRIM_VIEW;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.statemanager.StateManager;
import com.android.launcher3.statemanager.StateManager.StateListener;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.uioverrides.WallpaperColorInfo.OnChangeListener;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.util.MultiValueAlpha.AlphaProperty;
import com.android.launcher3.util.Themes;
import com.android.launcher3.widget.WidgetsFullSheet;

/**
 * Simple scrim which draws a flat color
 */
public class ScrimView<T extends Launcher> extends View implements Insettable, OnChangeListener,
        AccessibilityStateChangeListener {
    public static final Property<ScrimView, Integer> DRAG_HANDLE_ALPHA =
            new Property<ScrimView, Integer>(Integer.TYPE, "dragHandleAlpha") {
                public void set(ScrimView scrimView, Integer value) {
    private static final int ALPHA_CHANNEL_COUNT = 1;
    private static final long DRAG_HANDLE_BOUNCE_DURATION_MS = 300;
    // How much to delay before repeating the bounce.
    private static final long DRAG_HANDLE_BOUNCE_DELAY_MS = 200;
    // Repeat this many times (i.e. total number of bounces is 1 + this).
    private static final int DRAG_HANDLE_BOUNCE_REPEAT_COUNT = 2;


    protected final T mLauncher;
    private final WallpaperColorInfo mWallpaperColorInfo;
    private final AccessibilityManager mAM;
    protected final int mEndScrim;
    protected final boolean mIsScrimDark;

    private final StateListener<LauncherState> mAccessibilityLauncherStateListener =
            new StateListener<LauncherState>() {
        @Override
        public void onStateTransitionComplete(LauncherState finalState) {
            setImportantForAccessibility(finalState == ALL_APPS
                    ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        }
    };

    protected float mMaxScrimAlpha;

    protected float mProgress = 1;
    protected int mScrimColor;

    protected int mCurrentFlatColor;
    protected int mEndFlatColor;
    protected int mEndFlatColorAlpha;

    protected final int mDragHandleSize;
    private final int mDragHandleTouchSize;
    private final int mDragHandlePaddingInVerticalBarLayout;
    protected final Rect mDragHandleBounds;
    private final RectF mHitRect = new RectF();
    private ObjectAnimator mDragHandleAnim;

    private final MultiValueAlpha mMultiValueAlpha;

    public ScrimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLauncher = Launcher.cast(Launcher.getLauncher(context));
        mWallpaperColorInfo = WallpaperColorInfo.INSTANCE.get(context);
        mEndScrim = Themes.getAttrColor(context, R.attr.allAppsScrimColor);
        mIsScrimDark = ColorUtils.calculateLuminance(mEndScrim) < 0.5f;

        mMaxScrimAlpha = 0.7f;

        mDragHandleSize = context.getResources()
        mDragHandleSize = new Point(res.getDimensionPixelSize(R.dimen.vertical_drag_handle_width),
                .getDimensionPixelSize(R.dimen.vertical_drag_handle_size);
        mDragHandleBounds = new Rect(0, 0, mDragHandleSize, mDragHandleSize);
        mDragHandleTouchSize = res.getDimensionPixelSize(R.dimen.vertical_drag_handle_touch_size);
        mDragHandlePaddingInVerticalBarLayout = context.getResources()
                .getDimensionPixelSize(R.dimen.vertical_drag_handle_padding_in_vertical_bar_layout);
        mAM = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        setFocusable(false);
        mMultiValueAlpha = new MultiValueAlpha(this, ALPHA_CHANNEL_COUNT);
    }

    public AlphaProperty getAlphaProperty(int index) {
        return mMultiValueAlpha.getProperty(index);
        updateDragHandleVisibility(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mWallpaperColorInfo.addOnChangeListener(this);
        onExtractedColorsChanged(mWallpaperColorInfo);

        mAM.addAccessibilityStateChangeListener(this);
        onAccessibilityStateChanged(mAM.isEnabled());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWallpaperColorInfo.removeOnChangeListener(this);
        mAM.removeAccessibilityStateChangeListener(this);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        mScrimColor = wallpaperColorInfo.getMainColor();
        mEndFlatColor = compositeColors(mEndScrim, setColorAlphaBound(
                mScrimColor, Math.round(mMaxScrimAlpha * 255)));
        mEndFlatColorAlpha = Color.alpha(mEndFlatColor);
        updateColors();
        invalidate();
    }

    public void setProgress(float progress) {
        if (mProgress != progress) {
            mProgress = progress;
            stopDragHandleEducationAnim();
            updateColors();
            updateSysUiColors();
            invalidate();
        }
    }

    public void reInitUi() { }

    protected void updateColors() {
        mCurrentFlatColor = mProgress >= 1 ? 0 : setColorAlphaBound(
                mEndFlatColor, Math.round((1 - mProgress) * mEndFlatColorAlpha));
    }

    protected void updateSysUiColors() {
        // Use a light system UI (dark icons) if all apps is behind at least half of the
        // status bar.
        boolean forceChange = mProgress <= 0.1f;
        if (forceChange) {
            mLauncher.getSystemUiController().updateUiState(UI_STATE_SCRIM_VIEW, !mIsScrimDark);
        } else {
            mLauncher.getSystemUiController().updateUiState(UI_STATE_SCRIM_VIEW, 0);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrentFlatColor != 0) {
            canvas.drawColor(mCurrentFlatColor);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superHandledTouch = super.onTouchEvent(event);
        if (!value && mDragHandle != null && event.getAction() == ACTION_DOWN
                && mHitRect.contains(event.getX(), event.getY())) {
                if (startDragHandleEducationAnim()) {
                    return true;
                }
            }
            stopDragHandleEducationAnim();
        }
        return superHandledTouch;
    }
    /**
     * Animates the drag handle to demonstrate how to get to all apps.
     * @return Whether the animation was started (false if drag handle is invisible).
     */
    public boolean startDragHandleEducationAnim() {
        stopDragHandleEducationAnim();

        if (mDragHandle == null || mDragHandle.getAlpha() != 255) {
            return false;
        }

            topBounds.offset(0, -bounds.height() / 2);
        final float progressToReachTop = 0.6f;
            Keyframe frameTop = Keyframe.ofObject(0.6f, topBounds);
            frameBot.setInterpolator(ACCEL);
            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(drawable, holder);
        long totalBounceDuration = DRAG_HANDLE_BOUNCE_DURATION_MS + DRAG_HANDLE_BOUNCE_DELAY_MS;
        // The bounce finishes by this progress, the rest of the duration just delays next bounce.
        float delayStartProgress = 1f - (float) DRAG_HANDLE_BOUNCE_DELAY_MS / totalBounceDuration;
        mDragHandleAnim.addUpdateListener((v) -> invalidate(invalidateRegion));
        mDragHandleAnim.setDuration(totalBounceDuration);
        mDragHandleAnim.setInterpolator(clampToProgress(LINEAR, 0, delayStartProgress));
        mDragHandleAnim.setRepeatCount(DRAG_HANDLE_BOUNCE_REPEAT_COUNT);
        getOverlay().add(drawable);

            anim.addListener(new AnimatorListenerAdapter() {
                mDragHandleAnim = null;
            anim.start();
        return true;
    }

    private void stopDragHandleEducationAnim() {
        if (mDragHandleAnim != null) {
            mDragHandleAnim.end();
        final int top = getMeasuredHeight() - mDragHandleSize - grid.getInsets().bottom;
            topMargin = grid.workspacePadding.bottom;
                left = width - grid.getInsets().right - mDragHandleSize;
                        - mDragHandlePaddingInVerticalBarLayout;
                left = mDragHandleSize + grid.getInsets().left;
            left = (width - mDragHandleSize) / 2;
        // Inset outwards to increase touch size.
        mHitRect.inset((mDragHandleSize.x - mDragHandleTouchSize) / 2f,
        float inset = -mDragHandleSize / 2;
    }

    @Override
    public void onAccessibilityStateChanged(boolean enabled) {
        StateManager<LauncherState> stateManager = mLauncher.getStateManager();
        stateManager.removeStateListener(mAccessibilityLauncherStateListener);

        if (enabled) {
            stateManager.addStateListener(mAccessibilityLauncherStateListener);
            mAccessibilityLauncherStateListener.onStateTransitionComplete(stateManager.getState());
        } else {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        updateDragHandleVisibility(null);
    }

    protected void updateDragHandleVisibility() {
    private void updateDragHandleVisibility(Drawable recycle) {
        boolean visible = mLauncher.getDeviceProfile().isVerticalBarLayout() || Utilities.getLawnchairPrefs(mLauncher).getDockShowArrow();
                        mLauncher.getDrawable(R.drawable.drag_handle_indicator);
    }

    protected boolean shouldDragHandleBeVisible() {
        return mLauncher.getDeviceProfile().isVerticalBarLayout() || mAM.isEnabled();
    @Override
    public void onFocusChanged(boolean gainFocus, int direction,
            Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    public void onStateTransitionStart(LauncherState toState) {}
    }

    @Override
    public void onStateTransitionStart(LauncherState toState) {
            return  mDragHandleBounds.contains((int) x, (int) y)

    }

    @Override
    public void onStateTransitionComplete(LauncherState finalState) {
                int originalImportanceForAccessibility = getImportantForAccessibility();
                setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                return OptionsPopupView.onWidgetsClicked(ScrimView.this);
                if (widgetsFullSheet == null) {
                    setImportantForAccessibility(originalImportanceForAccessibility);
                    return false;
                }
                widgetsFullSheet.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View view) {}

                    @Override
                    public void onViewDetachedFromWindow(View view) {
                        setImportantForAccessibility(originalImportanceForAccessibility);
                        widgetsFullSheet.removeOnAttachStateChangeListener(this);
                    }
                });
                return true;

    }
    /**
     * @return The top of this scrim view, or {@link Float#MAX_VALUE} if there's no distinct top.
     */
    public int getDragHandleSize() {
        return mDragHandleSize;
    protected void onDrawFlatColor(Canvas canvas) {

    protected void onDrawRoundRect(Canvas canvas, float left, float top, float right, float bottom,
        // Override in inheriting classes
        return 255;
}
