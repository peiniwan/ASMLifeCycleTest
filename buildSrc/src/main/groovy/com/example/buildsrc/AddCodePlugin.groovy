package com.example.buildsrc

import com.android.build.api.transform.*
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class AddCodePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.android.registerTransform(new AddCodeTransform(project))
        project.android.registerTransform(new AddCodeTransform2(project))
    }
}


