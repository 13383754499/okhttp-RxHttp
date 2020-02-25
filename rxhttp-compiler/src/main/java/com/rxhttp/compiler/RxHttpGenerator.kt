package com.rxhttp.compiler

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import java.io.IOException
import java.util.*
import javax.annotation.processing.Filer
import javax.lang.model.element.VariableElement
import javax.lang.model.util.Elements
import kotlin.Boolean
import kotlin.Long
import kotlin.String

class RxHttpGenerator {
    private lateinit var mParamsAnnotatedClass: ParamsAnnotatedClass
    private lateinit var mParserAnnotatedClass: ParserAnnotatedClass
    private lateinit var mDomainAnnotatedClass: DomainAnnotatedClass
    private lateinit var mConverterAnnotatedClass: ConverterAnnotatedClass
    private var defaultDomain: VariableElement? = null
    fun setAnnotatedClass(annotatedClass: ParamsAnnotatedClass) {
        mParamsAnnotatedClass = annotatedClass
    }

    fun setAnnotatedClass(annotatedClass: ConverterAnnotatedClass) {
        mConverterAnnotatedClass = annotatedClass
    }

    fun setAnnotatedClass(annotatedClass: DomainAnnotatedClass) {
        mDomainAnnotatedClass = annotatedClass
    }

    fun setAnnotatedClass(annotatedClass: ParserAnnotatedClass) {
        mParserAnnotatedClass = annotatedClass
    }

    fun setAnnotatedClass(defaultDomain: VariableElement) {
        this.defaultDomain = defaultDomain
    }

    @Throws(IOException::class)
    fun generateCode(elementUtils: Elements?, filer: Filer?, platform: String) {
        val httpSenderName = ClassName("rxhttp", "HttpSender")
        val rxHttpPluginsName = ClassName("rxhttp", "RxHttpPlugins")
        val okHttpClientName = ClassName("okhttp3", "OkHttpClient")
        val schedulerName = ClassName("io.reactivex", "Scheduler")
        val converterName = ClassName("rxhttp.wrapper.callback", "IConverter")
        val schedulersName = ClassName("io.reactivex.schedulers", "Schedulers")
        val functionsName = ClassName("io.reactivex.functions", "Function")
        val jsonObjectName = ClassName("com.google.gson", "JsonObject")
        val jsonArrayName = ClassName("com.google.gson", "JsonArray")
        val stringName = String::class.asClassName()
        val objectName = Any::class.asClassName()
        val mutableListName = MutableList::class.asClassName()
        val mapKVName = functionsName.parameterizedBy(paramPHName, paramPHName)
        val mapStringName = functionsName.parameterizedBy(stringName, stringName)
        val subObject = TypeVariableName("Any")
        val listName = mutableListName.parameterizedBy(subObject)
        val listObjectName = mutableListName.parameterizedBy(objectName)
        val t = TypeVariableName("T")
        val anyTypeName = Any::class.asTypeName()
        val anyT = TypeVariableName("T", anyTypeName)
        val progressName = ClassName("rxhttp.wrapper.entity", "Progress")
        val progressTName = progressName.parameterizedBy(t)
        val progressStringName = progressName.parameterizedBy(stringName)
        val consumerName = ClassName("io.reactivex.functions", "Consumer")
        val observableName = ClassName("io.reactivex", "Observable")
        val consumerProgressStringName = consumerName.parameterizedBy(progressStringName)
        val consumerProgressTName = consumerName.parameterizedBy(progressTName)
        val parserName = ClassName("rxhttp.wrapper.parse", "Parser")
        val parserTName = parserName.parameterizedBy(t)
        val observableTName = observableName.parameterizedBy(t)
        val simpleParserName = ClassName("rxhttp.wrapper.parse", "SimpleParser")
        val upFileName = ClassName("rxhttp.wrapper.entity", "UpFile")
        val listUpFileName = mutableListName.parameterizedBy(upFileName)
        val listFileName = mutableListName.parameterizedBy(File::class.asTypeName())
        val subString = WildcardTypeName.producerOf(stringName)
        val mapName = MutableMap::class.asClassName().parameterizedBy(subString, TypeVariableName("*"))
        val noBodyParamName = ClassName(packageName, "NoBodyParam")
        val rxHttpNoBodyName = ClassName(packageName, "RxHttp_NoBodyParam")
        val formParamName = ClassName(packageName, "FormParam")
        val rxHttpFormName = ClassName(packageName, "RxHttp_FormParam")
        val jsonParamName = ClassName(packageName, "JsonParam")
        val rxHttpJsonName = ClassName(packageName, "RxHttp_JsonParam")
        val jsonArrayParamName = ClassName(packageName, "JsonArrayParam")
        val rxHttpJsonArrayName = ClassName(packageName, "RxHttp_JsonArrayParam")
        val rxHttpNoBody = RXHTTP.parameterizedBy(noBodyParamName, rxHttpNoBodyName)
        val rxHttpForm = RXHTTP.parameterizedBy(formParamName, rxHttpFormName)
        val rxHttpJson = RXHTTP.parameterizedBy(jsonParamName, rxHttpJsonName)
        val rxHttpJsonArray = RXHTTP.parameterizedBy(jsonArrayParamName, rxHttpJsonArrayName)
        val methodList = ArrayList<FunSpec>() //方法集合
        val companionMethodList = ArrayList<FunSpec>()
        methodList.add(//添加构造方法
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.PROTECTED)
                .addParameter("param", p)
                .addStatement("this.param = param")
                .build())

