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
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.scene_objects.GVRConeSceneObject;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.utility.Log;

/**
 * This sample can be used with a Laser Cursor as well as an Object Cursor. By default the Object
 * Cursor is enabled. To switch to a Laser Cursor simply rename the "laser_cursor_settings.xml"
 * in the assets directory to "settings.xml"
 */
public class SceneEditorMain extends GVRMain {
    private static final String TAG = SceneEditorMain.class.getSimpleName();
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
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext,R.mipmap
                .ic_launcher)));
        final GVRConeSceneObject cubeSceneObject = new GVRConeSceneObject(gvrContext, true,material);
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        MovableBehavior movableCubeBehavior = new MovableBehavior(cursorManager);
        cubeSceneObject.attachComponent(movableCubeBehavior);
        mainScene.addSceneObject(cubeSceneObject);

        movableCubeBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(final SelectableBehavior behavior, ObjectState prev,
                                       ObjectState
                    current) {
                if(prev == ObjectState.CLICKED) {
                    if(behavior.getOwnerObject().getComponent(EditableBehavior
                            .getComponentType()) == null) {
                        Log.d(TAG,"Attaching Editable Behavior");
                        behavior.getOwnerObject().attachComponent(editableBehavior);
                    }
                }
            }
        });


    }

    @Override
    public void onStep() {
    }

    void close() {
        if (cursorManager != null) {
            cursorManager.close();
        }
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
