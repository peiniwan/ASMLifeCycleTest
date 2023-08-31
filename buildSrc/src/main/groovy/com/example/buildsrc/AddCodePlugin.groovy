package com.example.buildsrc


import org.gradle.api.Plugin
import org.gradle.api.Project

class AddCodePlugin implements Plugin<Project> {
    void apply(Project project) {
//        project.android.registerTransform(new AddCodeTransform(project))
        project.android.registerTransform(new AddCodeTransform2(project))
//        project.android.registerTransform(new AddCodeTransform3(project))

    }
}


