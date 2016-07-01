package org.gearvrf.io.sceneeditor;

import org.gearvrf.GVRBehavior;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.sceneeditor.EditObjectView.WindowCloseListener;

public class EditableBehavior extends GVRBehavior implements WindowCloseListener {
    public static final String TAG = EditableBehavior.class.getSimpleName();

    private static long TYPE_EDITABLE = ((long) EditableBehavior.class.hashCode() << 32) & (System
            .currentTimeMillis() & 0xffffffff);

    private CursorManager cursorManager;
    private GVRScene scene;
    private EditObjectView editableView;

    protected EditableBehavior(CursorManager cursorManager, GVRScene scene) {
        super(cursorManager.getGvrContext());
        mType = getComponentType();
        this.scene = scene;
        this.cursorManager = cursorManager;
    }

    @Override
    public void onAttach(final GVRSceneObject newOwner) {
        final int cursorControllerId = cursorManager.enableSettingsCursor();
        final float[] lookAt = scene.getMainCameraRig().getLookAt();
        getGVRContext().getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                editableView = new EditObjectView(getGVRContext(), scene, cursorControllerId,
                        EditableBehavior.this);
                editableView.setSceneObject(newOwner);
                editableView.render(lookAt[0]*5.0f,lookAt[1]*5.0f,lookAt[2]*5.0f);
            }
        });
    }

    @Override
    public void onClose() {
        cursorManager.disableSettingsCursor();
        getOwnerObject().detachComponent(getComponentType());
    }

    public static long getComponentType() {
        return TYPE_EDITABLE;
    }
}
