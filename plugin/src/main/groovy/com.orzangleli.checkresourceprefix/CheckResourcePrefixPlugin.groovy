package com.orzangleli.checkresourceprefix

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.orzangleli.checkresourceprefix.output.OutputResource
import com.orzangleli.checkresourceprefix.output.OutputResourceDetail
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder

import java.nio.charset.Charset

class CheckResourcePrefixPlugin implements Plugin<Project> {

    Map<String, Resource> mResourceMap
    Map<String, List<Resource>> mConflictResourceMap;

    @Override
    void apply(Project project) {
        mResourceMap = new HashMap<>();
        mConflictResourceMap = new HashMap<>()

        boolean isLibrary = project.plugins.hasPlugin("com.android.library")
        def variants = isLibrary ? ((LibraryExtension) (project.property("android"))).libraryVariants :
            ((AppExtension) (project.property("android"))).applicationVariants

        def isDebug = false
        def isContainsAssembleTask = false

        project.gradle.taskGraph.whenReady {
            println("正在打印所有的任务")
            it.allTasks.forEach { task ->
                def taskName = task.name
                if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains("bundle")) {
                    if (taskName.toLowerCase().endsWith("debug")) {
                        isDebug = true
                    }
                    isContainsAssembleTask = true
                    // break foreach
                    return true
                }
            }
        }


