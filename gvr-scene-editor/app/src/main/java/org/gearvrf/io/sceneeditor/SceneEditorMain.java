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
import android.view.Gravity;

import org.gearvrf.FutureWrapper;
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
import org.gearvrf.GVRTexture;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.io.sceneeditor.EditableBehavior.DetachListener;
import org.gearvrf.io.sceneeditor.FileBrowserView.FileViewListener;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject.IntervalFrequency;
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
    private static final float CUBE_WIDTH = 200.0f;
    private static final float TARGET_RADIUS = 1.0f;
    private static final String FILE_BROWSER_DISPLAY_STRING = "Load model from sdcard";
    private GVRScene mainScene;
    private CursorManager cursorManager;
    private EditableBehavior editableBehavior;
    private GVRContext gvrContext;
    private GVRSceneObject fileBrowserIcon;
    private GVRTextViewSceneObject fileBrowserTextView;

    @Override
    public void onInit(GVRContext gvrContext) {
        this.gvrContext = gvrContext;
        mainScene = gvrContext.getNextMainScene();
        mainScene.getMainCameraRig().getLeftCamera().setBackgroundColor(Color.DKGRAY);
        mainScene.getMainCameraRig().getRightCamera().setBackgroundColor(Color.DKGRAY);
        cursorManager = new CursorManager(gvrContext, mainScene);
        editableBehavior = new EditableBehavior(cursorManager, mainScene, detachListener);

        float[] position = new float[]{0.0f, 0.0f, -5.0f};
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R.mipmap
                .ic_launcher)));
        final GVRCubeSceneObject cubeSceneObject = new GVRCubeSceneObject(gvrContext, true,
                material);
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        addToSceneEditor(cubeSceneObject);
        addFileBrowserIcon();
        addSurroundings(gvrContext, mainScene);
    }

    private void loadModelToScene(String modelFileName) {
        try {
            GVRSceneObject model = gvrContext.loadModelFromSD(modelFileName);
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

    FileViewListener fileViewListener = new FileViewListener() {
        @Override
        public void onClose() {
            cursorManager.disableSettingsCursor();
            Log.d(TAG,"Re-enable file browser icon");
            fileBrowserIcon.setEnable(true);
            fileBrowserTextView.setEnable(true);
        }
        @Override
        public void onModelSelected(String modelFileName) {
            loadModelToScene(modelFileName);
        }
    };

    private void loadSeparateObj() {
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
    }

    private void loadFromModel() {
        try {
            GVRSceneObject fileBrowserIcon = gvrContext.loadModel("box.fbx");
        } catch (IOException e) {
            Log.e(TAG,"Could not load browser icon");
            return;
        }
    }

    private void addFileBrowserIcon() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R.drawable.folder_icon)));
        fileBrowserIcon = new GVRCubeSceneObject(gvrContext, true,
                material);


        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager);
        fileBrowserIcon.attachComponent(selectableBehavior);
        fileBrowserIcon.getTransform().setPosition(0, -3, -5);
        fileBrowserIcon.getTransform().rotateByAxis(-25,1,0,0);
        mainScene.addSceneObject(fileBrowserIcon);

        fileBrowserTextView = new GVRTextViewSceneObject(gvrContext,FILE_BROWSER_DISPLAY_STRING);
        fileBrowserTextView.setTextColor(Color.WHITE);
        fileBrowserTextView.setBackgroundColor(Color.BLACK);
        fileBrowserTextView.setGravity(Gravity.CENTER);

        fileBrowserTextView.getTransform().setPosition(0,-4.6f,-5);
        fileBrowserTextView.getTransform().rotateByAxis(-45,1,0,0);
        mainScene.addSceneObject(fileBrowserTextView);
        fileBrowserTextView.setTextSize(6);

        mainScene.addSceneObject(fileBrowserIcon);

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
                                    mainScene, cursorControllerId, fileViewListener);
                            fileBrowserView.render();
                            fileBrowserIcon.setEnable(false);
                            fileBrowserTextView.setEnable(false);
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

    private EditableBehavior.DetachListener detachListener = new DetachListener() {
        @Override
        public void onDetach() {
            fileBrowserTextView.setEnable(true);
            fileBrowserIcon.setEnable(true);
        }
    };

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
                    fileBrowserTextView.setEnable(false);
                    fileBrowserIcon.setEnable(false);
                }
            }
        }
    };

    @Override
    public GVRTexture getSplashTexture(GVRContext gvrContext) {
        Bitmap bitmap = BitmapFactory.decodeResource(
                gvrContext.getContext().getResources(),
                R.mipmap.ic_launcher);
        // return the correct splash screen bitmap
        return new GVRBitmapTexture(gvrContext, bitmap);
    }

    private void addSurroundings(GVRContext gvrContext, GVRScene scene) {
        FutureWrapper<GVRMesh> futureQuadMesh = new FutureWrapper<GVRMesh>(
                gvrContext.createQuad(CUBE_WIDTH, CUBE_WIDTH));
        Future<GVRTexture> futureCubemapTexture = gvrContext
                .loadFutureCubemapTexture(
                        new GVRAndroidResource(gvrContext, R.raw.earth));

        GVRMaterial cubemapMaterial = new GVRMaterial(gvrContext,
                GVRMaterial.GVRShaderType.Cubemap.ID);
        cubemapMaterial.setMainTexture(futureCubemapTexture);

        // surrounding cube
        GVRSceneObject frontFace = new GVRSceneObject(gvrContext,
                futureQuadMesh, futureCubemapTexture);
        frontFace.getRenderData().setMaterial(cubemapMaterial);
        frontFace.setName("front");
        frontFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        scene.addSceneObject(frontFace);
        frontFace.getTransform().setPosition(0.0f, 0.0f, -CUBE_WIDTH * 0.5f);

        GVRSceneObject backFace = new GVRSceneObject(gvrContext, futureQuadMesh,
                futureCubemapTexture);
        backFace.getRenderData().setMaterial(cubemapMaterial);
        backFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        backFace.setName("back");
        scene.addSceneObject(backFace);
        backFace.getTransform().setPosition(0.0f, 0.0f, CUBE_WIDTH * 0.5f);
        backFace.getTransform().rotateByAxis(180.0f, 0.0f, 1.0f, 0.0f);

        GVRSceneObject leftFace = new GVRSceneObject(gvrContext, futureQuadMesh,
                futureCubemapTexture);
        leftFace.getRenderData().setMaterial(cubemapMaterial);
        leftFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        leftFace.setName("left");
        scene.addSceneObject(leftFace);
        leftFace.getTransform().setPosition(-CUBE_WIDTH * 0.5f, 0.0f, 0.0f);
        leftFace.getTransform().rotateByAxis(90.0f, 0.0f, 1.0f, 0.0f);

        GVRSceneObject rightFace = new GVRSceneObject(gvrContext,
                futureQuadMesh, futureCubemapTexture);
        rightFace.getRenderData().setMaterial(cubemapMaterial);
        rightFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        rightFace.setName("right");
        scene.addSceneObject(rightFace);
        rightFace.getTransform().setPosition(CUBE_WIDTH * 0.5f, 0.0f, 0.0f);
        rightFace.getTransform().rotateByAxis(-90.0f, 0.0f, 1.0f, 0.0f);

        GVRSceneObject topFace = new GVRSceneObject(gvrContext, futureQuadMesh,
                futureCubemapTexture);
        topFace.getRenderData().setMaterial(cubemapMaterial);
        topFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        topFace.setName("top");
        scene.addSceneObject(topFace);
        topFace.getTransform().setPosition(0.0f, CUBE_WIDTH * 0.5f, 0.0f);
        topFace.getTransform().rotateByAxis(90.0f, 1.0f, 0.0f, 0.0f);

        GVRSceneObject bottomFace = new GVRSceneObject(gvrContext,
                futureQuadMesh, futureCubemapTexture);
        bottomFace.getRenderData().setMaterial(cubemapMaterial);
        bottomFace.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
        bottomFace.setName("bottom");
        scene.addSceneObject(bottomFace);
        bottomFace.getTransform().setPosition(0.0f, -CUBE_WIDTH * 0.5f, 0.0f);
        bottomFace.getTransform().rotateByAxis(-90.0f, 1.0f, 0.0f, 0.0f);
    }

}
