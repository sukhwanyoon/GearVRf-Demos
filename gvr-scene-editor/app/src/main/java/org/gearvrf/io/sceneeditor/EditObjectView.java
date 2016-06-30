/*
 * Copyright (c) 2016. Samsung Electronics Co., LTD
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.io.sceneeditor;

import android.view.KeyEvent;
import android.view.View;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.io.cursor3d.CustomKeyEvent;
import org.gearvrf.io.cursor3d.R;
import org.gearvrf.utility.Log;

class EditObjectView extends BaseView implements View.OnClickListener {
    private static final String TAG = EditObjectView.class.getSimpleName();

    //Called on main thread
    EditObjectView(final GVRContext context, final GVRScene scene, int settingsCursorId) {
        super(context, scene, settingsCursorId, R.layout.edit_object_layout);
        render(5.0f, 0.0f, BaseView.QUAD_DEPTH);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.tvBackButton) {
            navigateBack(false);
        } else if (id == R.id.done) {
            Log.d(TAG, "Done clicked, close menu");
            navigateBack(true);
        }
    }

    private void navigateBack(boolean cascading) {
        hide();
    }

    @Override
    void onSwipeEvent(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case CustomKeyEvent.KEYCODE_SWIPE_LEFT:
                Log.d(TAG, "Swipe left");
                //Back event: Issue normal back
                navigateBack(false);
                break;
            case CustomKeyEvent.KEYCODE_SWIPE_RIGHT:
                Log.d(TAG, "Swipe right");
                //OK event: Issue cascading back
                navigateBack(true);
                break;
            default:
                //No need to handle other event types
                break;
        }
    }
}
