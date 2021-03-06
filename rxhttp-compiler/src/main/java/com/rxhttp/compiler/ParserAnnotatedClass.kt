package com.rxhttp.compiler

import com.squareup.javapoet.*
import rxhttp.wrapper.annotation.Parser
import java.io.IOException
import java.lang.Deprecated
import java.util.*
import javax.annotation.processing.Filer
import javax.lang.model.element.*
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import kotlin.Int
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.require

class ParserAnnotatedClass {

    private val mElementMap = LinkedHashMap<String, TypeElement>()
    private val mTypeMap = LinkedHashMap<String, List<TypeMirror>>()

    fun add(typeElement: TypeElement) {
        val annotation = typeElement.getAnnotation(Parser::class.java)
        val name: String = annotation.name
        require(name.isNotEmpty()) {
            String.format("methodName() in @%s for class %s is null or empty! that's not allowed",
                Parser::class.java.simpleName, typeElement.qualifiedName.toString())
        }
        try {
            annotation.wrappers
        } catch (e: MirroredTypesException) {
            val typeMirrors = e.typeMirrors
            mTypeMap[name] = typeMirrors
        }
        mElementMap[name] = typeElement
    }

    fun getMethodList(filer: Filer): List<MethodSpec> {
        val t = TypeVariableName.get("T")
        val className = ClassName.get(Class::class.java)
        val classTName = ParameterizedTypeName.get(className, t)

        val listTName = ParameterizedTypeName.get(ClassName.get(List::class.java), t)
        val callName = ClassName.get("okhttp3", "Call")
        val responseName = ClassName.get("okhttp3", "Response")
        val requestName = ClassName.get("okhttp3", "Request")
        val parserName = ClassName.get("rxhttp.wrapper.parse", "Parser")
        val progressName = ClassName.get("rxhttp.wrapper.entity", "Progress")
        val logUtilName = ClassName.get("rxhttp.wrapper.utils", "LogUtil")
        val logTimeName = ClassName.get("rxhttp.wrapper.utils", "LogTime")
        val typeName = TypeName.get(String::class.java)
        val parserTName = ParameterizedTypeName.get(parserName, t)
        val simpleParserName = ClassName.get("rxhttp.wrapper.parse", "SimpleParser")
        val type = ClassName.get("java.lang.reflect", "Type")
        val parameterizedType = ClassName.get("rxhttp.wrapper.entity", "ParameterizedTypeImpl")
        val methodList = ArrayList<MethodSpec>()

        methodList.add(
            MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException::class.java)
                .addStatement("return newCall().execute()")
                .returns(responseName)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("execute")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(t)
                .addException(IOException::class.java)
                .addParameter(parserTName, "parser")
                .addStatement("return parser.onParse(execute())")
                .returns(t)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("executeString")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException::class.java)
                .addStatement("return executeClass(String.class)")
                .returns(typeName)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("executeList")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(t)
                .addException(IOException::class.java)
                .addParameter(classTName, "type")
                .addStatement("\$T tTypeList = \$T.get(List.class, type)", type, parameterizedType)
                .addStatement("return execute(new \$T<>(tTypeList))", simpleParserName)
                .returns(listTName)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("executeClass")
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(t)
                .addException(IOException::class.java)
                .addParameter(classTName, "type")
                .addStatement("return execute(new \$T<>(type))", simpleParserName)
                .returns(t)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("newCall")
                .addAnnotation(Override::class.java)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode("""
                    Request request = buildRequest();
                    OkHttpClient okClient = getOkHttpClient();
                    return okClient.newCall(request);
                """.trimIndent())
                .returns(callName)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("buildRequest")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addCode(
                    """
                    boolean debug = ${"$"}T.isDebug();    
                    if (request == null) {
                        doOnStart();
                        request = param.buildRequest();
                        if (debug) 
                            LogUtil.log(request, getOkHttpClient().cookieJar());
                    }
                    if (debug) {
                        request = request.newBuilder()
                            .tag(LogTime.class, new ${"$"}T())
                            .build();
                    }
                    return request;
                """.trimIndent(), logUtilName, logTimeName)
                .returns(requestName)
                .build())

        methodList.add(
            MethodSpec.methodBuilder("doOnStart")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addJavadoc("请求开始前内部调用，用于添加默认域名等操作\n")
                .addStatement("setConverter(param)")
                .addStatement("addDefaultDomainIfAbsent(param)")
                .build())

        if (isDependenceRxJava()) {
            val schedulerName = getClassName("Scheduler")
            val observableName = getClassName("Observable")
            val consumerName = getClassName("Consumer")

            methodList.add(
                MethodSpec.methodBuilder("subscribeOnCurrent")
                    .addAnnotation(Deprecated::class.java)
                    .addJavadoc("@deprecated please user {@link #setSync()} instead\n")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return setSync()")
                    .returns(r)
                    .build())

            methodList.add(
                MethodSpec.methodBuilder("setSync")
                    .addJavadoc("sync request \n")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("isAsync = false")
                    .addStatement("return (R)this")
                    .returns(r)
                    .build())

            val observableTName = ParameterizedTypeName.get(observableName, t)
            val consumerProgressName = ParameterizedTypeName.get(consumerName, progressName)

            methodList.add(
                MethodSpec.methodBuilder("asParser")
                    .addModifiers(Modifier.PUBLIC)
                    .addTypeVariable(t)
                    .addParameter(parserTName, "parser")
                    .addParameter(schedulerName, "scheduler")
                    .addParameter(consumerProgressName, "progressConsumer")
                    .addCode("""
                        ObservableCall observableCall = isAsync ? new ObservableCallEnqueue(this)
                            : new ObservableCallExecute(this);                                
                        return observableCall.asParser(parser, scheduler, progressConsumer);
                    """.trimIndent())
                    .returns(observableTName)
                    .build())
        }


        val rxHttpExtensions = RxHttpExtensions()

        //获取自定义的解析器
        for ((parserAlias, typeElement) in mElementMap) {

            //生成RxHttp扩展方法(kotlin编写的方法)
            rxHttpExtensions.generateRxHttpExtendFun(typeElement, parserAlias)

            if (isDependenceRxJava()) { //依赖了RxJava，则生成自定义的asXxx方法
                //onParser方法返回类型
                val returnTypeMirror = getOnParserFunReturnType(typeElement) ?: continue
                val onParserFunReturnType = TypeName.get(returnTypeMirror)

                val typeVariableNames = ArrayList<TypeVariableName>()
                typeElement.typeParameters.forEach {
                    typeVariableNames.add(TypeVariableName.get(it))
                }

                //遍历public构造方法
                typeElement.getConstructorFun().forEach {
                    //根据构造方法参数，获取asXxx方法需要的参数
                    val parameterList = ArrayList<ParameterSpec>()
                    var typeIndex = 0
                    it.parameters.forEach { variableElement ->
                        val variableType = variableElement.asType()
                        if (variableType.toString() == "java.lang.reflect.Type[]") {
                            typeVariableNames.forEach { typeVariableName ->
                                //Type类型参数转Class<T>类型
                                val classTypeName =
                                    ParameterizedTypeName.get(className, typeVariableName)
                                val variableName =
                                    "${typeVariableName.name.lowercase(Locale.getDefault())}Type"
                                val parameterSpec =
                                    ParameterSpec.builder(classTypeName, variableName).build()
                                parameterList.add(parameterSpec)
                            }
                        } else if (variableType.toString() == "java.lang.reflect.Type"
                            && typeIndex < typeVariableNames.size
                        ) {
                            //Type类型参数转Class<T>类型
                            val classTypeName = ParameterizedTypeName.get(
                                className, typeVariableNames[typeIndex++]
                            )
                            val variableName = variableElement.simpleName.toString()
                            val parameterSpec =
                                ParameterSpec.builder(classTypeName, variableName).build()
                            parameterList.add(parameterSpec)
                        } else {
                            parameterList.add(ParameterSpec.get(variableElement))
                        }
                    }

                    //方法名
                    var methodName = "as$parserAlias"
                    //方法体
                    val methodBody =
                        "return asParser(new \$T<>(${
                            getParamsName(it.parameters, parameterList, typeVariableNames.size)
                        }))"

                    //生成的as方法返回类型(Observable<T>类型)
                    var asFunReturnType = ParameterizedTypeName.get(
                        getClassName("Observable"), onParserFunReturnType)
                    methodList.add(
                        MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addTypeVariables(typeVariableNames)
                            .addParameters(parameterList)
                            .varargs(it.isVarArgs && parameterList.last().type is ArrayTypeName)
                            .addStatement(methodBody, ClassName.get(typeElement))  //方法里面的表达式
                            .returns(asFunReturnType)
                            .build())

                    var haveClassTypeParam = false
                    parameterList.forEach { p ->
                        if (p.type.toString().startsWith("java.lang.Class")) {
                            haveClassTypeParam = true
                        }
                    }
                    //有泛型且有Class类型参数
                    if (typeVariableNames.size == 1 && haveClassTypeParam) {
                        val wrapperListClass = mutableListOf<ClassName>()
                        if (typeVariableNames.size == 1)
                            wrapperListClass.add(ClassName.get("java.util", "List"))

                        //泛型的包裹类型，取自Parser注解的wrappers字段
                        mTypeMap[parserAlias]?.forEach { mirror ->
                            val tempClassName = ClassName.bestGuess(mirror.toString())
                            if (!wrapperListClass.contains(tempClassName)) {
                                wrapperListClass.add(tempClassName)
                            }
                        }

                        wrapperListClass.forEach { wrapperClass ->

                            //1、asXxx方法返回值
                            val onParserFunReturnWrapperType = if (onParserFunReturnType is ParameterizedTypeName) {
                                //返回类型有n个泛型，需要对每个泛型再次包装
                                val typeNames = ArrayList<TypeName>()
                                for (typeArg in onParserFunReturnType.typeArguments) {
                                    typeNames.add(ParameterizedTypeName.get(wrapperClass, typeArg))
                                }
                                ParameterizedTypeName.get(onParserFunReturnType.rawType, *typeNames.toTypedArray())
                            } else {
                                ParameterizedTypeName.get(wrapperClass, onParserFunReturnType)
                            }
                            asFunReturnType = ParameterizedTypeName.get(getClassName("Observable"), onParserFunReturnWrapperType)

                            //2、asXxx方法名
                            val name = wrapperClass.toString()
                            val simpleName = name.substring(name.lastIndexOf(".") + 1)
                            methodName = "as$parserAlias${simpleName}"

                            //3、asXxx方法体
                            val funBody = CodeBlock.builder()
                            val paramsName = StringBuilder()
                            //遍历参数，取出参数名
                            parameterList.forEachIndexed { index, param ->
                                if (index > 0) paramsName.append(", ")
                                if (param.type.toString().startsWith("java.lang.Class")) {
                                    /*
                                     * Class类型参数，需要进行再次包装，最后再取参数名
                                     * 格式：Type tTypeList = ParameterizedTypeImpl.get(List.class, tType);
                                     */
                                    val variableName = "${param.name}$simpleName"
                                    val expression =
                                        "\$T $variableName = \$T.get($simpleName.class, ${param.name})"
                                    funBody.addStatement(expression, type, parameterizedType)
                                    val parameterType = it.parameters[index].asType()
                                    if ("java.lang.reflect.Type[]" == parameterType.toString()) {
                                        paramsName.append("new Type[]{$variableName}")
                                    } else {
                                        paramsName.append(variableName)
                                    }
                                } else {
                                    paramsName.append(param.name)
                                }
                            }
                            val returnStatement = "return asParser(new \$T<>($paramsName))"
                            funBody.addStatement(returnStatement, ClassName.get(typeElement))

                            //4、生成asXxx方法
                            methodList.add(
                                MethodSpec.methodBuilder(methodName)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addTypeVariables(typeVariableNames)
                                    .addParameters(parameterList)
                                    .varargs(it.isVarArgs && parameterList.last().type is ArrayTypeName)
                                    .addCode(funBody.build())  //方法里面的表达式
                                    .returns(asFunReturnType)
                                    .build())
                        }
                    }
                }
            }
        }
        rxHttpExtensions.generateClassFile(filer)
        return methodList
    }


    //获取构造方法
    private fun TypeElement.getConstructorFun(): MutableList<ExecutableElement> {
        val funList = ArrayList<ExecutableElement>()
        enclosedElements.forEach {
            if (it is ExecutableElement
                && it.kind == ElementKind.CONSTRUCTOR
                && it.getModifiers().contains(Modifier.PUBLIC)
            ) {
                funList.add(it)
            }
        }
        return funList
    }

    /**
     * @param variableElements 解析器构造方法参数列表
     * @param parameterSpecs 通过解析器构造方法参数列表转换而来的实际参数列表，parameterSpecs.size() >= variableElements.size()
     * @param typeCount 解析器泛型数量
     */
    private fun getParamsName(
        variableElements: List<VariableElement>,
        parameterSpecs: MutableList<ParameterSpec>,
        typeCount: Int
    ): String {
        val sb = StringBuilder()
        var paramIndex = 0
        var variableIndex = 0
        val variableSize = variableElements.size
        val paramSize = parameterSpecs.size
        while (paramIndex < paramSize && variableIndex < variableSize) {
            if (variableIndex > 0) sb.append(", ")
            val type = variableElements[variableIndex++].asType()
            if ("java.lang.reflect.Type[]" == type.toString()) {
                sb.append("new Type[]{")
                for (i in 0 until typeCount) {
                    if (i > 0) sb.append(", ")
                    sb.append(parameterSpecs[paramIndex++].name)
                }
                sb.append("}")
            } else
                sb.append(parameterSpecs[paramIndex++].name)
        }
        return sb.toString()
    }

    //获取泛型字符串 比如:<T> 、<K,V>等等
    private fun getTypeVariableString(
        typeVariableNames: ArrayList<TypeVariableName>,
        wrapperClass: ClassName
    ): String {
        val name = wrapperClass.toString()
        val simpleName = name.substring(name.lastIndexOf(".") + 1)

        val type = StringBuilder()
        val size = typeVariableNames.size
        for (i in typeVariableNames.indices) {
            if (i == 0) type.append("<")
            type.append("$simpleName<")
            type.append("${typeVariableNames[i].name}>")
            type.append(if (i < size - 1) "," else ">")
        }
        return type.toString()
    }

    //获取泛型字符串 比如:<T> 、<K,V>等等
    private fun getTypeVariableString(typeVariableNames: ArrayList<TypeVariableName>): String {
        val type = StringBuilder()
        val size = typeVariableNames.size
        for (i in typeVariableNames.indices) {
            if (i == 0) type.append("<")
            type.append(typeVariableNames[i].name)
            type.append(if (i < size - 1) "," else ">")
        }
        return type.toString()
    }


    //获取onParser方法返回类型
    private fun getOnParserFunReturnType(typeElement: TypeElement): TypeMirror? {
        typeElement.enclosedElements.forEach {
            if (it is ExecutableElement   //是方法
                && it.getModifiers().contains(Modifier.PUBLIC)  //public修饰
                && !it.getModifiers().contains(Modifier.STATIC) //非静态
                && it.simpleName.toString() == "onParse"  //onParse方法
                && it.parameters.size == 1  //只有一个参数
                && TypeName.get(it.parameters[0].asType()).toString() == "okhttp3.Response"  //参数是okhttp3.Response类型
            ) {
                return it.returnType;
            }
        }
        return null
    }
}