        companionMethodList.add(
            FunSpec.builder("setDebug")
                .addAnnotation(JvmStatic::class)
                .addParameter("debug", Boolean::class)
                .addStatement("%T.setDebug(debug)", httpSenderName)
                .build())

        companionMethodList.add(
            FunSpec.builder("init")
                .addAnnotation(JvmStatic::class)
                .addParameter("okHttpClient", okHttpClientName)
                .addStatement("%T.init(okHttpClient)", httpSenderName)
                .build())

        companionMethodList.add(
            FunSpec.builder("init")
                .addAnnotation(JvmStatic::class)
                .addParameter("okHttpClient", okHttpClientName)
                .addParameter("debug", Boolean::class)
                .addStatement("%T.init(okHttpClient,debug)", httpSenderName)
                .build())

        val annoDeprecated = AnnotationSpec.builder(Deprecated::class)
            .addMember(
                "\n\"please user [setResultDecoder] instead\"," +
                    "\n    ReplaceWith(\"setResultDecoder(decoder)\", \"RxHttp.setResultDecoder\")")
            .build()

        companionMethodList.add(
            FunSpec.builder("setOnConverter")
                .addAnnotation(JvmStatic::class)
                .addAnnotation(annoDeprecated)
                .addKdoc("@deprecated please user [setResultDecoder] instead\n")
                .addParameter("decoder", mapStringName)
                .addStatement("setResultDecoder(decoder)")
                .build())

        companionMethodList.add(
            FunSpec.builder("setResultDecoder")
                .addAnnotation(JvmStatic::class)
                .addKdoc("设置统一数据解码/解密器，每次请求成功后会回调该接口并传入Http请求的结果" +
                    "\n通过该接口，可以统一对数据解密，并将解密后的数据返回即可" +
                    "\n若部分接口不需要回调该接口，发请求前，调用 [setDecoderEnabled] 方法设置false即可\n")
                .addParameter("decoder", mapStringName)
                .addStatement("%T.setResultDecoder(decoder)", rxHttpPluginsName)
                .build())

        companionMethodList.add(
            FunSpec.builder("setConverter")
                .addAnnotation(JvmStatic::class)
                .addKdoc("设置全局转换器\n")
                .addParameter("globalConverter", converterName)
                .addStatement("%T.setConverter(globalConverter)", rxHttpPluginsName)
                .build())

