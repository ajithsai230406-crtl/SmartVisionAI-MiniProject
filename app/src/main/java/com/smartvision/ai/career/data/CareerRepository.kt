package com.smartvision.ai.career.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.smartvision.ai.data.repository.OpenRouterManager
import com.smartvision.ai.career.domain.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CareerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val openRouterManager: OpenRouterManager
) {
    private fun getUserId(): String = auth.currentUser?.uid ?: "guest_uid"

    // ─── RESUMES ──────────────────────────────────────────────────────────────
    suspend fun saveResume(resume: ResumeData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val docId = resume.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val data = resume.copy(id = docId, timestamp = System.currentTimeMillis())
            firestore.collection("users").document(uid)
                .collection("resumes").document(docId).set(data).await()
            Unit
        }
    }

    suspend fun getResumes(isDraftOnly: Boolean = false): Result<List<ResumeData>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val query = firestore.collection("users").document(uid).collection("resumes")
            val snap = query.get().await()
            val list = snap.documents.mapNotNull { doc ->
                doc.toObject(ResumeData::class.java)
            }.sortedByDescending { it.timestamp }
            if (isDraftOnly) list.filter { it.isDraft } else list
        }
    }

    suspend fun deleteResume(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            firestore.collection("users").document(uid)
                .collection("resumes").document(id).delete().await()
            Unit
        }
    }

    // ─── COVER LETTERS ─────────────────────────────────────────────────────────
    suspend fun saveCoverLetter(letter: CoverLetterData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val docId = letter.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val data = letter.copy(id = docId, timestamp = System.currentTimeMillis())
            firestore.collection("users").document(uid)
                .collection("cover_letters").document(docId).set(data).await()
            Unit
        }
    }

    suspend fun getCoverLetters(): Result<List<CoverLetterData>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val snap = firestore.collection("users").document(uid).collection("cover_letters").get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(CoverLetterData::class.java)
            }.sortedByDescending { it.timestamp }
        }
    }

    suspend fun deleteCoverLetter(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            firestore.collection("users").document(uid)
                .collection("cover_letters").document(id).delete().await()
            Unit
        }
    }

    // ─── INTERVIEW PREP ────────────────────────────────────────────────────────
    suspend fun saveInterviewPrep(prep: InterviewPrepData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val docId = prep.id.ifBlank { java.util.UUID.randomUUID().toString() }
            val data = prep.copy(id = docId, timestamp = System.currentTimeMillis())
            firestore.collection("users").document(uid)
                .collection("interview_prep").document(docId).set(data).await()
            Unit
        }
    }

    suspend fun getInterviewPreps(): Result<List<InterviewPrepData>> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = getUserId()
            val snap = firestore.collection("users").document(uid).collection("interview_prep").get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(InterviewPrepData::class.java)
            }.sortedByDescending { it.timestamp }
        }
    }

    // ─── GEMINI AI GENERATIVE OPERATIONS ─────────────────────────────────────────

    suspend fun analyzeResumeAts(resumeText: String): Result<AtsAnalysis> = withContext(Dispatchers.Default) {
        runCatching {
            val prompt = """
                You are an expert ATS (Applicant Tracking System) Analyzer and technical recruiter.
                Analyze the following resume content and output a strictly valid JSON object matching the schema below.
                Ensure there is no extra text or wrapping other than the raw JSON payload. Do not use Markdown code block wrappers.
                
                RESUME CONTENT:
                $resumeText
                
                JSON SCHEMA TO RETURN:
                {
                  "score": 75,
                  "grammarIssues": ["Grammar recommendation 1", "Grammar recommendation 2"],
                  "formattingIssues": ["Layout recommendation 1", "Layout recommendation 2"],
                  "missingSections": ["Certifications", "Projects"],
                  "keywordOptimization": ["Docker", "Kubernetes", "CI/CD"],
                  "skillRecommendations": ["Kotlin", "Jetpack Compose"],
                  "strengths": ["Clear work accomplishments", "Good technical skills list"],
                  "weaknesses": ["Objective summary could be more impactful", "Certifications section missing"],
                  "rewrittenSuggestions": "A concise rewrite of the professional summary to improve ATS scoring..."
                }
            """.trimIndent()

            val raw = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
            val clean = cleanJson(raw)
            val json = JSONObject(clean)
            
            AtsAnalysis(
                score = json.optInt("score", 50),
                grammarIssues = json.optStringList("grammarIssues"),
                formattingIssues = json.optStringList("formattingIssues"),
                missingSections = json.optStringList("missingSections"),
                keywordOptimization = json.optStringList("keywordOptimization"),
                skillRecommendations = json.optStringList("skillRecommendations"),
                strengths = json.optStringList("strengths"),
                weaknesses = json.optStringList("weaknesses"),
                rewrittenSuggestions = json.optString("rewrittenSuggestions", "")
            )
        }
    }

    suspend fun generateCoverLetter(
        resumeText: String,
        jobRole: String,
        companyName: String,
        experienceLevel: String,
        style: String
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val prompt = """
                Write a tailored, professional cover letter for the role of "$jobRole" at company "$companyName".
                Experience level: $experienceLevel
                Tone / Style: $style
                
                Candidate Resume Info (if available, incorporate relevant skills and projects, but do not hallucinate detailed timelines):
                $resumeText
                
                Format the cover letter beautifully with clear paragraphs, standard layout (Dear Hiring Manager, ..., Sincerely).
            """.trimIndent()

            openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
        }
    }

    suspend fun generateInterviewQuestions(
        resumeSummary: String,
        jobRole: String,
        domain: String
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.Default) {
        runCatching {
            val prompt = """
                Generate 6 interview questions based on the candidate's resume/skills, job role, and domain.
                The questions should include a mixture of Technical, Behavioral, HR, and Aptitude categories.
                Output a strictly valid JSON array of objects. Do not include markdown code block wrappers (like ```json ... ```) or any trailing explanations.
                
                Job Role: $jobRole
                Domain/Industry: $domain
                Resume Skills/Info:
                $resumeSummary
                
                JSON Schema Array:
                [
                  {
                    "question": "Question text...",
                    "category": "TECHNICAL", // Must be one of: TECHNICAL, BEHAVIORAL, HR, APTITUDE
                    "difficulty": "MEDIUM", // Must be one of: EASY, MEDIUM, HARD
                    "suggestedAnswer": "A sample recommended response...",
                    "explanation": "Brief explanation of what the interviewer is looking for in this question."
                  }
                ]
            """.trimIndent()

            val raw = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
            val clean = cleanJson(raw)
            val arr = JSONArray(clean)
            val list = mutableListOf<InterviewQuestion>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    InterviewQuestion(
                        id = java.util.UUID.randomUUID().toString(),
                        question = obj.getString("question"),
                        category = obj.optString("category", "TECHNICAL").uppercase(),
                        difficulty = obj.optString("difficulty", "MEDIUM").uppercase(),
                        suggestedAnswer = obj.optString("suggestedAnswer", ""),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
            list
        }
    }

    suspend fun evaluateMockAnswer(
        question: String,
        suggestedAnswer: String,
        userAnswer: String
    ): Result<Pair<Int, String>> = withContext(Dispatchers.Default) {
        runCatching {
            val prompt = """
                Evaluate the following candidate response to an interview question.
                Provide a score from 0 to 100 based on accuracy, relevance, and communication, and a short constructive feedback text.
                Return your answer strictly in the format: "SCORE: [number] | FEEDBACK: [feedback text]".
                
                Question: $question
                Suggested Standard Answer: $suggestedAnswer
                Candidate Response: $userAnswer
            """.trimIndent()

            val raw = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
            val scorePart = raw.substringAfter("SCORE:").substringBefore("|").trim()
            val feedbackPart = raw.substringAfter("FEEDBACK:").trim()
            val score = scorePart.toIntOrNull() ?: 50
            Pair(score, feedbackPart.ifBlank { raw })
        }
    }

    private fun cleanJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            clean = clean.substringAfter("json").substringAfter("\n")
            if (clean.endsWith("```")) {
                clean = clean.substringBeforeLast("```")
            }
        }
        return clean.trim()
    }

    private fun JSONObject.optStringList(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.optString(i))
        }
        return list
    }
}
