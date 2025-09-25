package com.dream11.validation

import groovy.util.logging.Slf4j
import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

@Slf4j
final class BeanValidator {
    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory()
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator()

    private BeanValidator() {
    }

    static void validate(Object bean, String beanName) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(bean)

        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(violation -> violation.message)
                    .toList()

            String fullErrorMessage = "Validation failed for ${beanName}: ${errorMessages.join(', ')}"
            log.error(fullErrorMessage)
            throw new IllegalArgumentException(fullErrorMessage)
        }

        log.debug("Validation successful for ${beanName}")
    }
}
