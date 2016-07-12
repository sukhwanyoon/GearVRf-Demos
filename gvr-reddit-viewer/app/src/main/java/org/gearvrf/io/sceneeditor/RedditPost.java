package org.gearvrf.io.sceneeditor;

public class RedditPost {
    String title;
    String url;

    RedditPost(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "{ title:" + title + " url:" + url + " }";
    }
}
