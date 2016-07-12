package org.gearvrf.io.sceneeditor;

import android.graphics.Color;
import android.view.Gravity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTransform;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVRRepeatMode;
import org.gearvrf.animation.GVRScaleAnimation;
import org.gearvrf.io.cursor3d.CursorManager;
import org.gearvrf.io.cursor3d.SelectableBehavior;
import org.gearvrf.io.cursor3d.SelectableBehavior.ObjectState;
import org.gearvrf.io.cursor3d.SelectableBehavior.StateChangedListener;
import org.gearvrf.io.sceneeditor.RedditWebView.WebWindowListener;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.utility.Log;

import java.util.ArrayList;
import java.util.List;

public class SubRedditViewer {
    private static final String TAG = SubRedditViewer.class.getSimpleName();
    private static final int NUM_VIEWS_CENTER = 4;
    private static final int NUM_VIEWS_TOP = 4;
    private static final int NUM_VIEWS_BOTTOM = 4;
    private static final float WINDOW_DEPTH = -18f;
    private static final int NUM_COLUMNS = 3;
    private static final int NUM_VIEWS = NUM_COLUMNS * 3;
    private static final String NEXT_BUTTON_MODEL = "arrow.fbx";
    private GVRContext gvrContext;
    private GVRScene mainScene;
    List<RedditWebView> webViewList;
    private String afterArgument;
    private String subreddit;
    private GVRTextViewSceneObject nextButton;
    private CursorManager cursorManager;
    private GVRAnimation nbScaleUpAnimation, nbScaleDownAnimation;
    private GVRAnimationEngine animationEngine;
    private GVRTextViewSceneObject backButton;

    private CloseSubRedditListener closeSubRedditListener;
    private GVRScaleAnimation bbScaleUpAnimation;
    private GVRScaleAnimation bbScaleDownAnimation;

    interface CloseSubRedditListener {
        void onCloseSubreddit();
    }

    SubRedditViewer(GVRContext context, GVRScene scene, CursorManager cursorManager,
                    CloseSubRedditListener listener) {
        gvrContext = context;
        mainScene = scene;
        webViewList = new ArrayList<RedditWebView>(NUM_VIEWS);
        this.cursorManager = cursorManager;
        animationEngine = gvrContext.getAnimationEngine();
        closeSubRedditListener = listener;
    }

    private void setPosition(GVRTransform transform, int index) {
        float y = (9 - (index / NUM_COLUMNS) * RedditWebView.QUAD_Y) * 1.2f;
        float x = ((index - (index / NUM_COLUMNS) * NUM_COLUMNS) - 1) * RedditWebView.QUAD_X * 1.2f;
        transform.setPosition(x, y, WINDOW_DEPTH);
        if (x <= -16 || x >= 16) {
            int degrees = x < 0 ? 45 : -45;
            int offset = x < 0 ? 1 : -1;
            transform.rotateByAxisWithPivot(degrees, 0, 1, 0, transform.getPositionX() +
                            offset * RedditWebView.QUAD_X / 2,
                    transform.getPositionY(), transform.getPositionZ());
        }
    }

