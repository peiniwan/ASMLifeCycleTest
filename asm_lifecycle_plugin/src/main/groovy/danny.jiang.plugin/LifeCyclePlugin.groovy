package danny.jiang.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 不用buildsrc写插件
 * 插件使用前先 uploadArchives
 * 记录每一个页面的打开和关闭事件
 */
public class LifeCyclePlugin implements Plugin<Project> {
    void apply(Project project) {
        System.out.println("==LifeCyclePlugin gradle plugin==")

        def android = project.extensions.getByType(AppExtension)
        println '----------- registering AutoTrackTransform  -----------'
        LifeCycleTransform transform = new LifeCycleTransform()
        android.registerTransform(transform)
    }
}