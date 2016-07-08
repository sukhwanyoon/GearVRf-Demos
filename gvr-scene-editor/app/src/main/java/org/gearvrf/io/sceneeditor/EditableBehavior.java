package org.gearvrf.io.sceneeditor;

import org.gearvrf.GVRBehavior;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRSceneObject.BoundingVolume;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVRRepeatMode;
import org.gearvrf.animation.GVRRotationByAxisAnimation;
import org.gearvrf.animation.GVRRotationByAxisWithPivotAnimation;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.sceneeditor.EditObjectView.WindowCloseListener;
import org.gearvrf.utility.Log;

import java.io.IOException;

public class EditableBehavior extends GVRBehavior implements WindowCloseListener {
    public static final String TAG = EditableBehavior.class.getSimpleName();
    private static final String ARROW_MODEL = "arrow.fbx";
    private static long TYPE_EDITABLE = ((long) EditableBehavior.class.hashCode() << 32) & (System
            .currentTimeMillis() & 0xffffffff);

    private CursorManager cursorManager;
    private GVRScene scene;
    private EditObjectView editableView;
    private GVRSceneObject arrow;
    private GVRAnimationEngine animationEngine;
    private GVRAnimation rotationAnimation;

    protected EditableBehavior(CursorManager cursorManager, GVRScene scene) {
        super(cursorManager.getGvrContext());
        mType = getComponentType();
        this.scene = scene;
        this.cursorManager = cursorManager;
        animationEngine = cursorManager.getGvrContext().getAnimationEngine();
        try {
            arrow = getGVRContext().loadModel(ARROW_MODEL);
            rotationAnimation = new GVRRotationByAxisAnimation(arrow, 1.0f, 360.0f, 0.0f, 1.0f,
                    0.0f).setRepeatMode(GVRRepeatMode.REPEATED).setRepeatCount(-1);
        } catch (IOException e) {
            Log.e(TAG, "Could not load arrow model:", e.getMessage());
        }

    }

    @Override
    public void onAttach(final GVRSceneObject newOwner) {
        final int cursorControllerId = cursorManager.enableSettingsCursor();
        getGVRContext().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editableView = new EditObjectView(getGVRContext(), scene, cursorControllerId,
                        EditableBehavior.this);
                editableView.setSceneObject(newOwner);
                editableView.render();
            }
        });
        if (arrow != null) {
            scene.addSceneObject(arrow);
            rotationAnimation.start(animationEngine);
            adjustArrowPosition(newOwner);
        }
    }

    private void adjustArrowPosition(GVRSceneObject ownerObject) {
        if(ownerObject != null) {
            BoundingVolume volume = ownerObject.getBoundingVolume();
            arrow.getTransform().setPosition(volume.center.x, volume.center.y + volume.radius,
                    volume.center.z);
        }
    }

    @Override
    public void onDetach(GVRSceneObject oldOwner) {
        scene.removeSceneObject(arrow);
        animationEngine.stop(rotationAnimation);
    }


    @Override
    public void onClose() {
        cursorManager.disableSettingsCursor();
        getOwnerObject().detachComponent(getComponentType());
    }

    @Override
    public void onScaleChange() {
        adjustArrowPosition(getOwnerObject());
    }

    @Override
    public void onModelSelected(String modelFileName) {

    }

    public static long getComponentType() {
        return TYPE_EDITABLE;
    }
}
