package dm.shakespeare2.templates.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dm.shakespeare2.actor.Actor.Envelop;
import dm.shakespeare2.function.Tester;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnSender {

  Class<? extends Tester<? super Envelop>> testerClass() default VoidTester.class;

  String testerName() default "";
}