        companionMethodList.add(
            FunSpec.builder("setOnParamAssembly")
                .addAnnotation(JvmStatic::class)
                .addKdoc("设置统一公共参数回调接口,通过该接口,可添加公共参数/请求头，每次请求前会回调该接口" +
                    "\n若部分接口不需要添加公共参数,发请求前，调用 [setAssemblyEnabled]方法设置false即可\n")
                .addParameter("onParamAssembly", mapKVName)
                .addStatement("%T.setOnParamAssembly(onParamAssembly)", rxHttpPluginsName)
                .build())

        companionMethodList.add(
            FunSpec.builder("getOkHttpClient")
                .addAnnotation(JvmStatic::class)
                .addStatement("return %T.getOkHttpClient()", httpSenderName)
                .build())

        methodList.add(
            FunSpec.builder("setParam")
                .addParameter("param", p)
                .addStatement("this.param = param")
                .addStatement("return this as R")
                .returns(r)
                .build())
        methodList.addAll(mParamsAnnotatedClass.getMethodList(filer, companionMethodList))
        methodList.addAll(mParserAnnotatedClass.getMethodList(platform))
        methodList.addAll(mConverterAnnotatedClass.methodList)
        val funBuilder = FunSpec.builder("addDefaultDomainIfAbsent")
            .addModifiers(KModifier.PROTECTED)
            .addKdoc("给Param设置默认域名(如何缺席的话)，此方法会在请求发起前，被RxHttp内部调用\n")
            .addParameter("param", p)
        if (defaultDomain != null) {
            funBuilder.addStatement("val newUrl = addDomainIfAbsent(param.getSimpleUrl(), %T.%L)",
                defaultDomain!!.enclosingElement.asType().asTypeName(),
                defaultDomain!!.simpleName.toString())
                .addStatement("param.setUrl(newUrl)")
        }
        funBuilder.addStatement("return param")
            .returns(p)
        methodList.add(funBuilder.build())
        methodList.addAll(mDomainAnnotatedClass.getMethodList(companionMethodList))

        companionMethodList.add(
            FunSpec.builder("format")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("url", String::class)
                .addParameter("formatArgs", Any::class, KModifier.VARARG)
                .addStatement("return \n    if(formatArgs.size == 0) url else String.format(url, formatArgs)")
                .build())
        val schedulerField = PropertySpec.builder("scheduler", schedulerName.copy(nullable = true), KModifier.PROTECTED)
            .mutable()
            .initializer("%T.io()", schedulersName)
            .addKdoc("The request is executed on the IO thread by default\n")
            .build()
        val converterSpec = PropertySpec.builder("localConverter", converterName, KModifier.PROTECTED)
            .mutable()
            .initializer("%T.getConverter()", rxHttpPluginsName)
            .build()
        val paramSpec = PropertySpec.builder("param", p, KModifier.PROTECTED)
            .getter(FunSpec.getterBuilder()
                .addStatement("return field").build())
            .mutable()
            .build()

        val suppressAnno = AnnotationSpec.builder(Suppress::class)
            .addMember("\"UNCHECKED_CAST\"")
            .build()

        val companionType = TypeSpec.companionObjectBuilder()
            .addFunctions(companionMethodList)
            .build()
        val rxHttp = TypeSpec.classBuilder(CLASSNAME)
            .addKdoc("Github" +
                "\nhttps://github.com/liujingxing/RxHttp" +
                "\nhttps://github.com/liujingxing/RxLife\n")
            .addModifiers(KModifier.PUBLIC, KModifier.OPEN)
            .addAnnotation(suppressAnno)
            .addProperty(paramSpec)
            .addProperty(schedulerField)
            .addProperty(converterSpec)
            .addTypeVariable(p)
            .addTypeVariable(r)
            .addType(companionType)
            .addFunctions(methodList)
            .build()
        // Write file
        FileSpec.builder(packageName, "RxHttp")
            .addType(rxHttp)
            .build().writeTo(filer!!)

