/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
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

package org.gearvrf.io.sceneeditor;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTransform;
import org.gearvrf.ISensorEvents;
import org.gearvrf.SensorEvent;
import org.gearvrf.io.SwipeKeyEvents;
import org.gearvrf.io.cursor3d.CustomKeyEvent;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;

import java.util.List;

class RedditWebView {
    private static final String TAG = RedditWebView.class.getSimpleName();
    public static final float QUAD_X = 16.0f;
    public static final float QUAD_Y = 9.0f;
    public static final float QUAD_DEPTH = -13f;
    private static final int SCROLL_AMOUNT = 200;

    private GVRFrameLayout frameLayout;
    private WebView webView;
    private TextView tvTitle;
    private String title;
    private int frameWidth;
    private int frameHeight;
    private float quadHeight;
    private float halfQuadHeight;
    private float quadWidth;
    private float halfQuadWidth;
    GVRScene scene;
    GVRActivity activity;
    GVRContext gvrContext;
    private GVRViewSceneObject webViewSceneObject;
    private boolean sensorEnabled = true;

    private Handler uiThreadHandler;
    private final static PointerProperties[] pointerProperties;
    private final static PointerCoords[] pointerCoordsArray;
    private final static PointerCoords pointerCoords;
    private WebWindowListener webWindowListener;

    static {
        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerProperties = new PointerProperties[]{properties};
        pointerCoords = new PointerCoords();
        pointerCoordsArray = new PointerCoords[]{pointerCoords};
    }

    private ProgressBar progressBar;

    interface WebWindowListener {
        void onClose();
        void onCreateWindow(GVRTransform windowTransform);
    }

    RedditWebView(GVRContext context, GVRScene scene, WebWindowListener webWindowListener) {
        this(context, scene, QUAD_Y, QUAD_X, webWindowListener);
    }

    RedditWebView(GVRContext context, GVRScene scene, float
            quadHeight, float quadWidth, WebWindowListener webWindowListener) {
        this.gvrContext = context;
        this.scene = scene;
        this.quadHeight = quadHeight;
        this.halfQuadHeight = quadHeight / 2;
        this.quadWidth = quadWidth;
        this.halfQuadWidth = quadWidth / 2;

        activity = context.getActivity();
        frameLayout = new GVRFrameLayout(activity);
        frameLayout.setBackgroundColor(Color.TRANSPARENT);
        View.inflate(activity, R.layout.web_view_layout, frameLayout);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        this.webWindowListener = webWindowListener;
    }

    void render(final String url, final String title) {
        this.title = title;
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                webView = (WebView)findViewById(R.id.wvRedditPost);
                tvTitle = (TextView) findViewById(R.id.tvTitle);
                progressBar = (ProgressBar) findViewById(R.id.pbProgressBar);
                webView.setWebViewClient(new RedditWebViewClient());
                webViewSceneObject = new GVRViewSceneObject(gvrContext, frameLayout,
                        QUAD_X, QUAD_Y);
                webView.getSettings().setJavaScriptEnabled(true);
                webWindowListener.onCreateWindow(webViewSceneObject.getTransform());
                webViewSceneObject.setSensor(new GVRBaseSensor(gvrContext));
                webViewSceneObject.getEventReceiver().addListener(sensorEvents);
                frameWidth = frameLayout.getWidth();
                frameHeight = frameLayout.getHeight();
                show();
                setLoading(true);
                webView.loadUrl(url);
            }
        });
    }

    void reload(final String url, String title) {
        this.title = title;
        uiThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                tvTitle.setText(R.string.loading_title);
                setLoading(true);
                webView.loadUrl(url);
            }
        });
    }

    public void setLoading(boolean loading) {
        if(loading) {
            progressBar.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }

    private class RedditWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            tvTitle.setText(title);
            view.zoomOut();
            setLoading(false);
            if(url.contains("imgur")) {
                view.scrollBy(0,500);
            }

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }
    }

    private ISensorEvents sensorEvents = new ISensorEvents() {
        @Override
        public void onSensorEvent(final SensorEvent event) {
            int id = event.getCursorController().getId();

            if (!sensorEnabled) {
                return;
            }
            List<KeyEvent> keyEvents = event.getCursorController()
                    .getKeyEvents();

            if (keyEvents.isEmpty() == false) {
                for (KeyEvent keyEvent : keyEvents) {
                    if (keyEvent.getAction() == CustomKeyEvent.ACTION_SWIPE) {
                        sendSwipeEvent(keyEvent);
                        continue;
                    }
                    if(keyEvent.getAction() == SwipeKeyEvents.ACTION_SCROLL) {
                        sendSwipeEvent(keyEvent);
                    }
                    sendMotionEvent(event.getHitPoint(), keyEvent.getAction());
                }
            } else {
                sendMotionEvent(event.getHitPoint(), MotionEvent.ACTION_MOVE);
            }
        }
    };

    private void sendSwipeEvent(final KeyEvent keyEvent) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                KeyEvent clone = new KeyEvent(keyEvent);
                onSwipeEvent(clone);
            }
        });
    }

    private void sendMotionEvent(float[] hitPoint, final int action) {
        float x = (hitPoint[0] + halfQuadWidth) / quadWidth;
        float y = -(hitPoint[1] - halfQuadHeight) / quadHeight;
        pointerCoords.x = x * getFrameWidth();
        pointerCoords.y = y * getFrameHeight();

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long now = SystemClock.uptimeMillis();
                final MotionEvent clone = MotionEvent.obtain(now, now + 1, action, 1,
                        pointerProperties, pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);

                dispatchMotionEvent(clone);
            }
        });
    }

    int getFrameWidth() {
        return frameWidth;
    }

    int getFrameHeight() {
        return frameHeight;
    }

    void show() {
        scene.addSceneObject(webViewSceneObject);
    }

    void hide() {
        scene.removeSceneObject(webViewSceneObject);
        webViewSceneObject.getEventReceiver().removeListener(sensorEvents);
    }

    void disable() {
        scene.removeSceneObject(webViewSceneObject);
        webViewSceneObject.getEventReceiver().removeListener(sensorEvents);
        webWindowListener.onClose();
    }

    void enable() {
        scene.addSceneObject(webViewSceneObject);
        webViewSceneObject.getEventReceiver().addListener(sensorEvents);
    }

    View findViewById(int id) {
        return frameLayout.findViewById(id);
    }

    void dispatchMotionEvent(MotionEvent motionEvent) {
        frameLayout.dispatchTouchEvent(motionEvent);
        frameLayout.invalidate();
        motionEvent.recycle();
    }

    void onSwipeEvent(KeyEvent keyEvent) {
        if(keyEvent.getKeyCode() == SwipeKeyEvents.KEYCODE_SCROLL_UP) {
            webView.scrollBy(0, SCROLL_AMOUNT);
        } else if(keyEvent.getKeyCode() == SwipeKeyEvents.KEYCODE_SCROLL_DOWN) {
            webView.scrollBy(0, -SCROLL_AMOUNT);
        }
    }

    void setSensorEnabled(boolean enabled) {
        sensorEnabled = enabled;
    }

    boolean isSensorEnabled() {
        return sensorEnabled;
    }

    GVRSceneObject getSceneObject() {
        return webViewSceneObject;
    }
}
