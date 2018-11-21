package com.chajian.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES

class MyPlugin extends Transform implements Plugin<Project> {

    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension)
        android.registerTransform(this)

        /**
         * 对外注册api:
         android.registerTransform(new XTransform());
         android.registerTransform(new XTransform(), dependencies)
         内部注册api
         TransformManager.addTransform();
         */
    }

//    Transform中的核心方法，
//    inputs中是传过来的输入流，其中有两种格式，一种是jar包格式一种是目录格式。
//    outputProvider 获取到输出目录，最后将修改的文件复制到输出目录，这一步必须做不然编译会报错

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider,
                   boolean isIncremental) throws IOException, TransformException, InterruptedException {
        println '//===============TracePlugin visit start===============//'
        //删除之前的输出
        if (outputProvider != null)
            outputProvider.deleteAll()
        //遍历input
        inputs.each { TransformInput input ->
            // 遍历文件夹
            input.directoryInputs.each {
                DirectoryInput directoryInput ->
//                    File dir = directoryInput.file
                    if (directoryInput.file.isDirectory()) {
                        //遍历文件夹
                        directoryInput.file.eachFileRecurse {
                            File file ->
                                // ...对目录进行插入字节码
                                def name = file.name
                                if (name.endsWith(".class") && !name.startsWith("R\$") &&
                                        !"R.class".equals(name) && !"BuildConfig.class".equals(name)) {
                                    println(name + "    is chang......")
                                    ClassReader classReader = new ClassReader(file.bytes)
                                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
                                    def className = name.split(".class")[0]
                                    ClassVisitor cv = new TraceVisitor(className, classWriter)
                                    classReader.accept(cv, EXPAND_FRAMES)
                                    byte[] code = classWriter.toByteArray()
                                    FileOutputStream fos = new FileOutputStream(
                                            file.parentFile.absolutePath + File.separator + name)
                                    fos.write(code)
                                    fos.close()
                                }
                        }
                    }
                    // 获取output目录 处理完输入文件之后，要把输出给下一个任务
                    def dest = outputProvider.getContentLocation(directoryInput.name,
                            directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                    // 将input的目录复制到output指定目录
                    FileUtils.copyDirectory(directoryInput.file, dest)
            }
            ////遍历jar文件 对jar不操作，但是要输出到out路径
            input.jarInputs.each {
                JarInput jarInput ->
                    if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
                        // ...对jar进行插入字节码
                    }

                    // 重命名输出文件（同目录copyFile会冲突）
                    def jarName = jarInput.name
                    println("jar = " + jarInput.file.getAbsolutePath())
                    def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4)
                    }
                    def dest = outputProvider.getContentLocation(jarName + md5Name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
            }
        }

    }

/**
 * Returns the unique name of the transform.
 *
 * <p>This is associated with the type of work that the transform does. It does not have to be
 * unique per variant.
 */

    @Override
    String getName() {
        return this.getClass().simpleName
    }

//转换过程中需要资源流的范围,在转换过程中不会被消耗,转换结束后, 会将资源流放回资源池去
    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return super.getReferencedScopes()
    }

    //转换输出类型,默认是getInputTypes()
    //需要处理的数据类型，有两种枚举类型
    //CLASSES和RESOURCES，CLASSES代表处理的java的class文件，RESOURCES代表要处理java的资源
    @Override
    Set<QualifiedContent.ContentType> getOutputTypes() {
        return TransformManager.CONTENT_CLASS
    }
    /**
     * 定义了你要处理的类型
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * Returns the scope(s) of the Transform. This indicates which scopes the transform consumes.
     * Transform的作用域，主要是三大类：SCOPE_FULL_PROJECT SCOPE_FULL_WITH_IR_FOR_DEXING  SCOPE_FULL_LIBRARY_WITH_LOCAL_JARS
     * //    EXTERNAL_LIBRARIES        只有外部库
     * //    PROJECT                       只有项目内容
     * //    PROJECT_LOCAL_DEPS            只有项目的本地依赖(本地jar)
     * //    PROVIDED_ONLY                 只提供本地或远程依赖项
     * //    SUB_PROJECTS              只有子项目。
     * //    SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * //    TESTED_CODE                   由当前变量(包括依赖项)测试的代码

     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * Returns whether the Transform can perform incremental work.
     *
     * <p>If it does, then the TransformInput may contain a list of changed/removed/added files, unless
     * something else triggers a non incremental run.
     * 指明当前Transform是否支持增量编译
     */
    @Override
    boolean isIncremental() {
        return false
    }
}