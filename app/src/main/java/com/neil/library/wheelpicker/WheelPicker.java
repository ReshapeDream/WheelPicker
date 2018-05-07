package com.neil.library.wheelpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import com.neil.library.wheelpicker.listener.OnItemSelectedListener;
import com.neil.library.wheelpicker.listener.OnWheelChangeListener;
import com.neil.library.wheelpicker.listener.PickerState;

import java.util.ArrayList;
import java.util.Arrays;

public class WheelPicker<M, T extends WheelEntity<M>> extends View implements IWheelPicker<M, T>, Runnable {
    private static final String TAG = "WheelPicker";

    public static final int ALIGN_CENTER = 0x0000, ALIGN_LEFT = 0x0001, ALIGN_RIGHT = 0x0010;
    private ArrayList<T> mData;
    private OnWheelChangeListener onWheelChangeListener;
    private OnItemSelectedListener<T> onItemSelectedListener;

    private Paint mPaint;//画笔
    private TextPaint mTextPaint;//文本画笔
    private float lineHeight;//每行的高度
    private Scroller mScroller;//
    private VelocityTracker mTracker;//获取手指的加速度

    private Rect mContentRect;//内容区域
    private Rect mSelectRect;//选中的矩形区域
    private int mWheelCenterX, mWheelCenterY;//滚轮选择器中心坐标
    private int mVisibleItemCount;//可见条目数量
    private int mSelectedPosition = -1;//选中的条目
    private PickerState mPickerState = PickerState.SCROLL_STATE_IDLE;//Picker的状态


    private float textSize;
    private float selectedTextSize;
    private int unSelectedTextColor;
    private int selectedTextColor;
    private int initSelectPosition = -1;//默认可见条目居中
    private int itemTextAlign;//默认 居中
    private boolean isMultipleLine;//是否支持多行显示；
    private int scaledMaximumFlingVelocity;
    private int scaledMinimumFlingVelocity;
    private int mMinFlinY;
    private int mMaxFlinY;

    private float[] itemHeights;//存储每个条目的高度
    private float allItemTotalHeight;//所有条目总高度;

