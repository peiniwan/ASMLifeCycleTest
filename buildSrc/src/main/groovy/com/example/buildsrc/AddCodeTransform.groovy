import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.api.transform.TransformInvocation
import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import org.gradle.api.Project
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.codec.digest.DigestUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import MyInject


/**
 * 修改第三方开源库中一个类的方法，具体是需要在这个方法之前加入一段我们自己的代码来根据需求实现拦截，
 * 我的做法是：在项目中写一个静态方法，然后在这个方法之前调用我们的静态方法，这样就可以实现拦截功能了
 * 没成功
 */
class AddCodeTransform extends Transform {

    private ClassPool classPool = ClassPool.getDefault()
    Project project

    AddCodeTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "AddCodeTransform"
    }
    /**
     * 需要处理的数据类型，目前 ContentType有六种枚举类型，通常我们使用比较频繁的有前两种：
     * 1、CONTENT_CLASS：表示需要处理 java 的 class 文件。
     * 2、CONTENT_JARS：表示需要处理 java 的 class 与 资源文件。
     * 3、CONTENT_RESOURCES：表示需要处理 java 的资源文件。
     * 4、CONTENT_NATIVE_LIBS：表示需要处理 native 库的代码。
     * 5、CONTENT_DEX：表示需要处理 DEX 文件。
     * 6、CONTENT_DEX_WITH_RESOURCES：表示需要处理 DEX 与 java 的资源文件。
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * Transform 要操作的内容范围，目前 Scope 有五种基本类型：
     * 1、PROJECT：只有项目内容
     * 2、SUB_PROJECTS：只有子项目
     * 3、EXTERNAL_LIBRARIES：只有外部库
     * 4、TESTED_CODE：由当前变体（包括依赖项）所测试的代码
     * 5、PROVIDED_ONLY：只提供本地或远程依赖项
     * SCOPE_FULL_PROJECT 是一个Scope集合，包含Scope.PROJECT,Scope.SUB_PROJECTS,Scope.EXTERNAL_LIBRARIES 这三项，即当前Transform的作用域包括当前项目、子项目以及外部的依赖库
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        //通常我们使用 SCOPE_FULL_PROJECT
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 是否需要增量编译
     */
    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        println "build finished, now println all task execution time:"


        //添加android.jar目录
        classPool.appendClassPath(project.android.bootClasspath[0].toString())
        def outputProvider = transformInvocation.outputProvider
        // 删除之前的输出
        if (outputProvider != null) {
            outputProvider.deleteAll()
        }
        //对类型为“文件夹”的input进行遍历
        transformInvocation.inputs.each { input ->
            input.directoryInputs.each { dirInput ->
                handleDirectory(dirInput.file)
                MyInject.injectDir(dirInput.file.absolutePath, "com/example/asmlifecycletest")

                def dest = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                // 将input的目录复制到output指定目录
                FileUtils.copyDirectory(dirInput.file, dest)
            }

        }
        //对类型为“文件夹”的input进行遍历
        transformInvocation.inputs.each { input ->
            input.jarInputs.each { jarInput ->
                if (jarInput.file.exists()) {
                    def srcFile = handleJar(jarInput.file)

                    //必须给jar重新命名，否则会冲突
                    def jarName = jarInput.name
                    def md5 = DigestUtils.md5Hex(jarInput.file.absolutePath)
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4)
                    }
                    def dest = outputProvider.getContentLocation(md5 + jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(srcFile, dest)
                }
            }
        }

    }

    void handleDirectory(File dir) {
        //将类路径添加到classPool中
        classPool.insertClassPath(dir.absolutePath)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { file ->
                def filePath = file.absolutePath
                classPool.insertClassPath(filePath)
                if (shouldModify(filePath)) {
                    def inputStream = new FileInputStream(file)
                    CtClass ctClass = modifyClass(inputStream)
                    ctClass.writeFile()
                    //调用detach方法释放内存
                    ctClass.detach()
                }
            }
        }
    }

    /**
     * 主要步骤：
     * 1.遍历所有jar文件
     * 2.解压jar然后遍历所有的class
     * 3.读取class的输入流并使用javassit修改，然后保存到新的jar文件中
     */
    File handleJar(File jarFile) {
        classPool.appendClassPath(jarFile.absolutePath)
        def inputJarFile = new JarFile(jarFile)
        def entries = inputJarFile.entries()
        //创建一个新的文件
        def outputJarFile = new File(jarFile.parentFile, "temp_" + jarFile.name)
        if (outputJarFile.exists()) outputJarFile.delete()
        def jarOutputStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputJarFile)))
        while (entries.hasMoreElements()) {
            def jarInputEntry = entries.nextElement()
            def jarInputEntryName = jarInputEntry.name

            def outputJarEntry = new JarEntry(jarInputEntryName)
            jarOutputStream.putNextEntry(outputJarEntry)

            def inputStream = inputJarFile.getInputStream(jarInputEntry)
            if (!shouldModify(jarInputEntryName)) {
                jarOutputStream.write(IOUtils.toByteArray(inputStream))
                inputStream.close()
                continue
            }

            def ctClass = modifyClass(inputStream)
            def byteCode = ctClass.toBytecode()
            ctClass.detach()
            inputStream.close()
            jarOutputStream.write(byteCode)
            jarOutputStream.flush()
        }
        inputJarFile.close()
        jarOutputStream.closeEntry()
        jarOutputStream.flush()
        jarOutputStream.close()
        return outputJarFile
    }

    static boolean shouldModify(String filePath) {
        return filePath.endsWith(".class") &&
                !filePath.contains("R.class") &&
                !filePath.contains('$') &&
                !filePath.contains('R$') &&
                !filePath.contains("BuildConfig.class") &&
                filePath.contains("TestAdd")
    }


    /**
     * 新生产了一个类，不知为何没成功
     * 只供参考
     */
    CtClass modifyClass(InputStream is) {
        def classFile = new ClassFile(new DataInputStream(new BufferedInputStream(is)))
        def ctClass = classPool.get(classFile.name)
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
        return ctClass

    }
}
