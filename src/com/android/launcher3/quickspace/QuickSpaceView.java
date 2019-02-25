/*
 * Copyright (C) 2018 CypherOS
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
package com.android.launcher3.quickspace;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri.Builder;
import android.os.Handler;
import android.os.Process;
import android.provider.CalendarContract;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherTab;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.util.Themes;

import com.android.launcher3.quickspace.QuickspaceController.OnDataListener;
import com.android.launcher3.quickspace.receivers.QuickSpaceActionReceiver;
import com.android.launcher3.quickspace.views.DateTextView;

public class QuickSpaceView extends FrameLayout implements AnimatorUpdateListener, Runnable, OnDataListener {

    public final ColorStateList mColorStateList;
    public BubbleTextView mBubbleTextView;
    public final Handler mHandler;
    public final int mQuickspaceBackgroundRes;

    public DateTextView mClockView;
    public ViewGroup mQuickspaceContent;
    public ImageView mEventSubIcon;
    public TextView mEventTitleSub;
    public ViewGroup mWeatherContentSub;
    public ImageView mWeatherIconSub;
    public TextView mWeatherTempSub;
    public View mTitleSeparator;
    public TextView mEventTitle;
    public ViewGroup mWeatherContent;
    public ImageView mWeatherIcon;
    public TextView mWeatherTemp;

    public boolean mIsQuickEvent;
    public boolean mFinishedInflate;
    public boolean mWeatherAvailable;

    private QuickSpaceActionReceiver mActionReceiver;
    public QuickspaceController mController;

    public QuickSpaceView(Context context, AttributeSet set) {
        super(context, set);
        mActionReceiver = new QuickSpaceActionReceiver(context);
        mController = new QuickspaceController(context);
        mHandler = new Handler();
        mColorStateList = ColorStateList.valueOf(Themes.getAttrColor(getContext(), R.attr.workspaceTextColor));
        mQuickspaceBackgroundRes = R.drawable.bg_quickspace;
        setClipChildren(false);
    }

    @Override
    public void onDataUpdated() {
        mController.getEventController().initQuickEvents();
        if (mIsQuickEvent != mController.isQuickEvent()) {
            mIsQuickEvent = mController.isQuickEvent();
            prepareLayout();
        }
        mWeatherAvailable = mController.isWeatherAvailable();
        getQuickSpaceView();
        if (mIsQuickEvent) {
            loadDoubleLine();
        } else {
            loadSingleLine();
        }
    }

    public final void loadDoubleLine() {
        setBackgroundResource(mQuickspaceBackgroundRes);
        mEventTitle.setText(mController.getEventController().getTitle());
        mEventTitle.setEllipsize(TruncateAt.END);
        mEventTitleSub.setText(mController.getEventController().getActionTitle());
        mEventTitleSub.setEllipsize(TruncateAt.END);
        mEventTitleSub.setOnClickListener(mController.getEventController().getAction());
        mEventSubIcon.setImageTintList(mColorStateList);
        mEventSubIcon.setImageResource(mController.getEventController().getActionIcon());
        bindWeather(mWeatherContentSub, mWeatherTempSub, mWeatherIconSub);
    }

    public final void loadSingleLine() {
        LayoutTransition transition = mQuickspaceContent.getLayoutTransition();
        mQuickspaceContent.setLayoutTransition(transition == null ? new LayoutTransition() : null);
        setBackgroundResource(0);
        bindWeather(mWeatherContent, mWeatherTemp, mWeatherIcon);
        bindClockAndSeparator(false);
    }

    public final void bindClockAndSeparator(boolean forced) {
        boolean hasGoogleCalendar = LauncherAppState.getInstanceNoCreate().isCalendarAppAvailable();
        mClockView.setVisibility(View.VISIBLE);
        mClockView.setOnClickListener(hasGoogleCalendar ? mActionReceiver.getCalendarAction() : null);
        if (forced) {
            mClockView.reloadDateFormat();
        }
        if (Utilities.getQuickspaceBackground(getContext()) != "none") {
            mTitleSeparator.setVisibility(View.GONE);
        } else {
            mTitleSeparator.setVisibility(mWeatherAvailable ?  View.VISIBLE : View.GONE);
        }
    }

    public final void bindWeather(View container, TextView title, ImageView icon) {
        boolean hasGoogleApp = LauncherAppState.getInstanceNoCreate().isSearchAppAvailable();
        mWeatherAvailable = mController.isWeatherAvailable();
        if (mWeatherAvailable) {
            container.setVisibility(View.VISIBLE);
            container.setOnClickListener(hasGoogleApp ? mActionReceiver.getWeatherAction() : null);
            title.setText(mController.getWeatherTemp());
            icon.setImageIcon(mController.getWeatherIcon());
            return;
        }
        container.setVisibility(View.GONE);
    }

    public void reloadConfiguration() {
        if (!mIsQuickEvent) {
            bindClockAndSeparator(true);
        }
    }

    public final void loadViews() {
        mEventTitle = (TextView) findViewById(R.id.quick_event_title);
        mEventTitleSub = (TextView) findViewById(R.id.quick_event_title_sub);
        mEventSubIcon = (ImageView) findViewById(R.id.quick_event_icon_sub);
        mWeatherIcon = (ImageView) findViewById(R.id.weather_icon);
        mWeatherIconSub = (ImageView) findViewById(R.id.quick_event_weather_icon);
        mQuickspaceContent = (ViewGroup) findViewById(R.id.quickspace_content);
        mWeatherContent = (ViewGroup) findViewById(R.id.weather_content);
        mWeatherContentSub = (ViewGroup) findViewById(R.id.quick_event_weather_content);
        mWeatherTemp = (TextView) findViewById(R.id.weather_temp);
        mWeatherTempSub = (TextView) findViewById(R.id.quick_event_weather_temp);
        mClockView = (DateTextView) findViewById(R.id.clock_view);
        mTitleSeparator = findViewById(R.id.separator);
        setTypeface(mEventTitle, mEventTitleSub, mWeatherTemp, mWeatherTempSub, mClockView);
    }

    private void setTypeface(TextView... views) {
        Typeface tf;
        switch (Utilities.getDateStyleFont(getContext())) {
            case "systembold":
                tf = Typeface.create("sans-serif", Typeface.BOLD);
                break;

            case "systemitalic":
                tf = Typeface.create("sans-serif", Typeface.ITALIC);
                break;

            case "system":
                tf = Typeface.create("sans-serif", Typeface.NORMAL);
                break;

            case "systemlight":
                tf = Typeface.create("sans-serif-light", Typeface.NORMAL);
                break;

            case "abelreg":
                tf = Typeface.create("abelreg", Typeface.NORMAL);
                break;

            case "adamcg":
                tf = Typeface.create("adamcg-pro", Typeface.NORMAL);
                break;

            case "alien":
                tf = Typeface.create("alien-league", Typeface.NORMAL);
                break;

            case "archivo":
                tf = Typeface.create("archivonar", Typeface.NORMAL);
                break;

            case "badscript":
                tf = Typeface.create("badscript", Typeface.NORMAL);
                break;

            case "bariol":
                tf = Typeface.create("bariol-reg", Typeface.NORMAL);
                break;

            case "bignoodle":
                tf = Typeface.create("bignoodle-regular", Typeface.NORMAL);
                break;

            case "biko":
                tf = Typeface.create("biko", Typeface.NORMAL);
                break;

            case "cherryswash":
                tf = Typeface.create("cherryswash", Typeface.NORMAL);
                break;

            case "ginora":
                tf = Typeface.create("ginora-sans", Typeface.NORMAL);
                break;

            case "gobold":
                tf = Typeface.create("gobold-light-sys", Typeface.NORMAL);
                break;

            case "google":
                tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/GoogleSans-Regular.ttf");
                break;

            case "ibmplex":
                tf = Typeface.create("ibmplex-mono", Typeface.NORMAL);
                break;

            case "inkferno":
                tf = Typeface.create("inkferno", Typeface.NORMAL);
                break;

            case "instruction":
                tf = Typeface.create("instruction", Typeface.NORMAL);
                break;

            case "jacklane":
                tf = Typeface.create("jack-lane", Typeface.NORMAL);
                break;

            case "kellyslab":
                tf = Typeface.create("kellyslab", Typeface.NORMAL);
                break;

            case "monad":
                tf = Typeface.create("monad", Typeface.NORMAL);
                break;

            case "noir":
                tf = Typeface.create("noir", Typeface.NORMAL);
                break;

            case "outrun":
                tf = Typeface.create("outrun-future", Typeface.NORMAL);
                break;

            case "pompiere":
                tf = Typeface.create("pompiere", Typeface.NORMAL);
                break;

            case "raleway":
                tf = Typeface.create("raleway-light", Typeface.NORMAL);
                break;

            case "reemkufi":
                tf = Typeface.create("reemkufi", Typeface.NORMAL);
                break;

            case "riviera":
                tf = Typeface.create("riviera", Typeface.NORMAL);
                break;

            case "satisfy":
                tf = Typeface.create("satisfy", Typeface.NORMAL);
                break;

            case "seaweedsc":
                tf = Typeface.create("seaweedsc", Typeface.NORMAL);
                break;

            case "sedgwick":
                tf = Typeface.create("sedgwick-ave", Typeface.NORMAL);
                break;

            case "snowstorm":
                tf = Typeface.create("snowstorm-sys", Typeface.NORMAL);
                break;

            case "theoutbox":
                tf = Typeface.create("the-outbox", Typeface.NORMAL);
                break;

            case "vibur":
                tf = Typeface.create("vibur", Typeface.NORMAL);
                break;

            case "voltaire":
                tf = Typeface.create("voltaire", Typeface.NORMAL);
                break;

            default:
                tf = Typeface.createFromAsset(getContext().getAssets(), "fonts/GoogleSans-Regular.ttf");
                break;
        }

        int color;
        int accentcolor = getContext().getColor(R.color.qsb_icons_accent);
        switch (Utilities.getQuickspaceColor(getContext())) {
            case "auto":
                color = Themes.getAttrColor(getContext(), R.attr.workspaceTextColor);
                break;
            case "light":
                color = Color.WHITE;
                break;
            case "dark":
                color = Color.BLACK;
                break;
            case "accent":
                color = accentcolor;
                break;
            default:
                color = Themes.getAttrColor(getContext(), R.attr.workspaceTextColor);
                break;
        }

        int bgDrawable;
        switch (Utilities.getQuickspaceBackground(getContext())) {
            case "none":
                bgDrawable = 0;
                break;
            case "lighter":
                bgDrawable = R.drawable.glance_bg_lighter;
                break;
            case "light":
                bgDrawable = R.drawable.glance_bg_light;
                break;
            case "dark":
                bgDrawable = R.drawable.glance_bg_dark;
                break;
            case "darker":
                bgDrawable = R.drawable.glance_bg_darker;
                break;
            case "theme":
                bgDrawable = R.drawable.glance_bg_systemtheme;
                break;
            default:
                bgDrawable = 0;
                break;
        }

        for (TextView view : views) {
            if (view != null) {
                view.setTypeface(tf);
                view.setAllCaps(Utilities.isDateStyleUppercase(getContext()));
                view.setLetterSpacing(Utilities.getDateStyleTextSpacing(getContext()));
                view.setTextColor(color);
                int bgPadWid = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(R.dimen.quickspace_bg_extra_width), getContext().getResources().getDisplayMetrics());
                int bgPadHei = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(R.dimen.quickspace_bg_extra_height), getContext().getResources().getDisplayMetrics());
                if (view != mWeatherTemp || view != mWeatherTempSub) {
                    view.setBackgroundResource(bgDrawable);
                    view.setPadding(bgPadWid,bgPadHei,bgPadWid,bgPadHei);
                    if (bgDrawable != 0) {
                        mWeatherContent.setPadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimensionPixelSize(R.dimen.quickspace_no_divider_padding), getContext().getResources().getDisplayMetrics()),0,0,0)
                    }
                }
                if (mWeatherContent != null) {
                    mWeatherContent.setBackgroundResource(bgDrawable);
                    mWeatherContent.setPadding(bgPadWid,bgPadHei,bgPadWid,bgPadHei);
                }
            }
        }
    }

    public void prepareLayout() {
        int indexOfChild = indexOfChild(mQuickspaceContent);
        removeView(mQuickspaceContent);
        addView(LayoutInflater.from(getContext()).inflate(mIsQuickEvent ?
                R.layout.quickspace_doubleline :
                R.layout.quickspace_singleline, this, false), indexOfChild);
        loadViews();
    }

    public void getQuickSpaceView() {
        if (!(mQuickspaceContent.getVisibility() == View.VISIBLE)) {
            mQuickspaceContent.setVisibility(View.VISIBLE);
            mQuickspaceContent.setAlpha(0.0f);
            mQuickspaceContent.animate().setDuration(200).alpha(1.0f);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        invalidate();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mController != null && mFinishedInflate) {
            mController.addListener(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mController != null) {
            mController.removeListener(this);
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        loadViews();
        mFinishedInflate = true;
        mBubbleTextView = findViewById(R.id.dummyBubbleTextView);
        mBubbleTextView.setTag(new ItemInfo() {
            @Override
            public ComponentName getTargetComponent() {
                return new ComponentName(getContext(), "");
            }
        });
        mBubbleTextView.setContentDescription("");
        if (isAttachedToWindow()) {
            if (mController != null) {
                mController.addListener(this);
            }
        }
    }

    @Override
    public void onLayout(boolean b, int n, int n2, int n3, int n4) {
        super.onLayout(b, n, n2, n3, n4);
        //mEventTitle.setText(cn); Todo: set the event info here
    }

    public void onPause() {
        mHandler.removeCallbacks(this);
    }

    public void run() {
    }

    public void setPadding(int n, int n2, int n3, int n4) {
        super.setPadding(0, 0, 0, 0);
    }

}