    private Handler mHandler = new Handler();
    private int scaledTouchSlop;
    //当数据改变时是否滚动到上个位置，可能用于时间和日期选择时，需要为true
    private boolean onDataChangeScrollToLastPosition;
    private boolean isDataChange = false;//数据改变


    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WheelPicker(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker);
        textSize = array.getDimension(R.styleable.WheelPicker_textSize, getResources().getDimension(R.dimen.WheelTextSize));
        unSelectedTextColor = array.getColor(R.styleable.WheelPicker_unSelectedTextColor, getResources().getColor(R.color.WheelUnSelectTextColor));
        selectedTextColor = array.getColor(R.styleable.WheelPicker_selectedTextColor, getResources().getColor(R.color.WheelSelectTextColor));
        initSelectPosition = array.getInt(R.styleable.WheelPicker_initSelectPosition, -1);
        itemTextAlign = array.getInt(R.styleable.WheelPicker_itemTextAlign, ALIGN_CENTER);
        isMultipleLine = array.getBoolean(R.styleable.WheelPicker_multipleLine, false);
        onDataChangeScrollToLastPosition = array.getBoolean(R.styleable.WheelPicker_onDataChangeScrollToLastPosition, false);
        array.recycle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.LINEAR_TEXT_FLAG);
        mTextPaint = new TextPaint(mPaint);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(unSelectedTextColor);
        mTextPaint.setStyle(Paint.Style.STROKE);
        fontMetrics = mTextPaint.getFontMetrics();
        lineHeight = fontMetrics.bottom - fontMetrics.top;

        mContentRect = new Rect();
        mSelectRect = new Rect();
        mScroller = new Scroller(context);

        //获取
        ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
        scaledTouchSlop = (int) Math.min(viewConfiguration.getScaledTouchSlop() / 2, lineHeight / 2);
        scaledMaximumFlingVelocity = viewConfiguration.getScaledMaximumFlingVelocity();
        scaledMinimumFlingVelocity = viewConfiguration.getScaledMinimumFlingVelocity();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //设置内容区域
        mContentRect.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());
        mWheelCenterX = mContentRect.centerX();
        mWheelCenterY = mContentRect.centerY();
        computeItemHeights();

        if (initSelectPosition == -1) {
            computeSelectedPosition();
            if (mSelectedPosition != -1) {
                initSelectPosition = mSelectedPosition;
            }
        }
        scrollToPosition(initSelectPosition);
    }

    private Paint.FontMetrics fontMetrics;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制文本
        drawText(canvas);

        //绘制一条中线，测试
        canvas.drawLine(0, mWheelCenterY, mContentRect.width(), mWheelCenterY, mTextPaint);

        //绘制完成后，滚动到初始设置的位置或者
        if (mSelectedPosition == -1 && mData != null && mData.size() > 0 && mPickerState == PickerState.SCROLL_STATE_IDLE) {//
            if (initSelectPosition != -1) {
                mSelectedPosition = initSelectPosition > mData.size() - 1 ? mData.size() - 1 : initSelectPosition;//防止越界
            } else {
                computeSelectedPosition();
            }
            scrollToPosition(mSelectedPosition);
        }
    }


    private void drawText(Canvas canvas) {
        if (mData == null) {
            return;
        }
        int contentWidth = mContentRect.width();
        int contentHeight = mContentRect.height();

        float offsetY = mScrollOffsetY;//文本竖向偏移量 //通过滑动获取到一个值
        float fontBaselineY = 0;//文字的y向定点值,绘制时需要
        float textWidth = 0;//绘制文本的宽度
        mVisibleItemCount = 0;
        int itemLines = 0;//行数
        //绘制文字
        String toDrawStr = "";
        int selectedItemPosition = getSelectedItemPosition();
        //获取未选中的颜色的 r,g,b
        int r = Color.red(unSelectedTextColor);
        int g = Color.green(unSelectedTextColor);
        int b = Color.blue(unSelectedTextColor);
        int a = Color.alpha(unSelectedTextColor);
        for (int i = 0; i < mData.size(); i++) {
            if (mSelectedPosition != -1) {
                if (i == selectedItemPosition) {
                    mTextPaint.setColor(selectedTextColor);
                } else {
                    int alpha = a - 12 * Math.abs(i - selectedItemPosition - 1);
                    mTextPaint.setColor(Color.argb(alpha < 50 ? 50 : alpha, r, g, b));//渐变透明
                }
            }
            itemLines = 0;
            fontBaselineY = offsetY - fontMetrics.top;
            toDrawStr = mData.get(i).getShowStr();
            textWidth = mTextPaint.measureText(toDrawStr);//测量文本的宽度
            if (textWidth > contentWidth && isMultipleLine) {//比较控件宽度与文本的宽度  //绘制多行文本
                int lastIndex = 0;
                String tmpStr = "";
                int numPerLine = 0;
                float tmpStrWidth = 0;
                while (lastIndex + numPerLine < toDrawStr.length()) {
                    tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine);
                    while (mTextPaint.measureText(tmpStr) < contentWidth && lastIndex + numPerLine < toDrawStr.length()) {
                        numPerLine++;
                        tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine);
                    }
                    if (lastIndex + numPerLine < toDrawStr.length()) {
                        tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine - 1);
                        lastIndex = lastIndex + numPerLine - 1;
                    } else {
                        lastIndex = lastIndex + numPerLine;
                    }
                    numPerLine = 0;

                    tmpStrWidth = mTextPaint.measureText(tmpStr);
                    if (itemTextAlign == ALIGN_CENTER) {
                        canvas.drawText(tmpStr, (contentWidth - tmpStrWidth) / 2, fontBaselineY + itemLines * lineHeight, mTextPaint);
                    } else if (itemTextAlign == ALIGN_LEFT) {
                        canvas.drawText(tmpStr, 0, fontBaselineY + itemLines * lineHeight, mTextPaint);
                    } else if (itemTextAlign == ALIGN_RIGHT) {
                        canvas.drawText(tmpStr, contentWidth - tmpStrWidth, fontBaselineY + itemLines * lineHeight, mTextPaint);
                    }
                    itemLines++;
                    offsetY += lineHeight;
                }
            } else {//绘制单行文本
                if (itemTextAlign == ALIGN_CENTER) {
                    canvas.drawText(toDrawStr, (contentWidth - textWidth) / 2, fontBaselineY, mTextPaint);
                } else if (itemTextAlign == ALIGN_LEFT) {
                    canvas.drawText(toDrawStr, 0, fontBaselineY, mTextPaint);
                } else if (itemTextAlign == ALIGN_RIGHT) {
                    canvas.drawText(toDrawStr, contentWidth - textWidth, fontBaselineY, mTextPaint);
                }
                offsetY += lineHeight;
            }
            if (offsetY - mScrollOffsetY < contentHeight) {
                mVisibleItemCount++;
            }
        }
        computeFlingYLimit(offsetY, mScrollOffsetY);
        setVisibleItemCount(mVisibleItemCount);
    }

    /**
     * 计算获取 滚动时 Y轴的最大值与最小值
     *
     * @param offsetY
     * @param mScrollOffsetY
     */
    private void computeFlingYLimit(float offsetY, float mScrollOffsetY) {
        if (mData.size() == 0) return;
        mMinFlinY = (int) (-offsetY + mScrollOffsetY + mContentRect.height() / 2 + itemHeights[mData.size() - 1] / 2 - 1);
        mMaxFlinY = (int) ((mContentRect.height() - itemHeights[0]) / 2 + 1);
    }

    private float mLostPointX = 0;
    private float mLostPointY = 0;
    private float mDownPointX = 0;
    private float mDownPointY = 0;
    private boolean isClick = false;//是否是点击
    private float mScrollOffsetY = 0;
    private boolean isForceStopScroll = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(true);
                if (null == mTracker) {
                    mTracker = VelocityTracker.obtain();
                } else {
                    mTracker.clear();
                }
                mTracker.addMovement(event);
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                    isForceStopScroll = true;
                }
                mDownPointX = mLostPointX = event.getX();
                mDownPointY = mLostPointY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(mDownPointY - event.getY()) < scaledTouchSlop) {
                    isClick = true;
                    Log.i(TAG, "isClick MOVE================================================");
                    break;
                }
                isClick = false;

                mPickerState = PickerState.SCROLL_STATE_DRAGGING;
                mTracker.addMovement(event);
                mScrollOffsetY += (event.getY() - mLostPointY);
                mLostPointY = event.getY();
                computeSelectedPosition();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:

                Log.i(TAG, "isClick up================================================");

                if (null != getParent())
                    getParent().requestDisallowInterceptTouchEvent(false);
                if (isClick){//如果click 可能也产生了滑动，要滚回去
                    int dy = 0;
                    if (mScrollOffsetY > mMaxFlinY) {
                        dy = (int) (mMaxFlinY - mScrollOffsetY);
                    } else if (mScrollOffsetY < mMinFlinY) {
                        dy = (int) (mMinFlinY - mScrollOffsetY);
                    } else {
                        dy = computeDistanceToEndPoint((int) mScrollOffsetY);
                    }
                    mScroller.startScroll(0, (int) mScrollOffsetY, 0, dy, 1);//给定duration，防止闪屏
                    mHandler.post(this);
                    break;
                }
                isForceStopScroll = false;
                mTracker.addMovement(event);
                mTracker.computeCurrentVelocity(1000, scaledMaximumFlingVelocity);
                int yVelocity = (int) mTracker.getYVelocity();
                if (Math.abs(yVelocity) > scaledMinimumFlingVelocity) {
                    mScroller.fling(0, (int) mScrollOffsetY, 0, yVelocity, 0, 0, mMinFlinY, mMaxFlinY);
                    int newY = mScroller.getFinalY() + computeDistanceToEndPoint(mScroller.getFinalY());
                    if (newY > mMaxFlinY) {
                        newY = mMaxFlinY;
                    } else if (newY < mMinFlinY) {
                        newY = mMinFlinY;
                    }
                    mScroller.setFinalY(newY);
                } else {
                    int dy = 0;
                    if (mScrollOffsetY > mMaxFlinY) {
                        dy = (int) (mMaxFlinY - mScrollOffsetY);
                    } else if (mScrollOffsetY < mMinFlinY) {
                        dy = (int) (mMinFlinY - mScrollOffsetY);
                    } else {
                        dy = computeDistanceToEndPoint((int) mScrollOffsetY);
                    }
                    mScroller.startScroll(0, (int) mScrollOffsetY, 0, dy, 1);//给定duration，防止闪屏
                }
                mHandler.post(this);
                break;

            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;

    }


    private int computeDistanceToEndPoint(int toY) {
        if (mData == null || mData.size() == 0) {
            return 0;
        }
        int i1 = mWheelCenterY - toY;//
        float dis = 0;
        int toPosition = 0;
        for (int i = 0; i < mData.size(); i++) {
            dis += itemHeights[i];
            toPosition = i;
            if (dis > i1) {
                break;
            }
        }
        dis -= itemHeights[toPosition] / 2;
        return (int) (i1 - dis);
    }

    @Override
    public void scrollToPosition(int position) {
        if (mData == null || mData.size() == 0) {
            return;
        }
        if (position > mData.size() - 1)
            position = mData.size() - 1;//防止越界
        if (position < 0)
            position = 0;//防止越界

        float dis = 0;
        for (int i = 0; i < position; i++) {
            dis += itemHeights[i];
        }
        dis += (itemHeights[position]) / 2;
        mScroller.startScroll(0, (int) mScrollOffsetY, 0, (int) (mWheelCenterY - dis - mScrollOffsetY), 1);
        mHandler.postDelayed(this, 16);
    }

    @Override
    public int getVisibleItemCount() {
        return mVisibleItemCount;
    }

    @Override
    public void setVisibleItemCount(int count) {
        mVisibleItemCount = count;
    }

    @Override
    public boolean isCyclic() {
        return false;
    }

    @Override
    public void setCyclic(boolean isCyclic) {

    }

    @Override
    public int getSelectedItemPosition() {
        //防止越界
        mSelectedPosition = Math.max(mSelectedPosition, 0);
        mSelectedPosition = Math.min(mSelectedPosition, mData.size() - 1);
        return mSelectedPosition;
    }

    @Override
    public void setInitSelectItemPosition(int position) {
        if (mData == null || mData.size() == 0) {
            return;
        }
        if (position > mData.size() - 1) {
            initSelectPosition = mData.size() - 1;
        }
        if (position < 0) {
            position = 0;
        }
        initSelectPosition = position;
    }

    @Override
    public ArrayList<T> getData() {
        return mData;
    }

    @Override
    public void setData(ArrayList<T> data) {
        mData = data;
        onDataChange();
    }


    @Override
    public void onDataChange() {
        isDataChange = true;
        if (mContentRect.width() == 0) {
            requestLayout();
        }
        if (!onDataChangeScrollToLastPosition) {
            lastSelectPosition = -1;//防止重新设置数据后，页面不更新
        } else {
            lastSelectPosition = Math.min(lastSelectPosition, mData.size() - 1);
        }
        mScrollOffsetY = 0;
        itemHeights = new float[mData.size()];
        computeItemHeights();
        invalidate();
        if (initSelectPosition == -1) {
            computeSelectedPosition();
            if (mSelectedPosition != -1) {
                initSelectPosition = mSelectedPosition;
            }
        }
        if (onDataChangeScrollToLastPosition && lastSelectPosition != -1) {
            scrollToPosition(lastSelectPosition);
        } else {
            scrollToPosition(initSelectPosition);
        }
        Log.i(TAG, "onDataChange  scrollToPosition ==================" + mPickerState);
    }


    /**
     * 计算每个条目的高度
     */
    private void computeItemHeights() {
        if (mData == null || mData.size() == 0) {
            allItemTotalHeight = 0;
            return;
        }
        allItemTotalHeight = 0;
        if (!isMultipleLine) {
            allItemTotalHeight = mData.size() * lineHeight;
            Arrays.fill(itemHeights, lineHeight);
        } else {
            String toDrawStr;
            float textW;
            int contentWidth = mContentRect.width();
            if (contentWidth == 0) {
                return;
            }
            int itemLines = 0;
            for (int i = 0; i < mData.size(); i++) {
                toDrawStr = mData.get(i).getShowStr();
                textW = mTextPaint.measureText(toDrawStr);
                if (textW > contentWidth) {//比控件宽度与文本的宽度  //绘制多行文本
                    itemLines = 0;
                    int lastIndex = 0;
                    String tmpStr = "";
                    int numPerLine = 0;
                    while (lastIndex + numPerLine < toDrawStr.length()) {
                        tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine);
                        while (mTextPaint.measureText(tmpStr) < contentWidth && lastIndex + numPerLine < toDrawStr.length()) {
                            numPerLine++;
                            tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine);
                        }
                        if (lastIndex + numPerLine < toDrawStr.length()) {
                            tmpStr = toDrawStr.substring(lastIndex, lastIndex + numPerLine - 1);
                            lastIndex = lastIndex + numPerLine - 1;
                        } else {
                            lastIndex = lastIndex + numPerLine;
                        }
                        numPerLine = 0;
                        itemLines++;
                        itemHeights[i] = itemLines * lineHeight;
                    }
                    allItemTotalHeight += itemLines * lineHeight;
                } else {
                    allItemTotalHeight += lineHeight;
                    itemHeights[i] = lineHeight;
                }
            }
        }
    }

    @Override
    public int getSelectedTextColor() {
        return selectedTextColor;
    }

    @Override
    public void setSelectedColor(int color) {
        this.selectedTextColor = color;
    }

    @Override
    public float getTextSize() {
        return textSize;
    }

    @Override
    public void setTextSize(float size) {
        this.textSize = size;
        mTextPaint.setTextSize(size);
        fontMetrics = mTextPaint.getFontMetrics();
        lineHeight = fontMetrics.bottom - fontMetrics.top;
        scaledTouchSlop = (int) Math.min(ViewConfiguration.get(getContext()).getScaledTouchSlop() / 2, lineHeight / 2);
    }


    @Override
    public int getUnSelectedTextColor() {
        return unSelectedTextColor;
    }


    @Override
    public void setUnSelectedItemTextColor(int color) {
        this.unSelectedTextColor = color;
    }

    @Override
    public void setOnWheelChangeListener(OnWheelChangeListener listener) {
        this.onWheelChangeListener = listener;
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener<T> listener) {
        this.onItemSelectedListener = listener;
    }

    @Override
    public OnItemSelectedListener getOnItemSelectedListener() {
        return this.onItemSelectedListener;
    }

    @Override
    public Typeface getTypeface() {
        return null;
    }

    @Override
    public void setTypeface(Typeface tf) {

    }

    private int lastSelectPosition = -1;

    @Override
    public void run() {
        if (mData == null || mData.size() == 0) return;
        if (mSelectedPosition == -1) return;
        mScrollOffsetY = mScroller.getCurrY();
        computeSelectedPosition();
        if (mScroller.isFinished() && !isForceStopScroll) {
            mPickerState = PickerState.SCROLL_STATE_IDLE;
            if (onItemSelectedListener != null &&
                    (lastSelectPosition != getSelectedItemPosition() || (onDataChangeScrollToLastPosition && isDataChange))) {
                onItemSelectedListener.onItemSelected(this, mData.get(getSelectedItemPosition()), getSelectedItemPosition());
                isDataChange = false;
            }
            lastSelectPosition = getSelectedItemPosition();
        }

        if (mScroller.computeScrollOffset()) {
            mPickerState = PickerState.SCROLL_STATE_SCROLLING;
            mHandler.postDelayed(this, 16);
        }
        postInvalidate();
    }

    /**
     * 计算选中的条目位置
     */
    private void computeSelectedPosition() {
        if (mData == null) return;
        float itemTotal = 0;
        float dis = mWheelCenterY - mScrollOffsetY;
        for (int i = 0; i < mData.size(); i++) {
            if (Math.abs(itemTotal - dis) < itemHeights[i]) {
                mSelectedPosition = i;
                break;
            }
            itemTotal += itemHeights[i];
        }
    }
}
