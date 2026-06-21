package com.smartvision.ai.student.data

import android.graphics.Bitmap
import com.smartvision.ai.data.local.StudentHelperHistoryDao
import com.smartvision.ai.data.local.StudentHelperHistoryEntity
import com.smartvision.ai.data.repository.AuthRepository
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
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
    private val authRepository: AuthRepository,
    private val historyRepository: HistoryRepository
) {

    suspend fun transcribeImage(bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = """
                You are an advanced academic OCR engine with expertise in mathematics, science, and educational materials.
                Your task is to extract ALL text, equations, formulas, and question content from this image with maximum precision.

                TRANSCRIPTION RULES:
                1. Output ONLY the raw transcribed text. No commentary, explanation, preamble, or extra text.
                2. Preserve the EXACT structure:
                   - Question numbers (e.g., "Q1.", "1.", "(a)", etc.)
                   - Multiple choice options (A), (B), (C), (D) on separate lines
                   - Paragraph breaks and line breaks as they appear
                   - Multi-line equations — keep each line separate
                3. Mathematical notation:
                   - Exponents: x^2, x^n
                   - Subscripts: H_2O, a_n
                   - Fractions: (numerator)/(denominator) e.g., (x+1)/(x-1)
                   - Square roots: sqrt(x), cube root: cbrt(x), nth root: root(n,x)
                   - Integrals: integral(a to b) f(x) dx
                   - Derivatives: d/dx, dy/dx, partial f/partial x
                   - Summation: sum(i=1 to n) a_i
                   - Limits: lim(x->0) f(x)
                   - Matrices: [[a,b],[c,d]]
                   - Greek letters: alpha, beta, gamma, theta, pi, sigma, delta, lambda, omega
                   - Operators: ±, ×, ÷, ≤, ≥, ≠, ≈, ∞, ∈, ∀, ∃
                4. For MCQs: Clearly mark each option on its own line with its label.
                5. For diagrams/figures: Describe labeled values (e.g., "angle = 30°", "F = 10N").
                6. IGNORE: watermarks, page numbers, headers/footers, background noise, smudges.
                7. For handwritten text: Best-effort transcription, preserving structure.
                8. If multiple questions are visible, transcribe ALL of them separated by blank lines.
            """.trimIndent()

            openRouterManager.generateContent(
                prompt = prompt,
                image = bitmap,
                modelName = "openrouter/auto"
            )
        }
    }


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

            // Save to main app History tab as well
            val studentHistJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeStudent(
                com.smartvision.ai.domain.model.StudentHistoryDetail(
                    question = queryLabel,
                    answer = rawText,
                    subject = featureType
                )
            )
            historyRepository.save("Student", "📚 $featureType: ${queryLabel.take(50)}", rawText.take(220), studentHistJson)

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
            queryLower.contains("triangle") || queryLower.contains("circle") || queryLower.contains("rectangle") ||
            queryLower.contains("polynomial") || queryLower.contains("quadratic") || queryLower.contains("factorial") ||
            queryLower.contains("permutation") || queryLower.contains("combination") -> "Mathematics"
            
            queryLower.contains("physics") || queryLower.contains("force") || queryLower.contains("gravity") || 
            queryLower.contains("velocity") || queryLower.contains("energy") || queryLower.contains("quantum") || 
            queryLower.contains("speed") || queryLower.contains("acceleration") || queryLower.contains("mass") || 
            queryLower.contains("friction") || queryLower.contains("momentum") || queryLower.contains("newton") || 
            queryLower.contains("relativity") || queryLower.contains("electromagnetism") || queryLower.contains("thermodynamics") || 
            queryLower.contains("optics") || queryLower.contains("wave") || queryLower.contains("frequency") || 
            queryLower.contains("sound") || queryLower.contains("light") || queryLower.contains("nuclear") || 
            queryLower.contains("work done") || queryLower.contains("kilogram") || queryLower.contains("joule") || 
            queryLower.contains("watt") || queryLower.contains("voltage") || queryLower.contains("current") ||
            queryLower.contains("resistance") || queryLower.contains("capacitor") -> "Physics"
            
            queryLower.contains("chemistry") || queryLower.contains("acid") || queryLower.contains("reaction") || 
            queryLower.contains("molecule") || queryLower.contains("periodic") || queryLower.contains("bond") || 
            queryLower.contains("element") || queryLower.contains("atom") || queryLower.contains("solvent") || 
            queryLower.contains("catalyst") || queryLower.contains("ph level") || queryLower.contains("hydroxide") || 
            queryLower.contains("compound") || queryLower.contains("gas law") || queryLower.contains("molar") || 
            queryLower.contains("proton") || queryLower.contains("neutron") || queryLower.contains("electron") || 
            queryLower.contains("valency") || queryLower.contains("oxidation") || queryLower.contains("reduction") ||
            queryLower.contains("titration") || queryLower.contains("electrolysis") -> "Chemistry"
            
            queryLower.contains("biology") || queryLower.contains("cell") || queryLower.contains("dna") || 
            queryLower.contains("rna") || queryLower.contains("gene") || queryLower.contains("protein") || 
            queryLower.contains("enzyme") || queryLower.contains("photosynthesis") || queryLower.contains("respiration") || 
            queryLower.contains("evolution") || queryLower.contains("ecosystem") || queryLower.contains("organ") || 
            queryLower.contains("tissue") || queryLower.contains("mitosis") || queryLower.contains("meiosis") || 
            queryLower.contains("chromosome") || queryLower.contains("virus") || queryLower.contains("bacteria") ||
            queryLower.contains("anatomy") || queryLower.contains("species") || queryLower.contains("habitat") -> "Biology"
            
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

        // Detect if it's an MCQ
        val isMcq = queryLower.contains("(a)") || queryLower.contains("(b)") || queryLower.contains("(c)") ||
                    queryLower.contains("(d)") || queryLower.contains("option a") || queryLower.contains("option b") ||
                    Regex("\\b[abcd]\\)").containsMatchIn(queryLower) ||
                    Regex("\\b[ABCD]\\.\\s").containsMatchIn(question)

        val mcqInstruction = if (isMcq) """
            
            MCQ DETECTION: This is a multiple-choice question.
            - Identify the CORRECT option clearly.
            - In the SOLUTION section, explain WHY the correct option is right and why others are wrong.
            - Add a MCQ_ANSWER section at the end.
            
            MCQ_ANSWER: [Letter]) [Full text of correct option]
        """.trimIndent() else ""

        val masterPrompt = """
            You are an expert academic tutor and homework solver. You work exactly like Google Lens homework feature.
            You are solving a $subject question.

            Solve the following homework question step by step. You MUST format your response EXACTLY like this:

            SOLUTION:
            [Explain the approach and show step-by-step work. Number each step clearly as "1.", "2.", "3." etc. Each step should show the calculation or reasoning on the same line.]

            ANSWER:
            [Give ONLY the final answer value/result here. Keep it short — just the number, formula result, or brief conclusion. No explanation here.]

            KEY_FORMULA:
            [List the key formula(s) used in solving this problem. Write N/A if no specific formula was used.]

            $mcqInstruction

            IMPORTANT RULES:
            - For math: Apply order of operations (PEMDAS/BODMAS) correctly. Show every intermediate calculation.
            - For science: Include relevant formulas, units (SI), and real-world context.
            - For MCQs: Clearly state the correct option and explain why other options are incorrect.
            - For multi-part questions: Solve each part separately with clear labels (a), (b), (c) etc.
            - For theory questions: Give structured, exam-ready answers with definitions and key points.
            - Show every calculation step clearly with the intermediate result.
            - Each numbered step should be on its own line.
            - The ANSWER section must contain ONLY the final result (e.g. "8.5" or "x = 7" or "42").
            - Do NOT use markdown formatting like ** or ## or ###.
            - Do NOT add headers like "Step-by-step explanation" — just use the structured format above.
            - Keep explanations concise and clear.
            - For fractions use (a)/(b) notation, for exponents use ^ notation.

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
