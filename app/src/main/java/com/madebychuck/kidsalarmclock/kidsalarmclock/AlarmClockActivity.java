package com.madebychuck.kidsalarmclock.kidsalarmclock;

import android.annotation.SuppressLint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.os.CountDownTimer;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AlarmClockActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private int wake;
    private int sleep;

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private ClockView shapeView;
    private Button wakeButton;
    private Button sleepButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm_clock);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        wakeButton = (Button) findViewById(R.id.wake_button);
        wakeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openWake(v);
            }
        });

        sleepButton = (Button) findViewById(R.id.sleep_button);
        sleepButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openSleep(v);
            }
        });

        shapeView = (ClockView)findViewById(R.id.fullscreen_content);
        shapeView.setCircleColor(Color.BLUE);

        updateTimes();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        CountDownTimer cdt = new CountDownTimer(Long.MAX_VALUE, 15000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateClock();
            }

            @Override
            public void onFinish() {

            }
        };
        cdt.start();
    }

    private void updateClock() {
        Calendar cal = Calendar.getInstance();
        int minute = cal.get(Calendar.MINUTE);
        //24 hour format
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);
        int time = hourofday * 3600 + minute * 60;
        boolean okToWake = false;
        if (sleep >= wake) {
            // Normal case.
            okToWake = time >= wake && time < sleep;
        } else {
            okToWake = time >= wake || time < sleep;
        }

        if (okToWake) {
            shapeView.setCircleColor(0xFF01BC01);
            shapeView.setLabelColor(0xFF005400);
        } else {
            shapeView.setCircleColor(0xFFBA0B0B);
            shapeView.setLabelColor(0xFF690000);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("h:mma");
        shapeView.setLabelText(sdf.format(cal.getTime()));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void openWake(View v) {
        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int wakeHour = sharedPref.getInt("wakeHour", 7);
        int wakeMin = sharedPref.getInt("wakeMin", 45);
        TimePickerFragment wakeFragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putInt("hour", wakeHour);
        args.putInt("min", wakeMin);
        wakeFragment.setArguments(args);
        wakeFragment.events = new TimePickerFragment.TimePickerEvents() {
            @Override
            public void onTimeSet(int hourOfDay, int minute) {
                SharedPreferences.Editor e = sharedPref.edit();
                e.putInt("wakeHour", hourOfDay);
                e.putInt("wakeMin", minute);
                e.commit();
                updateTimes();
            }
        };

        FragmentManager fm = getSupportFragmentManager();
        wakeFragment.show(fm, "wakePicker");
    }

    private void openSleep(View v) {
        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int sleepHour = sharedPref.getInt("sleepHour", 20);
        int sleepMin = sharedPref.getInt("sleepMin", 0);
        TimePickerFragment sleepFragment = new TimePickerFragment();
        Bundle args = new Bundle();
        args.putInt("hour", sleepHour);
        args.putInt("min", sleepMin);
        sleepFragment.setArguments(args);
        sleepFragment.events = new TimePickerFragment.TimePickerEvents() {
            @Override
            public void onTimeSet(int hourOfDay, int minute) {
                SharedPreferences.Editor e = sharedPref.edit();
                e.putInt("sleepHour", hourOfDay);
                e.putInt("sleepMin", minute);
                e.commit();
                updateTimes();
            }
        };

        FragmentManager fm = getSupportFragmentManager();
        sleepFragment.show(fm, "sleepPicker");
    }

    private void updateTimes() {
        final SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int sleepHour = sharedPref.getInt("sleepHour", 7);
        int sleepMin = sharedPref.getInt("sleepMin", 45);
        int wakeHour = sharedPref.getInt("wakeHour", 20);
        int wakeMin = sharedPref.getInt("wakeMin", 0);
        sleep = sleepHour * 3600 + sleepMin * 60;
        wake = wakeHour * 3600 + wakeMin * 60;
        SimpleDateFormat sdf = new SimpleDateFormat("h:mma");
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, wakeHour);
        cal.set(Calendar.MINUTE, wakeMin);
        wakeButton.setText("Wake (" + sdf.format(cal.getTime()) + ")");
        Calendar cal2 = Calendar.getInstance();
        cal2.set(Calendar.HOUR_OF_DAY, sleepHour);
        cal2.set(Calendar.MINUTE, sleepMin);
        sleepButton.setText("Sleep (" + sdf.format(cal2.getTime()) + ")");
    }
}
