Android KCExtraImageView
============================
###如何引用此组件：
已经独立成maven项目，mvn install之后可以在项目maven添加以下依赖引用：

```
<dependency>
  <groupId>com.github.destinyd.kcextraimageview</groupId>
  <artifactId>kcextraimageview</artifactId>
  <version>0.1.0</version>
  <type>apklib</type>
</dependency>
```

###需求所需要方法说明
实例方法
```
public void set_drawable(Drawable drawable);
```
请直接使用
```
public void setImageDrawable(Drawable drawable);
```

```
public void setDuration(int Duration){}
```
用于设置动画时间，单位毫秒

```
public void setScaleThresholdToFullscreen(float scaleThresholdToFullscreen);
```
用于设置缩放导致转到全屏显示的阙值

当悬浮模式缩放比例 大于 一般模式缩放比例 * 此阈值 时，动画转至全屏模式
当全屏模式缩放比例 小于 全屏模式最适缩放比例 * 此阈值 时，动画转至一般模式

```
public void setDistanceToDrag(float distanceToDrag);
```
用于设置上提进入悬浮模式的距离（distanceToDrag为像素）， 默认为10.0

###注意事项
由于Android视图有自己的触摸操作的分发模式，所以当在复合视图中，会存在需要全屏操作图片时，触发其他视图的触摸操作。

因此我写了一个全屏拦截触摸事件的视图，以保证更好的体验效果。

但是有点小麻烦就是必须重写Activity的一些事件（如果只是单视图可以不需要）
```
@Override
protected void onResume() {
  super.onResume();
  KCTopestHookLayer topestHookLayer = KCTopestHookLayer.init(this);
  topestHookLayer.addHookView(findViewById(R.id.iv_image));
}

@Override
protected void onPause() {
  KCTopestHookLayer.clear(this);
  super.onPause();
}
```
其中
addHookView(View view);
为添加拦截对象

需要把所有KCExtraImageView对象都addHookView一遍

具体可以参考ExampleImagesActivity
