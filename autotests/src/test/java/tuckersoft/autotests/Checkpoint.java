package tuckersoft.autotests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca una clase de test como una de las cinco estrellas. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Checkpoint {
    int value();

    String nombre();
}
