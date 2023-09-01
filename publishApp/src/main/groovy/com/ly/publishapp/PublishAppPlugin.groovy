
import org.gradle.api.Plugin
import org.gradle.api.Project

import com.ly.publishapp.*
import PublishAppTask

/**
 * 多渠道打包   运行app-tasks-ly-publishApp这个task就行
 */
class PublishAppPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {//入口
        project.extensions.create("publishAppInfo", PublishAppInfoExtension.class)
        project.tasks.create("publishApp", PublishAppTask.class)

    }
}