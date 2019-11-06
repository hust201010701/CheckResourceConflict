package com.orzangleli.checkresourceprefix

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.BaseVariantImpl
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder

class CheckResourcePrefixPlugin implements Plugin<Project> {

    Map<String, Resource> mResourceMap
    Map<String, List<Resource>> mConflictResourceMap;

    @Override
    void apply(Project project) {
        mResourceMap = new HashMap<>();
        mConflictResourceMap = new HashMap<>();

        boolean isLibrary = project.plugins.hasPlugin("com.android.library")
        def variants = isLibrary ? ((LibraryExtension) (project.property("android"))).libraryVariants :
            ((AppExtension) (project.property("android"))).applicationVariants

        def isDebug = false
        def isContainsAssembleTask = false

        project.gradle.taskGraph.whenReady {
            println("正在打印所有的任务")
            it.allTasks.forEach { task ->
                def taskName = task.name
                println(taskName)
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

                    // 打印出所有冲突的资源
                    Iterator<Map.Entry<String, List<Resource>>> iterator = mConflictResourceMap.entrySet().iterator()
                    while(iterator.hasNext()) {
                        Map.Entry<String, List<Resource>> entry = iterator.next()
                        List<Resource> valueList = entry.getValue()
                        StringBuilder stringBuilder = new StringBuilder()
                        stringBuilder.append("--------------------------------- 资源冲突 ---------------------------------").append("\n")
                        for (Resource value : valueList) {
                            stringBuilder.append("---------------------- "  + value.belongFilePath() + " 文件中的 " + value.getUniqueId() + " ----------------------").append("\n")
                        }
                        stringBuilder.append("----------------------------------------------------------------")
                        println(stringBuilder)
                    }
                }
            }
        }
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
        println(uniqueId)
        if (mResourceMap.containsKey(uniqueId)) {
            Resource oldOne = mResourceMap.get(uniqueId)
            if (oldOne != resource) {
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