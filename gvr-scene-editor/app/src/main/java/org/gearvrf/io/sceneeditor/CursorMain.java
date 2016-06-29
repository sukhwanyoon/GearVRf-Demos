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
package org.gearvrf.io.sceneeditor;

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
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRTransform;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.scene_objects.GVRConeSceneObject;
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
    private GVRScene mainScene;
    private CursorManager cursorManager;
    private EditableBehavior editableBehavior;

    @Override
    public void onInit(final GVRContext gvrContext) {
        mainScene = gvrContext.getNextMainScene();
        mainScene.getMainCameraRig().getLeftCamera().setBackgroundColor(Color.BLACK);
        mainScene.getMainCameraRig().getRightCamera().setBackgroundColor(Color.BLACK);
        cursorManager = new CursorManager(gvrContext, mainScene);
        editableBehavior = new EditableBehavior(cursorManager, mainScene);
        float[] position = new float[]{0.0f, 0.0f, -5.0f};

        final GVRCubeSceneObject cubeSceneObject = new GVRCubeSceneObject(gvrContext, true,
                gvrContext
                .loadFutureTexture(new GVRAndroidResource(gvrContext,R.mipmap.ic_launcher)));
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        MovableBehavior movableCubeBehavior = new MovableBehavior(cursorManager);
        cubeSceneObject.attachComponent(movableCubeBehavior);
        mainScene.addSceneObject(cubeSceneObject);
        cubeSceneObject.attachComponent(editableBehavior);

        movableCubeBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(final SelectableBehavior behavior, ObjectState prev,
                                       ObjectState
                    current) {
                if(current == ObjectState.CLICKED) {
//                    if(behavior.getOwnerObject().getComponent(EditableBehavior
//                            .getComponentType()) == null) {
//                        Log.d(TAG,"Attaching Editable Behavior");
//                        gvrContext.runOnTheFrameworkThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                //cubeSceneObject.attachComponent(editableBehavior);
//                                behavior.getOwnerObject().attachComponent(editableBehavior);
//                            }
//                        });
//
//
//                    } else {
//                        Log.d(TAG,"Detaching Editable Behavior");
//                        behavior.getOwnerObject().detachComponent(EditableBehavior
//                                .getComponentType());
//                    }
                }
            }
        });

        GVRConeSceneObject cone = new GVRConeSceneObject(gvrContext);

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
        red.setDiffuseColor(1, 0, 0, 1);
        blue.setDiffuseColor(0, 0, 1, 1);
        green.setDiffuseColor(0, 1, 0, 1);

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
        cubeBehind.getRenderData().setMaterial(red);
        cubeBehind.getRenderData().setShaderTemplate(GVRPhongShader.class);
        cubeBehind.getRenderData().getMaterial().setOpacity(0.5f);
        cubeBehind.getRenderData().setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        root.addChildObject(cubeBehind);

        MovableBehavior movableBehavior = new MovableBehavior(cursorManager, new ObjectState[] {
                ObjectState.DEFAULT, ObjectState.COLLIDING, ObjectState.CLICKED, ObjectState.BEHIND
        });
        float[] position = new float[]{-2, 2, -10};
        root.getTransform().setPosition(position[0], position[1], position[2]);
        root.attachComponent(movableBehavior);
        mainScene.addSceneObject(root);

        movableBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior selectableBehavior, ObjectState prev,
                                       ObjectState current) {
                if(current == ObjectState.CLICKED) {

                }
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
