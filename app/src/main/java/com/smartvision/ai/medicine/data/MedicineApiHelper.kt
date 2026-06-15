package com.smartvision.ai.medicine.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MedicineApiHelper {

    private suspend fun getHttpResponse(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("Accept", "application/json")
        
        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            sb.toString()
        } else {
            throw Exception("HTTP Error: $responseCode")
        }
    }

    suspend fun fetchRxNormCui(name: String): String? {
        return try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val url = "https://rxnav.nlm.nih.gov/REST/rxcui.json?name=$encodedName"
            val response = getHttpResponse(url)
            val json = JSONObject(response)
            val idGroup = json.optJSONObject("idGroup") ?: return null
            val rxnormIdArr = idGroup.optJSONArray("rxnormId") ?: return null
            if (rxnormIdArr.length() > 0) {
                rxnormIdArr.getString(0)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchRxNormInteractions(rxcui: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val url = "https://rxnav.nlm.nih.gov/REST/interaction/interaction.json?rxcui=$rxcui"
            val response = getHttpResponse(url)
            val json = JSONObject(response)
            val typeGroupArr = json.optJSONArray("interactionTypeGroup") ?: return emptyList()
            for (i in 0 until typeGroupArr.length()) {
                val typeGroup = typeGroupArr.getJSONObject(i)
                val typeArr = typeGroup.optJSONArray("interactionType") ?: continue
                for (j in 0 until typeArr.length()) {
                    val type = typeArr.getJSONObject(j)
                    val pairArr = type.optJSONArray("interactionPair") ?: continue
                    for (k in 0 until pairArr.length()) {
                        val pair = pairArr.getJSONObject(k)
                        val description = pair.optString("description")
                        if (description.isNotBlank()) {
                            list.add(description)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore and return empty list
        }
        return list.take(5) // Limit to top 5 interactions
    }

    suspend fun fetchOpenFdaLabel(name: String): JSONObject? {
        return try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            // Search openfda brand_name and generic_name or general search
            val url = "https://api.fda.gov/drug/label.json?search=openfda.brand_name:\"$encodedName\"+openfda.generic_name:\"$encodedName\"&limit=1"
            val response = try {
                getHttpResponse(url)
            } catch (e: Exception) {
                // Try fallback general search
                val fallbackUrl = "https://api.fda.gov/drug/label.json?search=$encodedName&limit=1"
                getHttpResponse(fallbackUrl)
            }
            val json = JSONObject(response)
            val results = json.optJSONArray("results")
            if (results != null && results.length() > 0) {
                results.getJSONObject(0)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun queryFdaAndRxNorm(query: String): String {
        return withContext(Dispatchers.IO) {
            val resultObj = JSONObject()
            resultObj.put("query", query)

            // 1. Get RxNorm CUI
            val rxcui = fetchRxNormCui(query)
            if (rxcui != null) {
                resultObj.put("rxnorm_cui", rxcui)
                val interactions = fetchRxNormInteractions(rxcui)
                val interArr = JSONArray()
                interactions.forEach { interArr.put(it) }
                resultObj.put("interactions", interArr)
            }

            // 2. Query OpenFDA
            val fdaJson = fetchOpenFdaLabel(query)
            if (fdaJson != null) {
                val openFda = fdaJson.optJSONObject("openfda")
                if (openFda != null) {
                    resultObj.put("brand_name", openFda.optJSONArray("brand_name"))
                    resultObj.put("generic_name", openFda.optJSONArray("generic_name"))
                }
                
                // Extract key textual sections
                resultObj.put("warnings", fdaJson.optJSONArray("warnings") ?: fdaJson.optString("warnings", ""))
                resultObj.put("adverse_reactions", fdaJson.optJSONArray("adverse_reactions") ?: fdaJson.optString("adverse_reactions", ""))
                resultObj.put("drug_interactions", fdaJson.optJSONArray("drug_interactions") ?: fdaJson.optString("drug_interactions", ""))
                resultObj.put("contraindications", fdaJson.optJSONArray("contraindications") ?: fdaJson.optString("contraindications", ""))
                resultObj.put("indications_and_usage", fdaJson.optJSONArray("indications_and_usage") ?: fdaJson.optString("indications_and_usage", ""))
                resultObj.put("dosage_and_administration", fdaJson.optJSONArray("dosage_and_administration") ?: fdaJson.optString("dosage_and_administration", ""))
            }

            resultObj.toString()
        }
    }
}
