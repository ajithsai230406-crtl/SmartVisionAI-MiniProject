package com.smartvision.ai.career.presentation

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.career.data.CareerRepository
import com.smartvision.ai.career.domain.*
import com.smartvision.ai.data.ScreenContext
import com.smartvision.ai.data.ScreenContextManager
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.domain.model.HistoryRecord
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.utils.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CareerViewModel @Inject constructor(
    private val careerRepository: CareerRepository,
    private val historyRepository: HistoryRepository,
    private val screenContextManager: ScreenContextManager,
    val ocrRecognitionRepository: OcrRecognitionRepository,
    private val ttsManager: TtsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ─── GENERAL STATUS ──────────────────────────────────────────────────────
    private val _resumes = MutableStateFlow<List<ResumeData>>(emptyList())
    val resumes: StateFlow<List<ResumeData>> = _resumes.asStateFlow()

    private val _coverLetters = MutableStateFlow<List<CoverLetterData>>(emptyList())
    val coverLetters: StateFlow<List<CoverLetterData>> = _coverLetters.asStateFlow()

    private val _interviewPreps = MutableStateFlow<List<InterviewPrepData>>(emptyList())
    val interviewPreps: StateFlow<List<InterviewPrepData>> = _interviewPreps.asStateFlow()

    // ─── RESUME BUILDER STATE ────────────────────────────────────────────────
    val builderName = MutableStateFlow("")
    val builderEmail = MutableStateFlow("")
    val builderPhone = MutableStateFlow("")
    val builderAddress = MutableStateFlow("")
    val builderWebsite = MutableStateFlow("")
    val builderGithub = MutableStateFlow("")
    val builderLinkedin = MutableStateFlow("")
    val builderPortfolio = MutableStateFlow("")
    val builderSummary = MutableStateFlow("")
    val builderEducation = MutableStateFlow<List<EducationEntry>>(emptyList())
    val builderSkills = MutableStateFlow<List<String>>(emptyList())
    val builderProjects = MutableStateFlow<List<ProjectEntry>>(emptyList())
    val builderExperience = MutableStateFlow<List<ExperienceEntry>>(emptyList())
    val builderCertifications = MutableStateFlow<List<String>>(emptyList())
    val builderAchievements = MutableStateFlow<List<String>>(emptyList())
    val builderLanguages = MutableStateFlow<List<String>>(emptyList())
    val builderPhotoPath = MutableStateFlow<String?>(null)
    val builderTemplate = MutableStateFlow("ATS_FRIENDLY")

    // ─── RESUME ANALYZER STATE ───────────────────────────────────────────────
    private val _atsResult = MutableStateFlow<AtsAnalysis?>(null)
    val atsResult: StateFlow<AtsAnalysis?> = _atsResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // ─── COVER LETTER GENERATOR STATE ─────────────────────────────────────────
    val letterRole = MutableStateFlow("")
    val letterCompany = MutableStateFlow("")
    val letterExperience = MutableStateFlow("")
    val letterStyle = MutableStateFlow("PROFESSIONAL")
    val letterResumeText = MutableStateFlow("")

    private val _generatedLetter = MutableStateFlow("")
    val generatedLetter: StateFlow<String> = _generatedLetter.asStateFlow()

    private val _isLetterGenerating = MutableStateFlow(false)
    val isLetterGenerating: StateFlow<Boolean> = _isLetterGenerating.asStateFlow()

    // ─── INTERVIEW PREPARATION STATE ──────────────────────────────────────────
    val prepRole = MutableStateFlow("")
    val prepDomain = MutableStateFlow("")
    val prepResumeText = MutableStateFlow("")

    private val _questions = MutableStateFlow<List<InterviewQuestion>>(emptyList())
    val questions: StateFlow<List<InterviewQuestion>> = _questions.asStateFlow()

    private val _isPrepGenerating = MutableStateFlow(false)
    val isPrepGenerating: StateFlow<Boolean> = _isPrepGenerating.asStateFlow()

    val currentQuestionIndex = MutableStateFlow(0)
    val isMockMode = MutableStateFlow(false)
    val userMockAnswerText = MutableStateFlow("")
    val isEvaluatingAnswer = MutableStateFlow(false)

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            careerRepository.getResumes().onSuccess { _resumes.value = it }
            careerRepository.getCoverLetters().onSuccess { _coverLetters.value = it }
            careerRepository.getInterviewPreps().onSuccess { _interviewPreps.value = it }
        }
    }

    // ─── SCREEN CONTEXT BINDINGS ─────────────────────────────────────────────
    fun setScreenContext(
        mode: String,
        resumeText: String = "",
        atsScore: Int = -1,
        letterText: String = "",
        currentQuestion: String = "",
        mockAnswer: String = ""
    ) {
        screenContextManager.updateContext(
            ScreenContext.CareerContext(
                mode = mode,
                resumeSummary = resumeText,
                analysisScore = atsScore,
                coverLetterContent = letterText,
                currentQuestion = currentQuestion,
                userMockAnswer = mockAnswer
            ),
            moduleName = "Career Assistant"
        )
    }

    fun clearContext() {
        screenContextManager.clearContext()
    }

    // ─── RESUME BUILDER METHODS ──────────────────────────────────────────────
    fun loadResumeIntoBuilder(resume: ResumeData) {
        builderName.value = resume.name
        builderEmail.value = resume.email
        builderPhone.value = resume.phone
        builderAddress.value = resume.address
        builderWebsite.value = resume.website
        builderGithub.value = resume.github
        builderLinkedin.value = resume.linkedin
        builderPortfolio.value = resume.portfolio
        builderSummary.value = resume.summary
        builderEducation.value = resume.education
        builderSkills.value = resume.skills
        builderProjects.value = resume.projects
        builderExperience.value = resume.experience
        builderCertifications.value = resume.certifications
        builderAchievements.value = resume.achievements
        builderLanguages.value = resume.languages
        builderPhotoPath.value = resume.photoPath
        builderTemplate.value = resume.templateType
    }

    fun saveResumeDraft(id: String? = null, isDraft: Boolean = true, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val resume = ResumeData(
                id = id ?: UUID.randomUUID().toString(),
                name = builderName.value,
                email = builderEmail.value,
                phone = builderPhone.value,
                address = builderAddress.value,
                website = builderWebsite.value,
                github = builderGithub.value,
                linkedin = builderLinkedin.value,
                portfolio = builderPortfolio.value,
                summary = builderSummary.value,
                education = builderEducation.value,
                skills = builderSkills.value,
                projects = builderProjects.value,
                experience = builderExperience.value,
                certifications = builderCertifications.value,
                achievements = builderAchievements.value,
                languages = builderLanguages.value,
                photoPath = builderPhotoPath.value,
                templateType = builderTemplate.value,
                isDraft = isDraft,
                timestamp = System.currentTimeMillis()
            )
            careerRepository.saveResume(resume).onSuccess {
                loadAllData()
                if (!isDraft) {
                    historyRepository.save(
                        type = "Career",
                        title = "Resume Finalized",
                        details = "${resume.name} - ${resume.templateType.replace("_", " ")}",
                        detailsJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeCareer(
                            com.smartvision.ai.domain.model.CareerHistoryDetail(
                                actionType = "RESUME_BUILDER",
                                title = "Finalized Resume: ${resume.name}",
                                date = formatTimestamp(System.currentTimeMillis()),
                                details = "ATS Score: N/A | Sections filled: ${if (resume.summary.isNotBlank()) 1 else 0} summary, ${resume.skills.size} skills",
                                generatedContent = resume.summary
                            )
                        )
                    )
                }
                onResult(true)
            }.onFailure {
                onResult(false)
            }
        }
    }

    fun deleteResume(id: String) {
        viewModelScope.launch {
            careerRepository.deleteResume(id).onSuccess { loadAllData() }
        }
    }

    // ─── RESUME ANALYZER METHODS ─────────────────────────────────────────────
    fun analyzeResume(text: String) {
        if (text.isBlank() || _isAnalyzing.value) return
        _isAnalyzing.value = true
        setScreenContext(mode = "RESUME_ANALYZER", resumeText = text)
        viewModelScope.launch {
            careerRepository.analyzeResumeAts(text)
                .onSuccess { analysis ->
                    _atsResult.value = analysis
                    _isAnalyzing.value = false
                    setScreenContext(mode = "RESUME_ANALYZER", resumeText = text, atsScore = analysis.score)
                    
                    historyRepository.save(
                        type = "Career",
                        title = "Resume ATS Analyzed",
                        details = "ATS Score: ${analysis.score}%",
                        detailsJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeCareer(
                            com.smartvision.ai.domain.model.CareerHistoryDetail(
                                actionType = "RESUME_ANALYZER",
                                title = "ATS Analysis Score: ${analysis.score}%",
                                date = formatTimestamp(System.currentTimeMillis()),
                                details = "ATS Score: ${analysis.score}% | missing ${analysis.missingSections.size} sections | recommended ${analysis.skillRecommendations.size} skills",
                                atsScore = analysis.score,
                                generatedContent = "Strengths:\n${analysis.strengths.joinToString("\n")}\n\nWeaknesses:\n${analysis.weaknesses.joinToString("\n")}\n\nMissing Sections:\n${analysis.missingSections.joinToString("\n")}\n\nKeyword Optimization:\n${analysis.keywordOptimization.joinToString("\n")}\n\nSuggestions:\n${analysis.rewrittenSuggestions}"
                            )
                        )
                    )
                }
                .onFailure {
                    _isAnalyzing.value = false
                }
        }
    }

    fun extractTextFromPdf(context: Context, uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val pdfRenderer = android.graphics.pdf.PdfRenderer(parcelFileDescriptor)
                val textBuilder = StringBuilder()
                
                val pageCount = pdfRenderer.pageCount
                for (i in 0 until pageCount) {
                    val page = pdfRenderer.openPage(i)
                    val width = 1500
                    val height = (width * 1.4142f).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    
                    val result = ocrRecognitionRepository.recognizeFromBitmap(bitmap, preprocess = false).getOrNull()
                    result?.text?.let { textBuilder.append(it).append("\n") }
                    bitmap.recycle()
                }
                pdfRenderer.close()
                parcelFileDescriptor.close()
                
                withContext(Dispatchers.Main) {
                    onResult(textBuilder.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult("Error parsing PDF resume: ${e.localizedMessage}")
                }
            }
        }
    }

    // ─── COVER LETTER METHODS ────────────────────────────────────────────────
    fun generateLetter() {
        val role = letterRole.value.trim()
        val company = letterCompany.value.trim()
        val exp = letterExperience.value.trim()
        if (role.isBlank() || company.isBlank() || _isLetterGenerating.value) return
        
        _isLetterGenerating.value = true
        viewModelScope.launch {
            careerRepository.generateCoverLetter(
                resumeText = letterResumeText.value,
                jobRole = role,
                companyName = company,
                experienceLevel = exp,
                style = letterStyle.value
            ).onSuccess { letter ->
                _generatedLetter.value = letter
                _isLetterGenerating.value = false
                setScreenContext(mode = "COVER_LETTER", letterText = letter)
                
                // Save cover letter
                val entity = CoverLetterData(
                    id = UUID.randomUUID().toString(),
                    jobRole = role,
                    companyName = company,
                    experienceLevel = exp,
                    letterStyle = letterStyle.value,
                    generatedText = letter,
                    timestamp = System.currentTimeMillis()
                )
                careerRepository.saveCoverLetter(entity).onSuccess { loadAllData() }

                historyRepository.save(
                    type = "Career",
                    title = "Cover Letter Generated",
                    details = "$role at $company",
                    detailsJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeCareer(
                        com.smartvision.ai.domain.model.CareerHistoryDetail(
                            actionType = "COVER_LETTER",
                            title = "Cover Letter: $role at $company",
                            date = formatTimestamp(System.currentTimeMillis()),
                            details = "Style: ${entity.letterStyle} | Experience: ${entity.experienceLevel}",
                            generatedContent = letter
                        )
                    )
                )
            }.onFailure {
                _isLetterGenerating.value = false
            }
        }
    }

    fun deleteCoverLetter(id: String) {
        viewModelScope.launch {
            careerRepository.deleteCoverLetter(id).onSuccess { loadAllData() }
        }
    }

    // ─── INTERVIEW PREP METHODS ──────────────────────────────────────────────
    fun generateQuestions() {
        val role = prepRole.value.trim()
        val domain = prepDomain.value.trim()
        if (role.isBlank() || domain.isBlank() || _isPrepGenerating.value) return
        
        _isPrepGenerating.value = true
        viewModelScope.launch {
            careerRepository.generateInterviewQuestions(
                resumeSummary = prepResumeText.value,
                jobRole = role,
                domain = domain
            ).onSuccess { questionList ->
                _questions.value = questionList
                currentQuestionIndex.value = 0
                _isPrepGenerating.value = false
                
                if (questionList.isNotEmpty()) {
                    setScreenContext(mode = "INTERVIEW_PREP", currentQuestion = questionList[0].question)
                }

                val prepSession = InterviewPrepData(
                    id = UUID.randomUUID().toString(),
                    jobRole = role,
                    domain = domain,
                    questions = questionList,
                    timestamp = System.currentTimeMillis()
                )
                careerRepository.saveInterviewPrep(prepSession).onSuccess { loadAllData() }

                // Serialize questions to JSON array string
                val qArr = JSONArray()
                questionList.forEach { q ->
                    qArr.put(JSONObject().apply {
                        put("question", q.question)
                        put("category", q.category)
                        put("difficulty", q.difficulty)
                        put("suggestedAnswer", q.suggestedAnswer)
                        put("explanation", q.explanation)
                    })
                }

                historyRepository.save(
                    type = "Career",
                    title = "Interview Prep Generated",
                    details = "$role - $domain",
                    detailsJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeCareer(
                        com.smartvision.ai.domain.model.CareerHistoryDetail(
                            actionType = "INTERVIEW_PREP",
                            title = "Interview Prep: $role ($domain)",
                            date = formatTimestamp(System.currentTimeMillis()),
                            details = "Contains ${questionList.size} custom prep questions",
                            interviewQuestionsJson = qArr.toString()
                        )
                    )
                )
            }.onFailure {
                _isPrepGenerating.value = false
            }
        }
    }

    fun submitMockAnswer() {
        val idx = currentQuestionIndex.value
        val list = _questions.value
        if (idx >= list.size || isEvaluatingAnswer.value) return
        val currentQ = list[idx]
        val answer = userMockAnswerText.value.trim()
        if (answer.isBlank()) return

        isEvaluatingAnswer.value = true
        setScreenContext(
            mode = "INTERVIEW_PREP",
            currentQuestion = currentQ.question,
            mockAnswer = answer
        )

        viewModelScope.launch {
            careerRepository.evaluateMockAnswer(
                question = currentQ.question,
                suggestedAnswer = currentQ.suggestedAnswer,
                userAnswer = answer
            ).onSuccess { (score, feedback) ->
                isEvaluatingAnswer.value = false
                val updatedQuestions = _questions.value.toMutableList()
                updatedQuestions[idx] = currentQ.copy(
                    userAnswer = answer,
                    evaluationScore = score,
                    evaluationFeedback = feedback
                )
                _questions.value = updatedQuestions
                userMockAnswerText.value = ""

                // Speak feedback aloud
                speakFeedback("Score: $score percent. $feedback")
            }.onFailure {
                isEvaluatingAnswer.value = false
            }
        }
    }

    fun selectQuestionIndex(idx: Int) {
        val list = _questions.value
        if (idx in list.indices) {
            currentQuestionIndex.value = idx
            userMockAnswerText.value = ""
            setScreenContext(mode = "INTERVIEW_PREP", currentQuestion = list[idx].question)
        }
    }

    fun speakQuestionAloud() {
        val idx = currentQuestionIndex.value
        val list = _questions.value
        if (idx in list.indices) {
            ttsManager.speak(list[idx].question)
        }
    }

    private fun speakFeedback(feedback: String) {
        ttsManager.speak(feedback)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // ─── EXPORT/SHARE FILE GENERATORS ────────────────────────────────────────

    fun exportResumeToPdf(context: Context, resume: ResumeData, filename: String): Uri? {
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }

        var y = 50f
        
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(resume.name.ifBlank { "Resume Name" }, 50f, y, paint)
        y += 25f

        paint.textSize = 10f
        paint.isFakeBoldText = false
        val contactText = "${resume.email} | ${resume.phone} | ${resume.address}"
        canvas.drawText(contactText, 50f, y, paint)
        y += 25f

        if (resume.summary.isNotBlank()) {
            paint.isFakeBoldText = true
            canvas.drawText("Summary", 50f, y, paint)
            y += 15f
            paint.isFakeBoldText = false
            canvas.drawText(resume.summary, 50f, y, paint)
            y += 30f
        }

        if (resume.skills.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Skills", 50f, y, paint)
            y += 15f
            paint.isFakeBoldText = false
            canvas.drawText(resume.skills.joinToString(", "), 50f, y, paint)
            y += 30f
        }

        if (resume.education.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Education", 50f, y, paint)
            y += 15f
            paint.isFakeBoldText = false
            resume.education.forEach { edu ->
                canvas.drawText("${edu.degree} in ${edu.fieldOfStudy} - ${edu.school}", 50f, y, paint)
                y += 15f
                canvas.drawText("${edu.startDate} - ${edu.endDate} | Grade: ${edu.grade}", 50f, y, paint)
                y += 20f
            }
            y += 10f
        }

        if (resume.experience.isNotEmpty()) {
            paint.isFakeBoldText = true
            canvas.drawText("Experience", 50f, y, paint)
            y += 15f
            paint.isFakeBoldText = false
            resume.experience.forEach { exp ->
                canvas.drawText("${exp.role} at ${exp.company} (${exp.location})", 50f, y, paint)
                y += 15f
                canvas.drawText("${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}", 50f, y, paint)
                y += 15f
                canvas.drawText(exp.description, 50f, y, paint)
                y += 25f
            }
        }

        pdfDoc.finishPage(page)

        val resolver = context.contentResolver
        val cv = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SmartVisionAI")
            }
        }
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, cv) ?: return null
        resolver.openOutputStream(uri)?.use { stream ->
            pdfDoc.writeTo(stream)
            stream.flush()
        }
        pdfDoc.close()
        return uri
    }

    fun exportResumeToDocx(context: Context, resume: ResumeData, filename: String): Uri? {
        val markdown = buildString {
            append("# ${resume.name.uppercase()}\n")
            append("${resume.email} | ${resume.phone} | ${resume.address}\n")
            if (resume.linkedin.isNotBlank()) append("LinkedIn: ${resume.linkedin} | ")
            if (resume.github.isNotBlank()) append("GitHub: ${resume.github}\n")
            append("\n---\n\n")

            if (resume.summary.isNotBlank()) {
                append("## PROFESSIONAL SUMMARY\n")
                append("${resume.summary}\n\n")
            }

            if (resume.skills.isNotEmpty()) {
                append("## KEY SKILLS\n")
                append("${resume.skills.joinToString(", ")}\n\n")
            }

            if (resume.experience.isNotEmpty()) {
                append("## PROFESSIONAL EXPERIENCE\n")
                resume.experience.forEach { exp ->
                    append("### ${exp.role} - ${exp.company} (${exp.location})\n")
                    append("*${exp.startDate} - ${if (exp.isCurrent) "Present" else exp.endDate}*\n")
                    append("${exp.description}\n\n")
                }
            }

            if (resume.education.isNotEmpty()) {
                append("## EDUCATION\n")
                resume.education.forEach { edu ->
                    append("### ${edu.degree} in ${edu.fieldOfStudy}\n")
                    append("${edu.school} (${edu.startDate} - ${edu.endDate})\n")
                    if (edu.grade.isNotBlank()) append("Grade: ${edu.grade}\n")
                    append("\n")
                }
            }
        }

        val resolver = context.contentResolver
        val cv = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.docx")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Downloads.RELATIVE_PATH, "Documents/SmartVisionAI")
            }
        }
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, cv) ?: return null
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(markdown.toByteArray())
            stream.flush()
        }
        return uri
    }

    fun exportCoverLetterToPdf(context: Context, letterText: String, filename: String): Uri? {
        val pdfDoc = android.graphics.pdf.PdfDocument()
        val width = 595
        val height = 842
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }

        var y = 60f
        val lines = letterText.split("\n")
        lines.forEach { line ->
            if (y > 780f) return@forEach // simple overflow protection
            canvas.drawText(line, 55f, y, paint)
            y += 16f
        }
        pdfDoc.finishPage(page)

        val resolver = context.contentResolver
        val cv = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "$filename.pdf")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Documents/SmartVisionAI")
            }
        }
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }
        val uri = resolver.insert(collection, cv) ?: return null
        resolver.openOutputStream(uri)?.use { stream ->
            pdfDoc.writeTo(stream)
            stream.flush()
        }
        pdfDoc.close()
        return uri
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeaking()
    }
}
