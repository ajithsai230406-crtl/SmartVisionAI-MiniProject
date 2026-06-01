package com.smartvision.ai.student.domain

// ─── Domain Models ────────────────────────────────────────────────────────────

enum class Subject(val displayName: String, val emoji: String, val systemPrompt: String) {
    MATHEMATICS(
        "Mathematics", "📐",
        "You are an expert math tutor. Solve problems step-by-step with clear notation. " +
        "Use plain text for formulas (e.g. x^2, sqrt(x), pi). Provide the final answer clearly. " +
        "Add a 'Quick Tip' or shortcut at the end when relevant."
    ),
    PHYSICS(
        "Physics", "⚛️",
        "You are an expert physics tutor. Explain concepts clearly with real-world analogies. " +
        "Show all formulas in plain text. Label every step. Mention SI units. " +
        "End with a 'Key Formula' summary."
    ),
    CHEMISTRY(
        "Chemistry", "🧪",
        "You are an expert chemistry tutor. Explain reactions and concepts step-by-step. " +
        "Write chemical equations clearly in plain text. " +
        "Note safety information where relevant. End with a memory trick."
    ),
    PROGRAMMING(
        "Programming", "💻",
        "You are an expert programming tutor. Explain code concepts clearly. " +
        "Provide code examples in plain text using proper indentation. " +
        "Explain each line. Suggest best practices and common pitfalls. " +
        "Mention time/space complexity for algorithms."
    ),
    GENERAL(
        "General", "🌐",
        "You are a helpful academic tutor. Answer questions clearly and concisely. " +
        "Use bullet points for multi-part answers. Provide examples. " +
        "Keep explanations suitable for a student audience."
    )
}

/** A single message in the chat thread */
data class ChatMessage(
    val id:        Long   = System.currentTimeMillis(),
    val text:      String,
    val isUser:    Boolean,
    val subject:   Subject? = null,
    val isLoading: Boolean  = false,
    val hasError:  Boolean  = false
)

/** A saved note / solution */
data class StudentNote(
    val id:          Long   = System.currentTimeMillis(),
    val question:    String,
    val answer:      String,
    val subject:     Subject,
    val timestamp:   Long   = System.currentTimeMillis()
)

/** AI response parsed from Gemini */
data class AiSolution(
    val question:      String,
    val mainAnswer:    String,
    val steps:         List<String>,
    val keyFormula:    String?,
    val quickTip:      String?,
    val practiceQ:     List<String>
)

/** Overall UI state */
sealed class StudentState {
    object Idle     : StudentState()
    object Thinking : StudentState()
    data class Success(val solution: AiSolution) : StudentState()
    data class Error(val message: String)        : StudentState()
}

// ─── Prompt builder ───────────────────────────────────────────────────────────
object PromptBuilder {
    fun build(question: String, subject: Subject, mode: ExplainMode): String {
        val modeInstruction = when (mode) {
            ExplainMode.SOLVE    -> "Solve this: "
            ExplainMode.EXPLAIN  -> "Explain in simple terms: "
            ExplainMode.EXAMPLES -> "Give 2 worked examples for: "
            ExplainMode.PRACTICE -> "Generate 3 practice questions similar to: "
            ExplainMode.SIMPLIFY -> "Explain as if I'm 10 years old: "
        }
        return """${subject.systemPrompt}

$modeInstruction $question

Respond in this EXACT structure:
ANSWER: [main answer / solution]
STEPS:
1. [step one]
2. [step two]
3. [step three]
FORMULA: [key formula or N/A]
TIP: [quick tip / memory trick or N/A]
PRACTICE:
- [practice question 1]
- [practice question 2]"""
    }

    /** Parse Gemini's structured response into AiSolution */
    fun parse(raw: String, question: String, subject: Subject): AiSolution {
        fun extract(tag: String): String {
            val pattern = Regex("$tag:\\s*(.+?)(?=\\n[A-Z]+:|$)", RegexOption.DOT_MATCHES_ALL)
            return pattern.find(raw)?.groupValues?.get(1)?.trim() ?: ""
        }

        val answer   = extract("ANSWER").ifBlank { raw.lines().take(3).joinToString(" ") }
        val stepsRaw = extract("STEPS")
        val formula  = extract("FORMULA").takeIf { it.isNotBlank() && it != "N/A" }
        val tip      = extract("TIP").takeIf { it.isNotBlank() && it != "N/A" }
        val practiceRaw = extract("PRACTICE")

        val steps = stepsRaw.lines()
            .filter { it.trim().matches(Regex("\\d+\\..*")) || it.trim().startsWith("-") }
            .map { it.trim().replaceFirst(Regex("^\\d+\\.\\s*|-\\s*"), "") }
            .filter { it.isNotBlank() }

        val practice = practiceRaw.lines()
            .filter { it.trim().startsWith("-") }
            .map { it.trim().removePrefix("- ") }
            .filter { it.isNotBlank() }

        return AiSolution(
            question   = question,
            mainAnswer = answer,
            steps      = steps.ifEmpty {
                raw.lines().filter { it.isNotBlank() }.drop(1).take(5)
            },
            keyFormula = formula,
            quickTip   = tip,
            practiceQ  = practice
        )
    }
}

enum class ExplainMode(val label: String, val emoji: String) {
    SOLVE    ("Solve",       "🔢"),
    EXPLAIN  ("Explain",     "💡"),
    EXAMPLES ("Examples",    "📝"),
    PRACTICE ("Practice",    "🏋️"),
    SIMPLIFY ("Simplify",    "🧒")
}
