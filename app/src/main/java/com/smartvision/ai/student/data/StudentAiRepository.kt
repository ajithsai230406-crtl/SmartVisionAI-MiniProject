package com.smartvision.ai.student.data

import com.smartvision.ai.data.local.StudentHelperHistoryDao
import com.smartvision.ai.data.local.StudentHelperHistoryEntity
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.OpenRouterManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentAiRepository @Inject constructor(
    private val openRouterManager: OpenRouterManager,
    private val historyDao: StudentHelperHistoryDao,
    private val firebaseRepository: FirebaseRepository,
    private val authRepository: AuthRepository
) {

    suspend fun generateStudentHelperContent(
        prompt: String,
        featureType: String,
        queryLabel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val rawText = openRouterManager.generateContent(
                prompt = prompt,
                modelName = "openrouter/auto"
            )

            // Save to Room DB history
            val entity = StudentHelperHistoryEntity(
                query = queryLabel,
                response = rawText,
                timestamp = System.currentTimeMillis(),
                featureType = featureType
            )
            historyDao.insert(entity)

            // Automatically save to Firestore StudyHistory collection
            val uid = authRepository.getCurrentUid()
            firebaseRepository.saveStudyHistory(
                uid = uid,
                question = queryLabel,
                answer = rawText,
                subject = featureType,
                timestamp = entity.timestamp
            )

            rawText
        }
    }

    suspend fun getAiAnswer(question: String): Result<String> {
        val queryLower = question.lowercase()
        
        // Detect if it's a math expression (numbers, operators, equals)
        val isMathExpression = question.trim().matches(Regex("^[\\d\\s+\\-*/^()=.,%√πe]+$"))
        
        val subject = when {
            isMathExpression || 
            queryLower.contains("math") || queryLower.contains("equation") || queryLower.contains("calculate") || 
            queryLower.contains("solve") || queryLower.contains("matrix") || queryLower.contains("integral") || 
            queryLower.contains("derivative") || queryLower.contains("sum") || queryLower.contains("fraction") || 
            queryLower.contains("algebra") || queryLower.contains("geometry") || queryLower.contains("trigonometry") || 
            queryLower.contains("theorem") || queryLower.contains("logarithm") || queryLower.contains("calculus") || 
            queryLower.contains("limit") || queryLower.contains("vector") || queryLower.contains("multiply") || 
            queryLower.contains("divide") || queryLower.contains("subtract") || queryLower.contains("add") || 
            queryLower.contains("statistics") || queryLower.contains("probability") || queryLower.contains("area of") || 
            queryLower.contains("perimeter") || queryLower.contains("volume") || queryLower.contains("angle") || 
            queryLower.contains("triangle") || queryLower.contains("circle") || queryLower.contains("rectangle") -> "Mathematics"
            
            queryLower.contains("physics") || queryLower.contains("force") || queryLower.contains("gravity") || 
            queryLower.contains("velocity") || queryLower.contains("energy") || queryLower.contains("quantum") || 
            queryLower.contains("speed") || queryLower.contains("acceleration") || queryLower.contains("mass") || 
            queryLower.contains("friction") || queryLower.contains("momentum") || queryLower.contains("newton") || 
            queryLower.contains("relativity") || queryLower.contains("electromagnetism") || queryLower.contains("thermodynamics") || 
            queryLower.contains("optics") || queryLower.contains("wave") || queryLower.contains("frequency") || 
            queryLower.contains("sound") || queryLower.contains("light") || queryLower.contains("nuclear") || 
            queryLower.contains("work done") || queryLower.contains("kilogram") || queryLower.contains("joule") || 
            queryLower.contains("watt") -> "Physics"
            
            queryLower.contains("chemistry") || queryLower.contains("acid") || queryLower.contains("reaction") || 
            queryLower.contains("molecule") || queryLower.contains("periodic") || queryLower.contains("bond") || 
            queryLower.contains("element") || queryLower.contains("atom") || queryLower.contains("solvent") || 
            queryLower.contains("catalyst") || queryLower.contains("ph level") || queryLower.contains("hydroxide") || 
            queryLower.contains("compound") || queryLower.contains("gas law") || queryLower.contains("molar") || 
            queryLower.contains("proton") || queryLower.contains("neutron") || queryLower.contains("electron") || 
            queryLower.contains("valency") -> "Chemistry"
            
            queryLower.contains("code") || queryLower.contains("programming") || queryLower.contains("kotlin") || 
            queryLower.contains("java") || queryLower.contains("python") || queryLower.contains("algorithm") || 
            queryLower.contains("class") || queryLower.contains("function") || queryLower.contains("variable") || 
            queryLower.contains("database") || queryLower.contains("sql") || queryLower.contains("html") || 
            queryLower.contains("css") || queryLower.contains("javascript") || queryLower.contains("binary") || 
            queryLower.contains("hexadecimal") || queryLower.contains("compiler") || queryLower.contains("recursion") || 
            queryLower.contains("array") || queryLower.contains("list") || queryLower.contains("map") || 
            queryLower.contains("loop") || queryLower.contains("while") || queryLower.contains("if statement") -> "Programming"
            
            else -> "General"
        }

        val masterPrompt = """
            You are an expert academic tutor and homework solver. You work exactly like Google Lens homework feature.

            Solve the following homework question step by step. You MUST format your response EXACTLY like this:

            SOLUTION:
            [Explain the approach and show step-by-step work. Number each step clearly as "1.", "2.", "3." etc. Each step should show the calculation or reasoning on the same line.]

            ANSWER:
            [Give ONLY the final answer value/result here. Keep it short — just the number, formula result, or brief conclusion. No explanation here.]

            IMPORTANT RULES:
            - For math: Apply order of operations (PEMDAS/BODMAS) correctly
            - Show every calculation step clearly with the intermediate result
            - Each numbered step should be on its own line
            - The ANSWER section must contain ONLY the final result (e.g. "8.5" or "x = 7" or "42")
            - Do NOT use markdown formatting like ** or ## or ### 
            - Do NOT add headers like "Step-by-step explanation" — just use the SOLUTION: and ANSWER: format
            - Keep explanations concise and clear

            Question:
            $question
        """.trimIndent()

        return generateStudentHelperContent(
            prompt = masterPrompt,
            featureType = subject,
            queryLabel = question
        )
    }
}
