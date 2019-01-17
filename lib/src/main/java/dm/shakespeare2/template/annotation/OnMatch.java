package dm.shakespeare2.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dm.shakespeare2.actor.BehaviorBuilder.Matcher;

/**
 * Created by davide-maestroni on 09/06/2018.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OnMatch {

  Class<? extends Matcher<?>> matcherClass() default VoidMatcher.class;

  String matcherName() default "";
}
