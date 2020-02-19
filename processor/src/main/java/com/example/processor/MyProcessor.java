package com.example.processor;

import com.example.annotations.AutoBind;
import com.example.annotations.BindView;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

/**方案1
 * 运行注解处理器
 * 1、在 processors 库的 main 目录下新建 resources 资源文件夹；
 * 2、在 resources文件夹下建立 META-INF/services 目录文件夹；
 * 3、在 META-INF/services 目录文件夹下创建 javax.annotation.processing.Processor 文件；
 * 4、在 javax.annotation.processing.Processor 文件写入注解处理器的全称，包括包路径；
 */
/** 方案2
 * 每一个注解处理器类都必须有一个空的构造函数，默认不写就行;
 *  类前面标注 @AutoService(Processor.class)
 */
@AutoService(Processor.class)
public class MyProcessor extends AbstractProcessor {

    //Types是一个用来处理TypeMirror的工具
    private Types typeUtils;
    //Elements是一个用来处理Element的工具
    private Elements elementUtils;
    //生成java源码
    private Filer filer;
    //每个需要自动生成的类 对应一个生成类的操作对象
    private Map<String, AnnotatedClass> mAnnotatedClassMap;
    /**
     * Messager提供给注解处理器一个报告错误、警告以及提示信息的途径。
     * 它不是注解处理器开发者的日志工具，
     * 而是用来写一些信息给使用此注解器的第三方开发者的
     */
    private Messager messager;

