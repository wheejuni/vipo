package com.hightemplar.vipo.validation

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ConversationQuestionValidator::class])
@MustBeDocumented
annotation class ValidConversationQuestion(
    val message: String = "Question contains inappropriate content or is too short",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class ConversationQuestionValidator : ConstraintValidator<ValidConversationQuestion, String> {
    
    private val inappropriateWords = setOf(
        // Add inappropriate words that should be filtered
        "spam", "test123", "asdf"
    )
    
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value.isNullOrBlank()) {
            return false
        }
        
        // Check minimum length (more than just whitespace)
        if (value.trim().length < 3) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Question must be at least 3 characters long")
                ?.addConstraintViolation()
            return false
        }
        
        // Check for inappropriate content
        val lowerCaseValue = value.lowercase()
        if (inappropriateWords.any { lowerCaseValue.contains(it) }) {
            context?.disableDefaultConstraintViolation()
            context?.buildConstraintViolationWithTemplate("Question contains inappropriate content")
                ?.addConstraintViolation()
            return false
        }
        
        return true
    }
}