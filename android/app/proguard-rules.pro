# 文案 JSON 以 Java 资源形式打包在 :core 里，按绝对路径读取，无需额外规则。
# kotlinx.serialization 自带 R8 规则；这里额外保留数据模型的序列化器，防止被裁剪。
-keepclassmembers @kotlinx.serialization.Serializable class com.tianaofeng.xingxia.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tianaofeng.xingxia.**$$serializer { *; }

# 可点的小组件（心情、抛硬币）：Glance 按类名反射创建 ActionCallback，保留无参构造
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
