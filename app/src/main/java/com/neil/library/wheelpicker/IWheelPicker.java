package com.neil.library.wheelpicker;

import android.graphics.Typeface;

import com.neil.library.wheelpicker.listener.OnItemSelectedListener;
import com.neil.library.wheelpicker.listener.OnWheelChangeListener;

import java.util.ArrayList;

public interface  IWheelPicker<M,T extends WheelEntity<M>>  {
    /**
     * 滚动到指定位置
     * @param position
     */
    void scrollToPosition(int position);
    /**
     * 滚轮选择器可见数据项的数量
     *
     * @return
     */
    int getVisibleItemCount();

    /**
     * 设置滚轮选择器可见数据项数量
     */
    void setVisibleItemCount(int count);

    /**
     * 是否为循环状态
     *
     * @return
     */
    boolean isCyclic();

    /**
     * 设置是否为循环状态
     *
     * @param isCyclic
     */
    void setCyclic(boolean isCyclic);


    /**
     * 获取当前选中的Item位置
     *
     * @return
     */
    int getSelectedItemPosition();

    /**
     * 设置初始化是选中的位置
     *
     * @param position
     */
    void setInitSelectItemPosition(int position);

    /**
     * @return 获取数据列表
     */
    ArrayList<T> getData();

    /**
     * 设置数据列表
     *
     * @param data
     */
    void setData(ArrayList<T> data);

    /**
     * 当数据改变时，#setData时调用
     */
    void onDataChange();

    /**
     * 获取选中的条目文本的颜色
     *
     * @return
     */
    int getSelectedTextColor();

    /**
     * 设置选中的文本的颜色
     *
     * @param color
     */
    void setSelectedColor(int color);

    /**
     * 获取文本的大小
     *
     * @return
     */
    float getTextSize();

    /**
     * 设置文本的大小
     *
     * @return
     */
    void setTextSize(float size);

    /**
     * 获取未选中的文本的颜色
     *
     * @return
     */
    int getUnSelectedTextColor();
    /**
     * 设置未选中的条目文本的颜色
     *
     * @param color
     */
    void setUnSelectedItemTextColor(int color);

    /**
     * 设置滚动监听
     *
     * @param listener
     */
    void setOnWheelChangeListener(OnWheelChangeListener listener);

    /**
     * 设置选中监听
     *
     * @param listener
     */
    void setOnItemSelectedListener(OnItemSelectedListener<T> listener);

    /**
     * 获取监听
     * @return
     */
    OnItemSelectedListener getOnItemSelectedListener();

    /**
     * 获取数据项文本字体对象
     * @return
     */
    Typeface getTypeface();

    /**
     * 设置数据项文本字体对象
     * @param tf
     */
    void setTypeface(Typeface tf);
}
