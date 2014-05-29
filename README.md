Android KCExtraImageView
============================
###依赖项：
无


###需要权限：
无

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

###注意事项
需求中
实例方法
```
public void set_drawable(Drawable drawable);
```
请直接使用
```
public void setImageDrawable(Drawable drawable);
```
即可

###其他
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
