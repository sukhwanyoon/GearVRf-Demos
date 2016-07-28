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
import android.os.Environment;
import android.view.Gravity;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRSpotLight;
import org.gearvrf.GVRTexture;
import org.gearvrf.IAssetEvents;
import org.gearvrf.io.cursor3d.Cursor;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.MovableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.io.sceneeditor.EditableBehavior.DetachListener;
import org.gearvrf.io.sceneeditor.FileBrowserView.FileViewListener;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.utility.Log;
import org.gearvrf.utlis.sceneserializer.SceneSerializer;
import org.gearvrf.utlis.sceneserializer.SceneSerializer.SceneLoaderListener;
import org.joml.Quaternionf;

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
    private static final float TARGET_RADIUS = 1.0f;
    private static final String LOAD_MODEL_DISPLAY_STRING = "Load model from sdcard";
    private static final String SCENE_SAVE_DISPLAY_STRING = "Save scene to sdcard";
    private static final String MODEL_PICKER_TITLE = "Pick a model to load";
    private static final String ENVIRON_PICKER_TITLE = "Pick a background file";
    private static final String CUBEMAP_EXTENSION = ".zip";
    private static final float ENVIRONMENT_SCALE = 200.0f;
    private static final String LOAD_ENVIRONMENT_DISPLAY_STRING = "Load environment from sdcard";
    private static final float TEXT_VIEW_OFFSET = 1.0f;
    private GVRScene mainScene;
    private CursorManager cursorManager;
    private EditableBehavior editableBehavior;
    private GVRContext gvrContext;

    private GVRSceneObject loadModelIcon;
    private GVRTextViewSceneObject loadModelTextView;
    private GVRSceneObject saveSceneIcon;
    private GVRTextViewSceneObject saveSceneTextView;
    private GVRSceneObject loadEnvironIcon;
    private GVRTextViewSceneObject loadEnvironTextView;

    private FileBrowserView fileBrowserView;
    private SceneSerializer sceneSerializer;
    private GVRCubeSceneObject environmentCube;
    private GVRSphereSceneObject environmentSphere;
    private GVRSceneObject environmentSceneObject;
    private String currentModel;

    @Override
    public void onInit(GVRContext gvrContext) {
        this.gvrContext = gvrContext;
        mainScene = gvrContext.getNextMainScene();
        mainScene.getMainCameraRig().getLeftCamera().setBackgroundColor(Color.DKGRAY);
        mainScene.getMainCameraRig().getRightCamera().setBackgroundColor(Color.DKGRAY);
        cursorManager = new CursorManager(gvrContext, mainScene);
        editableBehavior = new EditableBehavior(cursorManager, mainScene, detachListener);
        sceneSerializer = new SceneSerializer();

        float[] position = new float[]{0.0f, -10.0f, 0f};
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R.mipmap
                .ic_launcher)));
        final GVRCubeSceneObject cubeSceneObject = new GVRCubeSceneObject(gvrContext, true,
                material);
        cubeSceneObject.getTransform().setPosition(position[0], position[1], position[2]);
        addToSceneEditor(cubeSceneObject);
        addSceneEditorMenu();
        gvrContext.getEventReceiver().addListener(assetEventListener);
        try {
            sceneSerializer.importScene(gvrContext, mainScene, sceneLoaderListener);
        } catch (IOException e) {
            Log.e(TAG, "Could not import scene, no such file:%s", e.getMessage());
        }
    }

    private SceneLoaderListener sceneLoaderListener = new SceneLoaderListener() {
        @Override
        public void onEnvironmentLoaded(GVRSceneObject envSceneObject) {
            if (envSceneObject == null) {
                addDefaultSurroundings(gvrContext);
            } else {
                environmentSceneObject = envSceneObject;
            }
        }

        @Override
        public void onSceneObjectLoaded(GVRSceneObject sceneObject) {
            attachMovableBehavior(sceneObject);
        }
    };

    IAssetEvents assetEventListener = new IAssetEvents() {
        @Override
        public void onAssetLoaded(GVRContext context, GVRSceneObject model, String filePath,
                                  String errors) {
            Log.d(TAG, "onAssetLoaded:%s", filePath);
        }

        @Override
        public void onModelLoaded(GVRContext context, GVRSceneObject model, String filePath) {
            Log.d(TAG, "onModelLoaded:%s", filePath);
            if (filePath != null && filePath.equals(currentModel)) {
                model.getTransform().setPosition(0, 0, -5);
                addToSceneEditor(model);
                fileBrowserView.modelLoaded();
                sceneSerializer.addToSceneData(model, filePath);
            }
        }

        @Override
        public void onTextureLoaded(GVRContext context, GVRTexture texture, String filePath) {
            Log.d(TAG, "onTextureLoaded:%s", filePath);
        }

        @Override
        public void onModelError(GVRContext context, String error, String filePath) {
            Log.d(TAG, "onModelError:%s:%s", error, filePath);
            if (filePath != null && filePath.equals(currentModel)) {
                fileBrowserView.modelLoaded();
            }
        }

        @Override
        public void onTextureError(GVRContext context, String error, String filePath) {
            Log.d(TAG, "onTextureError:%s:%s", error, filePath);
            if (filePath != null && filePath.equals(currentModel)) {
                fileBrowserView.modelLoaded();
            }
        }
    };

    private void loadModelToScene(String modelFileName) {
        Log.d(TAG, "Loading the model to scene:%s" + modelFileName);
        try {
            currentModel = modelFileName;
            gvrContext.loadModelFromSD(modelFileName);
        } catch (IOException e) {
            Log.e(TAG, "Could not load model:" + modelFileName + e.getMessage());
        }
    }

    FileViewListener modelFileViewListener = new FileViewListener() {
        @Override
        public void onClose() {
            cursorManager.disableSettingsCursor();
            Log.d(TAG, "Re-enable file browser icon");
            setMenuVisibility(true);
        }

        @Override
        public void onFileSelected(String modelFileName) {
            loadModelToScene(modelFileName);
        }
    };

    FileViewListener environFileViewListener = new FileViewListener() {
        @Override
        public void onClose() {
            cursorManager.disableSettingsCursor();
            Log.d(TAG, "Re-enable file browser icon");
            setMenuVisibility(true);
        }

        @Override
        public void onFileSelected(String fileName) {
            addSurroundings(fileName);
        }
    };

    private void addSceneEditorMenu() {
        addLoadModelIcon();
        addSaveSceneIcon();
        addLoadEnvironIcon();
    }

    private void addLoadEnvironIcon() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R
                .drawable.environment_icon)));
        loadEnvironIcon = new GVRSceneObject(gvrContext, 1, 1);
        loadEnvironIcon.getRenderData().setMaterial(material);

        SelectableBehavior fileBrowserBehavior = new SelectableBehavior(cursorManager);
        loadEnvironIcon.attachComponent(fileBrowserBehavior);
        loadEnvironIcon.getTransform().setPosition(1f, -3, -5);
        loadEnvironIcon.getTransform().rotateByAxis(-25, 1, 0, 0);
