import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PublishAppTask extends DefaultTask {

    PublishAppTask() {
        group = "ly"
        dependsOn "build"
    }

    @TaskAction
    void doAction() {
        //打包已完成

        //这里的路径其实是不严谨的·
        def oldApkPath = "${project.getBuildDir()}/outputs/apk/release/app-release.apk"

        //获取参数
        def qihuPath = project.extensions.publishAppInfo.qihuPath
        def keyStorePath = project.extensions.publishAppInfo.keyStorePath
        def keyStorePass = project.extensions.publishAppInfo.keyStorePass
        def keyStoreKeyAlias = project.extensions.publishAppInfo.keyStoreKeyAlias
        def keyStoreKeyAliasPass = project.extensions.publishAppInfo.keyStoreKeyAliasPass
        def apkOutputDir = project.extensions.publishAppInfo.outputPath
        //360加固-登录
        execCmd("java -jar ${qihuPath} -login userName pass")
        //360加固-签名信息配置
        execCmd("java -jar ${qihuPath}  -importsign ${keyStorePath} ${keyStorePass} ${keyStoreKeyAlias} ${keyStoreKeyAliasPass}")
        //360加固-渠道信息配置
        execCmd("java -jar ${qihuPath} -importmulpkg ${project.extensions.publishAppInfo.channelPath}")
        //360加固-开始加固
        execCmd("java -jar ${qihuPath} -jiagu ${oldApkPath} ${apkOutputDir} -autosign  -automulpkg")
        println "加固完成"
    }

    void execCmd(cmd) {
        project.exec {
            executable 'bash'
            args '-c', cmd
        }
    }
}