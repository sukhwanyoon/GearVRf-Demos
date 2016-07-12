package org.gearvrf.io.sceneeditor;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.Gravity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRTransform;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVRRepeatMode;
import org.gearvrf.animation.GVRScaleAnimation;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FrontPageViewer implements StateChangedListener {
    public static final String TAG = FrontPageViewer.class.getSimpleName();
    private static final int NUM_COLUMNS = 4;
    private static final float TEXT_VIEW_WIDTH = 2.0f;
    private static final float TEXT_VIEW_HEIGHT = 2.0f;
    private static final float INITIAL_X = (NUM_COLUMNS / 2) * TEXT_VIEW_WIDTH - TEXT_VIEW_WIDTH
            / 2;
    private static final int NUM_ROWS = 3;
    private static final float INITIAL_Y = ((NUM_ROWS - 1) / 2) * TEXT_VIEW_HEIGHT;
    private static final float VIEW_DEPTH = -5.5f;
    private GVRContext gvrContext;
    private GVRScene gvrScene;
    int index = 0;
    String[] subreddits;
    private CursorManager cursorManager;
    private GVRAnimationEngine animationEngine;
    private GVRTextViewSceneObject title;
    private TextViewViewerTuple[] textViewViewerTuples;
    private Map<GVRTextViewSceneObject, TextViewViewerTuple> tupleMap;
    private LoadSubRedditListener listener;

    interface LoadSubRedditListener {
        void onClickSubReddit(String subreddit);
    }

    FrontPageViewer(GVRContext gvrContext, GVRScene gvrScene, CursorManager cursorManager,
                    LoadSubRedditListener listener) {
        this.gvrContext = gvrContext;
        this.gvrScene = gvrScene;
        subreddits = gvrContext.getContext().getResources().getStringArray(R.array.subreddits);
        textViewViewerTuples = new TextViewViewerTuple[subreddits.length];
        this.cursorManager = cursorManager;
        animationEngine = gvrContext.getAnimationEngine();
        tupleMap = new HashMap<GVRTextViewSceneObject, TextViewViewerTuple>(subreddits.length);
        this.listener = listener;
    }

    public void addTextView(int i, long uniques) {
        textViewViewerTuples[i] = new TextViewViewerTuple();
        textViewViewerTuples[i].textViewSceneObject = new GVRTextViewSceneObject(gvrContext);
        textViewViewerTuples[i].viewers = uniques;
        textViewViewerTuples[i].subreddit = subreddits[i];
        GVRTextViewSceneObject textViewSceneObject = textViewViewerTuples[i].textViewSceneObject;
        textViewSceneObject.setTextColor(Color.WHITE);
        textViewSceneObject.setTextSize(6);
        textViewSceneObject.setBackgroundColor(Color.TRANSPARENT);
        textViewSceneObject.setGravity(Gravity.CENTER);

        SpannableString span1 = new SpannableString(subreddits[i]);
        span1.setSpan(new AbsoluteSizeSpan(30), 0, subreddits[i].length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        String viewers = uniques + " viewers";
        SpannableString span2 = new SpannableString(viewers);
        span2.setSpan(new AbsoluteSizeSpan(20), 0, viewers.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        CharSequence finalText = TextUtils.concat(span1, "\n", span2);
        textViewSceneObject.setText(finalText);
        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager);
        textViewSceneObject.attachComponent(selectableBehavior);
        selectableBehavior.setStateChangedListener(this);
        textViewViewerTuples[i].scaleUpAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (textViewSceneObject.getTransform(),
                0.5f, 1.1f).setRepeatMode(GVRRepeatMode.ONCE);
        textViewViewerTuples[i].scaleDownAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (textViewSceneObject
                .getTransform(), 0.5f, 0.909f).setRepeatMode(GVRRepeatMode.ONCE);
        gvrScene.addSceneObject(textViewSceneObject);
        setPosition(textViewSceneObject.getTransform(), i);
        tupleMap.put(textViewSceneObject, textViewViewerTuples[i]);
    }

    private void setPosition(GVRTransform transform, int index) {
        float y = 1 - (index / NUM_COLUMNS) * 1.2f;
        float x = -3 + (index - (index / NUM_COLUMNS) * NUM_COLUMNS) * 2.2f;
        transform.setPosition(x, y, VIEW_DEPTH);
        if (x <= -3 || x >= 3) {
            int degrees = x < 0 ? 45 : -45;
            int offset = x < 0 ? 1 : -1;
            transform.rotateByAxisWithPivot(degrees, 0, 1, 0, transform.getPositionX() + offset,
                    transform

                    .getPositionY(), transform.getPositionZ());
        }
    }

    public void hideFrontPage() {
        for (TextViewViewerTuple tuple : textViewViewerTuples) {
            gvrScene.removeSceneObject(tuple.textViewSceneObject);
        }
        gvrScene.removeSceneObject(title);
    }

    public void showFrontPage() {
        for (TextViewViewerTuple tuple : textViewViewerTuples) {
            gvrScene.addSceneObject(tuple.textViewSceneObject);
        }
        gvrScene.addSceneObject(title);
    }

    public void createFrontPage() {
        index = 0;

        addTitle();
        FutureCallback callback = new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                JsonArray hour = result.getAsJsonArray("hour");
                Log.d(TAG, hour.toString());
                JsonArray recentHour = hour.get(2).getAsJsonArray();
                long epoch = recentHour.get(0).getAsLong();
                long uniques = recentHour.get(1).getAsLong();
                long views = recentHour.get(2).getAsLong();
                Log.d(TAG, subreddits[index] + " epoch:" + epoch + " uniques:" + uniques + " " +
                        "views" + views);
                addTextView(index, uniques);
                index++;
                if (index < subreddits.length) {
                    Log.d(TAG, "Requesting subreddit now:" + subreddits[index]);
                    Ion.with(gvrContext.getActivity()).load(getTrafficUrl(subreddits[index]))
                            .asJsonObject().setCallback(this);
                } else {
                    fillColor();
                }
            }
        };
        Ion.with(gvrContext.getActivity()).load(getTrafficUrl(subreddits[index])).asJsonObject()
                .setCallback(callback);
    }

    private void addTitle() {
        title = new GVRTextViewSceneObject(gvrContext, 4, 1, "Hottest Subreddits");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setBackgroundColor(Color.TRANSPARENT);
        title.setGravity(Gravity.CENTER);
        title.getTransform().setPosition(0, 2, VIEW_DEPTH);
        title.getTransform().rotateByAxis(25, 1, 0, 0);
        gvrScene.addSceneObject(title);
    }

    private void fillColor() {
        Log.d(TAG, "Filling Color now");
        Arrays.sort(textViewViewerTuples);
        int c1r = 0xFF, c1g = 0x51, c1b = 0x2f;
        int c2r = 0xf0, c2g = 0x98, c2b = 0x19;
        int steps = textViewViewerTuples.length;
        for (int i = 0; i < steps; i++) {
            float ratio = (float) i / (float) steps;
            int red = (int) (c2r * ratio + c1r * (1 - ratio));
            int green = (int) (c2g * ratio + c1g * (1 - ratio));
            int blue = (int) (c2b * ratio + c1b * (1 - ratio));
            textViewViewerTuples[i].textViewSceneObject.setBackgroundColor(Color.argb(255, red,
                    green, blue));
        }
    }

    private String getTrafficUrl(String subreddit) {
        return "https://www.reddit.com" +
                subreddit + "/about/traffic/.json";
    }

    @Override
    public void onStateChanged(SelectableBehavior behavior, ObjectState prev, ObjectState current) {
        if (current == ObjectState.COLLIDING) {
            GVRTextViewSceneObject sceneObject = (GVRTextViewSceneObject) behavior.getOwnerObject();
            TextViewViewerTuple tuple = tupleMap.get(sceneObject);
            animationEngine.start(tuple.scaleUpAnimation);
        }
        if (prev == ObjectState.COLLIDING) {
            GVRTextViewSceneObject sceneObject = (GVRTextViewSceneObject) behavior.getOwnerObject();
            TextViewViewerTuple tuple = tupleMap.get(sceneObject);
            animationEngine.start(tuple.scaleDownAnimation);
        }
        if(prev == ObjectState.CLICKED) {
            hideFrontPage();
            GVRTextViewSceneObject sceneObject = (GVRTextViewSceneObject) behavior.getOwnerObject();
            TextViewViewerTuple tuple = tupleMap.get(sceneObject);
            listener.onClickSubReddit(tuple.subreddit);
            animationEngine.start(tuple.scaleDownAnimation);
        }
    }

    private static class TextViewViewerTuple implements Comparable<TextViewViewerTuple> {
        long viewers;
        GVRTextViewSceneObject textViewSceneObject;
        GVRScaleAnimation scaleUpAnimation, scaleDownAnimation;
        String subreddit;

        @Override
        public int compareTo(TextViewViewerTuple another) {
            return (int) (viewers - another.viewers);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TextViewViewerTuple that = (TextViewViewerTuple) o;

            if (viewers != that.viewers) return false;
            return textViewSceneObject != null ? textViewSceneObject.equals(that
                    .textViewSceneObject) : that.textViewSceneObject == null;
        }

        @Override
        public int hashCode() {
            int result = (int) (viewers ^ (viewers >>> 32));
            result = 31 * result + (textViewSceneObject != null ? textViewSceneObject.hashCode()
                    : 0);
            return result;
        }
    }
}
