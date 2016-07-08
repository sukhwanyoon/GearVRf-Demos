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
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRShaderTemplate;
import org.gearvrf.GVRTexture;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.io.sceneeditor.EditObjectView.WindowCloseListener;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.utility.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

/**
 * This sample can be used with a Laser Cursor as well as an Object Cursor. By default the Object
 * Cursor is enabled. To switch to a Laser Cursor simply rename the "laser_cursor_settings.xml"
 * in the assets directory to "settings.xml"
 */
public class SceneEditorMain extends GVRMain {
    private static final String TAG = SceneEditorMain.class.getSimpleName();
    private static final String FILEBROWSER_ASSET = "generic-houses-1.fbx";
    private static final float TARGET_RADIUS = 1.0f;
    private GVRScene mainScene;
    private CursorManager cursorManager;
    private EditableBehavior editableBehavior;
    private GVRContext gvrContext;

    @Override
    public void onInit(GVRContext gvrContext) {
        this.gvrContext = gvrContext;
        mainScene = gvrContext.getNextMainScene();
        mainScene.getMainCameraRig().getLeftCamera().setBackgroundColor(Color.DKGRAY);
        mainScene.getMainCameraRig().getRightCamera().setBackgroundColor(Color.DKGRAY);
        cursorManager = new CursorManager(gvrContext, mainScene);
        editableBehavior = new EditableBehavior(cursorManager, mainScene);

        float[] position = new float[]{0.0f, 0.0f, -5.0f};
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R.mipmap
                .ic_launcher)));
        final GVRCubeSceneObject cubeSceneObject = new GVRCubeSceneObject(gvrContext, true,
                material);
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        addToSceneEditor(cubeSceneObject);
        addFileBrowserIcon();
    }

    private void loadModelToScene(String modelFileName) {
        try {
            GVRSceneObject model = gvrContext.loadModelFromSD(modelFileName);
            model = model.getChildByIndex(0);
            model.getParent().removeChildObject(model);
            model.getTransform().setPosition(0,0,-5);
            addToSceneEditor(model);
            int end = modelFileName.lastIndexOf(".");
            int start = modelFileName.lastIndexOf(File.separator, end) + 1;
            String name = "so_" + modelFileName.substring(start, end);
            model.setName(name);
        } catch (IOException e) {
            Log.e(TAG,"Could not load model:" + modelFileName  + e.getMessage());
        }

    }

    private void addFileBrowserIcon() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setDiffuseColor(1,1,0,1);
        Future<GVRMesh> mesh;
        try {
            mesh = gvrContext.loadFutureMesh(new GVRAndroidResource(gvrContext,
                    "box.obj"));
        } catch (IOException e) {
            Log.e(TAG,"Could not load folder resource");
            return;
        }
        GVRSceneObject fileBrowserIcon = new GVRSceneObject(gvrContext);
        GVRRenderData renderData = new GVRRenderData(gvrContext);
        renderData.setMesh(mesh);
        renderData.setMaterial(material);
        renderData.setShaderTemplate(GVRPhongShader.class);
        fileBrowserIcon.attachRenderData(renderData);
        fileBrowserIcon.getTransform().setPosition(0, -3, -5);
        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager);
        fileBrowserIcon.attachComponent(selectableBehavior);
        mainScene.addSceneObject(fileBrowserIcon);
        final WindowCloseListener listener = new WindowCloseListener() {
            @Override
            public void onClose() {
                cursorManager.disableSettingsCursor();
            }

            @Override
            public void onScaleChange() {

            }

            @Override
            public void onModelSelected(String modelFileName) {
                loadModelToScene(modelFileName);
            }
        };
        selectableBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current) {
                if (current == ObjectState.CLICKED) {
                    final int cursorControllerId = cursorManager.enableSettingsCursor();
                    gvrContext.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            FileBrowserView fileBrowserView = new FileBrowserView(gvrContext,
                                    mainScene, cursorControllerId, listener);
                            fileBrowserView.render();
                        }
                    });
                }
            }
        });
    }

    private void addToSceneEditor(GVRSceneObject newSceneObject) {
        MovableBehavior movableCubeBehavior = new MovableBehavior(cursorManager);
        newSceneObject.attachComponent(movableCubeBehavior);
        mainScene.addSceneObject(newSceneObject);
        movableCubeBehavior.setStateChangedListener(stateChangedListener);
        float radius = newSceneObject.getBoundingVolume().radius;
        float scalingFactor = TARGET_RADIUS/radius;
        Log.d(TAG,"Radius:%f, scaling factor:%f",radius, scalingFactor);
        newSceneObject.getTransform().setScale(scalingFactor,scalingFactor,scalingFactor);
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

    private StateChangedListener stateChangedListener = new StateChangedListener() {
        @Override
        public void onStateChanged(final SelectableBehavior behavior, ObjectState prev,
                                   ObjectState
                                           current) {
            if (prev == ObjectState.CLICKED) {
                if (behavior.getOwnerObject().getComponent(EditableBehavior
                        .getComponentType()) == null) {
                    Log.d(TAG, "Attaching Editable Behavior");
                    behavior.getOwnerObject().attachComponent(editableBehavior);
                }
            }
        }
    };
}
