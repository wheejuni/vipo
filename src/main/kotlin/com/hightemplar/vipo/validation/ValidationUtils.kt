package com.hightemplar.vipo.validation

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator
import org.springframework.stereotype.Component

@Component
class ValidationUtils(private val validator: Validator) {

    fun <T> validateAndThrow(obj: T) {
        val violations = validator.validate(obj)
        if (violations.isNotEmpty()) {
            val errorMessages = violations.joinToString(", ") { "${it.propertyPath}: ${it.message}" }
            throw ValidationException("Validation failed: $errorMessages")
        }
    }

    fun <T> validate(obj: T): Set<ConstraintViolation<T>> {
        return validator.validate(obj)
    }

    fun <T> isValid(obj: T): Boolean {
        return validator.validate(obj).isEmpty()
    }
}

class ValidationException(message: String) : RuntimeException(message)