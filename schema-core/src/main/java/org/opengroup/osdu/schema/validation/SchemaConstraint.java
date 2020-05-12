package org.opengroup.osdu.schema.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.opengroup.osdu.schema.constants.SchemaConstants;

@Documented
@Constraint(validatedBy = SchemaValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaConstraint {
    String message() default SchemaConstants.INVALID_SCHEMA_INPUT;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}