        //创建RxHttp_NoBodyParam类
        methodList.clear()

        methodList.add(
            FunSpec.constructorBuilder()
                .addParameter("param", noBodyParamName)
                .callSuperConstructor("param")
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.add(key,value)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("addEncoded")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.addEncoded(key,value)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())


        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addParameter("isAdd", Boolean::class)
                .beginControlFlow("if(isAdd)")
                .addStatement("param.add(key,value)")
                .endControlFlow()
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("map", mapName)
                .addStatement("param.addAll(map)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("removeAllBody")
                .addStatement("param.removeAllBody()")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("removeAllBody")
                .addParameter("key", String::class)
                .addStatement("param.removeAllBody(key)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("set")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.set(key,value)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("setEncoded")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.setEncoded(key,value)")
                .addStatement("return this")
                .returns(rxHttpNoBodyName)
                .build())

        methodList.add(
            FunSpec.builder("queryValue")
                .addParameter("key", String::class)
                .addStatement("return param.queryValue(key)")
                .returns(Any::class.asTypeName().copy(nullable = true))
                .build())

        methodList.add(
            FunSpec.builder("queryValues")
                .addParameter("key", String::class)
                .addStatement("return param.queryValues(key)")
                .returns(listObjectName)
                .build())
        val rxHttpNoBodySpec = TypeSpec.classBuilder("RxHttp_NoBodyParam")
            .addKdoc("Github" +
                "\nhttps://github.com/liujingxing/RxHttp" +
                "\nhttps://github.com/liujingxing/RxLife\n")
            .addModifiers(KModifier.PUBLIC, KModifier.OPEN)
            .superclass(rxHttpNoBody)
            .addFunctions(methodList)
            .build()
        FileSpec.builder(packageName, "RxHttp_NoBodyParam")
            .addType(rxHttpNoBodySpec)
            .build().writeTo(filer)


        //创建RxHttp_FormParam类
        methodList.clear()