//        loadEnvironIcon.getTransform().rotateByAxis(-25, 0, 1, 0);

        mainScene.addSceneObject(loadEnvironIcon);

        loadEnvironTextView = new GVRTextViewSceneObject(gvrContext, 2.5f, 1.0f,
                LOAD_ENVIRONMENT_DISPLAY_STRING);
        loadEnvironTextView.setTextColor(Color.WHITE);
        loadEnvironTextView.setBackgroundColor(R.drawable.rounded_rect_bg);
        loadEnvironTextView.setGravity(Gravity.CENTER);
        loadEnvironTextView.getRenderData().setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        loadEnvironTextView.getTransform().setPosition(0f, -TEXT_VIEW_OFFSET, 0);
        loadEnvironTextView.getTransform().rotateByAxis(-45, 1, 0, 0);
        loadEnvironIcon.addChildObject(loadEnvironTextView);
        loadEnvironTextView.setTextSize(6);

        fileBrowserBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current, Cursor cursor) {
                if (current == ObjectState.CLICKED) {
                    final int cursorControllerId = cursorManager.enableSettingsCursor(cursor);
                    gvrContext.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fileBrowserView = new FileBrowserView(gvrContext,
                                    mainScene, cursorControllerId, environFileViewListener,
                                    FileBrowserView.ENVIRONMENT_EXTENSIONS, ENVIRON_PICKER_TITLE);
                            fileBrowserView.render();
                            setMenuVisibility(false);
                        }
                    });
                }
            }
        });
    }

    private void addSaveSceneIcon() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R
                .drawable.save_icon)));
        saveSceneIcon = new GVRSceneObject(gvrContext, 1, 1);
        saveSceneIcon.getRenderData().setMaterial(material);

        SelectableBehavior saveSceneBehavior = new SelectableBehavior(cursorManager);
        saveSceneIcon.attachComponent(saveSceneBehavior);
        saveSceneIcon.getTransform().setPosition(0, -2.5f, -5);
        saveSceneIcon.getTransform().rotateByAxis(-25, 1, 0, 0);

        //mainScene.addSceneObject(saveSceneIcon);

        saveSceneTextView = new GVRTextViewSceneObject(gvrContext, SCENE_SAVE_DISPLAY_STRING);
        saveSceneTextView.setTextColor(Color.WHITE);
        saveSceneTextView.setBackgroundColor(R.drawable.rounded_rect_bg);
        saveSceneTextView.setGravity(Gravity.CENTER);

        saveSceneTextView.getTransform().setPosition(0, -TEXT_VIEW_OFFSET, 0);
        saveSceneTextView.getTransform().rotateByAxis(-45, 1, 0, 0);
        saveSceneIcon.addChildObject(saveSceneTextView);
        saveSceneTextView.setTextSize(6);

        saveSceneBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current, Cursor cursor) {
                if (current == ObjectState.CLICKED) {
                    Log.d(TAG, "Reset scene now");
                }
            }
        });
    }

    private void addLoadModelIcon() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(gvrContext.loadTexture(new GVRAndroidResource(gvrContext, R
                .drawable.model_3d_icon)));
        loadModelIcon = new GVRSceneObject(gvrContext, 1, 1);
        loadModelIcon.getRenderData().setMaterial(material);

        SelectableBehavior fileBrowserBehavior = new SelectableBehavior(cursorManager);
        loadModelIcon.attachComponent(fileBrowserBehavior);
        loadModelIcon.getTransform().setPosition(-1f, -3, -5);
        loadModelIcon.getTransform().rotateByAxis(-25, 1, 0, 0);
