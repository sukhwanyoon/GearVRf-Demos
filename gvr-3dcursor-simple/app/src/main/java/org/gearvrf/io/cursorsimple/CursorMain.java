/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gearvrf.io.cursorsimple;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRPhongShader;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRSphereCollider;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRTransform;
import org.gearvrf.IPickEvents;
import org.gearvrf.io.cursor3d.Cursor;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ISelectableEvents;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.utility.Log;

import java.io.IOException;

/**
 * This sample can be used with a Laser Cursor as well as an Object Cursor. By default the Object
 * Cursor is enabled. To switch to a Laser Cursor simply rename the "laser_cursor_settings.xml"
 * in the assets directory to "settings.xml"
 */
public class CursorMain extends GVRMain {
    private static final String TAG = CursorMain.class.getSimpleName();
    private static final String ASTRONAUT_MODEL = "Astronaut.fbx";
    private static final String ROCKET_MODEL = "Rocket.fbx";
    private GVRScene mainScene;
    private CursorManager cursorManager;
    private GVRSceneObject rocket;
    private GVRSceneObject astronaut;

    private static float LOOKAT_COLOR_MASK_R = 1.0f;
    private static float LOOKAT_COLOR_MASK_G = 0.8f;
    private static float LOOKAT_COLOR_MASK_B = 0.8f;
    private static float PICKED_COLOR_MASK_R = 1.0f;
    private static float PICKED_COLOR_MASK_G = 0.5f;
    private static float PICKED_COLOR_MASK_B = 0.5f;
    private GVRSceneObject mPickedObject = null;
    private IPickEvents mPickHandler;
    public class PickHandler implements IPickEvents
    {
        public void onEnter(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo)
        {
            android.util.Log.d(TAG,"OnEnter");
            sceneObj.getRenderData().getMaterial().setColor(LOOKAT_COLOR_MASK_R, LOOKAT_COLOR_MASK_G, LOOKAT_COLOR_MASK_B);
            mPickedObject = sceneObj;
        }
        public void onExit(GVRSceneObject sceneObj)
        {
            android.util.Log.d(TAG,"OnExit");
            sceneObj.getRenderData().getMaterial().setColor(1.0f, 1.0f, 1.0f);
        }
        public void onNoPick(GVRPicker picker)
        {
            mPickedObject = null;
        }
        public void onPick(GVRPicker picker) {

        }
        public void onInside(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo) {
        }
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        mainScene = gvrContext.getNextMainScene();
        mainScene.getMainCameraRig().getLeftCamera().setBackgroundColor(Color.BLACK);
        mainScene.getMainCameraRig().getRightCamera().setBackgroundColor(Color.BLACK);
        cursorManager = new CursorManager(gvrContext, mainScene);
        mPickHandler = new PickHandler();
        mainScene.getEventReceiver().addListener(mPickHandler);
        GVRModelSceneObject astronautModel, rocketModel;

        float[] position = new float[]{5.0f, 0.0f, -10.0f};
        try {
            astronautModel = gvrContext.loadModel(ASTRONAUT_MODEL);
            rocketModel = gvrContext.loadModel(ROCKET_MODEL);
        } catch (IOException e) {
            Log.e(TAG, "Could not load the assets:", e);
            return;
        }

        astronaut = astronautModel.getChildByIndex(0);
        astronaut.getTransform().setPosition(position[0], position[1], position[2]);
        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager, true);
        astronaut.attachComponent(selectableBehavior);
        astronautModel.removeChildObject(astronaut);
        mainScene.addSceneObject(astronaut);

        position[0] = -5.0f;
        MovableBehavior movableRocketBehavior = new MovableBehavior(cursorManager, new
                ObjectState[]{
                ObjectState.DEFAULT, ObjectState.BEHIND, ObjectState.COLLIDING, ObjectState
                .CLICKED});
        rocket = rocketModel.getChildByIndex(0);
        rocket.getTransform().setPosition(position[0], position[1], position[2]);
        rocket.attachComponent(movableRocketBehavior);
        rocketModel.removeChildObject(rocket);
        mainScene.addSceneObject(rocket);