    private void createWebView(final String url, final String title, final WebWindowListener
            webWindowListener) {
        gvrContext.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RedditWebView baseWebView = new RedditWebView(SubRedditViewer.this.gvrContext,
                        mainScene, webWindowListener);
                baseWebView.render(url, title);
                webViewList.add(baseWebView);
            }
        });
    }

    private List<RedditPost> getPostsFromJson(JsonObject result) {
        List<RedditPost> posts = new ArrayList<RedditPost>();

        JsonObject data = result.getAsJsonObject("data");
        afterArgument = data.get("after").getAsString();
        Log.d(TAG,"After is" + afterArgument);
        JsonArray jsonPosts = data.getAsJsonArray("children");
        String url, title;
        for (int i = 0; i < jsonPosts.size(); i++) {
            JsonObject jsonPost = jsonPosts.get(i).getAsJsonObject();
            url = jsonPost.get("data").getAsJsonObject().get("url").getAsString();
            title = jsonPost.get("data").getAsJsonObject().get("title").getAsString();
            url = imgurCheck(url);
            RedditPost redditPost = new RedditPost(title, url);
            posts.add(redditPost);
        }
        return posts;
    }

    private static final String IMGUR_ADDRESS = "i.imgur.com";

    private String imgurCheck(String url) {
        int index = url.indexOf(IMGUR_ADDRESS);
        if (index != -1) {
            StringBuffer buffer = new StringBuffer();
            buffer.append(url.substring(0, index));
            buffer.append('m');
            buffer.append(url.substring(index + 1, url.lastIndexOf('.')));
            return buffer.toString();
        }
        return url;
    }

    private void reloadSubReddit(boolean isNewSubReddit) {
        for(RedditWebView webView: webViewList) {
            webView.setLoading(true);
        }
        FutureCallback callback = new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                List<RedditPost> redditPostList = getPostsFromJson(result);
                RedditPost redditPost;
                for (int i = 0; i < NUM_VIEWS && redditPostList.get(i) != null; i++) {
                    redditPost = redditPostList.get(i);
                    webViewList.get(i).reload(redditPost.getUrl(), redditPost.getTitle());
                }
            }
        };
        getNewData(callback, isNewSubReddit);
    }

    private void getNewData(FutureCallback callback, boolean isNewSubreddit) {
        StringBuffer urlBuffer = new StringBuffer();
        urlBuffer.append("https://www.reddit.com").append(subreddit).append("/top/.json?limit=")
                .append(NUM_VIEWS);
        if (afterArgument != null && !isNewSubreddit) {
            urlBuffer.append("&after=").append(afterArgument);
        }
        Log.d(TAG,"Requesting URL:" + urlBuffer.toString());
        Ion.with(gvrContext.getActivity()).load(urlBuffer.toString())
                .asJsonObject()
                .setCallback(callback);
    }

    public void addNextButton() {

        nextButton = new GVRTextViewSceneObject(gvrContext, "Next");
        nextButton.setBackGround(gvrContext.getContext().getResources().getDrawable(R.drawable
                .rounded_corner));
        nextButton.setTextSize(20);
        nextButton.getTransform().setPosition(1, -5f, WINDOW_DEPTH /4);
        nextButton.getTransform().rotateByAxis(-45, 1, 0, 0);
        nextButton.setGravity(Gravity.CENTER);
        nextButton.setTextColor(Color.WHITE);
        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager);
        nextButton.attachComponent(selectableBehavior);
        nbScaleUpAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (nextButton.getTransform(),
                        0.5f, 1.2f).setRepeatMode(GVRRepeatMode.ONCE);
        nbScaleDownAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (nextButton.getTransform(), 0.5f, 0.8f).setRepeatMode(GVRRepeatMode.ONCE);

        selectableBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current) {
                if (current == ObjectState.COLLIDING) {
                    animationEngine.start(nbScaleUpAnimation);
                }
                if (prev == ObjectState.COLLIDING) {
                    animationEngine.start(nbScaleDownAnimation);
                }
                if (prev == ObjectState.CLICKED) {
                    reloadSubReddit(false);
                }
            }
        });
        mainScene.addSceneObject(nextButton);
    }

    public void addBackButton() {
        backButton = new GVRTextViewSceneObject(gvrContext, 1,1,"X");
        backButton.setBackGround(gvrContext.getContext().getResources().getDrawable(R.drawable
                .rounded_corner));
        backButton.setTextSize(20);
        backButton.getTransform().setPosition(-1, -5f, WINDOW_DEPTH / 4);
        backButton.getTransform().rotateByAxis(-45, 1, 0, 0);
        backButton.setGravity(Gravity.CENTER);
        backButton.setTextColor(Color.WHITE);

        SelectableBehavior selectableBehavior = new SelectableBehavior(cursorManager);
        backButton.attachComponent(selectableBehavior);
        bbScaleUpAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (backButton.getTransform(),
                        0.5f, 1.2f).setRepeatMode(GVRRepeatMode.ONCE);
        bbScaleDownAnimation = (GVRScaleAnimation) new GVRScaleAnimation
                (backButton.getTransform(), 0.5f, 0.8f).setRepeatMode(GVRRepeatMode.ONCE);

        selectableBehavior.setStateChangedListener(new StateChangedListener() {
            @Override
            public void onStateChanged(SelectableBehavior behavior, ObjectState prev,
                                       ObjectState current) {
                if (current == ObjectState.COLLIDING) {
                    animationEngine.start(bbScaleUpAnimation);
                }
                if (prev == ObjectState.COLLIDING) {
                    animationEngine.start(bbScaleDownAnimation);
                }
                if (prev == ObjectState.CLICKED) {
                    closeSubReddit();
                }
            }
        });
        mainScene.addSceneObject(backButton);
    }

    private void closeSubReddit() {


        mainScene.removeSceneObject(nextButton);
        mainScene.removeSceneObject(backButton);
        for (RedditWebView webView : webViewList) {
            mainScene.removeSceneObject(webView.getSceneObject());
        }
        closeSubRedditListener.onCloseSubreddit();
    }

    public void showSubReddit(String subreddit) {
        if (this.subreddit == null) {
            createSubRedditView(subreddit);
            return;
        }

        this.subreddit = subreddit;
        reloadSubReddit(true);
        mainScene.addSceneObject(nextButton);
        mainScene.addSceneObject(backButton);
        for (RedditWebView webView : webViewList) {
            mainScene.addSceneObject(webView.getSceneObject());
        }
    }

    private void createSubRedditView(String subreddit) {
        this.subreddit = subreddit;
        addNextButton();
        addBackButton();

        FutureCallback callback = new FutureCallback<JsonObject>() {
            @Override
            public void onCompleted(Exception e, JsonObject result) {
                List<RedditPost> redditPostList = (getPostsFromJson(result));
                for (int i = 0; i < NUM_VIEWS && redditPostList.get(i) != null; i++) {
                    final int index = i;
                    WebWindowListener webWindowListener = new WebWindowListener() {
                        @Override
                        public void onClose() {
                        }

                        @Override
                        public void onCreateWindow(GVRTransform windowTransform) {
                            setPosition(windowTransform, index);
                        }
                    };
                    RedditPost redditPost = redditPostList.get(i);
                    createWebView(redditPost.getUrl(), redditPost.getTitle(), webWindowListener);
                }
            }
        };
        getNewData(callback,true);
    }
}
