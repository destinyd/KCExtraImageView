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
