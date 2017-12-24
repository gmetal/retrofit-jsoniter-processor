package gr.gmetal.jsoniter.processor;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;
import com.jsoniter.annotation.JsonObject;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.TypeLiteral;
import com.jsoniter.static_codegen.StaticCodegenConfig;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({ "com.jsoniter.annotation.JsonObject" })
public class JsonIterProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {

        final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(JsonObject.class);
        final Messager messager = processingEnv.getMessager();

        if (elements == null || elements.size() == 0) {
            return true;
        }

        List<TypeLiteral> typeLiteralList = new ArrayList<>();
        JavaFileObject builderFile;
        try {
            builderFile = processingEnv.getFiler().createSourceFile("MyConfig.java");
            PrintWriter out = new PrintWriter(builderFile.openWriter());

            for (Element element : elements) {
                if (element.getKind() != ElementKind.CLASS) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                        "@JsonObject can only be used for classes!");
                    return false;
                } else {
                    messager.printMessage(Diagnostic.Kind.WARNING, "Adding class: "
                        + element.getClass().getSimpleName()
                        + " to type literals");
                }
                typeLiteralList.add(TypeLiteral.create(element.getClass()));
                out.println(element.getClass().getCanonicalName());
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        DemoCodegenConfig config = new DemoCodegenConfig(typeLiteralList);

        config.setup();

        CodegenAccess.staticGenDecoders(config.whatToCodegen(),
            new CodegenAccess.StaticCodegenTarget("./"));
        com.jsoniter.output.CodegenAccess.staticGenEncoders(config.whatToCodegen(),
            new com.jsoniter.output.CodegenAccess.StaticCodegenTarget("./"));

        return true;
    }

    class DemoCodegenConfig implements StaticCodegenConfig {

        private TypeLiteral[] typeLiterals;

        public DemoCodegenConfig(final List<TypeLiteral> typeLiteralList) {

            typeLiterals = new TypeLiteral[typeLiteralList.size()];
            typeLiterals = typeLiteralList.toArray(typeLiterals);
        }

        @Override
        public void setup() {
            // register custom decoder or extensions before codegen
            // so that we doing codegen, we know in which case, we need to callback
            JsonIterator.setMode(DecodingMode.STATIC_MODE);
            JsonStream.setMode(EncodingMode.STATIC_MODE);
            JsonStream.setIndentionStep(2);
        }

        @Override
        public TypeLiteral[] whatToCodegen() {

            //return new TypeLiteral[] {
            //    // generic types, need to use this syntax
            //    new TypeLiteral<List<Integer>>() {
            //
            //    }, new TypeLiteral<List<User>>() {
            //
            //}, new TypeLiteral<Map<String, Object>>() {
            //
            //},
            //    // array
            //    TypeLiteral.create(int[].class),
            //    // object
            //    TypeLiteral.create(User.class)
            //};

            return typeLiterals;
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {

        return SourceVersion.latestSupported();
    }
}
