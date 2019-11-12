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
import org.jdom2.located.LocatedElement
import org.jdom2.located.LocatedJDOMFactory

import java.nio.charset.Charset

class CheckResourcePrefixPlugin implements Plugin<Project> {

    Map<String, Resource> mResourceMap
    Map<String, List<Resource>> mConflictResourceMap

    @Override
    void apply(Project project) {
        mResourceMap = new HashMap<>();
        mConflictResourceMap = new HashMap<>()

        project.extensions.create('checkResourceConfig', CheckResourceConfig)
        CheckResourceConfig checkResourceConfig = project.checkResourceConfig

        boolean isLibrary = project.plugins.hasPlugin("com.android.library")
        def variants = isLibrary ?
            ((LibraryExtension) (project.property("android"))).libraryVariants :
            ((AppExtension) (project.property("android"))).applicationVariants

        def isDebug = false
        def isContainsAssembleTask = false

        //        project.gradle.taskGraph.whenReady {
        //            println("正在打印所有的任务")
        //            it.allTasks.forEach { task ->
        //                def taskName = task.name
        //                if (taskName.contains("assemble") || taskName.contains("resguard") || taskName.contains("bundle")) {
        //                    if (taskName.toLowerCase().endsWith("debug")) {
        //                        isDebug = true
        //                    }
        //                    isContainsAssembleTask = true
        //                    // break foreach
        //                    return true
        //                }
        //            }
        //        }

        project.afterEvaluate {
            variants.forEach { variant ->
                variant as BaseVariantImpl

                def thisTaskName = "checkResource${variant.name.capitalize()}"
                println("thisTaskName = " + thisTaskName)
                def thisTask = project.task(thisTaskName)
                thisTask.group = "check"
                def compileReleaseJavaWithJavac = project.tasks.findByName("compile${variant.name.capitalize()}JavaWithJavac")
                compileReleaseJavaWithJavac.dependsOn(thisTask)

                thisTask.doLast {
                    long startTime = System.currentTimeMillis()
                    println("开始执行 CheckResource 任务：" + thisTaskName)

                    def files = variant.allRawAndroidResources.files

                    files.forEach { file -> traverseResources(file)
                    }
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create()
                    // 打印出所有冲突的资源
                    Iterator<Map.Entry<String, List<Resource>>> iterator = mConflictResourceMap.
                        entrySet().
                        iterator()
                    List<OutputResource> fileResourceList = new ArrayList<>()
                    List<OutputResource> valueResourceList = new ArrayList<>()

                    List<String> whiteUniqueIdList = new ArrayList<>()
                    if (checkResourceConfig.whiteListFile == null ||
                        checkResourceConfig.whiteListFile ==
                        "") {
                        whiteUniqueIdList.clear()
                    } else {
                        whiteUniqueIdList = getWhiteUniqueIdFromFile(checkResourceConfig.whiteListFile)
                    }

                    File resultFileDir
                    // 把 html 复制到 build 文件夹下
                    if (checkResourceConfig.outputDir == null ||
                        checkResourceConfig.outputDir ==
                        "") {
                        resultFileDir = project.buildDir
                    } else {
                        resultFileDir = new File(checkResourceConfig.outputDir)
                    }
                    File resultFile = copyHtmlTemplateToBuildDir(resultFileDir, variant.name)

                    while (iterator.hasNext()) {
                        boolean isValueType
                        Map.Entry<String, List<Resource>> entry = iterator.next()
                        List<Resource> valueList = entry.getValue()

                        OutputResource outputResource = new OutputResource()
                        List<OutputResourceDetail> outputResourceDetailList = new ArrayList<>()
                        String uniqueId = null
                        for (Resource value : valueList) {
                            uniqueId = value.getUniqueId()
                            OutputResourceDetail outputResourceDetail = new OutputResourceDetail()
                            def resource
                            if (value.isValueType()) {
                                isValueType = true
                                resource = (ValueResource) value
                                if (outputResource.getTitle() == null) {
                                    outputResource.setTitle(resource.getResName() + " (数量：" + valueList.size() + ")")
                                }
                            } else {
                                isValueType = false
                                resource = (FileResource) value
                                if (outputResource.getTitle() == null) {
                                    outputResource.setTitle(resource.getFileName() + " (数量：" + valueList.size() + ")")
                                }
                            }
                            String modulePath = resource.belongFilePath()
                            String relatedFileName = modulePath
                            if (modulePath.contains("/")) {
                                relatedFileName =
                                    modulePath.substring(modulePath.lastIndexOf("/") + 1)
                            }
                            if (isValueType) {
                                ValueResource valueResource = (ValueResource) value
                                relatedFileName =
                                    relatedFileName + "(Line: " + valueResource.getLine() + ")"
                            }
                            outputResourceDetail.setTitle(
                                pretifyName(relatedFileName, 50) + "-> " + modulePath)
                            outputResourceDetailList.add(outputResourceDetail)
                        }
                        outputResource.setChildren(outputResourceDetailList)
                        if (!whiteUniqueIdList.contains(uniqueId)) {
                            if (isValueType) {
                                valueResourceList.add(outputResource)
                            } else {
                                fileResourceList.add(outputResource)
                            }
                        }
                    }

                    String template = FileUtils.readFileToString(resultFile,
                        Charset.forName("UTF-8"))
                    template = template.replaceAll("#File_Resouce_Conflicts#",
                        gson.toJson(fileResourceList))
                    template = template.replaceAll("#Value_Resouce_Conflicts#",
                        gson.toJson(valueResourceList))
                    FileUtils.write(resultFile, template, Charset.forName("UTF-8"))

                    long cost = System.currentTimeMillis() - startTime
                    println("资源冲突检查完毕，耗时 " + cost + " ms，请查看输出文件 $resultFile")
                    println("checkResourceConfig.autoPreviewResult 配置为 " + checkResourceConfig.autoPreviewResult)

                    if (checkResourceConfig.autoPreviewResult) {
                        // 调用浏览器打开M页FileUtils
                        UrlUtil.browse("file://$resultFile.path")
                    }
                }
            }
        }
    }

    List<String> getWhiteUniqueIdFromFile(String path) {
        List<String> readLines = null
        File file = new File(path)
        if (file.exists()) {
            readLines = FileUtils.readLines(file, "UTF-8")
        }
        List<String> result = new ArrayList<>()
        if (readLines != null) {
            for (String line : readLines) {
                if (line != null && line.trim() != "" && !line.startsWith("#")) {
                    result.add(line.trim())
                }
            }
        }
        return result
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

    File copyHtmlTemplateToBuildDir(File buildDir, String variantName) {
        File resultHtmlFile = new File(
            buildDir.path + "/" + "outputs" + "/" + "resource_check_result" + "/" + variantName + "_index.html")
        InputStream inputStream = this.getClass().
            getResourceAsStream("/templates/check_resource_conflict_result.html")
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

            if (oldOne != null && !oldOne.compare(resource)) {
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
        saxBuilder.setJDOMFactory(new LocatedJDOMFactory())
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
                    if (item instanceof LocatedElement) {
                        resource.setLine(((LocatedElement) item).getLine())
                    }
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