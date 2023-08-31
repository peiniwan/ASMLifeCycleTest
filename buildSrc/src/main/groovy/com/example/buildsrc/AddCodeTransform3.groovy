package com.example.buildsrc

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 *  Java 字节码操作库 javassist 来操作和修改类的字节码
 * 双击拦截
 */
class AddCodeTransform3 extends Transform {

    private static final def CLICK_LISTENER = "android.view.View\$OnClickListener"

    def pool = ClassPool.default
    def project

    private static final def TAG = "AddCodeTransform3"

    AddCodeTransform3(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return TAG
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        project.android.bootClasspath.each {
            pool.appendClassPath(it.absolutePath)
        }

        transformInvocation.inputs.each {

            it.jarInputs.each {
                pool.insertClassPath(it.file.absolutePath)

                // 重命名输出文件（同目录copyFile会冲突）
                def jarName = it.name
                def md5Name = DigestUtils.md5Hex(it.file.getAbsolutePath())
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def dest = transformInvocation.outputProvider.getContentLocation(
                        jarName + md5Name, it.contentTypes, it.scopes, Format.JAR)
                FileUtils.copyFile(it.file, dest)
            }


            it.directoryInputs.each {
                def preFileName = it.file.absolutePath
                pool.insertClassPath(preFileName)

                findTarget(it.file, preFileName)

                // 获取output目录
                def dest = transformInvocation.outputProvider.getContentLocation(
                        it.name,
                        it.contentTypes,
                        it.scopes,
                        Format.DIRECTORY)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(it.file, dest)
            }
        }
    }

    private void findTarget(File dir, String fileName) {
        if (dir.isDirectory()) {
            dir.listFiles().each {
                findTarget(it, fileName)
            }
        } else {
            modify(dir, fileName)
        }
    }

    private void modify(File dir, String fileName) {
        def filePath = dir.absolutePath

        if (!filePath.endsWith(SdkConstants.DOT_CLASS)) {
            return
        }
        if (filePath.contains('R$') || filePath.contains('R.class')
                || filePath.contains("BuildConfig.class")) {
            return
        }

        //filePath    Users/allin1322/AndroidStudioProjects/ASMLifeCycleTest/app/build/intermediates/transforms/AddCodeTransform/debug/0/com/example/asmlifecycletest/MainActivity.class
        //fileName    /Users/allin1322/AndroidStudioProjects/ASMLifeCycleTest/app/build/intermediates/transforms/AddCodeTransform/debug/0/com/example/asmlifecycletest/MainActivity$1.class
        //className   .com.example.asmlifecycletest.MainActivity$1.class
        //name        com.example.asmlifecycletest.MainActivity$1
        def className = filePath.replace(fileName, "")
                .replace("\\", ".")
                .replace("/", ".")
        def name = className.replace(SdkConstants.DOT_CLASS, "").substring(1)

        CtClass ctClass = pool.get(name)
        CtClass[] interfaces = ctClass.getInterfaces()

        if (interfaces.contains(pool.get(CLICK_LISTENER))) {
            if (name.contains("\$")) {
                CtClass outer = pool.get(name.substring(0, name.indexOf("\$")))

                CtField field = ctClass.getFields().find {
                    return it.type == outer
                }
                if (field != null) {
                    //必须使用全类名，否则编译会找不到类
                    def body = "   if(com.example.asmlifecycletest.FastClickUtil.isFastDoubleClick(\$1.getContext())){\n" +
                            "                    return;\n" +
                            "                }"
                    addCode(ctClass, body, fileName)
                }
            } else {
                def body = ""
                addCode(ctClass, body, fileName)
            }
        }
    }

    private void addCode(CtClass ctClass, String body, String fileName) {
        ctClass.defrost()
        CtMethod method = ctClass.getDeclaredMethod("onClick", pool.get("android.view.View"))
        method.insertBefore(body)

        ctClass.writeFile(fileName)
        ctClass.detach()
    }

}