    /**
     * init()方法会被注解处理工具调用，并输入ProcessingEnviroment参数。
     * ProcessingEnviroment提供很多有用的工具类Elements, Types 和 Filer
     * @param processingEnv 提供给 processor 用来访问工具框架的环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        mAnnotatedClassMap = new TreeMap<>();
    }

    /**
     * 这相当于每个处理器的主函数main()，你在这里写你的扫描、评估和处理注解的代码，以及生成Java文件。
     * 输入参数RoundEnviroment，可以让你查询出包含特定注解的被注解元素
     * @param annotations   请求处理的注解类型
     * @param roundEnvironment  有关当前和以前的信息环境
     * @return  如果返回 true，则这些注解已声明并且不要求后续 Processor 处理它们；
     *          如果返回 false，则这些注解未声明并且可能要求后续 Processor 处理它们
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        /**
         * 首先需要把要生成的类所需要的信息都收集到
         *  如：类名，需要的view 和 view 的类型，view 的 resId 等等
         */
        try {
            mAnnotatedClassMap.clear();
            //获取被BindView注解的所有元素
            System.out.println("step ---> Get all @BindView element");
            for(Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)){
                TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                //获取元素对应的类名
                String viewWithClassName = typeElement.getQualifiedName().toString();
                //如果这个元素对应的类还没添加进mAnnotatedClassMap 则添加进去
                System.out.println("step ---> Get annotatedClass");
                AnnotatedClass annotatedClass = mAnnotatedClassMap.get(viewWithClassName);
                if(annotatedClass == null){
                    annotatedClass = new AnnotatedClass(typeElement, elementUtils);
                    mAnnotatedClassMap.put(viewWithClassName, annotatedClass);
                }
                System.out.println("annotatedClass" + annotatedClass);
                // annotatedClass 中还有对应类中的view信息 根据element获取BindViewFieId对象
                System.out.println("step ---> Get bindViewField");
                BindViewFieId bindViewFieId = new BindViewFieId(element);
                annotatedClass.addField(bindViewFieId);
                System.out.println("bindViewFieId" + bindViewFieId);
            }
        }catch (IllegalArgumentException e){
            System.out.println("Auto bind view is error -----------");
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        /**
         *  所有的信息都收集完了之后 开始生成类
         *   view 变量 信息 都在annotatedClass中， 所以 生成类的操作写在 annotatedClass中
         */
        for (AnnotatedClass annotatedClass : mAnnotatedClassMap.values()){
            try {
                System.out.println("step ---> Process start create class");
                System.out.println("annotatedClass:" + annotatedClass);
                annotatedClass.createFile().writeTo(filer);
            } catch (IOException e) {
                System.out.println("process create class error");
                //e.printStackTrace();
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }

        return true;
    }

    /**
     * 这里必须指定，这个注解处理器是注册给哪个注解的。注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称
     * @return  注解器所支持的注解类型集合，如果没有这样的类型，则返回一个空集合
     */
    @Override
    public Set getSupportedAnnotationTypes() {
        Set annotataions = new LinkedHashSet();
        annotataions.add(AutoBind.class.getCanonicalName());
        return annotataions;
    }

    /**
     * 指定使用的Java版本，通常这里返回SourceVersion.latestSupported()，默认返回SourceVersion.RELEASE_6
     * @return  使用的Java版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    //主要用于 自动生成 class 文件 写入类、方法代码
    static class AnnotatedClass{

        TypeElement typeElement;
        Elements elements;
        //保存被注解 view 的信息
        ArrayList<BindViewFieId> viewList;

        AnnotatedClass(TypeElement typeElement, Elements elements){
            this.typeElement = typeElement;
            this.elements = elements;
            viewList = new ArrayList();
        }

        void addField(BindViewFieId bindViewFieId){
            viewList.add(bindViewFieId);
            System.out.println("AnnotatedClass.viewList : " + viewList);
        }

        static final ClassName BINDER = ClassName.get("com.example.api", "ViewBinder");
        static final ClassName PROVIDER = ClassName.get("com.example.api", "ViewFinder");

        JavaFile createFile(){
            //生成方法 生成自动绑定view的方法 bind
            System.out.println("step ---> createFile - ceate method");
            MethodSpec.Builder bindMethod = MethodSpec
                    .methodBuilder("bind")//方法名字
                    .addModifiers(Modifier.PUBLIC)//修饰符
                    .addAnnotation(Override.class)//方法注解
                    .addParameter(TypeName.get(typeElement.asType()),"host")//方法参数类型 参数名
                    .addParameter(TypeName.OBJECT, "obj")
                    .addParameter(PROVIDER, "viewFinder");
            //方法体 也就是findView
            for (BindViewFieId bindViewFieId : viewList){
                bindMethod.addStatement("host.$N = ($T)(viewFinder.findView(obj, $L))",
                        bindViewFieId.getName(),
                        ClassName.get(bindViewFieId.getType()),
                        bindViewFieId.getId());
            }

            //生成解绑方法 unBind
            MethodSpec.Builder unBindMethod = MethodSpec
                    .methodBuilder("unBind")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(TypeName.get(typeElement.asType()), "host");
            //对绑定过的view 解绑 赋空值
            for (BindViewFieId bindViewFieId : viewList){
                unBindMethod.addStatement("host.$N = null",
                        bindViewFieId.getName());
            }

            //生成类
            System.out.println("step ---> createFile - ceate class");
            TypeSpec bindClass = TypeSpec
                    .classBuilder(typeElement.getSimpleName() + "$AutoBind")//类名
                    .addModifiers(Modifier.PUBLIC)//修饰符
                    .addSuperinterface(ParameterizedTypeName.get(BINDER, TypeName.get(typeElement.asType())))//添加实现的接口
                    .addMethod(bindMethod.build())//添加方法
                    .addMethod(unBindMethod.build())
                    .build();

            String packageName = elements.getPackageOf(typeElement).getQualifiedName().toString();
            JavaFile result = JavaFile.builder(packageName, bindClass).build();
            try {
                result.writeTo(System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    //被注解 view 的信息
    static class BindViewFieId{
        private VariableElement variableElement;
        private int resId;

        BindViewFieId(Element element){
            //判断是否注解的是 view 变量
            if(element.getKind() == ElementKind.FIELD){
                variableElement = (VariableElement) element;
                System.out.println("BindViewField variableElement: " + variableElement);
                BindView bindView = variableElement.getAnnotation(BindView.class);
                resId = bindView.value();
                System.out.println("BindViewField resId: " + resId);
                //如果 注解中的 view id 不正确 则抛出异常
                if(resId < 0){
                    throw new IllegalArgumentException("@BindView get value is invalid");
                }
            }else { //@BindView不是用在注解上 则抛出异常
                throw new IllegalArgumentException("@BindView only can be annotated with fields");
            }
        }

        // 获取 view 名称
        Name getName(){
            return variableElement.getSimpleName();
        }
        // 获取 view resId
        int getId(){
            return resId;
        }
        // 获取 view 类型
        TypeMirror getType(){
            return variableElement.asType();
        }
    }
}
