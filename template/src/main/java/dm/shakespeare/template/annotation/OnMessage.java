package dm.shakespeare.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dm.shakespeare.function.Tester;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnMessage {

  Class<?>[] messageClasses() default {};

  Class<? extends Tester<?>> testerClass() default VoidTester.class;

  String testerName() default "";
}
