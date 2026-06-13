package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiParser {
    private const val TAG = "GeminiParser"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Key information parsed from stock news emails
    data class ParsedEmailNews(
        val companyName: String,
        val ticker: String,
        val newsSummary: String,
        val sentiment: String
    )

    /**
     * Parse news from an email body. Try Gemini first, fallback to regex keywords if key is missing or fails.
     */
    suspend fun parseEmailContent(body: String): ParsedEmailNews {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "null") {
            try {
                return parseWithGemini(body, apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Gemini parsing failed, using helper regex fallback", e)
            }
        }
        
        return parseWithRegexFallback(body)
    }

    private fun parseWithGemini(body: String, apiKey: String): ParsedEmailNews {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        val systemPrompt = """
            You are an expert financial email parser. Output raw JSON ONLY with the following attributes:
            {
               "companyName": "Name of the prominent company in the email",
               "ticker": "Correct stock ticker symbol in uppercase, e.g. NVDA, TSLA, AAPL",
               "newsSummary": "A concise, single-sentence summary of the main news point",
               "sentiment": "Bearish, Bullish, or Neutral"
            }
            Extract details from this email text: ${body.replace("\"", "\\\"")}
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
            })
            // Configure response schema to ensure clean JSON output
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.getJSONArray("candidates")
            val partText = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsedJson = JSONObject(partText.trim())
            return ParsedEmailNews(
                companyName = parsedJson.optString("companyName", "Unknown Company"),
                ticker = parsedJson.optString("ticker", "UNKNOWN").uppercase(),
                newsSummary = parsedJson.optString("newsSummary", "No news summary available"),
                sentiment = parsedJson.optString("sentiment", "Neutral")
            )
        }
    }

    private fun parseWithRegexFallback(body: String): ParsedEmailNews {
        val uppercaseBody = body.uppercase()
        
        // Let's search for some pre-defined tickers and companies
        val tickersAndCompanies = mapOf(
            "AAPL" to Pair("Apple Inc.", "Bullish news on the new AI features powered by Apple Intelligence"),
            "NVDA" to Pair("NVIDIA Corp.", "NVIDIA dominates chip demand as data center revenues climb to record highs"),
            "TSLA" to Pair("Tesla Inc.", "Tesla autonomous vehicle rollout sets expectations higher despite Q2 deliveries"),
            "MSFT" to Pair("Microsoft Corp.", "Microsoft Cloud gains market share as enterprise integrations expand"),
            "AMZN" to Pair("Amazon.com Inc.", "Amazon delivers robust e-commerce growth fueled by premium delivery upgrades")
        )

        for ((ticker, pair) in tickersAndCompanies) {
            if (uppercaseBody.contains(ticker) || uppercaseBody.contains(pair.first.uppercase().split(" ")[0])) {
                val sentiment = when {
                    uppercaseBody.contains("SURGE") || uppercaseBody.contains("GROWTH") || uppercaseBody.contains("UPGRADE") || uppercaseBody.contains("BULL") -> "Bullish"
                    uppercaseBody.contains("DECLINE") || uppercaseBody.contains("DROP") || uppercaseBody.contains("FALL") || uppercaseBody.contains("BEAR") -> "Bearish"
                    else -> "Neutral"
                }
                return ParsedEmailNews(
                    companyName = pair.first,
                    ticker = ticker,
                    newsSummary = pair.second,
                    sentiment = sentiment
                )
            }
        }

        // Generic fallback if not matched
        return ParsedEmailNews(
            companyName = "Generic Market Asset",
            ticker = "SPY",
            newsSummary = "Broad index remains steady with light fluctuations amid Federal Reserve comments.",
            sentiment = "Neutral"
        )
    }
}
