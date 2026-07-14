package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MobilityLog
import com.example.data.TicketPass
import com.example.data.ZurichFlowDatabase
import com.example.data.ZurichFlowRepository
import com.example.api.GeminiRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// --- App Screen Navigation ---
enum class AppScreen {
    HOME,
    PLANNER,
    MAP,
    TICKETS,
    ZURI,
    DISCOVER,
    SERVICE,
    DASHBOARD
}

// --- Localized Language Support ---
enum class ZurichLanguage(val label: String, val code: String) {
    ENGLISH("English", "EN"),
    GERMAN("Deutsch", "DE"),
    FRENCH("Français", "FR"),
    ITALIAN("Italiano", "IT"),
    ROMANSH("Rumantsch", "RM")
}

// --- Simulated Journey Plan Option ---
data class JourneyRoute(
    val id: String,
    val type: String, // "fastest", "quietest", "scenic", "emissions", "wheelchair", "cycling"
    val durationMinutes: Int,
    val transfers: Int,
    val co2SavedKg: Double,
    val costChf: Double,
    val occupancyLevel: Int, // 1 (low) to 3 (high)
    val arrivalConfidence: Int, // e.g., 99 (99% on-time confidence)
    val pathSegments: List<RouteSegment>,
    val description: String
)

data class RouteSegment(
    val mode: String, // "Tram", "Bus", "S-Bahn", "Walk", "Cycling", "Ferry", "Cable Car"
    val lineName: String,
    val duration: Int,
    val platform: String? = null
)

// --- Simulated Live Vehicle for Map ---
data class LiveVehicle(
    val id: String,
    val label: String,
    val mode: String, // "Tram", "Bus", "S-Bahn", "Ferry", "Cable Car"
    val progress: Float, // 0.0 to 1.0 back and forth
    val xOffset: Float,  // For mock canvas positioning
    val yOffset: Float,
    val destination: String,
    val status: String // "On Time", "Delayed +1 min", "Quiet"
)

class ZurichFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ZurichFlowRepository

    // --- State Flows ---
    val allTickets: StateFlow<List<TicketPass>>
    val favorites: StateFlow<List<com.example.data.FavoriteJourney>>
    val mobilityLogs: StateFlow<List<MobilityLog>>

    // --- Screen State ---
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // --- Language State ---
    private val _currentLanguage = MutableStateFlow(ZurichLanguage.ENGLISH)
    val currentLanguage: StateFlow<ZurichLanguage> = _currentLanguage.asStateFlow()

    // --- Journey Planner Inputs & Outputs ---
    private val _searchFrom = MutableStateFlow("Zürich HB")
    val searchFrom: StateFlow<String> = _searchFrom.asStateFlow()

    private val _searchTo = MutableStateFlow("Airport")
    val searchTo: StateFlow<String> = _searchTo.asStateFlow()

    private val _plannedRoutes = MutableStateFlow<List<JourneyRoute>>(emptyList())
    val plannedRoutes: StateFlow<List<JourneyRoute>> = _plannedRoutes.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // --- Zuri AI Assistant State ---
    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Grüezi! I am ZURI, your Zürich Futurist digital mobility companion. Ask me any travel, event, coffee, or route questions!" to false)
    )
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _isZuriThinking = MutableStateFlow(false)
    val isZuriThinking: StateFlow<Boolean> = _isZuriThinking.asStateFlow()

    // --- Live Simulated Vehicles ---
    private val _liveVehicles = MutableStateFlow<List<LiveVehicle>>(emptyList())
    val liveVehicles: StateFlow<List<LiveVehicle>> = _liveVehicles.asStateFlow()

    // --- Alert Service Updates ---
    private val _serviceUpdates = MutableStateFlow<List<String>>(emptyList())
    val serviceUpdates: StateFlow<List<String>> = _serviceUpdates.asStateFlow()

    // --- Smart Ticket Suggestion / Optimization state ---
    private val _optimizedTicketMessage = MutableStateFlow("")
    val optimizedTicketMessage: StateFlow<String> = _optimizedTicketMessage.asStateFlow()

    init {
        val database = ZurichFlowDatabase.getDatabase(application)
        repository = ZurichFlowRepository(database.flowDao())

        allTickets = repository.allTickets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        favorites = repository.favorites.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        mobilityLogs = repository.mobilityLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize default mock data on startup if database is empty
        viewModelScope.launch(Dispatchers.IO) {
            setupDefaultMockData()
            startMapSimulation()
            startServiceAdvisoryLoop()
            autoDetectLanguage()
        }
    }

    private fun autoDetectLanguage() {
        val deviceLang = Locale.getDefault().language
        val matched = ZurichLanguage.values().find { it.code.lowercase() == deviceLang.lowercase() }
        if (matched != null) {
            _currentLanguage.value = matched
        }
    }

    private suspend fun setupDefaultMockData() {
        // Clear or insert default elements if Room streams are empty (evaluated on subscriber lifecycle)
        // Insert a couple of default favorites to make the home screen beautiful immediately
        repository.addFavorite("Zürich HB", "Airport")
        repository.addFavorite("ETH Zürich", "Zürich HB")
        repository.addFavorite("Bellevue", "Kusnacht Goldbach")

        // Pre-purchase a default SwissPass so the pass section looks professional and "luxury"
        val mockSwissPass = TicketPass(
            type = "SwissPass",
            title = "SwissPass Halbtax (Half-Fare)",
            price = 185.00,
            validUntil = System.currentTimeMillis() + (365L * 24 * 3600 * 1000),
            qrCodeData = "SWISSPASS-HA-9823-774-ZH",
            isSwissPass = true,
            isGoogleWalletSynced = true
        )
        repository.buyTicket(mockSwissPass)

        // Insert a couple of default logs so dashboard shows historical metrics immediately
        repository.logJourney(MobilityLog(fromStation = "Zürich HB", toStation = "ETH Zürich", mode = "Tram", carbonSavedKg = 1.2, distanceKm = 1.8, durationMinutes = 7, energyKwh = 0.22))
        repository.logJourney(MobilityLog(fromStation = "Paradeplatz", toStation = "Bellevue", mode = "Walk", carbonSavedKg = 0.8, distanceKm = 1.1, durationMinutes = 12, energyKwh = 0.0))
        repository.logJourney(MobilityLog(fromStation = "Zürich HB", toStation = "Airport", mode = "S-Bahn", carbonSavedKg = 3.4, distanceKm = 9.5, durationMinutes = 9, energyKwh = 0.45))
        repository.logJourney(MobilityLog(fromStation = "Bürkliplatz", toStation = "Rüschlikon", mode = "Ferry", carbonSavedKg = 4.1, distanceKm = 6.2, durationMinutes = 25, energyKwh = 0.85))

        // Trigger default search suggestion
        performSearch("Zürich HB", "Airport")
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setLanguage(lang: ZurichLanguage) {
        _currentLanguage.value = lang
    }

    fun updateSearchInputs(from: String, to: String) {
        _searchFrom.value = from
        _searchTo.value = to
    }

    // --- Real-time Simulated Vehicles Loop ---
    private fun startMapSimulation() {
        viewModelScope.launch(Dispatchers.Default) {
            val vehicles = listOf(
                LiveVehicle("t4", "Tram 4", "Tram", 0.1f, 150f, 250f, "Tiefenbrunnen", "On Time"),
                LiveVehicle("t11", "Tram 11", "Tram", 0.4f, 320f, 120f, "Rehalp", "On Time"),
                LiveVehicle("s2", "S-Bahn S2", "S-Bahn", 0.2f, 80f, 400f, "Zürich Flughafen", "Quiet"),
                LiveVehicle("s16", "S-Bahn S16", "S-Bahn", 0.8f, 500f, 200f, "Herrliberg-Feldmeilen", "Delayed +1 min"),
                LiveVehicle("f_limmat", "Ferry 'Limmat'", "Ferry", 0.5f, 280f, 480f, "Bürkliplatz", "On Time"),
                LiveVehicle("b31", "Bus 31", "Bus", 0.6f, 400f, 350f, "Kienastenwies", "On Time"),
                LiveVehicle("c_poly", "Polybahn", "Cable Car", 0.9f, 240f, 160f, "ETH Zürich", "Quiet")
            )
            _liveVehicles.value = vehicles

            while (true) {
                delay(2000)
                _liveVehicles.value = _liveVehicles.value.map { v ->
                    val nextProgress = (v.progress + 0.05f) % 1.0f
                    // Dynamically alter offsets along custom path trajectories to simulate smooth movement
                    val newX = when(v.mode) {
                        "Tram" -> v.xOffset + Random.nextInt(-6, 6)
                        "S-Bahn" -> v.xOffset + Random.nextInt(-4, 4)
                        "Ferry" -> v.xOffset + Random.nextInt(-3, 3)
                        else -> v.xOffset + Random.nextInt(-5, 5)
                    }.coerceIn(50f, 750f)

                    val newY = when(v.mode) {
                        "Tram" -> v.yOffset + Random.nextInt(-6, 6)
                        "S-Bahn" -> v.yOffset + Random.nextInt(-4, 4)
                        "Ferry" -> v.yOffset + Random.nextInt(-3, 3)
                        else -> v.yOffset + Random.nextInt(-5, 5)
                    }.coerceIn(50f, 750f)

                    v.copy(
                        progress = nextProgress,
                        xOffset = newX,
                        yOffset = newY,
                        status = if (Random.nextInt(10) > 8) "Delayed +${Random.nextInt(1, 3)} min" else "On Time"
                    )
                }
            }
        }
    }

    // --- Service Advisories Center Loop ---
    private fun startServiceAdvisoryLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            val rawAdvisories = listOf(
                "Maintenance works at Polybahn starting tonight at 21:00. Alternative walking route signed.",
                "Weather Advisory: Strong winds on Lake Zürich. Lake Ferries operate with maximum 5-minute seasonal schedule buffers.",
                "Platform change: S16 to Herrliberg departing from Zürich HB Platform 43 instead of 41.",
                "Clean Energy Milestones: Zurich Flow reports 92% of all tram system network energy recovered via regenerative braking today.",
                "Zürich City Festival scheduled at Bürkliplatz: Extra night S-Bahn services will operate until 03:30."
            )
            _serviceUpdates.value = rawAdvisories.shuffled()

            while (true) {
                delay(15000)
                _serviceUpdates.value = rawAdvisories.shuffled()
            }
        }
    }

    // --- Multi-Modal Journey Search Generator ---
    fun performSearch(from: String, to: String) {
        if (from.isEmpty() || to.isEmpty()) return
        
        viewModelScope.launch {
            _isSearching.value = true
            delay(1000) // Aesthetic Swiss Precision response delay

            _searchFrom.value = from
            _searchTo.value = to

            val routes = generateRoutesFor(from, to)
            _plannedRoutes.value = routes
            _isSearching.value = false

            // Suggest optimized fare
            optimizeTicketFare(routes)
        }
    }

    private fun generateRoutesFor(from: String, to: String): List<JourneyRoute> {
        val cleanFrom = from.trim()
        val cleanTo = to.trim()

        return listOf(
            JourneyRoute(
                id = "fastest",
                type = "fastest",
                durationMinutes = 11,
                transfers = 0,
                co2SavedKg = 2.4,
                costChf = 4.40,
                occupancyLevel = 2,
                arrivalConfidence = 99,
                pathSegments = listOf(
                    RouteSegment("S-Bahn", "S16", 11, "Platform 41")
                ),
                description = "Direct high-speed regional rail. Simplest and most reliable route."
            ),
            JourneyRoute(
                id = "quietest",
                type = "quietest",
                durationMinutes = 18,
                transfers = 1,
                co2SavedKg = 2.2,
                costChf = 4.40,
                occupancyLevel = 1,
                arrivalConfidence = 98,
                pathSegments = listOf(
                    RouteSegment("Tram", "Tram 6", 8, "Haltestelle Central"),
                    RouteSegment("S-Bahn", "S2", 10, "Platform 3")
                ),
                description = "Alternative quiet routing avoiding main hub crowds."
            ),
            JourneyRoute(
                id = "scenic",
                type = "scenic",
                durationMinutes = 35,
                transfers = 1,
                co2SavedKg = 3.1,
                costChf = 8.80,
                occupancyLevel = 1,
                arrivalConfidence = 97,
                pathSegments = listOf(
                    RouteSegment("Ferry", "Ferry Limmat", 20, "Bürkliplatz"),
                    RouteSegment("S-Bahn", "S24", 15, "Platform 1")
                ),
                description = "Scenic Limmat cruise combined with clean airport connection."
            ),
            JourneyRoute(
                id = "emissions",
                type = "emissions",
                durationMinutes = 22,
                transfers = 1,
                co2SavedKg = 4.5,
                costChf = 4.40,
                occupancyLevel = 1,
                arrivalConfidence = 99,
                pathSegments = listOf(
                    RouteSegment("Walk", "Walking Path", 10),
                    RouteSegment("Tram", "Tram 10", 12, "Haltestelle ETH")
                ),
                description = "Minimal-energy transit path maximizing green regenerative tram kilometers."
            ),
            JourneyRoute(
                id = "wheelchair",
                type = "wheelchair",
                durationMinutes = 14,
                transfers = 0,
                co2SavedKg = 2.4,
                costChf = 4.40,
                occupancyLevel = 2,
                arrivalConfidence = 99,
                pathSegments = listOf(
                    RouteSegment("S-Bahn", "S2", 14, "Platform 43 - Step-free lift access available")
                ),
                description = "Fully optimized for low-floor access, ramp boarding and lift connectivity."
            ),
            JourneyRoute(
                id = "cycling",
                type = "cycling",
                durationMinutes = 25,
                transfers = 0,
                co2SavedKg = 0.0,
                costChf = 0.00,
                occupancyLevel = 1,
                arrivalConfidence = 95,
                pathSegments = listOf(
                    RouteSegment("Cycling", "Züri Velo #439", 25, "Station HB")
                ),
                description = "Healthy cycling route on dedicated Zürich bicycle lanes."
            )
        )
    }

    // --- Automatic Fare Optimization ---
    private fun optimizeTicketFare(routes: List<JourneyRoute>) {
        if (routes.isEmpty()) return
        val cheapest = routes.minOfOrNull { it.costChf } ?: 0.0
        val isDayPassOptimum = cheapest >= 8.80

        _optimizedTicketMessage.value = if (isDayPassOptimum) {
            "Optimal Fare Found: A Zürich Day Pass is CHF 8.80. Since your planned transfers sum to CHF ${"%.2f".format(cheapest * 2)}, we have unlocked the Day Pass for infinite rides within Zone 110 at no extra charge."
        } else {
            "Optimal Fare Found: A single Zürich Zone 110 Ticket is CHF 4.40. Active half-fare has been automatically applied to SwissPass."
        }
    }

    // --- Ticketing Actions ---
    fun purchaseTicket(type: String, title: String, price: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTicket = TicketPass(
                type = type,
                title = title,
                price = price,
                validUntil = System.currentTimeMillis() + (24L * 3600 * 1000), // Valid for 24 hours
                qrCodeData = "Z-FLOW-${type.uppercase()}-${Random.nextInt(1000, 9999)}-CH",
                isSwissPass = false
            )
            repository.buyTicket(newTicket)

            // Log ticket purchase as part of history
            repository.logJourney(
                MobilityLog(
                    fromStation = "Ticket Counter",
                    toStation = title,
                    mode = "Smart Purchase",
                    carbonSavedKg = 0.0,
                    distanceKm = 0.0,
                    durationMinutes = 1,
                    energyKwh = 0.0
                )
            )
        }
    }

    fun syncGoogleWallet(ticketId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Find ticket and update synced status
            // Simple logic update:
            // Since we buy tickets and keep them in list, let's update
            allTickets.value.find { it.id == ticketId }?.let { ticket ->
                val updated = ticket.copy(isGoogleWalletSynced = true)
                repository.buyTicket(updated)
            }
        }
    }

    fun removeTicket(ticketId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeTicket(ticketId)
        }
    }

    // --- Favorite Journeys Actions ---
    fun toggleFavorite(from: String, to: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavs = favorites.value
            val exists = currentFavs.find { it.fromName.equals(from, true) && it.toName.equals(to, true) }
            if (exists != null) {
                repository.removeFavorite(exists.id)
            } else {
                repository.addFavorite(from, to)
            }
        }
    }

    // --- ZURI Assistant Messaging flow ---
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(message to true)
        _chatMessages.value = currentList
        
        _isZuriThinking.value = true

        viewModelScope.launch {
            // Retrieve chat response from Gemini using our helper
            val aiResponse = GeminiRetrofitClient.askZuri(message, _chatMessages.value)
            
            _isZuriThinking.value = false
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(aiResponse to false)
            _chatMessages.value = updatedList
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf("Grüezi! I am ZURI, your Zürich Futurist digital mobility companion. Ask me anything!" to false)
    }

    // --- Commute Logging Manual Trigger (Sustainability Score multiplier) ---
    fun logCommuteManual(from: String, to: String, mode: String, km: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val minutes = (km * 2.5).toInt().coerceAtLeast(4)
            val carbonSaved = when(mode) {
                "Walk", "Cycling" -> km * 0.25
                "Tram", "Bus" -> km * 0.18
                "S-Bahn" -> km * 0.22
                else -> km * 0.12
            }
            val energy = when(mode) {
                "Walk", "Cycling" -> 0.0
                "Tram", "Bus" -> km * 0.12
                "S-Bahn" -> km * 0.08
                else -> km * 0.1
            }

            val log = MobilityLog(
                fromStation = from,
                toStation = to,
                mode = mode,
                carbonSavedKg = carbonSaved,
                distanceKm = km,
                durationMinutes = minutes,
                energyKwh = energy
            )
            repository.logJourney(log)
        }
    }

    fun resetLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    // --- Localized Dictionary helper ---
    fun getString(key: String): String {
        val lang = _currentLanguage.value
        return translations[key]?.get(lang) ?: key
    }

    companion object {
        // Highly localized string dictionaries for Swiss standards
        private val translations = mapOf(
            "app_title" to mapOf(
                ZurichLanguage.ENGLISH to "ZÜRICH FLOW",
                ZurichLanguage.GERMAN to "ZÜRICH FLOW",
                ZurichLanguage.FRENCH to "ZURICH FLOW",
                ZurichLanguage.ITALIAN to "ZURIGO FLOW",
                ZurichLanguage.ROMANSH to "ZURICH FLOW"
            ),
            "tagline" to mapOf(
                ZurichLanguage.ENGLISH to "Precision in Motion.",
                ZurichLanguage.GERMAN to "Präzision in Bewegung.",
                ZurichLanguage.FRENCH to "La précision en mouvement.",
                ZurichLanguage.ITALIAN to "La precisione in movimento.",
                ZurichLanguage.ROMANSH to "Precisun en moviment."
            ),
            "welcome" to mapOf(
                ZurichLanguage.ENGLISH to "GOOD MORNING, ZÜRICH.",
                ZurichLanguage.GERMAN to "GUTEN MORGEN, ZÜRICH.",
                ZurichLanguage.FRENCH to "BONJOUR, ZURICH.",
                ZurichLanguage.ITALIAN to "BUONGIORNO, ZURIGO.",
                ZurichLanguage.ROMANSH to "BUN DE, ZURICH."
            ),
            "sub_welcome" to mapOf(
                ZurichLanguage.ENGLISH to "Switzerland's digital mobility operate.",
                ZurichLanguage.GERMAN to "Das digitale Mobilitätssystem der Schweiz.",
                ZurichLanguage.FRENCH to "Le système d'exploitation de la mobilité suisse.",
                ZurichLanguage.ITALIAN to "Il sistema operativo per la mobilità svizzera.",
                ZurichLanguage.ROMANSH to "L'operativ da mobilitad digitala svizra."
            ),
            "tab_home" to mapOf(
                ZurichLanguage.ENGLISH to "Home",
                ZurichLanguage.GERMAN to "Startseite",
                ZurichLanguage.FRENCH to "Accueil",
                ZurichLanguage.ITALIAN to "Inizio",
                ZurichLanguage.ROMANSH to "Inizial"
            ),
            "tab_planner" to mapOf(
                ZurichLanguage.ENGLISH to "Planner",
                ZurichLanguage.GERMAN to "Fahrplan",
                ZurichLanguage.FRENCH to "Horaires",
                ZurichLanguage.ITALIAN to "Orario",
                ZurichLanguage.ROMANSH to "Orari"
            ),
            "tab_map" to mapOf(
                ZurichLanguage.ENGLISH to "Live Map",
                ZurichLanguage.GERMAN to "Netzplan",
                ZurichLanguage.FRENCH to "Plan Live",
                ZurichLanguage.ITALIAN to "Mappa",
                ZurichLanguage.ROMANSH to "Carta"
            ),
            "tab_tickets" to mapOf(
                ZurichLanguage.ENGLISH to "Tickets",
                ZurichLanguage.GERMAN to "Billette",
                ZurichLanguage.FRENCH to "Billets",
                ZurichLanguage.ITALIAN to "Biglietti",
                ZurichLanguage.ROMANSH to "Bilets"
            ),
            "tab_zuri" to mapOf(
                ZurichLanguage.ENGLISH to "ZURI AI",
                ZurichLanguage.GERMAN to "ZURI KI",
                ZurichLanguage.FRENCH to "ZURI IA",
                ZurichLanguage.ITALIAN to "ZURI IA",
                ZurichLanguage.ROMANSH to "ZURI UA"
            ),
            "tab_discover" to mapOf(
                ZurichLanguage.ENGLISH to "Discover",
                ZurichLanguage.GERMAN to "Entdecken",
                ZurichLanguage.FRENCH to "Découvrir",
                ZurichLanguage.ITALIAN to "Scopri",
                ZurichLanguage.ROMANSH to "Scuvrir"
            ),
            "tab_service" to mapOf(
                ZurichLanguage.ENGLISH to "Service",
                ZurichLanguage.GERMAN to "Dienst",
                ZurichLanguage.FRENCH to "Service",
                ZurichLanguage.ITALIAN to "Servizio",
                ZurichLanguage.ROMANSH to "Servetsch"
            ),
            "tab_dashboard" to mapOf(
                ZurichLanguage.ENGLISH to "Metrics",
                ZurichLanguage.GERMAN to "Statistik",
                ZurichLanguage.FRENCH to "Mesures",
                ZurichLanguage.ITALIAN to "Statistiche",
                ZurichLanguage.ROMANSH to "Metricas"
            ),
            "weather" to mapOf(
                ZurichLanguage.ENGLISH to "Zürich • 19°C Sunny • Air 99% Clean",
                ZurichLanguage.GERMAN to "Zürich • 19°C Sonnig • Luft 99% Sauber",
                ZurichLanguage.FRENCH to "Zurich • 19°C Ensoleillé • Air 99% Pur",
                ZurichLanguage.ITALIAN to "Zurigo • 19°C Soleggiato • Aria 99% Pulita",
                ZurichLanguage.ROMANSH to "Zurich • 19°C Solai • Aria 99% Munda"
            ),
            "next_departures" to mapOf(
                ZurichLanguage.ENGLISH to "NEXT DEPARTURES",
                ZurichLanguage.GERMAN to "NÄCHSTE ABFAHRTEN",
                ZurichLanguage.FRENCH to "PROCHAINS DÉPARTS",
                ZurichLanguage.ITALIAN to "PROSSIME PARTENZE",
                ZurichLanguage.ROMANSH to "PROXIMS DEPARTS"
            ),
            "service_status" to mapOf(
                ZurichLanguage.ENGLISH to "SERVICE ADVISORIES",
                ZurichLanguage.GERMAN to "BETRIEBSMELDUNGEN",
                ZurichLanguage.FRENCH to "INFO DE TRAFIC",
                ZurichLanguage.ITALIAN to "STATO DEL SERVIZIO",
                ZurichLanguage.ROMANSH to "MESSAGES DA SERVETSCH"
            ),
            "passes" to mapOf(
                ZurichLanguage.ENGLISH to "YOUR ACTIVE PASSES",
                ZurichLanguage.GERMAN to "IHRE AKTIVEN BILLETTE",
                ZurichLanguage.FRENCH to "VOS BILLETS ACTIFS",
                ZurichLanguage.ITALIAN to "I TUOI BIGLIETTI",
                ZurichLanguage.ROMANSH to "VOSS BILETS ACTIVS"
            ),
            "favorites" to mapOf(
                ZurichLanguage.ENGLISH to "FAVOURITE JOURNEYS",
                ZurichLanguage.GERMAN to "FAVORISIERTE REISEN",
                ZurichLanguage.FRENCH to "TRAJETS FAVORIS",
                ZurichLanguage.ITALIAN to "PERCORSI PREFERITI",
                ZurichLanguage.ROMANSH to "VIAGIS FAVURIS"
            ),
            "carbon_saving" to mapOf(
                ZurichLanguage.ENGLISH to "Carbon Saved",
                ZurichLanguage.GERMAN to "CO₂-Einsparung",
                ZurichLanguage.FRENCH to "Carbone Économisé",
                ZurichLanguage.ITALIAN to "Emissioni Risparmiate",
                ZurichLanguage.ROMANSH to "Co2 Sparfagnà"
            )
        )
    }
}
