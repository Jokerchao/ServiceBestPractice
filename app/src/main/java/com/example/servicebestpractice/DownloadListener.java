package com.example.servicebestpractice;

/**
 * Created by KingChaos on 2017/4/26.
 */

public interface DownloadListener {
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
