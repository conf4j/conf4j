package org.conf4j.bean.validation;

import org.conf4j.core.ext.ConfigurationExtension;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

public class BeanValidationExtension implements ConfigurationExtension {

    private static final String NAME = "Bean Validation Extension";

    private final Validator validator;

    public BeanValidationExtension() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Override
    public void afterConfigBeanAssembly(Object resolvedBean) {
        Set<ConstraintViolation<Object>> violations = validator.validate(resolvedBean);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException("Invalid configurations", violations);
        }
    }

    @Override
    public String getExtensionName() {
        return NAME;
    }

}
