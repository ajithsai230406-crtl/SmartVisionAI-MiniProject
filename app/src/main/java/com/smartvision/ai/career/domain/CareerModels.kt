package com.smartvision.ai.career.domain

data class ResumeData(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val website: String = "",
    val github: String = "",
    val linkedin: String = "",
    val portfolio: String = "",
    val summary: String = "",
    val education: List<EducationEntry> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<ProjectEntry> = emptyList(),
    val experience: List<ExperienceEntry> = emptyList(),
    val certifications: List<String> = emptyList(),
    val achievements: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val photoPath: String? = null,
    val templateType: String = "ATS_FRIENDLY", // ATS_FRIENDLY, PROFESSIONAL, MODERN, FRESHER, INTERNSHIP
    val isDraft: Boolean = true,
    val timestamp: Long = 0L
)

data class EducationEntry(
    val school: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val grade: String = ""
)

data class ProjectEntry(
    val title: String = "",
    val description: String = "",
    val technologies: List<String> = emptyList(),
    val url: String = ""
)

data class ExperienceEntry(
    val company: String = "",
    val role: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = "",
    val isCurrent: Boolean = false
)

data class AtsAnalysis(
    val score: Int = 0,
    val grammarIssues: List<String> = emptyList(),
    val formattingIssues: List<String> = emptyList(),
    val missingSections: List<String> = emptyList(),
    val keywordOptimization: List<String> = emptyList(),
    val skillRecommendations: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val rewrittenSuggestions: String = ""
)

data class CoverLetterData(
    val id: String = "",
    val resumeId: String = "",
    val jobRole: String = "",
    val companyName: String = "",
    val experienceLevel: String = "",
    val letterStyle: String = "PROFESSIONAL", // PROFESSIONAL, MODERN, FORMAL
    val generatedText: String = "",
    val timestamp: Long = 0L
)

data class InterviewPrepData(
    val id: String = "",
    val jobRole: String = "",
    val domain: String = "",
    val questions: List<InterviewQuestion> = emptyList(),
    val timestamp: Long = 0L
)

data class InterviewQuestion(
    val id: String = "",
    val question: String = "",
    val category: String = "TECHNICAL", // TECHNICAL, BEHAVIORAL, HR, APTITUDE
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val suggestedAnswer: String = "",
    val explanation: String = "",
    val userAnswer: String = "",
    val evaluationScore: Int = -1, // 0 - 100
    val evaluationFeedback: String = ""
)
