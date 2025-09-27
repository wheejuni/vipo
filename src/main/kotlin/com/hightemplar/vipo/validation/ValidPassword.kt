package com.hightemplar.vipo.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PasswordValidator::class])
@MustBeDocumented
annotation class ValidPassword(
    val message: String = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class PasswordValidator : ConstraintValidator<ValidPassword, String> {
    
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) {
            return false
        }
        
        // Check for at least one uppercase letter
        if (!value.any { it.isUpperCase() }) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Password must contain at least one uppercase letter")
                ?.addConstraintViolation()
            return false
        }
        
        // Check for at least one lowercase letter
        if (!value.any { it.isLowerCase() }) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Password must contain at least one lowercase letter")
                ?.addConstraintViolation()
            return false
        }
        
        // Check for at least one digit
        if (!value.any { it.isDigit() }) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Password must contain at least one digit")
                ?.addConstraintViolation()
            return false
        }
        
        // Check for at least one special character
        val specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        if (!value.any { specialChars.contains(it) }) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Password must contain at least one special character")
                ?.addConstraintViolation()
            return false
        }
        
        return true
    }
}