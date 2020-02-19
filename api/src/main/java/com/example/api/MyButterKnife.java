package com.example.api;

import android.app.Activity;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyButterKnife {

    private static final ViewFinder finder = new ViewFinder();
    private static final Map<String, ViewBinder> binderMap = new LinkedHashMap<>();

    public static void bind(Activity activity){
        bind(activity, activity, finder);
    }

    /**
     * 注解绑定
     * @param host 表示需要注解的类 activity等等
     * @param obj 表示查找 View 的地方 Activity Fragment 等
     * @param finder ui绑定者提供接口
     */
    public static void bind(Object host, Object obj, ViewFinder finder){
        String className = host.getClass().getName();
        ViewBinder binder = binderMap.get(className);
        try {
            if(binder == null){
                Class clazz = Class.forName(className + "$AutoBind");
                binder = (ViewBinder) clazz.newInstance();
                binderMap.put(className, binder);
            }
            if (binder != null) {
                //把finder类跟使用注解类的 类 绑定
                binder.bind(host, obj, finder);
            }
        }catch (Exception e){
            Log.e("MyButterKnife.bind", e.toString());
        }
    }

    public static void unBind(Object host){
        String className = host.getClass().getName();
        ViewBinder binder = binderMap.get(className);
        if(binder != null){
            binder.unBind(host);
        }
        binderMap.remove(className);
    }
}
