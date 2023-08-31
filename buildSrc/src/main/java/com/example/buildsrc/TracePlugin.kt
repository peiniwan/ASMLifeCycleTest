package com.example.buildsrc

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Trace beginSection and endSection
 * 获取方法运行时间，分析Trace文件
 * https://app.yinxiang.com/fx/386dceae-99ee-4732-9ac5-d40b2927b1d2
 */
class TracePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions
                .getByType(AppExtension::class.java)
                .registerTransform(TraceTransform())
    }
}