        project.afterEvaluate {

            variants.forEach { variant ->
                variant as BaseVariantImpl

                def mergeResourcesTask = variant.mergeResourcesProvider.get()
                def thisTaskName = "aaa${variant.name.capitalize()}"
                println("thisTaskName = " + thisTaskName)
                def thisTask = project.task(thisTaskName)
                thisTask.group = "check"
                thisTask.doLast {
                    println("开始执行这个任务：" + thisTaskName)
                    //        if (isDebug) {
                    //            println()
                    //            println("This project is building in debug mode, so we won't check resources prefix.")
                    //            println()
                    //            return true
                    //        }
//
//                    if (!isContainsAssembleTask) {
//                        println()
//                        println("The building doesn't contain assemble task, so we cancel resource prefix.")
//                        println()
//                        return true
//                    }

                    def files = variant.allRawAndroidResources.files

                    files.forEach { file ->
                        traverseResources(file)
                    }
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create()
                    // 打印出所有冲突的资源
                    Iterator<Map.Entry<String, List<Resource>>> iterator = mConflictResourceMap.entrySet().iterator()
                    List<OutputResource> fileResourceList = new ArrayList<>()
                    List<OutputResource> valueResourceList = new ArrayList<>()

                    // 把 html 复制到 build 文件夹下
                    File resultFile = copyHtmlTemplateToBuildDir(project.buildDir)

                    while(iterator.hasNext()) {
                        boolean isValueType
                        Map.Entry<String, List<Resource>> entry = iterator.next()
                        List<Resource> valueList = entry.getValue()

                        OutputResource outputResource = new OutputResource()
                        List<OutputResourceDetail> outputResourceDetailList = new ArrayList<>()

                        for (Resource value : valueList) {
                            OutputResourceDetail outputResourceDetail = new OutputResourceDetail()
                            def resource
                            if (value.isValueType()) {
                                isValueType = true
                                resource = (ValueResource) value
                                if (outputResource.getTitle() == null) {
                                    outputResource.setTitle(resource.getResName())
                                }
                            } else {
                                isValueType = false
                                resource = (FileResource) value
                                if (outputResource.getTitle() == null) {
                                    outputResource.setTitle(resource.getFileName())
                                }
                            }
                            String modulePath = resource.belongFilePath()
                            String relatedFileName = modulePath
                            if (modulePath.contains("/")) {
                                relatedFileName = modulePath.substring(modulePath.lastIndexOf("/") + 1)
                            }
                            outputResourceDetail.setTitle(pretifyName(relatedFileName, 50) + "-> " + modulePath)
                            outputResourceDetailList.add(outputResourceDetail)
                        }
                        outputResource.setChildren(outputResourceDetailList)
                        if (isValueType) {
                            valueResourceList.add(outputResource)
                        } else {
                            fileResourceList.add(outputResource)
                        }
                    }

                    String template = FileUtils.readFileToString(resultFile, Charset.forName("UTF-8"))
                    template = template.replaceAll("#File_Resouce_Conflicts#", gson.toJson(fileResourceList))
                    template = template.replaceAll("#Value_Resouce_Conflicts#", gson.toJson(valueResourceList))
                    FileUtils.write(resultFile, template, Charset.forName("UTF-8"))
                    println("资源冲突检查完毕，请查看输出文件 $resultFile")

                    // 调用浏览器打开M页FileUtils
                    UrlUtil.browse("file://$resultFile.path")
                }
            }
        }
    }

    String pretifyName(String content, int targetSize) {
        int size = content.size()
        if (size < targetSize) {
            content += " "
            for (int i = 0; i < targetSize - size; i++) {
                content += "-"
            }
        }
        return content
    }

    File copyHtmlTemplateToBuildDir(File buildDir) {
        File resultHtmlFile  = new File(buildDir.path + "/" + "outputs" + "/" + "resource_check_result" + "/" + "index.html")
        InputStream inputStream = this.getClass().getResourceAsStream("/templates/check_resource_conflict_result.html")
        FileUtils.copyInputStreamToFile(inputStream, resultHtmlFile)
        return resultHtmlFile
    }

    void traverseResources(File file) {
        if (file.isDirectory()) {
            for (File f in file.listFiles()) {
                traverseResources(f)
            }
        } else {
            //判断是值类型资源还是文件资源
            boolean isValueType = isValueResource(file)
            if (isValueType) {
                findAndRecordValueResource(file)
            } else {
                findAndRecordFileResource(file)
            }
        }
    }

    void recordResource(Resource resource) {
        // 如果包含了，那么把相同资源方法一个Map中
        def uniqueId = resource.getUniqueId()
        if (mResourceMap.containsKey(uniqueId)) {
            Resource oldOne = mResourceMap.get(uniqueId)
            if (resource != oldOne) {
                List<Resource> resources = mConflictResourceMap.get(uniqueId)
                if (resources == null) {
                    resources = new ArrayList<Resource>()
                    resources.add(oldOne)
                }
                resources.add(resource)
                mConflictResourceMap.put(uniqueId, resources)
            }
        }
        mResourceMap.put(uniqueId, resource)
    }

    Resource findAndRecordValueResource(File file) {
        String lastDirectory = file.parentFile.name
        String filePath = file.path
        // 构造器
        SAXBuilder saxBuilder = new SAXBuilder()
        // 获取文档
        Document document = saxBuilder.build(file)
        // 得到根元素: resources
        Element element = document.getRootElement()
        if (element != null) {
            List<Element> children = element.getChildren()
            for (Element item : children) {
                if (isInWhileList(item)) {
                    String resName = item.getAttributeValue("name")
                    String resValue = item.getValue()

                    ValueResource resource = new ValueResource()
                    resource.setResName(resName)
                    resource.setResValue(resValue)
                    resource.setLastDirectory(lastDirectory)
                    resource.setFilePath(filePath)
                    recordResource(resource)
                }
            }
        }

    }

    boolean isInWhileList(Element element) {
        if (element.name == "color" || element.name == "string") {
            return true
        }
        return false
    }



    void findAndRecordFileResource(File file) {
        FileResource resource = new FileResource()
        resource.setPath(file.path)
        resource.setLastDirectory(file.parentFile.name)
        resource.setFileName(file.name)
        resource.setMd5(MD5Util.getMD5(file))
        recordResource(resource)
    }

    // 是否是值类型的资源
    boolean isValueResource(File file) {
        if (file.parentFile.name == "values" || file.parentFile.name.startsWith("values-")) {
            return true
        }
        return false
    }
}