        position[0] = 2.0f;
        position[1] = 2.0f;
        GVRCubeSceneObject cubeSceneObject = new GVRCubeSceneObject(gvrContext, true, gvrContext
                .loadFutureTexture(new GVRAndroidResource(gvrContext, R.mipmap.ic_launcher)));
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        MovableBehavior movableCubeBehavior = new MovableBehavior(cursorManager);
        cubeSceneObject.attachComponent(movableCubeBehavior);
        cubeSceneObject.attachComponent(new GVRSphereCollider(gvrContext));
        mainScene.addSceneObject(cubeSceneObject);

        movableCubeBehavior.setSelectableEventListener(new ISelectableEvents() {
            @Override
            public void onEnter(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onInside(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onExit(Cursor cursor, GVRSceneObject sceneObject) {

            }

            @Override
            public void onClick(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {
                GVRTransform transform = astronaut.getTransform();
                transform.setPositionZ(transform.getPositionZ() - 1);
            }

            @Override
            public void onClickReleased(Cursor cursor, GVRSceneObject sceneObject) {

            }

            @Override
            public void onDrag(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onBehind(Cursor cursor, GVRSceneObject sceneObject) {

            }
        });

        addCustomMovableCube(gvrContext);
    }

    @Override
    public void onStep() {
    }

    void close() {
        if (cursorManager != null) {
            cursorManager.close();
        }
    }

    private void addCustomMovableCube(GVRContext gvrContext) {
        GVRSceneObject root = new GVRSceneObject(gvrContext);
        GVRMaterial red = new GVRMaterial(gvrContext);
        GVRMaterial blue = new GVRMaterial(gvrContext);
        GVRMaterial green = new GVRMaterial(gvrContext);
        GVRMaterial alphaRed = new GVRMaterial(gvrContext);
        red.setDiffuseColor(1, 0, 0, 1);
        blue.setDiffuseColor(0, 0, 1, 1);
        green.setDiffuseColor(0, 1, 0, 1);
        alphaRed.setDiffuseColor(1, 0, 0, 0.5f);

        GVRCubeSceneObject cubeDefault = new GVRCubeSceneObject(gvrContext, true, red);
        cubeDefault.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(cubeDefault);

        GVRMesh cubeMesh = cubeDefault.getRenderData().getMesh();

        GVRSceneObject cubeColliding = new GVRSceneObject(gvrContext, cubeMesh);
        cubeColliding.getRenderData().setMaterial(blue);
        cubeColliding.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(cubeColliding);

        GVRSceneObject cubeClicked = new GVRSceneObject(gvrContext, cubeMesh);
        cubeClicked.getRenderData().setMaterial(green);
        cubeClicked.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(cubeClicked);

        GVRSceneObject cubeBehind = new GVRSceneObject(gvrContext, cubeMesh);
        cubeBehind.getRenderData().setMaterial(alphaRed);
        cubeBehind.getRenderData().setShaderTemplate(GVRPhongShader.class);
        cubeBehind.getRenderData().getMaterial().setOpacity(0.5f);
        cubeBehind.getRenderData().setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        root.addChildObject(cubeBehind);

        MovableBehavior movableBehavior = new MovableBehavior(cursorManager, new ObjectState[]{
                ObjectState.DEFAULT, ObjectState.COLLIDING, ObjectState.CLICKED, ObjectState.BEHIND
        });
        float[] position = new float[]{-2, 2, -10};
        root.getTransform().setPosition(position[0], position[1], position[2]);
        root.attachComponent(movableBehavior);
        mainScene.addSceneObject(root);

        movableBehavior.setSelectableEventListener(new ISelectableEvents() {
            @Override
            public void onEnter(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onInside(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onExit(Cursor cursor, GVRSceneObject sceneObject) {

            }

            @Override
            public void onClick(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {
                GVRTransform transform = astronaut.getTransform();
                transform.setPositionZ(transform.getPositionZ() + 1);
            }

            @Override
            public void onClickReleased(Cursor cursor, GVRSceneObject sceneObject) {

            }

            @Override
            public void onDrag(Cursor cursor, GVRSceneObject sceneObject, float[] hitpoint) {

            }

            @Override
            public void onBehind(Cursor cursor, GVRSceneObject sceneObject) {

            }
        });
    }

    @Override
    public GVRTexture getSplashTexture(GVRContext gvrContext) {
        Bitmap bitmap = BitmapFactory.decodeResource(
                gvrContext.getContext().getResources(),
                R.mipmap.ic_launcher);
        // return the correct splash screen bitmap
        return new GVRBitmapTexture(gvrContext, bitmap);
    }
}
