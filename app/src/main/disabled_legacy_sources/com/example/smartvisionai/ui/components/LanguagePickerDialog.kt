package com.example.smartvisionai.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

data class LanguageOption(
    val name:       String,
    val nativeName: String,
    val localeTag:  String,
    val flag:       String
)

val allLanguages = listOf(
    LanguageOption("English",               "English",          "en",    "🇬🇧"),
    LanguageOption("Hindi",                 "हिन्दी",             "hi",    "🇮🇳"),
    LanguageOption("Spanish",               "Español",           "es",    "🇪🇸"),
    LanguageOption("French",                "Français",          "fr",    "🇫🇷"),
    LanguageOption("German",                "Deutsch",           "de",    "🇩🇪"),
    LanguageOption("Arabic",                "العربية",            "ar",    "🇸🇦"),
    LanguageOption("Chinese (Simplified)",  "中文 (简体)",         "zh-CN", "🇨🇳"),
    LanguageOption("Chinese (Traditional)", "中文 (繁體)",         "zh-TW", "🇹🇼"),
    LanguageOption("Japanese",              "日本語",              "ja",    "🇯🇵"),
    LanguageOption("Korean",                "한국어",              "ko",    "🇰🇷"),
    LanguageOption("Portuguese",            "Português",         "pt",    "🇧🇷"),
    LanguageOption("Russian",               "Русский",           "ru",    "🇷🇺"),
    LanguageOption("Italian",               "Italiano",          "it",    "🇮🇹"),
    LanguageOption("Dutch",                 "Nederlands",        "nl",    "🇳🇱"),
    LanguageOption("Polish",                "Polski",            "pl",    "🇵🇱"),
    LanguageOption("Swedish",               "Svenska",           "sv",    "🇸🇪"),
    LanguageOption("Turkish",               "Türkçe",            "tr",    "🇹🇷"),
    LanguageOption("Indonesian",            "Bahasa Indonesia",  "id",    "🇮🇩"),
    LanguageOption("Malay",                 "Bahasa Melayu",     "ms",    "🇲🇾"),
    LanguageOption("Thai",                  "ภาษาไทย",            "th",    "🇹🇭"),
    LanguageOption("Vietnamese",            "Tiếng Việt",        "vi",    "🇻🇳"),
    LanguageOption("Bengali",               "বাংলা",              "bn",    "🇧🇩"),
    LanguageOption("Tamil",                 "தமிழ்",              "ta",    "🇮🇳"),
    LanguageOption("Telugu",                "తెలుగు",             "te",    "🇮🇳"),
    LanguageOption("Marathi",               "मराठी",              "mr",    "🇮🇳"),
    LanguageOption("Gujarati",              "ગુજરાતી",            "gu",    "🇮🇳"),
    LanguageOption("Kannada",               "ಕನ್ನಡ",              "kn",    "🇮🇳"),
    LanguageOption("Malayalam",             "മലയാളം",             "ml",    "🇮🇳"),
    LanguageOption("Punjabi",               "ਪੰਜਾਬੀ",             "pa",    "🇮🇳"),
    LanguageOption("Urdu",                  "اردو",               "ur",    "🇵🇰"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) allLanguages
        else allLanguages.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.nativeName.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = SurfaceDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CardBorder)
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                "Select Language",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier  = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                    decorationBox = { inner ->
                        if (query.isEmpty())
                            Text("Search language…", fontSize = 14.sp, color = TextMuted)
                        inner()
                    },
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                items(filtered, key = { it.localeTag }) { lang ->
                    val isSelected = lang.name == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CyanAccent.copy(.10f) else Color.Transparent)
                            .clickable { onSelect(lang.name) }
                            .padding(horizontal = 12.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Flag
                        Text(lang.flag, fontSize = 20.sp)

                        // Names
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                lang.name,
                                fontSize   = 14.sp,
                                color      = if (isSelected) CyanAccent else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                lang.nativeName,
                                fontSize = 11.sp,
                                color    = TextMuted
                            )
                        }

                        // Checkmark
                        if (isSelected) {
                            Icon(Icons.Default.Check, null,
                                tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
