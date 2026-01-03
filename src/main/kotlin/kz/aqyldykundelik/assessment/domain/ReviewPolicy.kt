package kz.aqyldykundelik.assessment.domain

enum class ReviewPolicy {
    NEVER,              // Никогда не показывать разбор
    AFTER_SUBMIT,       // Показывать сразу после сдачи попытки
    AFTER_CLOSE_WINDOW  // Показывать только после closesAt
}
