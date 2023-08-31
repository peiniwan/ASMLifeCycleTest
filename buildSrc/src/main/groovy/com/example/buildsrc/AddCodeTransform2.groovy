import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * 拦截监听和方法
 */
class AddCodeTransform2 extends Transform {

    private static final def CLICK_LISTENER = "android.view.View\$OnClickListener"

    def pool = ClassPool.default
    def project

    private static final def TAG= "AddCodeTransform2"

    AddCodeTransform2(Project project) {
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

                println TAG+"：copy directory: " + it.file.absolutePath
                println TAG+"：dest directory: " + dest.absolutePath
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
                || filePath.contains("BuildConfig.class") || !filePath.contains("MainActivity")) {
//            println TAG+"：filePath return：" + filePath
            return
        }

        //filePath    Users/allin1322/AndroidStudioProjects/ASMLifeCycleTest/app/build/intermediates/transforms/AddCodeTransform/debug/0/com/example/asmlifecycletest/MainActivity.class
        //fileName    /Users/allin1322/AndroidStudioProjects/ASMLifeCycleTest/app/build/intermediates/transforms/AddCodeTransform/debug/0/com/example/asmlifecycletest/MainActivity$1.class
        //className   .com.example.asmlifecycletest.MainActivity$1.class
        //name        com.example.asmlifecycletest.MainActivity$1
        def className = filePath.replace(fileName, "")
                .replace("\\", ".")
                .replace("/", ".")
        def name = className.replace(SdkConstants.DOT_CLASS, "")
                .substring(1)

        CtClass ctClass = pool.get(name)
        CtClass[] interfaces = ctClass.getInterfaces()

        if (interfaces.contains(pool.get(CLICK_LISTENER))) {
            //com.example.asmlifecycletest.MainActivity$1
            // 主类名$内部类名.class（如果匿名内部类，这内部类名为数字) 有接口并且有匿名内部类
            if (name.contains("\$")) {
                println TAG+"：class is inner class：" + ctClass.name
//                println TAG+"：CtClass: " + ctClass
                CtClass outer = pool.get(name.substring(0, name.indexOf("\$")))

                CtField field = ctClass.getFields().find {
                    return it.type == outer
                }
                if (field != null) {  //找到 this ,this不等于null
                    println "fieldStr: " + field.name //this
                    def body = "android.widget.Toast.makeText(" + field.name + "," +
                            "\"javassist\", android.widget.Toast.LENGTH_SHORT).show();"
                    addCode(ctClass, body, fileName)
                }
            } else { //外部的类
                println TAG+"：class is outer class: " + ctClass.name
                def body = "android.widget.Toast.makeText(\$1.getContext(), \"javassist\", android.widget.Toast.LENGTH_SHORT).show();"
                addCode(ctClass, body, fileName)
            }
        } else {
            if (name.contains("TestAdd")) {
                //判断是否需要解冻
                if (ctClass.isFrozen()) {
                    ctClass.defrost()
                }
                def method = ctClass.getDeclaredMethod("release")
                //必须使用全类名，否则编译会找不到类
                def body = '''
               int size = com.example.asmlifecycletest.CustomManager.getVideoManagerSize();
             if (size > 1) {
                android.util.Log.e(\"gh_tag\",\"拦截成功\");
                return;
              }
             '''
                method.insertBefore(body)

                ctClass.writeFile(fileName)
                ctClass.detach()
            }
        }
    }

    private void addCode(CtClass ctClass, String body, String fileName) {

        ctClass.defrost()
        CtMethod method = ctClass.getDeclaredMethod("onClick", pool.get("android.view.View"))
        method.insertAfter(body)

        ctClass.writeFile(fileName)
        ctClass.detach()
        println TAG+"：modify method: " + method.name + " succeed"
    }

}