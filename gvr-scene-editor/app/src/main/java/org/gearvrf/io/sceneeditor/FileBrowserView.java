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

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.utility.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class FileBrowserView extends BaseView implements OnClickListener, OnItemClickListener {
    private static final String TAG = FileBrowserView.class.getSimpleName();
    private static final String DEFAULT_PATH = "/sdcard/test";
    private String path;
    private ListView listView;
    private TextView dirView;
    private TextView loadingText;
    private FileViewListener fileViewListener;

    public interface FileViewListener extends WindowChangeListener {
        void onModelSelected(String modelFileName);
    }

    //Called on main thread
    FileBrowserView(final GVRContext context, final GVRScene scene, int settingsCursorId,
                    FileViewListener listener) {
        super(context, scene, settingsCursorId, R.layout.file_browser_layout);
        this.fileViewListener = listener;
        listView = (ListView) findViewById(R.id.lvFiles);
        dirView = (TextView) findViewById(R.id.tvDirName);
        loadingText = (TextView) findViewById(R.id.tvLoading);
        loadingText.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
        Button bDone = (Button) findViewById(R.id.bDone);
        bDone.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FileBrowserView.this.hide();
                fileViewListener.onClose();
            }
        });

        path = DEFAULT_PATH;
        chdir(path);
    }

    public void render() {
        super.renderFileBrowserView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bDone:
                hide();
                break;
        }
    }



    private void chdir(String filepath) {
        path = filepath;
        dirView.setText(path);
        loadingText.setVisibility(View.GONE);

        List values = new ArrayList();
        File dir = new File(path);
        if (!dir.canRead()) {
            dirView.setText(dirView.getText() + " (inaccessible)");
        }

        // only allow model extensions we can read
        File[] list = dir.listFiles(filenameFilter);

        // add .. so the user can go up a level
        if (list != null) {
            values.add("..");
            for (File file : list) {
                values.add(file.getName());
            }
        }

        // sort alphabetically
        Collections.sort(values);

        ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_list_item_2,
                android.R.id.text1, values);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(FileBrowserView.this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String filename = (String) listView.getItemAtPosition(position);
        if (filename.endsWith("..")) {
            // strip out the /..
            int index = path.lastIndexOf(File.separator);
            filename = path.substring(0, index);
        } else if (path.endsWith(File.separator)) {
            filename = path + filename;
        } else {
            filename = path + File.separator + filename;
        }

        if (new File(filename).isDirectory()) {
            chdir(filename);
        } else if (!filename.isEmpty()) {
            // strip out /sdcard
            filename = filename.substring(8);
            listView.setVisibility(View.GONE);
            loadingText.setVisibility(View.VISIBLE);
            // try to load the model
            Log.d(TAG,"Trying to load the model now");
            fileViewListener.onModelSelected(filename);

        }
    }

    public void modelLoaded() {
        hide();
        fileViewListener.onClose();
        loadingText.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    private FilenameFilter filenameFilter = new FilenameFilter() {
        private String[] extensions = new String[]{
                ".fbx", ".dae", ".gltf", ".glb", ".blend", ".3ds", ".ase", ".obj", ".xgl", ".dxf",
                ".lwo", ".lws", ".lxo", ".stl", ".ac", ".ms3d", ".cob", ".mdl", ".md2", ".md3",
                ".3d", ".ogex"
        };

        public boolean accept(File dir, String name) {
            String filename = dir.getName() + File.separator + name;
            if (new File(filename).isDirectory()) {
                return true;
            }
            for (String extension : extensions) {
                if (name.toLowerCase().endsWith(extension)) {
                    return true;
                }
            }

            return false;
        }
    };
}
