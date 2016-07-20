package org.gearvrf.io.sceneeditor.sceneserializer;

import android.os.Environment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.IAssetEvents;

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

public class SceneSerializer {

    private static final String DEFAULT_SCENE_NAME = "scene.json";
    private List<SceneObjectData> sceneObjectList;
    private Gson gson;

    public SceneSerializer() {
        sceneObjectList = new ArrayList<SceneObjectData>();
        gson = new Gson();
    }

    public void addToScene(GVRSceneObject gvrSceneObject, String source) {
        SceneObjectData sod = SceneObjectData.createSceneObjectData(gvrSceneObject,source);
        sceneObjectList.add(sod);
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
        String jsonScene = gson.toJson(sceneObjectList);
        FileWriter fw = new FileWriter(location);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(jsonScene);
        bw.close();
    }

    public void exportScene() throws IOException {
        File externalStorageLocation = new File(Environment.getExternalStorageDirectory(),
                DEFAULT_SCENE_NAME);
        exportScene(externalStorageLocation);
    }

    public void importScene(GVRContext gvrContext, GVRScene gvrScene, File file)
            throws
            IOException {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = br.readLine()) != null) {
            builder.append(line);
        }
        String json = builder.toString();
        br.close();
        Type collectionType = new TypeToken<Collection<SceneObjectData>>(){}.getType();
        Collection<SceneObjectData> sceneObjectDatas = gson.fromJson(json, collectionType);
        gvrContext.getEventReceiver().addListener(new ModelLoader(sceneObjectDatas,gvrContext,gvrScene));

    }

    private static class ModelLoader implements IAssetEvents {
        Collection<SceneObjectData> sceneObjectDatas;
        GVRContext context;
        GVRScene scene;
        Iterator<SceneObjectData> iterator;
        SceneObjectData currentSod;

        ModelLoader(Collection<SceneObjectData> sceneObjectDatas, GVRContext context, GVRScene
                scene) {
            this.sceneObjectDatas = sceneObjectDatas;
            this.scene = scene;
            this.context = context;
        }

        void startLoading() {
            iterator = sceneObjectDatas.iterator();
            if(iterator.hasNext()) {
                currentSod = iterator.next();
                try {
                    context.loadModelFromSD(currentSod.getSrc());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        @Override
        public void onAssetLoaded(GVRContext context, GVRSceneObject model, String filePath,
                                  String errors) {

        }

        @Override
        public void onModelLoaded(GVRContext context, GVRSceneObject model, String filePath) {
            model.getTransform().setModelMatrix(currentSod.getModelMatrix());
            model.setName(currentSod.getName());
            scene.addSceneObject(model);
            if(iterator.hasNext()) {
                currentSod = iterator.next();
                try {
                    context.loadModelFromSD(currentSod.getSrc());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onTextureLoaded(GVRContext context, GVRTexture texture, String filePath) {

        }

        @Override
        public void onModelError(GVRContext context, String error, String filePath) {

        }

        @Override
        public void onTextureError(GVRContext context, String error, String filePath) {

        }
    }
}
