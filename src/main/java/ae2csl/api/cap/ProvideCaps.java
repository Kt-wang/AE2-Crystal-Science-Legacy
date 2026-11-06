/*
 * AE2 Crystal Science - API ProvideCaps Annotation
 * Ported from 1.21.1 source
 *
 * Marks a type as providing certain capability classes.
 * Used with ProvidedCapsHelper to discover capabilities.
 */
package ae2csl.api.cap;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ProvideCaps {

    Class<?> value();
}
