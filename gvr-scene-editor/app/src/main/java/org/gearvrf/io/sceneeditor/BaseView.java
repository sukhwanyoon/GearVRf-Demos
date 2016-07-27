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
import android.os.Message;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.ISensorEvents;
import org.gearvrf.SensorEvent;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

abstract class BaseView {
    private static final String TAG = BaseView.class.getSimpleName();
    private static final float QUAD_X = 10.0f;
    private static final float QUAD_Y = 8f;
    private static final int MOTION_EVENT = 1;
    private static final int KEY_EVENT = 2;
    private GVRFrameLayout frameLayout;

    private float quadHeight;
    private float halfQuadHeight;
    private float quadWidth;
    private float halfQuadWidth;
    GVRScene scene;
    GVRActivity activity;
    GVRContext gvrContext;
    private GVRViewSceneObject layoutSceneObject;
    private boolean sensorEnabled = true;

    private Handler mainThreadHandler;
    private final static PointerProperties[] pointerProperties;
    private final static PointerCoords[] pointerCoordsArray;
    private final static PointerCoords pointerCoords;
    int settingsCursorId;

    static {
        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerProperties = new PointerProperties[]{properties};
        pointerCoords = new PointerCoords();
        pointerCoordsArray = new PointerCoords[]{pointerCoords};
    }

    interface WindowChangeListener {
        void onClose();
    }

    BaseView(GVRContext context, GVRScene scene, int settingsCursorId, int layoutID) {
        this(context, scene, settingsCursorId, layoutID, QUAD_Y, QUAD_X);
    }

