package com.example.buildsrc

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 打印类里的每个方法
 */
class TracePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions
                .getByType(AppExtension::class.java)
                .registerTransform(TraceTransform())
    }
}