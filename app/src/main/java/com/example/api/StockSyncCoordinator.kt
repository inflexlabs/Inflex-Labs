package com.example.api

import android.content.Context
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.StockNews
import com.example.data.SyncConfig
import com.example.data.SyncError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object StockSyncCoordinator {
    
    // In-Memory simulated Google Sheet representation for quick viewing in the UI
    data class SimulatedSheetRow(
        val rowIndex: Int,
        val timestamp: String,
        val ticker: String,
        val company: String,
        val sentiment: String,
        val price: String,
        val volume: String,
        val summary: String
    )

    private var currentSheetRowId = 142
    val simulatedSheetRows = mutableListOf<SimulatedSheetRow>(
        SimulatedSheetRow(140, "2026-06-13 09:15-07:00", "MSFT", "Microsoft Corp.", "Bullish", "$415.60", "2,351,200", "Microsoft Cloud gains market share as enterprise integrations expand"),
        SimulatedSheetRow(141, "2026-06-13 10:10-07:00", "AAPL", "Apple Inc.", "Bullish", "$183.50", "4,103,110", "Bullish news on the new AI features powered by Apple Intelligence")
    )

    // In-Memory simulated email boxes
    data class SimulatedEmail(
        val id: Int,
        val sender: String,
        val recipient: String,
        val subject: String,
        val body: String,
        val timestamp: String,
        val isErrorNotification: Boolean = false
    )

    val simulatedSentEmails = mutableListOf<SimulatedEmail>(
        SimulatedEmail(
            id = 1,
            sender = "system@marketpulse-sync.com",
            recipient = "inflexlabs@gmail.com",
            subject = "MarketPulse Sync Active Status Report",
            body = "MarketPulse automated synchronizer successfully initialized. Status: healthy. Sync tracking connected to sheet 'MarketPulse_Log_2026'.",
            timestamp = "2026-06-13 09:00:00"
        )
    )

    /**
     * Executes the sync flow completely.
     * Takes custom input or retrieves randomly generated stock market news.
     */
    suspend fun runSync(
        context: Context,
        db: AppDatabase,
        customEmailBody: String? = null,
        customEmailSubject: String? = null
    ): Result<StockNews> = withContext(Dispatchers.IO) {
        val configDao = db.syncConfigDao()
        val newsDao = db.stockNewsDao()
        val errorDao = db.syncErrorDao()

        // Ensure we have a config in database
        var config = configDao.getConfig()
        if (config == null) {
            config = SyncConfig()
            configDao.insertConfig(config)
        }

        val userEmail = config.userEmail
        val sheetName = config.googleSheetName
        val apiKey = config.financialApiKey

        // 1. Simulate reading Stock market email inbox.
        val emailSubject = customEmailSubject ?: "STOCK REPORT: NVDA Surge on Next-Gen Chip Allotments"
        val emailBody = customEmailBody ?: """
            NVIDIA Corp. highlights continuous supply chain dominance with extreme demand surges in ultra-advanced chips. 
            NVDA pricing points show high elasticity as server builders confirm major backorders extending through next year.
        """.trimIndent()

        // 2. ERROR TRIGGER TEST: Simulate Email Server Failure if config is set
        if (config.isSimulationErrorEmail) {
            val errorMsg = "Mail server authentication failed (SMTP Error 535: 5.7.8 Credentials Invalid)."
            val details = "Could not open Secure TLS connection to imap.marketpulse-inbox.com on port 993."
            val syncError = SyncError(errorMessage = errorMsg, errorDetails = details)
            errorDao.insertError(syncError)
            
            // Dispatch Error notification email
            val errEmail = SimulatedEmail(
                id = simulatedSentEmails.size + 1,
                sender = "alerts@marketpulse-sync.com",
                recipient = userEmail,
                subject = "⚠️ CRITICAL: MarketPulse Sync Error [Email Server Failure]",
                body = """
                    An automated error occurred during sync.
                    
                    Error: $errorMsg
                    Details: $details
                    Occurred at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                    
                    Please check your connection credentials in the settings menu.
                """.trimIndent(),
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                isErrorNotification = true
            )
            simulatedSentEmails.add(0, errEmail)
            
            return@withContext Result.failure(Exception(errorMsg))
        }

        // 3. Process news body through Gemini (parsed entities)
        val parsedNews = try {
            GeminiParser.parseEmailContent(emailBody)
        } catch (e: Exception) {
            val errorMsg = "Gemini AI Parsing Refused or Rate Limited."
            val details = "Response code from Generative API was un-processable. Fallback applied: " + e.localizedMessage
            errorDao.insertError(SyncError(errorMessage = errorMsg, errorDetails = details))
            return@withContext Result.failure(Exception(errorMsg))
        }

        // 4. Fetch stock metrics from financial api
        val stockMetrics = try {
            FinanceApiService.fetchStockData(parsedNews.ticker, apiKey)
        } catch (e: Exception) {
            val errorMsg = "Financial Data API rate limit / Socket timeout."
            val details = "Failed to query api.financialmodelingprep.com/api/v3/quote-short forticker ${parsedNews.ticker}: " + e.localizedMessage
            errorDao.insertError(SyncError(errorMessage = errorMsg, errorDetails = details))
            return@withContext Result.failure(Exception(errorMsg))
        }

        // 5. ERROR TRIGGER TEST: Simulate Google Sheets API failure if config is set
        if (config.isSimulationErrorSheets) {
            val errorMsg = "Google Sheets API Access Forbidden (HTTP 403: Sheets scope missing or client blocked)."
            val details = "Failed to write row $currentSheetRowId into document '$sheetName' because OAuth token was denied write access."
            val syncError = SyncError(errorMessage = errorMsg, errorDetails = details)
            errorDao.insertError(syncError)

            // Dispatch Error notification email as requested by user
            val errEmail = SimulatedEmail(
                id = simulatedSentEmails.size + 1,
                sender = "alerts@marketpulse-sync.com",
                recipient = userEmail,
                subject = "⚠️ CRITICAL: MarketPulse Sync Error [Google Sheets API Failure]",
                body = """
                    An automated error occurred during Google Sheets synchronization.
                    
                    Error: $errorMsg
                    Details: $details
                    Occurred at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                    Target Sheet: $sheetName
                    
                    Please check your Google OAuth permissions inside Settings.
                """.trimIndent(),
                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                isErrorNotification = true
            )
            simulatedSentEmails.add(0, errEmail)
            
            // Log local news status as failed sync
            val unsyncedNews = StockNews(
                companyName = parsedNews.companyName,
                ticker = parsedNews.ticker,
                price = stockMetrics.price,
                volume = stockMetrics.volume,
                newsSummary = parsedNews.newsSummary,
                sentiment = parsedNews.sentiment,
                sheetRowIndex = -1,
                syncStatus = "FAILED",
                emailSubject = emailSubject,
                emailBody = emailBody
            )
            newsDao.insertNews(unsyncedNews)

            return@withContext Result.failure(Exception(errorMsg))
        }

        // 6. Real simulation of standard Google Sheets sync
        currentSheetRowId += 1
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTimestamp = simpleDateFormat.format(Date())

        val sheetRow = SimulatedSheetRow(
            rowIndex = currentSheetRowId,
            timestamp = formattedTimestamp,
            ticker = parsedNews.ticker,
            company = parsedNews.companyName,
            sentiment = parsedNews.sentiment,
            price = "$${stockMetrics.price}",
            volume = "%,d".format(stockMetrics.volume),
            summary = parsedNews.newsSummary
        )
        // Insert at the beginning of list
        simulatedSheetRows.add(0, sheetRow)

        // Generate dispatch email
        val dispatchBody = """
            MarketPulse Automated stock-analysis sync triggered successfully.
            
            Company Analyzed: ${parsedNews.companyName} (${parsedNews.ticker})
            Consensus Sentiment: ${parsedNews.sentiment}
            Real-Time Financial Index metrics:
               * Trading Price: $${stockMetrics.price}
               * Share Daily Volume: %,d total transactions
            
            Google Sheets logged correctly on index Row $currentSheetRowId in document $sheetName.
            News Summary: ${parsedNews.newsSummary}
            
            Full Stock news received:
            $emailBody
        """.trimIndent()

        val digestEmail = SimulatedEmail(
            id = simulatedSentEmails.size + 1,
            sender = "system@marketpulse-sync.com",
            recipient = userEmail,
            subject = "📈 Dispatch Digest: ${parsedNews.ticker} MarketPulse Sync",
            body = dispatchBody,
            timestamp = formattedTimestamp
        )
        simulatedSentEmails.add(0, digestEmail)

        // 7. Save Stock news report locally in Room
        val finalStockNews = StockNews(
            companyName = parsedNews.companyName,
            ticker = parsedNews.ticker,
            price = stockMetrics.price,
            volume = stockMetrics.volume,
            newsSummary = parsedNews.newsSummary,
            sentiment = parsedNews.sentiment,
            sheetRowIndex = currentSheetRowId,
            syncStatus = "SUCCESS",
            emailSubject = emailSubject,
            emailBody = emailBody
        )
        newsDao.insertNews(finalStockNews)

        return@withContext Result.success(finalStockNews)
    }
}
