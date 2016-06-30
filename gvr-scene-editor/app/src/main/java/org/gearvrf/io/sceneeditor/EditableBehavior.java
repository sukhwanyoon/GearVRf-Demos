package org.gearvrf.io.sceneeditor;

import org.gearvrf.GVRBehavior;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRPhongShader;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRSceneObject.BoundingVolume;
import org.gearvrf.GVRTransform;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.scene_objects.GVRConeSceneObject;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.utility.Log;

import java.util.HashSet;
import java.util.Set;

public class EditableBehavior extends GVRBehavior implements StateChangedListener{
    public static final String TAG = EditableBehavior.class.getSimpleName();
    private static final float SCALEUP_FACTOR = 1.1f;
    private static final float SCALEDOWN_FACTOR = 0.9f;
    private static long TYPE_EDITABLE = ((long)EditableBehavior.class.hashCode() << 32) & (System
            .currentTimeMillis() & 0xffffffff);;
    private static final float MENU_RADIUS_OFFSET = 0.2f;
    private CursorManager cursorManager;
    private GVRScene scene;
    private Set<SelectableBehavior> behaviors;
    private enum MenuItem{
        SCALEUP, SCALEDOWN, ROTATEX
    }
    private GVRSceneObject menuRoot;

    protected EditableBehavior(CursorManager cursorManager, GVRScene scene) {
        super(cursorManager.getGvrContext());
        mType = getComponentType();
        GVRContext gvrContext = cursorManager.getGvrContext();
        menuRoot = new GVRSceneObject(gvrContext);
        this.cursorManager = cursorManager;
        behaviors = new HashSet<SelectableBehavior>();

        float[] position = new float[] { 0, 0, 0};
        GVRConeSceneObject scaleUpObject = new GVRConeSceneObject(gvrContext, true);
        position[1] = -0.6f;
        addMenuItem(gvrContext, scaleUpObject, this, MenuItem.SCALEUP, position, 0.5f, 0);

        GVRConeSceneObject scaleDownObject = new GVRConeSceneObject(gvrContext, true);
        position[0] = 0.5f;
        position[1] = -0.6f;
        addMenuItem(gvrContext, scaleDownObject,this,MenuItem.SCALEDOWN, position, 0.5f, 180);

        GVRSphereSceneObject rotateObject = new GVRSphereSceneObject(gvrContext, true);
        position[0] = 0.25f;
        position[1] = 0.0f;
        addMenuItem(gvrContext, rotateObject, this, MenuItem.ROTATEX, position, 0.3f, 0);


    }

    @Override
    public void onAttach(GVRSceneObject newOwner)
    {
        BoundingVolume volume = newOwner.getBoundingVolume();
        Log.d(TAG,"radius of attached object is:%f", volume.radius);
        menuRoot.getTransform().setPositionX(volume.radius + MENU_RADIUS_OFFSET);
        menuRoot.getTransform().setPositionY(0.5f);
        Log.d(TAG,"Adding the menu as a child");
        newOwner.addChildObject(menuRoot);
    }
    
    public static long getComponentType() {
        return TYPE_EDITABLE;
    }


    private void addMenuItem(GVRContext gvrContext, GVRSceneObject defaultSceneObject,
                             StateChangedListener stateChangedListener, MenuItem item, float[]
                                     position, float scale, int rotationX) {
        GVRSceneObject root = new GVRSceneObject(gvrContext);
        root.setTag(item);
        root.getTransform().setScale(scale, scale, scale);
        root.getTransform().setRotationByAxis(rotationX, 1, 0, 0);
        menuRoot.addChildObject(root);
        GVRMaterial red = new GVRMaterial(gvrContext);
        GVRMaterial blue = new GVRMaterial(gvrContext);
        GVRMaterial green = new GVRMaterial(gvrContext);
        GVRMaterial alphaRed = new GVRMaterial(gvrContext);
        red.setDiffuseColor(1, 0, 0, 1);
        blue.setDiffuseColor(0, 0, 1, 1);
        green.setDiffuseColor(0, 1, 0, 1);
        alphaRed.setDiffuseColor(1,0,0,0.5f);

        defaultSceneObject.getRenderData().setMaterial(red);
        defaultSceneObject.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(defaultSceneObject);

        GVRMesh defaultMesh = defaultSceneObject.getRenderData().getMesh();

        GVRSceneObject behind = new GVRSceneObject(gvrContext, defaultMesh);
        behind.getRenderData().setMaterial(alphaRed);
        behind.getRenderData().setShaderTemplate(GVRPhongShader.class);
        behind.getRenderData().getMaterial().setOpacity(0.5f);
        behind.getRenderData().setRenderingOrder(GVRRenderingOrder.TRANSPARENT);
        root.addChildObject(behind);

        GVRSceneObject colliding = new GVRSceneObject(gvrContext, defaultMesh);
        colliding.getRenderData().setMaterial(blue);
        colliding.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(colliding);

        GVRSceneObject clicked = new GVRSceneObject(gvrContext, defaultMesh);
        clicked.getRenderData().setMaterial(green);
        clicked.getRenderData().setShaderTemplate(GVRPhongShader.class);
        root.addChildObject(clicked);


        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager, true);
        root.getTransform().setPosition(position[0], position[1], position[2]);
        root.attachComponent(selectableBehavior);
        selectableBehavior.setStateChangedListener(stateChangedListener);
        behaviors.add(selectableBehavior);
    }

    @Override
    public void onStateChanged(SelectableBehavior behavior, ObjectState prev, ObjectState current) {
        MenuItem item = (MenuItem) behavior.getOwnerObject().getTag();
        GVRTransform transform = getOwnerObject().getTransform();
        if(current == ObjectState.CLICKED) {

            float[] scale = new float[]{transform.getScaleX(), transform.getScaleY(),
                    transform.getScaleZ()};
            switch (item) {
                case SCALEDOWN:
                    transform.setScale(scale[0] * SCALEDOWN_FACTOR,
                            scale[1] * SCALEDOWN_FACTOR, scale[2] * SCALEDOWN_FACTOR);
                    break;
                case SCALEUP:
                    transform.setScale(scale[0] * SCALEUP_FACTOR, scale[1] *
                            SCALEUP_FACTOR, scale[2] * SCALEUP_FACTOR);
                    break;
                case ROTATEX:
                    float rotation = transform.getRotationPitch();
                    transform.setRotationByAxis(rotation + 5, 1, 0, 0);
                    break;
            }
        }
    }
}
