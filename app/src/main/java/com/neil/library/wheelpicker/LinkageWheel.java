package com.neil.library.wheelpicker;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;
import android.support.annotation.Nullable;

import com.neil.library.wheelpicker.listener.OnItemSelectedListener;

import java.util.ArrayList;
import java.util.HashMap;

public class LinkageWheel<M, T extends WheelEntity<M>> extends LinearLayout {
    private ArrayList<WheelPicker<M, T>> pickers = new ArrayList<>();
    private T mSelectedData;
    private ArrayList<T> mData;

    private HashMap<Integer, T> allSelectedEntity = new HashMap<>();
    private boolean isInit = false;

    private static final String TAG = "LinkageWheel";

    public LinkageWheel(Context context) {
        this(context, null);
    }

    public LinkageWheel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinkageWheel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void setDefaultSelectedPosition(int position) {
        Log.i(TAG, "setDefaultSelectedPosition" + "==================");
        if (!isInit) {
            init();
        }
        int childCount = pickers.size();
        Log.i(TAG, childCount + "==================");
        for (int i = 0; i < childCount; i++) {
            pickers.get(i).setInitSelectItemPosition(position);
        }
    }

    private void init() {
        int childCount = getChildCount();
        if (isInit || childCount == 0) {
            return;
        }
        for (int i = 0; i < childCount; i++) {
            if (getChildAt(i) instanceof WheelPicker) {
                pickers.add((WheelPicker<M, T>) getChildAt(i));
            }
        }
        for (int i = 0; i < pickers.size(); i++) {
            final int index = i;
            if (i == 0 && pickers.get(0).getData() != null && pickers.get(0).getData().size() > 0) {
                mSelectedData = (pickers.get(0).getData().get(0));
            }
            pickers.get(i).setOnItemSelectedListener(new OnItemSelectedListener<T>() {
                @Override
                public void onItemSelected(IWheelPicker picker, T data, int position) {
                    if(index==0){
                        allSelectedEntity.clear();
                    }
                    allSelectedEntity.put(index, data);
                    if (index != pickers.size() - 1) {//不是最后一个
                        WheelPicker nextPicker = pickers.get(index + 1);
                        nextPicker.setData(data.getSubList());
                        if (data.getSubList() == null || data.getSubList().size() == 0) {
                            for (int j = index + 2; j < pickers.size(); j++) {
                                pickers.get(j).setData(new ArrayList());
                            }
                            //Listener
                            if (mOnSelectedLastItemListener != null)
                                mOnSelectedLastItemListener.onSelected(picker, data, position);
                        }
                    } else {
                        if (mOnSelectedLastItemListener != null)
                            mOnSelectedLastItemListener.onSelected(picker, data, position);
                    }
                }
            });
        }
        isInit = true;
    }

    public WheelEntity getSelectedData() {
        return mSelectedData;
    }

    private OnSelectedLastItemListener<T> mOnSelectedLastItemListener;

    public void setOnSelectedLastItemListener(OnSelectedLastItemListener<T> onSelectedLastItemListener) {
        this.mOnSelectedLastItemListener = onSelectedLastItemListener;
    }

    public interface OnSelectedLastItemListener<T> {
        void onSelected(IWheelPicker picker, T selected, int position);
    }

    public ArrayList<T> getData() {
        return mData;
    }

    public HashMap<Integer, T> getAllSelectedEntity() {
        return allSelectedEntity;
    }

    public void setData(ArrayList<T> mData) {
        Log.i(TAG, "setData" + "==================");
        this.mData = mData;
        if (!isInit) {
            init();
        }
        if (pickers.size() > 0) {
            pickers.get(0).setData(mData);
        }
    }
}