//        loadModelIcon.getTransform().rotateByAxis(25, 0, 1, 0);

        mainScene.addSceneObject(loadModelIcon);

        loadModelTextView = new GVRTextViewSceneObject(gvrContext, LOAD_MODEL_DISPLAY_STRING);
        loadModelTextView.setTextColor(Color.WHITE);
        loadModelTextView.setBackgroundColor(R.drawable.rounded_rect_bg);
        loadModelTextView.setGravity(Gravity.CENTER);
        loadModelTextView.getRenderData().setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        loadModelTextView.getTransform().setPosition(0, -TEXT_VIEW_OFFSET, 0);
        loadModelTextView.getTransform().rotateByAxis(-45, 1, 0, 0);
        loadModelIcon.addChildObject(loadModelTextView);
        loadModelTextView.setTextSize(6);

        fileBrowserBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current, Cursor cursor) {
                if (current == ObjectState.CLICKED) {
                    final int cursorControllerId = cursorManager.enableSettingsCursor(cursor);
                    gvrContext.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            fileBrowserView = new FileBrowserView(gvrContext,
                                    mainScene, cursorControllerId, modelFileViewListener,
                                    FileBrowserView.MODEL_EXTENSIONS, MODEL_PICKER_TITLE);
                            fileBrowserView.render();
                            setMenuVisibility(false);
                            try {
                                sceneSerializer.exportScene();
                            } catch (IOException e) {
                                Log.e(TAG, "%s", e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    void setMenuVisibility(boolean visibility) {
        loadModelIcon.setEnable(visibility);
        loadModelTextView.setEnable(visibility);
        saveSceneIcon.setEnable(visibility);
        saveSceneTextView.setEnable(visibility);
        loadEnvironIcon.setEnable(visibility);
        loadEnvironTextView.setEnable(visibility);
    }

    private void attachMovableBehavior(GVRSceneObject gvrSceneObject) {
        MovableBehavior movableCubeBehavior = new MovableBehavior(cursorManager);
        gvrSceneObject.attachComponent(movableCubeBehavior);
        movableCubeBehavior.setStateChangedListener(stateChangedListener);
    }

    private void addToSceneEditor(GVRSceneObject newSceneObject) {
        attachMovableBehavior(newSceneObject);
        mainScene.addSceneObject(newSceneObject);
        float radius = newSceneObject.getBoundingVolume().radius;
        float scalingFactor = TARGET_RADIUS / radius;
        newSceneObject.getTransform().setScale(scalingFactor, scalingFactor, scalingFactor);
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
            setMenuVisibility(true);
        }

        @Override
        public void onRemoveFromScene(GVRSceneObject gvrSceneObject) {
            sceneSerializer.removeFromSceneData(gvrSceneObject);
            mainScene.removeSceneObject(gvrSceneObject);
        }
    };

    private StateChangedListener stateChangedListener = new StateChangedListener() {
        public static final long CLICK_THRESHOLD = 500;
        public long prevClickTimeStamp = 0;

        @Override
        public void onStateChanged(final SelectableBehavior behavior, ObjectState prev,
                                   ObjectState current, Cursor cursor) {
            if (prev == ObjectState.CLICKED) {
                long currentTimeStamp = System.currentTimeMillis();
                if (prevClickTimeStamp != 0 && (currentTimeStamp - prevClickTimeStamp) <
                        CLICK_THRESHOLD) {
                    Log.d(TAG, "Double Click !!!!");
                    if (behavior.getOwnerObject().getComponent(EditableBehavior.getComponentType
                            ()) == null) {
                        Log.d(TAG, "Attaching Editable Behavior");
                        editableBehavior.setCursor(cursor);
                        behavior.getOwnerObject().attachComponent(editableBehavior);
                        setMenuVisibility(false);
                    }
                }
                prevClickTimeStamp = System.currentTimeMillis();
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

    private void addSurroundings(String fileName) {
        GVRAndroidResource resource = null;
        String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + fileName;
        try {
            resource = new GVRAndroidResource(fullPath);
        } catch (IOException e) {
            Log.e(TAG, "Could not load texture file:%s", e.getMessage());
            fileBrowserView.modelLoaded();
            return;
        }

        if (fileName.endsWith(CUBEMAP_EXTENSION)) {
            //TODO find the reason for the crash on just replacing the texture.
            initializeSurroundingCube();
            Future<GVRTexture> futureCubeTexture = gvrContext.loadFutureCubemapTexture
                    (resource);
            environmentCube.getRenderData().getMaterial().setMainTexture(futureCubeTexture);
            if (environmentSceneObject != environmentCube) {
                mainScene.removeSceneObject(environmentSceneObject);
                environmentSceneObject = environmentCube;
                mainScene.addSceneObject(environmentSceneObject);
            }
        } else {
            //TODO find the reason for the crash on just replacing the texture.
            initializeSurroundingSphere();
            Future<GVRTexture> futureSphereTexture = gvrContext.loadFutureTexture(resource);
            environmentSphere.getRenderData().getMaterial().setMainTexture(futureSphereTexture);
            if (environmentSceneObject != environmentSphere) {
                mainScene.removeSceneObject(environmentSceneObject);
                environmentSceneObject = environmentSphere;
                mainScene.addSceneObject(environmentSceneObject);
            }
        }
        sceneSerializer.setEnvironmentData(fullPath);
        fileBrowserView.modelLoaded();
    }

    private void initializeSurroundingSphere() {
        GVRMaterial material = new GVRMaterial(gvrContext);
        environmentSphere = new GVRSphereSceneObject(gvrContext, false,
                material);
        environmentSphere.getTransform().setScale(ENVIRONMENT_SCALE, ENVIRONMENT_SCALE,
                ENVIRONMENT_SCALE);
    }

    private void addDefaultSurroundings(GVRContext gvrContext) {
        Future<GVRTexture> futureCubemapTexture = gvrContext.loadFutureTexture(new
                GVRAndroidResource(gvrContext, R.drawable.skybox_gridroom));
        initializeSurroundingSphere();
        environmentSphere.getRenderData().getMaterial().setMainTexture(futureCubemapTexture);
        environmentSceneObject = environmentSphere;
        mainScene.addSceneObject(environmentSceneObject);
    }

    private void initializeSurroundingCube() {
        GVRMaterial cubemapMaterial = new GVRMaterial(gvrContext,
                GVRMaterial.GVRShaderType.Cubemap.ID);
        environmentCube = new GVRCubeSceneObject(gvrContext, false,
                cubemapMaterial);
        environmentCube.getTransform().setScale(ENVIRONMENT_SCALE, ENVIRONMENT_SCALE,
                ENVIRONMENT_SCALE);
    }

    public void saveState() {
        try {
            sceneSerializer.exportScene();
        } catch (IOException e) {
            Log.d(TAG, "Could not export scene:%s", e.getMessage());
        }
    }
}