        methodList.add(
            FunSpec.constructorBuilder()
                .callSuperConstructor("param")
                .addParameter("param", formParamName)
                .build())


        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.add(key,value)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addEncoded")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.addEncoded(key,value)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())


        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addParameter("isAdd", Boolean::class)
                .beginControlFlow("if(isAdd)")
                .addStatement("param.add(key,value)")
                .endControlFlow()
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("map", mapName)
                .addStatement("param.addAll(map)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("removeAllBody")
                .addStatement("param.removeAllBody()")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("removeAllBody")
                .addParameter("key", String::class)
                .addStatement("param.removeAllBody(key)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("set")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.set(key,value)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("setEncoded")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.setEncoded(key,value)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("queryValue")
                .addParameter("key", String::class)
                .addStatement("return param.queryValue(key)")
                .returns(Any::class.asClassName().copy(nullable = true))
                .build())

        methodList.add(
            FunSpec.builder("queryValues")
                .addParameter("key", String::class)
                .addStatement("return param.queryValues(key)")
                .returns(listObjectName)
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("file", File::class.java)
                .addStatement("param.add(key,file)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("key", String::class)
                .addParameter("file", File::class.java)
                .addStatement("param.addFile(key,file)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("key", String::class)
                .addParameter("filePath", String::class)
                .addStatement("param.addFile(key,filePath)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("key", String::class)
                .addParameter("value", String::class.asTypeName().copy(nullable = true))
                .addParameter("filePath", String::class)
                .addStatement("param.addFile(key,value,filePath)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("key", String::class)
                .addParameter("value", String::class.asTypeName().copy(nullable = true))
                .addParameter("file", File::class.java)
                .addStatement("param.addFile(key,value,file)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("file", upFileName)
                .addStatement("param.addFile(file)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("key", String::class)
                .addParameter("fileList", listFileName)
                .addStatement("param.addFile(key,fileList)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("addFile")
                .addParameter("fileList", listUpFileName)
                .addStatement("param.addFile(fileList)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("removeFile")
                .addParameter("key", String::class)
                .addStatement("param.removeFile(key)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("setMultiForm")
                .addStatement("param.setMultiForm()")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("setUploadMaxLength")
                .addParameter("maxLength", Long::class)
                .addStatement("param.setUploadMaxLength(maxLength)")
                .addStatement("return this")
                .returns(rxHttpFormName)
                .build())

        methodList.add(
            FunSpec.builder("asUpload")
                .addParameter("progressConsumer", consumerProgressStringName)
                .addStatement("return asUpload(%T(String::class.java), progressConsumer, null)", simpleParserName)
                .build())

        methodList.add(
            FunSpec.builder("asUpload")
                .addParameter("progressConsumer", consumerProgressStringName)
                .addParameter("observeOnScheduler", schedulerName)
                .addStatement("return asUpload(%T(String::class.java), progressConsumer, observeOnScheduler)", simpleParserName)
                .build())

        val parser = ParameterSpec.builder("parser", parserTName)
            .build()

        val observeOnScheduler = ParameterSpec.builder("observeOnScheduler", schedulerName.copy(nullable = true))
            .build()

        methodList.add(
            FunSpec.builder("asUpload")
                .addTypeVariable(anyT)
                .addParameter(parser)
                .addParameter("progressConsumer", consumerProgressTName)
                .addParameter(observeOnScheduler)
                .addStatement("setConverter(param)")
                .addStatement("var observable = %T\n" +
                    ".uploadProgress(addDefaultDomainIfAbsent(param), parser, scheduler)", httpSenderName)
                .beginControlFlow("if(observeOnScheduler != null)")
                .addStatement("observable=observable.observeOn(observeOnScheduler)")
                .endControlFlow()
                .addStatement("return observable.doOnNext(progressConsumer)\n" +
                    ".filter { it.isCompleted }\n" +
                    ".map { it.result }")
                .returns(observableTName)
                .build())
        val rxHttpFormSpec = TypeSpec.classBuilder("RxHttp_FormParam")
            .addKdoc("Github" +
                "\nhttps://github.com/liujingxing/RxHttp" +
                "\nhttps://github.com/liujingxing/RxLife\n")
            .addModifiers(KModifier.PUBLIC, KModifier.OPEN)
            .superclass(rxHttpForm)
            .addFunctions(methodList)
            .build()
        FileSpec.builder(packageName, "RxHttp_FormParam")
            .addType(rxHttpFormSpec)
            .build().writeTo(filer)

        //创建RxHttp_JsonParam类
        methodList.clear()

        methodList.add(
            FunSpec.constructorBuilder()
                .addParameter("param", jsonParamName)
                .callSuperConstructor("param")
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.add(key,value)")
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addParameter("isAdd", Boolean::class)
                .beginControlFlow("if(isAdd)")
                .addStatement("param.add(key,value)")
                .endControlFlow()
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("map", mapName)
                .addStatement("param.addAll(map)")
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addKdoc("将Json对象里面的key-value逐一取出，添加到另一个Json对象中，" +
                    "\n输入非Json对象将抛出 [IllegalStateException] 异常\n")
                .addParameter("jsonObject", String::class)
                .addStatement("param.addAll(jsonObject)")
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addKdoc("将Json对象里面的key-value逐一取出，添加到另一个Json对象中\n")
                .addParameter("jsonObject", jsonObjectName)
                .addStatement("param.addAll(jsonObject)")
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())

        methodList.add(
            FunSpec.builder("addJsonElement")
                .addKdoc("添加一个JsonElement对象(Json对象、json数组等)\n")
                .addParameter("key", String::class)
                .addParameter("jsonElement", String::class)
                .addStatement("param.addJsonElement(key,jsonElement)")
                .addStatement("return this")
                .returns(rxHttpJsonName)
                .build())
        val rxHttpJsonSpec = TypeSpec.classBuilder("RxHttp_JsonParam")
            .addKdoc("Github" +
                "\nhttps://github.com/liujingxing/RxHttp" +
                "\nhttps://github.com/liujingxing/RxLife\n")
            .addModifiers(KModifier.PUBLIC, KModifier.OPEN)
            .superclass(rxHttpJson)
            .addFunctions(methodList)
            .build()
        FileSpec.builder(packageName, "RxHttp_JsonParam")
            .addType(rxHttpJsonSpec)
            .build().writeTo(filer)

        //创建RxHttp_JsonArrayParam类
        methodList.clear()

        methodList.add(
            FunSpec.constructorBuilder()
                .addParameter("param", jsonArrayParamName)
                .callSuperConstructor("param")
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("any", Any::class)
                .addStatement("param.add(any)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addStatement("param.add(key,value)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("add")
                .addParameter("key", String::class)
                .addParameter("value", Any::class.asTypeName().copy(nullable = true))
                .addParameter("isAdd", Boolean::class)
                .beginControlFlow("if(isAdd)")
                .addStatement("param.add(key,value)")
                .endControlFlow()
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("map", mapName)
                .addStatement("param.addAll(map)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("list", listName)
                .addStatement("param.addAll(list)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addKdoc("添加多个对象，将字符串转JsonElement对象,并根据不同类型,执行不同操作,可输入任意非空字符串\n")
                .addParameter("jsonElement", String::class)
                .addStatement("param.addAll(jsonElement)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addParameter("jsonArray", jsonArrayName)
                .addStatement("param.addAll(jsonArray)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addAll")
                .addKdoc("将Json对象里面的key-value逐一取出，添加到Json数组中，成为单独的对象\n")
                .addParameter("jsonObject", jsonObjectName)
                .addStatement("param.addAll(jsonObject)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addJsonElement")
                .addParameter("jsonElement", String::class)
                .addStatement("param.addJsonElement(jsonElement)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())

        methodList.add(
            FunSpec.builder("addJsonElement")
                .addKdoc("添加一个JsonElement对象(Json对象、json数组等)\n")
                .addParameter("key", String::class)
                .addParameter("jsonElement", String::class)
                .addStatement("param.addJsonElement(key,jsonElement)")
                .addStatement("return this")
                .returns(rxHttpJsonArrayName)
                .build())
        val rxHttpJsonArraySpec = TypeSpec.classBuilder("RxHttp_JsonArrayParam")
            .addKdoc("Github" +
                "\nhttps://github.com/liujingxing/RxHttp" +
                "\nhttps://github.com/liujingxing/RxLife\n")
            .addModifiers(KModifier.PUBLIC, KModifier.OPEN)
            .superclass(rxHttpJsonArray)
            .addFunctions(methodList)
            .build()
        FileSpec.builder(packageName, "RxHttp_JsonArrayParam")
            .addType(rxHttpJsonArraySpec)
            .build().writeTo(filer)
    }

    companion object {
        private const val CLASSNAME = "RxHttp"
        const val packageName = "rxhttp.wrapper.param"
        @JvmField
        var RXHTTP = ClassName(packageName, CLASSNAME)
        private val P = TypeVariableName("P")
        private val R = TypeVariableName("R")
        private val placeHolder = TypeVariableName("*")
        private val paramName = ClassName(packageName, "Param")
        private val paramPName = paramName.parameterizedBy(P)
        private val paramPHName = paramName.parameterizedBy(placeHolder)
        private val rxHttpName = ClassName(packageName, CLASSNAME)
        private val rxHttpPRName = rxHttpName.parameterizedBy(P, R)
        @JvmField
        var p: TypeVariableName = TypeVariableName("P", paramPName)
        @JvmField
        var r: TypeVariableName = TypeVariableName("R", rxHttpPRName)
    }
}