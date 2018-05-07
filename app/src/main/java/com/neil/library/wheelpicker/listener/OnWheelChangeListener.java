package com.neil.library.wheelpicker.listener;


/**
 * Created by neil on 2018/5/2.
 */

public interface OnWheelChangeListener {
    /**
     * 当滚轮选择器滚动时回调该方法
     * 滚轮选择器滚动时会将当前滚动位置与滚轮初始位置之间的偏移距离返回，该偏移距离有正负之分，正值表示
     * 滚轮正在往上滚动，负值则表示滚轮正在往下滚动
     * <p>
     * Invoke when WheelPicker scroll stopped
     * WheelPicker will return a distance offset which between current scroll position and
     * initial position, this offset is a positive or a negative, positive means WheelPicker is
     * scrolling from bottom to top, negative means WheelPicker is scrolling from top to bottom
     *
     * @param offset 当前滚轮滚动距离上一次滚轮滚动停止后偏移的距离
     *               <p>
     *               Distance offset which between current scroll position and initial position
     */
    void onWheelScrolled(int offset);

    /**
     * 当滚轮选择器停止后回调该方法
     * 滚轮选择器停止后会回调该方法并将当前选中的数据项在数据列表中的位置返回
     * <p>
     * Invoke when WheelPicker scroll stopped
     * This method will be called when WheelPicker stop and return current selected item data's
     * position in list
     *
     * @param position 当前选中的数据项在数据列表中的位置
     *                 <p>
     *                 Current selected item data's position in list
     */
    void onWheelSelected(int position);

    /**
     * 当滚轮选择器滚动状态改变时回调该方法
     * 滚动选择器的状态总是会在静止、拖动和滑动三者之间切换，当状态改变时回调该方法
     * @param state 滚轮选择器滚动状态，其值仅可能为下列之一
     *              {@link PickerState#SCROLL_STATE_IDLE}
     *              表示滚动选择器处于静止状态
     *              {@link PickerState#SCROLL_STATE_DRAGGING}
     *              表示滚动选择器处于拖动状态
     *              {@link PickerState#SCROLL_STATE_SCROLLING}
     *              表示滚动选择器处于滑动状态
     */
    void onWheelScrollStateChanged(PickerState state);
}
