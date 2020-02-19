package com.example.api;

/**
 *  抽象功能
 *
 *  绑定 和 解绑
 */
public interface ViewBinder<T> {

    public void bind(T host, Object obj, ViewFinder finder );

    public void unBind(T host);
}
