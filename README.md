# WheelPicker
[![](https://jitpack.io/v/ReshapeDream/WheelPicker.svg)](https://jitpack.io/#ReshapeDream/WheelPicker)

项目中涉及多级联动的选择，而且忽然文本很长，没办法了，自己再造一个轮子！

可以自定义的属性
- textSize 文本大小
- unSelectedTextColor 未选中的文本的颜色 ps有渐变，距离中心越远，颜色越透明
- selectedTextColor  选中的文本的颜色
- initSelectPosition  初始选中的位置，一定要设置，不设置可能有bug,不想处理了，不会越界，如果设置的值大于数据项个数，显示最后一个
- itemTextAlign  显示文本的位置可选{center，left，right}
- multipleLine 是否支持多行，默认false,无论多长都是一行，true 则会把文本进行切割，统一按照itemTextAlign的设置绘制文本
- onDataChangeScrollToLastPosition 适用于多级联动，可能前一个WheelPicker滚动选择了一个其他项目，下一个WheelPicker的数据改变，是否滚动到上一次选中的位置

```
<com.neil.library.wheelpicker.WheelPicker
        android:id="@+id/wp"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:onDataChangeScrollToLastPosition="true"
        app:initSelectPosition="2"
        app:textSize="17sp"
        app:multipleLine="true"
        app:itemTextAlign="center"
        app:unSelectedTextColor="#444"
        app:selectedTextColor="#654321" />
```
### 多级联动 LinkageWheel
- `LinkageWheel`是一个`LinearLayout`,你想几级联动就放几个`WheelPicker`就可以了。
```
    <com.neil.library.wheelpicker.LinkageWheel
        android:orientation="horizontal"
        android:layout_width="match_parent"
        android:layout_height="match_parent">
        <com.neil.library.wheelpicker.WheelPicker
            android:id="@+id/wp"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:onDataChangeScrollToLastPosition="true"
            app:initSelectPosition="2"
            app:textSize="17sp"
            app:multipleLine="true"
            app:itemTextAlign="center"
            app:unSelectedTextColor="#444"
            app:selectedTextColor="#654321" />

        <com.neil.library.wheelpicker.WheelPicker
            android:id="@+id/wp2"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:onDataChangeScrollToLastPosition="true"
            app:initSelectPosition="3"
            app:textSize="17sp"
            app:multipleLine="true"
            app:itemTextAlign="center"
            app:unSelectedTextColor="#444"
            app:selectedTextColor="#123456" />
        .....
    </com.neil.library.wheelpicker.LinkageWheel>
```
#### 给`LinkageWheel`设置数据项
使用`WheelEntity<T>`
```
        ArrayList<WheelEntity<User>> usersLevel1=new ArrayList<>();
        ArrayList<WheelEntity<User>> usersLevel2;
        ArrayList<WheelEntity<User>> usersLevel3;
        WheelEntity<User> entity;
        User user;
        for (int i = 0; i < 20; i++) {
            user=new User();
            user.setName("wo shi name  "+i);
            entity = new WheelEntity<>(user,user.getName());
            entity.setShowStr(user.getName());
            usersLevel1.add(entity);

            //设置该Entity的下级
            usersLevel2=new ArrayList<>();
            entity.setSubList(usersLevel2);

            for (int j = 0; j < 20; j++) {
                user=new User();
                user.setName("wo shi name  "+j);
                entity = new WheelEntity<>(user,user.getName());
                entity.setShowStr(user.getName());
                usersLevel2.add(entity);

                //设置该Entity的下级
                usersLevel3=new ArrayList<>();
                entity.setSubList(usersLevel3);

                for (int k = 0; k < 15; k++) {
                    user=new User();
                    user.setName("wo shi name  "+k);
                    entity = new WheelEntity<>(user,user.getName());
                    entity.setShowStr(user.getName());
                    usersLevel3.add(entity);

                    //todo more level
                }
            }
        }
        linkageWheel.setData(usersLevel1);
```
#### 获取选中的数据
```
        linkageWheel.setOnSelectedLastItemListener(new LinkageWheel.OnSelectedLastItemListener<WheelEntity<User>>() {
            @Override
            public void onSelected(IWheelPicker iWheelPicker, WheelEntity<User> userWheelEntity, int i) {
                //传入的就是最后一级选中的数据，如果最后一级没有可选项，则传入前一级，依次类推
                User selectedUser = userWheelEntity.getData();
                /**
                 *  getAllSelectedEntity() 返回一个HashMap，保存的是选中的各级Entity
                 *  key 是0，1，2，3....
                 */
                HashMap<Integer, WheelEntity<User>> allSelectedEntity = linkageWheel.getAllSelectedEntity();
            }
        });
```

#### 简单效果图
这是一个朴素的自定义View,没有绚丽的动画效果。所以效果简陋！
横屏效果如下图：
<img src="https://raw.githubusercontent.com/ReshapeDream/WheelPicker/master/wheelDemo.png" width="480" height="300" alt="效果图"/>