    BaseView(GVRContext context, GVRScene scene, int settingsCursorId, int layoutID, float
            quadHeight, float quadWidth) {
        this.gvrContext = context;
        this.scene = scene;
        this.settingsCursorId = settingsCursorId;
        this.quadHeight = quadHeight;
        this.halfQuadHeight = quadHeight / 2;
        this.quadWidth = quadWidth;
        this.halfQuadWidth = quadWidth / 2;

        activity = context.getActivity();
        frameLayout = new GVRFrameLayout(activity);
        frameLayout.setBackgroundColor(Color.TRANSPARENT);
        View.inflate(activity, layoutID, frameLayout);
        mainThreadHandler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MOTION_EVENT:
                        MotionEvent motionEvent = (MotionEvent) msg.obj;
                        frameLayout.dispatchTouchEvent(motionEvent);
                        frameLayout.invalidate();
                        motionEvent.recycle();
                        break;
                    case KEY_EVENT:
                        KeyEvent keyEvent = (KeyEvent) msg.obj;
                        frameLayout.dispatchKeyEvent(keyEvent);
                        frameLayout.invalidate();;
                }
            }
        };
    }

    void renderEditObjectView() {
        scrollEnabled = false;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                layoutSceneObject = new GVRViewSceneObject(gvrContext, frameLayout,
                        gvrContext.createQuad(quadWidth, quadHeight));
                layoutSceneObject.getTransform().setPosition(0, 0, -10);
                layoutSceneObject.getTransform().rotateByAxisWithPivot(-35, 1, 0, 0, 0, 0, 0);
                Matrix4f cameraMatrix = gvrContext.getMainScene().getMainCameraRig()
                        .getHeadTransform()
                        .getModelMatrix4f();
                Matrix4f objectMatrix = layoutSceneObject.getTransform().getModelMatrix4f();
                Matrix4f finalMatrix = cameraMatrix.mul(objectMatrix);
                layoutSceneObject.getTransform().setModelMatrix(finalMatrix);

                layoutSceneObject.setSensor(new GVRBaseSensor(gvrContext));
                layoutSceneObject.getEventReceiver().addListener(sensorEvents);
                show();
            }
        });
    }

    void renderFileBrowserView() {
        scrollEnabled = true;
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                layoutSceneObject = new GVRViewSceneObject(gvrContext, frameLayout,
                        gvrContext.createQuad(quadWidth, quadHeight));
                layoutSceneObject.getTransform().setPosition(0, -4, -10);
                layoutSceneObject.getTransform().rotateByAxis(-35, 1, 0, 0);
                layoutSceneObject.setSensor(new GVRBaseSensor(gvrContext));
                layoutSceneObject.getEventReceiver().addListener(sensorEvents);
                show();
            }
        });
    }

    private boolean scrollEnabled;
    private ISensorEvents sensorEvents = new ISensorEvents() {
        private static final float SCALE = 5.0f;
        private float savedMotionEventX, savedMotionEventY, savedHitPointX,
                savedHitPointY;

        @Override
        public void onSensorEvent(final SensorEvent event) {
            int id = event.getCursorController().getId();

            if (id != settingsCursorId || !sensorEnabled) {
                return;
            }
            List<KeyEvent> keyEvents = event.getCursorController().getKeyEvents();
            List<MotionEvent> motionEvents = event.getCursorController().getMotionEvents();

            if(scrollEnabled) {
                for (MotionEvent motionEvent : motionEvents) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        pointerCoords.x = savedHitPointX
                                + ((motionEvent.getX() - savedMotionEventX) * SCALE);
                        pointerCoords.y = savedHitPointY
                                + ((motionEvent.getY() - savedMotionEventY) * SCALE);
                    } else {
                        float[] hitPoint = event.getHitPoint();
                        pointerCoords.x = ((hitPoint[0] + halfQuadWidth) / quadWidth) * frameLayout
                                .getWidth();
                        pointerCoords.y = (-(hitPoint[1] - halfQuadHeight) / quadHeight) * frameLayout

                                .getHeight();

                        if (motionEvent.getAction() == KeyEvent.ACTION_DOWN) {
                            // save the co ordinates on down
                            savedMotionEventX = motionEvent.getX();
                            savedMotionEventY = motionEvent.getY();

                            savedHitPointX = pointerCoords.x;
                            savedHitPointY = pointerCoords.y;
                        }
                    }

                    final MotionEvent clone = MotionEvent.obtain(
                            motionEvent.getDownTime(), motionEvent.getEventTime(),
                            motionEvent.getAction(), 1, pointerProperties,
                            pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                            InputDevice.SOURCE_TOUCHSCREEN, 0);

                    Message message = Message.obtain(mainThreadHandler, MOTION_EVENT, 0, 0, clone);
                    message.sendToTarget();
                }
            } else {
                for (KeyEvent keyEvent : keyEvents) {
                    sendMotionEventFromHitPoint(event.getHitPoint(), keyEvent.getAction());
                }
                if (keyEvents.isEmpty()) {
                    sendMotionEventFromHitPoint(event.getHitPoint(), MotionEvent.ACTION_MOVE);
                }
            }
        }
    };

    private void sendMotionEventFromHitPoint(float[] hitPoint, final int action) {
        float x = (hitPoint[0] + halfQuadWidth) / quadWidth;
        float y = -(hitPoint[1] - halfQuadHeight) / quadHeight;
        pointerCoords.x = x * getFrameWidth();
        pointerCoords.y = y * getFrameHeight();
        long now = SystemClock.uptimeMillis();
        final MotionEvent clone = MotionEvent.obtain(now, now + 1, action, 1,
                pointerProperties, pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
        Message msg = Message.obtain(mainThreadHandler, MOTION_EVENT, clone);
        msg.sendToTarget();
    }

    int getFrameWidth() {
        return frameLayout.getWidth();
    }

    int getFrameHeight() {
        return frameLayout.getHeight();
    }

    void show() {
        scene.addSceneObject(layoutSceneObject);
    }

    void hide() {
        scene.removeSceneObject(layoutSceneObject);
        layoutSceneObject.getEventReceiver().removeListener(sensorEvents);
    }

    View findViewById(int id) {
        return frameLayout.findViewById(id);
    }
}
