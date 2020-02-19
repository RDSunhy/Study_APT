package com.example.api;

import android.app.Activity;
import android.view.View;

/**
 *  用于在 Activity中 查找 view
 */

public class ViewFinder {

    public View findView(Object obj, int viewId){
        return ( (Activity) obj).findViewById(viewId);
    }
}
