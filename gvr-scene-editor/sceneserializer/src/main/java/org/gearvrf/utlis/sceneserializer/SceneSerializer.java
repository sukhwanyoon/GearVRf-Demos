package org.gearvrf.utlis.sceneserializer;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.IAssetEvents;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.utility.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class SceneSerializer {
    private static final String TAG = SceneSerializer.class.getSimpleName();
    public static final String ENVIRONMENT_SCENE_OBJECT_NAME = "environment";
    private static final String DEFAULT_SCENE_NAME = "scene.json";
    private static final String CUBEMAP_EXTENSION = ".zip";
    public static final float ENVIRONMENT_SCALE = 200.0f;
    private List<SceneObjectData> sceneObjectList;
    private static final String SCENE_OBJECTS_PROPERTY = "sceneObjects";
    private static final String ENVIRONMENT_PROPERTY = "environmentSrc";
    private String environmentSrc;
    private Gson gson;

    public SceneSerializer() {
        sceneObjectList = new ArrayList<SceneObjectData>();
        gson = new Gson();
        environmentSrc = null;
    }

    public void addToScene(GVRSceneObject gvrSceneObject, String source) {
        SceneObjectData sod = SceneObjectData.createSceneObjectData(gvrSceneObject,source);
        sceneObjectList.add(sod);
    }

    public void setEnvironment(String filePath) {
        environmentSrc = filePath;
    }

    public void removeFromScene(GVRSceneObject gvrSceneObject) {
        Iterator<SceneObjectData> iterator = sceneObjectList.iterator();
        while(iterator.hasNext()) {
            SceneObjectData sod = iterator.next();
            if(sod.getGvrSceneObject() == gvrSceneObject) {
                iterator.remove();
                return;
            }
        }
    }

    public void exportScene(File location) throws IOException {
        for(SceneObjectData sod: sceneObjectList) {
            sod.setModelMatrix(sod.getGvrSceneObject().getTransform().getModelMatrix());
            sod.setName(sod.getGvrSceneObject().getName());
        }
        JsonElement jsonSceneObjects = gson.toJsonTree(sceneObjectList);
        JsonObject scene = new JsonObject();
        scene.add(SCENE_OBJECTS_PROPERTY,jsonSceneObjects);
        scene.addProperty(ENVIRONMENT_PROPERTY, environmentSrc);

        FileWriter fw = new FileWriter(location);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(scene.toString());
        bw.close();
    }

    public void exportScene() throws IOException {
        File externalStorageLocation = new File(Environment.getExternalStorageDirectory(),
                DEFAULT_SCENE_NAME);
        exportScene(externalStorageLocation);
    }

    public void importScene(GVRContext gvrContext, GVRScene gvrScene) throws IOException {
        importScene(gvrContext,gvrScene,
                new File(Environment.getExternalStorageDirectory(),DEFAULT_SCENE_NAME));
    }

    public void importScene(GVRContext gvrContext, GVRScene gvrScene, File file)
            throws IOException {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(new FileReader(file));
        JsonObject sceneJsonObject = jsonElement.getAsJsonObject();

        JsonArray sceneObjectArray = sceneJsonObject.getAsJsonArray(SCENE_OBJECTS_PROPERTY);
        Type collectionType = new TypeToken<Collection<SceneObjectData>>(){}.getType();
        Collection<SceneObjectData> sceneObjectDatas = gson.fromJson(sceneObjectArray, collectionType);

        String environmentSource = sceneJsonObject.get(ENVIRONMENT_PROPERTY).getAsString();
        loadEnvironment(gvrContext, gvrScene, environmentSource);


        AssetObserver assetObserver = new AssetObserver(sceneObjectDatas,gvrContext,gvrScene);
        gvrContext.getEventReceiver().addListener(assetObserver);
        assetObserver.startLoading();
    }

    private void loadEnvironment(GVRContext gvrContext, GVRScene gvrScene, String environmentSource) {
        GVRAndroidResource resource = null;
        try {
            resource = new GVRAndroidResource(environmentSource);
        } catch (IOException e) {
            Log.e(TAG,"Could not load texture file:%s",e.getMessage());
            return;
        }

        if(environmentSource.endsWith(CUBEMAP_EXTENSION)) {
            GVRMaterial cubemapMaterial = new GVRMaterial(gvrContext,
                    GVRMaterial.GVRShaderType.Cubemap.ID);
            GVRCubeSceneObject environmentCube = new GVRCubeSceneObject(gvrContext,false,
                    cubemapMaterial);
            environmentCube.getTransform().setScale(ENVIRONMENT_SCALE,ENVIRONMENT_SCALE,
                    ENVIRONMENT_SCALE);
            Future<GVRTexture> futureCubeTexture = gvrContext.loadFutureCubemapTexture
                    (resource);
            environmentCube.getRenderData().getMaterial().setMainTexture
                    (futureCubeTexture);
            gvrScene.addSceneObject(environmentCube);
        } else {
            GVRMaterial material = new GVRMaterial(gvrContext);
            GVRSphereSceneObject environmentSphere = new GVRSphereSceneObject(gvrContext,false,
                    material);
            environmentSphere.getTransform().setScale(ENVIRONMENT_SCALE,ENVIRONMENT_SCALE,
                    ENVIRONMENT_SCALE);
            Future<GVRTexture> futureSphereTexture = gvrContext.loadFutureTexture(resource);
            environmentSphere.getRenderData().getMaterial().setMainTexture(futureSphereTexture);
            gvrScene.addSceneObject(environmentSphere);
        }
    }

    private static class AssetObserver implements IAssetEvents {
        Collection<SceneObjectData> sceneObjectDatas;
        GVRContext context;
        GVRScene scene;
        Iterator<SceneObjectData> iterator;
        SceneObjectData currentSod;

        AssetObserver(Collection<SceneObjectData> sceneObjectDatas, GVRContext context, GVRScene
                scene) {
            this.sceneObjectDatas = sceneObjectDatas;
            this.scene = scene;
            this.context = context;
        }

        void startLoading() {
            iterator = sceneObjectDatas.iterator();
            loadNextAsset();
        }


        @Override
        public void onAssetLoaded(GVRContext context, GVRSceneObject model, String filePath,
                                  String errors) {
            if(currentSod != null && currentSod.getSrc() == filePath) {
                model.getTransform().setModelMatrix(currentSod.getModelMatrix());
                model.setName(currentSod.getName());
                scene.addSceneObject(model);
                loadNextAsset();
            }
        }

        @Override
        public void onModelLoaded(GVRContext context, GVRSceneObject model, String filePath) {
            if(currentSod != null && currentSod.getSrc() == filePath) {
                model.getTransform().setModelMatrix(currentSod.getModelMatrix());
                model.setName(currentSod.getName());
                scene.addSceneObject(model);
                loadNextAsset();
            }
        }

        @Override
        public void onTextureLoaded(GVRContext context, GVRTexture texture, String filePath) {
            if(currentSod != null && currentSod.getSrc() == filePath) {
                Log.d(TAG,"Texture loaded:%s",filePath);
            }
        }

        @Override
        public void onModelError(GVRContext context, String error, String filePath) {
            if(currentSod != null && currentSod.getSrc() == filePath) {
                Log.e(TAG,"Model Loading Error for %s",filePath);
                loadNextAsset();
            }
        }

        @Override
        public void onTextureError(GVRContext context, String error, String filePath) {
            if(currentSod != null && currentSod.getSrc() == filePath) {
                Log.e(TAG, "Texture Loading error for %s", filePath);
                loadNextAsset();
            }
        }

        private void loadNextAsset() {
            while(iterator.hasNext()) {
                currentSod = iterator.next();
                try {
                    context.loadModelFromSD(currentSod.getSrc());
                    break;
                } catch (IOException e) {
                    Log.e(TAG,"Could not load model:%s from sdcard:%s",currentSod.getSrc(),
                            e.getMessage());
                }
            }
            currentSod = null;
        }
    }
}
