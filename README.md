## Gradle插件练习
分别使用buildSrc和独立module中编写gradle插件

* PublishAppPlugin 多渠道打包
* LifeCyclePlugin 使用ASM在每一个 Activity 打开时输出相应的 log 日志
* TracePlugin 使用ASM在每一个类每个方法前后添加日志
* BuildTimeCostPlugin 每个task消耗时长排序
* AddCodePlugin 使用 javassist 拦截监听和方法，例如第三方SDK之类，全局双击拦截
