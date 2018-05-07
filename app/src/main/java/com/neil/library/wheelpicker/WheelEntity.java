package com.neil.library.wheelpicker;

import java.util.ArrayList;

public class WheelEntity<T> {
    private T data;
    private String showStr;//展示的文本
    private ArrayList<WheelEntity<T>> subList=new ArrayList<>();

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getShowStr() {
        return showStr;
    }

    public void setShowStr(String showStr) {
        this.showStr = showStr;
    }

    public ArrayList<WheelEntity<T>> getSubList() {
        return subList;
    }

    public void setSubList(ArrayList<WheelEntity<T>> subList) {
        this.subList = subList;
    }
}
