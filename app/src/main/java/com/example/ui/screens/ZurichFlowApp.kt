package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.BorderStroke
import com.example.data.MobilityLog
import com.example.data.TicketPass
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.JourneyRoute
import com.example.ui.viewmodel.LiveVehicle
import com.example.ui.viewmodel.RouteSegment
import com.example.ui.viewmodel.ZurichFlowViewModel
import com.example.ui.viewmodel.ZurichLanguage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZurichFlowApp(
    viewModel: ZurichFlowViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    
    val tickets by viewModel.allTickets.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val logs by viewModel.mobilityLogs.collectAsState()

    // Screen responsive size classes
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Timing state for high-precision Swiss Clock
    var currentTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            currentTimeString = sdf.format(Date()) + " CET"
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 12.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = viewModel.getString("app_title"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8E8E93),
                                    modifier = Modifier.testTag("welcome_header_prefix")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = viewModel.getString("welcome"),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.5).sp,
                                        lineHeight = 26.sp
                                    ),
                                    color = Color.Black,
                                    modifier = Modifier.testTag("welcome_header")
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (currentTimeString.length >= 5) currentTimeString.substring(0, 5) else "08:00",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Light,
                                        fontSize = 26.sp,
                                        letterSpacing = (-1.0).sp
                                    ),
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF00C853), CircleShape)
                                    )
                                    Text(
                                        text = "NETWORK LIVE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.0.sp
                                        ),
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.Black
                    )
                )
                // Distinct border border-[#E5E5E5] line at bottom of header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE5E5E5))
                )
            }
        },
        bottomBar = {
            if (!isTablet) {
                ZurichBottomBar(
                    currentScreen = currentScreen,
                    onScreenSelected = { viewModel.setScreen(it) },
                    viewModel = viewModel
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Adaptive Navigation Rail for Tablets and Wide Displays
            if (isTablet) {
                ZurichNavRail(
                    currentScreen = currentScreen,
                    onScreenSelected = { viewModel.setScreen(it) },
                    viewModel = viewModel
                )
            }

            // Central Content Pane
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> HomeScreen(viewModel, tickets, favorites)
                    AppScreen.PLANNER -> PlannerScreen(viewModel)
                    AppScreen.MAP -> MapScreen(viewModel)
                    AppScreen.TICKETS -> TicketsScreen(viewModel, tickets)
                    AppScreen.ZURI -> ZuriScreen(viewModel)
                    AppScreen.DISCOVER -> DiscoverScreen(viewModel)
                    AppScreen.SERVICE -> ServiceScreen(viewModel)
                    AppScreen.DASHBOARD -> DashboardScreen(viewModel, logs)
                }
            }
        }
    }
}

// --- Dynamic Tablet Navigation Rail ---
@Composable
fun ZurichNavRail(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    viewModel: ZurichFlowViewModel
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(),
        header = {
            // Language selector inside rail header
            LanguageToggleMini(viewModel = viewModel)
        }
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        NavigationRailItem(
            selected = currentScreen == AppScreen.HOME,
            onClick = { onScreenSelected(AppScreen.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text(viewModel.getString("tab_home"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.PLANNER,
            onClick = { onScreenSelected(AppScreen.PLANNER) },
            icon = { Icon(Icons.Default.DirectionsTransit, contentDescription = "Planner") },
            label = { Text(viewModel.getString("tab_planner"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.MAP,
            onClick = { onScreenSelected(AppScreen.MAP) },
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text(viewModel.getString("tab_map"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.TICKETS,
            onClick = { onScreenSelected(AppScreen.TICKETS) },
            icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Tickets") },
            label = { Text(viewModel.getString("tab_tickets"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.ZURI,
            onClick = { onScreenSelected(AppScreen.ZURI) },
            icon = { Icon(Icons.Default.SmartToy, contentDescription = "Zuri AI") },
            label = { Text(viewModel.getString("tab_zuri"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.DISCOVER,
            onClick = { onScreenSelected(AppScreen.DISCOVER) },
            icon = { Icon(Icons.Default.Explore, contentDescription = "Discover") },
            label = { Text(viewModel.getString("tab_discover"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.DASHBOARD,
            onClick = { onScreenSelected(AppScreen.DASHBOARD) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Metrics") },
            label = { Text(viewModel.getString("tab_dashboard"), style = MaterialTheme.typography.bodyMedium) }
        )
        NavigationRailItem(
            selected = currentScreen == AppScreen.SERVICE,
            onClick = { onScreenSelected(AppScreen.SERVICE) },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Service") },
            label = { Text(viewModel.getString("tab_service"), style = MaterialTheme.typography.bodyMedium) }
        )
    }
}

// --- Bottom Navigation Bar ---
@Composable
fun ZurichBottomBar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    viewModel: ZurichFlowViewModel
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE5E5E5))
        )
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = currentScreen == AppScreen.HOME,
                onClick = { onScreenSelected(AppScreen.HOME) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text(viewModel.getString("tab_home"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.PLANNER,
                onClick = { onScreenSelected(AppScreen.PLANNER) },
                icon = { Icon(Icons.Default.DirectionsTransit, contentDescription = "Planner") },
                label = { Text(viewModel.getString("tab_planner"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.MAP,
                onClick = { onScreenSelected(AppScreen.MAP) },
                icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
                label = { Text(viewModel.getString("tab_map"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.TICKETS,
                onClick = { onScreenSelected(AppScreen.TICKETS) },
                icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Tickets") },
                label = { Text(viewModel.getString("tab_tickets"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            NavigationBarItem(
                selected = currentScreen == AppScreen.ZURI,
                onClick = { onScreenSelected(AppScreen.ZURI) },
                icon = { Icon(Icons.Default.SmartToy, contentDescription = "Zuri AI") },
                label = { Text(viewModel.getString("tab_zuri"), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

// --- Home Screen ---
@Composable
fun HomeScreen(
    viewModel: ZurichFlowViewModel,
    tickets: List<TicketPass>,
    favorites: List<com.example.data.FavoriteJourney>
) {
    val advisories by viewModel.serviceUpdates.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Language Selector Row
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = viewModel.getString("welcome"),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = viewModel.getString("sub_welcome"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Dynamic Language Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Language / Sprache: ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        ZurichLanguage.values().forEach { lang ->
                            val currentLang by viewModel.currentLanguage.collectAsState()
                            val isSelected = currentLang == lang
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 0.5.dp, 
                                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.setLanguage(lang) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = lang.code,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Weather banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud, 
                        contentDescription = "Weather", 
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = viewModel.getString("weather"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Quick Ticket Buy Shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.purchaseTicket("Single", "Single Ticket Zone 110", 4.40) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("buy_ticket_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1-Tap Single (CHF 4.40)", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { viewModel.purchaseTicket("Day Pass", "Day Pass Zone 110", 8.80) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1-Tap Day Pass (CHF 8.80)", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Next departures
        item {
            Text(
                text = viewModel.getString("next_departures").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DepartureRow(mode = "S-Bahn", line = "S16", dest = "Zürich Flughafen (Airport)", depIn = "2 min", plat = "Pl. 41", onTime = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    DepartureRow(mode = "Tram", line = "Tram 6", dest = "ETH Zürich", depIn = "5 min", plat = "Central", onTime = true)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    DepartureRow(mode = "Tram", line = "Tram 11", dest = "Bellevue / Rehalp", depIn = "7 min", plat = "HB", onTime = false, delayStr = "+2 min")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    DepartureRow(mode = "Ferry", line = "Ferry L", dest = "Bürkliplatz", depIn = "12 min", plat = "Ferry Pier", onTime = true)
                }
            }
        }

        // Active tickets / SwissPass
        item {
            Text(
                text = viewModel.getString("passes").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active travel passes or tickets. Purchase below or on Tickets Tab.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tickets) { ticket ->
                        SwissPassWidget(ticket = ticket, viewModel = viewModel)
                    }
                }
            }
        }

        // Favorites Shortcuts
        item {
            Text(
                text = viewModel.getString("favorites").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(favorites) { fav ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(220.dp)
                            .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
                            .clickable {
                                viewModel.updateSearchInputs(fav.fromName, fav.toName)
                                viewModel.performSearch(fav.fromName, fav.toName)
                                viewModel.setScreen(AppScreen.PLANNER)
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DirectionsTransit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(fav.fromName, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp).padding(vertical = 2.dp), tint = Color(0xFF8E8E93))
                            Text(fav.toName, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Reassuring Advisory Bulletins
        item {
            Text(
                text = viewModel.getString("service_status").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    advisories.forEachIndexed { idx, update ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Text(
                                text = update,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (idx < advisories.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DepartureRow(
    mode: String,
    line: String,
    dest: String,
    depIn: String,
    plat: String,
    onTime: Boolean,
    delayStr: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        if (mode == "S-Bahn") Color(0xFF0F52BA) else if (mode == "Ferry") Color(0xFF00A2C9) else Color(0xFFE30613),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = line,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(text = dest, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (onTime) "On Time" else "Delayed $delayStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (onTime) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = plat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        Text(
            text = depIn,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// --- Live Journey Planner Screen ---
@Composable
fun PlannerScreen(viewModel: ZurichFlowViewModel) {
    val searchFrom by viewModel.searchFrom.collectAsState()
    val searchTo by viewModel.searchTo.collectAsState()
    val routes by viewModel.plannedRoutes.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val optMessage by viewModel.optimizedTicketMessage.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var fromText by remember { mutableStateOf(searchFrom) }
    var toText by remember { mutableStateOf(searchTo) }

    // Update internal inputs when viewmodel state shifts (e.g., from favorites click)
    LaunchedEffect(searchFrom, searchTo) {
        fromText = searchFrom
        toText = searchTo
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form input cards
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SWISS PRECISION REISEPLANER", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                    
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it },
                        label = { Text("Departing From (Startpunkt)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_from_input"),
                        leadingIcon = { Icon(Icons.Default.DirectionsTransit, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it },
                        label = { Text("Arriving At (Zielort)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_to_input"),
                        leadingIcon = { Icon(Icons.Default.DirectionsTransit, contentDescription = null) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Popular Swiss nodes rapid buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ETH Zürich", "Airport", "Bahnhofstrasse", "Lake Zürich").forEach { dest ->
                            AssistChip(
                                onClick = { 
                                    toText = dest
                                    viewModel.updateSearchInputs(fromText, dest)
                                    viewModel.performSearch(fromText, dest)
                                },
                                label = { Text(dest, style = MaterialTheme.typography.bodyMedium) },
                                shape = RoundedCornerShape(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite / Star toggle
                        val isFav = favorites.any { it.fromName.equals(fromText, true) && it.toName.equals(toText, true) }
                        IconButton(
                            onClick = { viewModel.toggleFavorite(fromText, toText) }
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star favorite",
                                tint = if (isFav) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline
                            )
                        }

                        Button(
                            onClick = { 
                                viewModel.updateSearchInputs(fromText, toText)
                                viewModel.performSearch(fromText, toText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("search_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Calculating Chrono...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Precision Calculate", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Fare Optimizer Banner
        if (routes.isNotEmpty() && optMessage.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Optimized", tint = Color(0xFF2E7D32))
                        Text(
                            text = optMessage,
                            color = Color(0xFF1B5E20),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Journey Plans Lists
        if (routes.isNotEmpty()) {
            item {
                Text("OPTIMIZED SWISS FUTURIST ROUTE PROPOSALS", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
            }

            items(routes) { route ->
                JourneyRouteCard(route = route, fromText = fromText, toText = toText, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun JourneyRouteCard(route: JourneyRoute, fromText: String, toText: String, viewModel: ZurichFlowViewModel) {
    var isLogged by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type & Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                when (route.type) {
                                    "fastest" -> Color(0xFFE30613)
                                    "quietest" -> Color(0xFF4A4A4F)
                                    "scenic" -> Color(0xFF00A2C9)
                                    "emissions" -> Color(0xFF2E7D32)
                                    "wheelchair" -> Color(0xFF1976D2)
                                    else -> Color(0xFF8E8E93)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = route.type.uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Confidence: ${route.arrivalConfidence}% on-time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Text(
                    text = "${route.durationMinutes} min",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timelines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                route.pathSegments.forEachIndexed { idx, seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    when (seg.mode) {
                                        "Walk" -> Color.LightGray
                                        "Cycling" -> Color(0xFF388E3C)
                                        "Ferry" -> Color(0xFF00C9A7)
                                        "S-Bahn" -> Color(0xFF0F52BA)
                                        else -> Color(0xFFE30613)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(seg.lineName, color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                        if (idx < route.pathSegments.size - 1) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(12.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = route.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )

            // Platforms & Access Info
            route.pathSegments.find { it.platform != null }?.let { segWithPlat ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Departs ${segWithPlat.lineName} from ${segWithPlat.platform}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Metrics: Carbon & Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("CO₂ SAVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text("${route.co2SavedKg} kg", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2E7D32))
                    }
                    Column {
                        Text("FARE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text(if (route.costChf == 0.0) "Free" else "CHF ${"%.2f".format(route.costChf)}", style = MaterialTheme.typography.titleLarge)
                    }
                }

                // Log journey action (real persistence)
                if (isLogged) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Logged to Metrics", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val activeSeg = route.pathSegments.firstOrNull() ?: RouteSegment("Transit", "Line", 10)
                            viewModel.logCommuteManual(
                                from = fromText,
                                to = toText,
                                mode = activeSeg.mode,
                                km = if (route.type == "fastest") 9.5 else 4.0
                            )
                            isLogged = true
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Log Completed Trip", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// --- Live Network Map Screen ---
@Composable
fun MapScreen(viewModel: ZurichFlowViewModel) {
    val vehicles by viewModel.liveVehicles.collectAsState()
    var selectedVehicle by remember { mutableStateOf<LiveVehicle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Beautiful Minimal Architectural Vector Map drawn on dynamic Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEFEFF4)) // Soft silver background resembling a blueprint
                ) {
                    val width = size.width
                    val height = size.height

                    // 1. Draw Lake Zürich (Blue Radial Accent)
                    drawRect(
                        color = Color(0xFFD0E3F7),
                        topLeft = Offset(0f, height * 0.72f),
                        size = androidx.compose.ui.geometry.Size(width, height * 0.28f)
                    )

                    // 2. Draw Limmat River flowing through downtown
                    val riverPoints = listOf(
                        Offset(width * 0.45f, height * 0.72f),
                        Offset(width * 0.44f, height * 0.55f),
                        Offset(width * 0.46f, height * 0.40f),
                        Offset(width * 0.48f, height * 0.25f),
                        Offset(width * 0.45f, 0f)
                    )
                    for (i in 0 until riverPoints.size - 1) {
                        drawLine(
                            color = Color(0xFFD0E3F7),
                            start = riverPoints[i],
                            end = riverPoints[i+1],
                            strokeWidth = 24f
                        )
                    }

                    // 3. Draw Train Tracks S-Bahn (dashed black & white line)
                    val trackStart = Offset(0f, height * 0.35f)
                    val trackHB = Offset(width * 0.42f, height * 0.32f)
                    val trackEnd = Offset(width, height * 0.22f)

                    drawLine(color = Color(0xFF8E8E93), start = trackStart, end = trackHB, strokeWidth = 6f)
                    drawLine(color = Color(0xFF8E8E93), start = trackHB, end = trackEnd, strokeWidth = 6f)

                    // 4. Draw Stations grid nodes
                    val hbNode = Offset(width * 0.42f, height * 0.32f)
                    val ethNode = Offset(width * 0.65f, height * 0.28f)
                    val paradeNode = Offset(width * 0.38f, height * 0.52f)
                    val bellevueNode = Offset(width * 0.56f, height * 0.64f)
                    val airportNode = Offset(width * 0.75f, height * 0.10f)

                    // Draw connecting Tram lines (Solid Swiss Red thin lines)
                    drawLine(color = Color(0xFFE30613).copy(alpha = 0.6f), start = hbNode, end = ethNode, strokeWidth = 4f)
                    drawLine(color = Color(0xFFE30613).copy(alpha = 0.6f), start = hbNode, end = paradeNode, strokeWidth = 4f)
                    drawLine(color = Color(0xFFE30613).copy(alpha = 0.6f), start = paradeNode, end = bellevueNode, strokeWidth = 4f)
                    drawLine(color = Color(0xFFE30613).copy(alpha = 0.6f), start = bellevueNode, end = ethNode, strokeWidth = 4f)

                    // Draw station node markers
                    listOf(
                        "Zürich HB" to hbNode,
                        "ETH Zürich" to ethNode,
                        "Paradeplatz" to paradeNode,
                        "Bellevue" to bellevueNode,
                        "Kloten (Airport)" to airportNode
                    ).forEach { (name, pos) ->
                        drawCircle(color = Color.White, radius = 10f, center = pos)
                        drawCircle(color = Color.Black, radius = 10f, center = pos, style = Stroke(width = 2f))
                        drawCircle(color = Color(0xFFE30613), radius = 4f, center = pos)
                    }
                }

                // Render moving interactive vehicle markers over canvas coordinates!
                vehicles.forEach { vehicle ->
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (vehicle.xOffset / 800f * LocalConfiguration.current.screenWidthDp).dp,
                                y = (vehicle.yOffset / 800f * 400f).dp
                            )
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (vehicle.mode) {
                                    "S-Bahn" -> Color(0xFF0F52BA)
                                    "Ferry" -> Color(0xFF00A2C9)
                                    "Bus" -> Color(0xFFEF6C00)
                                    "Cable Car" -> Color(0xFF757575)
                                    else -> Color(0xFFE30613) // Tram
                                }
                            )
                            .border(1.5.dp, Color.White, CircleShape)
                            .clickable { selectedVehicle = vehicle }
                            .wrapContentSize(Alignment.Center)
                    ) {
                        Text(
                            text = if (vehicle.label.startsWith("Tram")) vehicle.label.substringAfter("Tram ") else vehicle.label.take(2),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp
                        )
                    }
                }

                // Label annotations for landmarks over map
                LabelOverlay(text = "ZÜRICH HB", xDp = 150, yDp = 110)
                LabelOverlay(text = "ETH ZÜRICH", xDp = 240, yDp = 100)
                LabelOverlay(text = "BELLEVUE", xDp = 210, yDp = 220)
                LabelOverlay(text = "LAKE ZÜRICH", xDp = 160, yDp = 310, isRiver = true)
                LabelOverlay(text = "LIMMAT", xDp = 175, yDp = 160, isRiver = true)

                // Map Legend Overlay (floating card)
                Card(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("LIVE NETWORK SYSTEM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                        LegendRow(color = Color(0xFFE30613), label = "Trams")
                        LegendRow(color = Color(0xFF0F52BA), label = "S-Bahn")
                        LegendRow(color = Color(0xFFEF6C00), label = "Buses")
                        LegendRow(color = Color(0xFF00A2C9), label = "Lake Ferries")
                    }
                }
            }
        }

        // Active vehicle details drawer
        selectedVehicle?.let { vehicle ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        if (vehicle.status == "On Time") Color(0xFF2E7D32) else Color(0xFFC62828),
                                        CircleShape
                                    )
                            )
                            Text(text = vehicle.label, style = MaterialTheme.typography.titleLarge)
                        }
                        Text("En Route to: ${vehicle.destination}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                        Text("Telemetry: Active • Regenerative brakes fully armed", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = vehicle.status,
                            color = if (vehicle.status == "On Time") Color(0xFF2E7D32) else Color(0xFFC62828),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Dismiss Info",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .clickable { selectedVehicle = null }
                                .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabelOverlay(text: String, xDp: Int, yDp: Int, isRiver: Boolean = false) {
    Box(
        modifier = Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .background(
                if (isRiver) Color.Transparent else Color.White.copy(alpha = 0.85f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isRiver) Color(0xFF0F52BA).copy(alpha = 0.7f) else Color.Black,
            fontSize = 8.sp
        )
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// --- Digital Tickets Screen ---
@Composable
fun TicketsScreen(viewModel: ZurichFlowViewModel, tickets: List<TicketPass>) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Buy, 1 = Wallet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(8.dp))
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("BUY TICKETS (KAUFEN)", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("MY WALLET (${tickets.size})", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text("AUTOMATIC SWISS FARE OPTIMISATION", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Quiet Luxury Fare System", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "Zürich Flow operates on a smart SwissPass automatic token network. We automatically aggregate your commutes and cap fare charges at the cheapest possible daily or monthly pass. Buy with complete peace of mind.",
                                style = MaterialTheme.typography.bodyMedium
                             )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("TICKET CATALOGUE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                }

                // Buy options
                item {
                    CatalogTicketRow(
                        title = "Single Ticket Zone 110",
                        desc = "Valid for 1 hour of unlimited transfers within Zone 110 (City of Zürich).",
                        price = 4.40,
                        type = "Single",
                        viewModel = viewModel
                    )
                }
                item {
                    CatalogTicketRow(
                        title = "Day Pass Zone 110",
                        desc = "Valid for 24 hours of infinite travel on all trams, buses, S-Bahn, and Limmat boats.",
                        price = 8.80,
                        type = "Day Pass",
                        viewModel = viewModel
                    )
                }
                item {
                    CatalogTicketRow(
                        title = "Monthly Abonnent - All Zones",
                        desc = "Premium regional commuter pass. Grants full metropolitan Zürich travel.",
                        price = 85.00,
                        type = "Monthly",
                        viewModel = viewModel
                    )
                }
                item {
                    CatalogTicketRow(
                        title = "SwissPass Halbtax (Half-Fare)",
                        desc = "Gives 50% discount on all Swiss public transit services. Instantly active.",
                        price = 185.00,
                        type = "SwissPass",
                        viewModel = viewModel
                    )
                }
            }
        } else {
            // Wallet view
            if (tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active tickets found in your digital wallet. Head to BUY to add one.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(tickets) { ticket ->
                        SwissPassWidget(ticket = ticket, viewModel = viewModel, expanded = true)
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogTicketRow(
    title: String,
    desc: String,
    price: Double,
    type: String,
    viewModel: ZurichFlowViewModel
) {
    var boughtMessage by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Price: CHF ${"%.2f".format(price)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    viewModel.purchaseTicket(type, title, price)
                    boughtMessage = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (boughtMessage) "Purchased" else "Buy (CHF)", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// --- SwissPass Wallet Card ---
@Composable
fun SwissPassWidget(ticket: TicketPass, viewModel: ZurichFlowViewModel, expanded: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ticket.isSwissPass) Color(0xFFC62828) else Color(0xFF1E1E1E) // SwissPass Red or graphite dark
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = (if (expanded) Modifier.fillMaxWidth() else Modifier.width(310.dp))
            .shadow(4.dp, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Swiss Pass Banner header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (ticket.isSwissPass) "SWISS PASS" else "ZÜRICH FLOW PASS",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SBB CFF FFS SBB-ZVV",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Embedded gold security chip mock
                Box(
                    modifier = Modifier
                        .size(32.dp, 24.dp)
                        .background(Color(0xFFE5C158), RoundedCornerShape(4.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = ticket.title,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Validity details & QR mock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "SECURE CARD ID",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = ticket.qrCodeData,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "VALID UNTIL",
                        color = Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ticket.validUntil)),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Beautiful custom generated QR code mock via Canvas drawing!
                Canvas(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    // Draw random precise pixels mimicking QR SwissPass ticket
                    val rand = java.util.Random(ticket.qrCodeData.hashCode().toLong())
                    val steps = 10
                    val stepX = w / steps
                    val stepY = h / steps
                    for (x in 0 until steps) {
                        for (y in 0 until steps) {
                            if (rand.nextBoolean()) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(x * stepX, y * stepY),
                                    size = androidx.compose.ui.geometry.Size(stepX, stepY)
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Wallet Sync action
                    if (ticket.isGoogleWalletSynced) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Synced to Google Wallet", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.syncGoogleWallet(ticket.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("google_wallet_sync_button")
                        ) {
                            Text("Sync to Google Wallet", color = Color.Black, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Remove pass
                    TextButton(
                        onClick = { viewModel.removeTicket(ticket.id) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                    ) {
                        Text("Delete ticket", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// --- Smart ZURI AI Assistant Screen ---
@Composable
fun ZuriScreen(viewModel: ZurichFlowViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isZuriThinking.collectAsState()
    
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chat Header
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF2E7D32), CircleShape) // Green running status
                    )
                    Column {
                        Text("ZURI • AI MOBILITY CO-PILOT", style = MaterialTheme.typography.titleLarge)
                        Text("Zürich's Intelligent Urban Operating System", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                    }
                }

                TextButton(onClick = { viewModel.clearChat() }) {
                    Text("Reset Chat", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Messages List Container
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { (txt, isUser) ->
                ChatBubble(txt = txt, isUser = isUser)
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                        Text(
                            text = "Zuri is consulting SBB schedule matrix...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Suggestion Chips
        Text("ASK ZURI ANYTHING:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val prompts = listOf(
                "When should I leave for the airport?",
                "Take me somewhere quiet.",
                "Fastest route to Zürich HB?",
                "Best coffee house near Bahnhofstrasse?",
                "Is my train delayed?"
            )
            items(prompts) { p ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(20.dp))
                        .clickable { viewModel.sendChatMessage(p) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(p, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Input text bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask Zuri (e.g. 'Plan a lakeside afternoon'...)") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("zuri_chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendChatMessage(inputText)
                        inputText = ""
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("zuri_send_button")
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send message")
            }
        }
    }
}

@Composable
fun ChatBubble(txt: String, isUser: Boolean) {
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val txtColor = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = txt,
                color = txtColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// --- City Discovery Screen ---
@Composable
fun DiscoverScreen(viewModel: ZurichFlowViewModel) {
    val items = listOf(
        DiscoveryPlace("Landesmuseum", "Museums & History", "A striking neo-Gothic castle holding 800 years of Swiss cultural identity.", "Zürich HB", "Landesmuseum"),
        DiscoveryPlace("Semper Aula", "Architecture & Science", "Designed by architect Gottfried Semper at ETH, representing timeless intellectual symmetry.", "Zürich HB", "ETH Zürich"),
        DiscoveryPlace("Bellevue Lakeside", "Lakeside Activity", "The primary promenade. Elegant quiet afternoons looking towards the Alps.", "Paradeplatz", "Bellevue"),
        DiscoveryPlace("Bahnhofstrasse", "Shopping & Style", "One of the world's most exclusive minimalist luxury avenues.", "Zürich HB", "Bahnhofstrasse"),
        DiscoveryPlace("Bürkliplatz Market", "Markets & Community", "Traditional fresh farmers markets beside the historic ferry docks.", "Zürich HB", "Bürkliplatz"),
        DiscoveryPlace("Uetliberg Peak", "Mountain Transport", "Zürich's local mountain peak. Reached via the panoramic S10 railway line.", "Zürich HB", "Uetliberg")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("CITY DISCOVERY (ENTDECKUNG)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Transform transit into seamless urban discovery. Selected nodes link directly into route planning.", style = MaterialTheme.typography.bodyLarge)
        }

        items(items) { place ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(place.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF8E8E93))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(place.name, style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(place.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateSearchInputs(place.defaultFrom, place.defaultTo)
                            viewModel.performSearch(place.defaultFrom, place.defaultTo)
                            viewModel.setScreen(AppScreen.PLANNER)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DirectionsTransit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Calculate Route From ${place.defaultFrom}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

data class DiscoveryPlace(
    val name: String,
    val category: String,
    val description: String,
    val defaultFrom: String,
    val defaultTo: String
)

// --- Live Service Centre Screen ---
@Composable
fun ServiceScreen(viewModel: ZurichFlowViewModel) {
    val advisories by viewModel.serviceUpdates.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("LIVE SERVICE CENTRE (BETRIEB)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Calm, objective real-time telemetry from Zürich's central transport dispatch.", style = MaterialTheme.typography.bodyLarge)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Column {
                        Text("SYSTEM HEURISTIC: PRECISE", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1B5E20))
                        Text("99.8% of all operations running exactly on-schedule in conformity with Swiss standards.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20))
                    }
                }
            }
        }

        item {
            Text("ACTIVE ADVISORIES", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
        }

        items(advisories) { adv ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TELEMETRY BULLETIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("ACTIVE", color = Color(0xFFE30613), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(adv, style = MaterialTheme.typography.bodyLarge)
                    Text("Impact advisory: Reassuringly minimal. Secondary backup systems have automated routing buffers.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                }
            }
        }
    }
}

// --- Metrics / Sustainability Dashboard Screen ---
@Composable
fun DashboardScreen(viewModel: ZurichFlowViewModel, logs: List<MobilityLog>) {
    var fromText by remember { mutableStateOf("Zürich HB") }
    var toText by remember { mutableStateOf("Airport") }
    var distanceKmText by remember { mutableStateOf("9.5") }
    var selectedMode by remember { mutableStateOf("Tram") }

    val totalKm = logs.sumOf { it.distanceKm }
    val totalCarbonSaved = logs.sumOf { it.carbonSavedKg }
    val totalEnergyKwh = logs.sumOf { it.energyKwh }

    // Mobility Score Calculation
    val baseScore = 75
    val logBonus = (logs.size * 5).coerceAtMost(25)
    val mobilityScore = baseScore + logBonus

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("MY MOBILITY PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
        }

        // Concentric dial score drawing
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Concentric Dial
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val stroke = 12f
                        // Outer background ring
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            radius = size.minDimension / 2 - stroke / 2,
                            style = Stroke(width = stroke)
                        )
                        // Active color ring (Swiss Red)
                        drawArc(
                            color = Color(0xFFE30613),
                            startAngle = -90f,
                            sweepAngle = (mobilityScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = stroke)
                        )
                    }

                    Column {
                        Text("MOBILITY SCORE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$mobilityScore", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                            Text("/100", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Text("Optimized. High integration with Swiss clean-rail networks.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Metrics Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CO₂ SAVED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                        Text("${"%.2f".format(totalCarbonSaved)} kg", style = MaterialTheme.typography.displayMedium, color = Color(0xFF2E7D32))
                        Text("Equivalent to 3 alpine pine trees.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("TOTAL DISTANCE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                        Text("${"%.2f".format(totalKm)} km", style = MaterialTheme.typography.displayMedium)
                        Text("Across Zürich canton.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                    }
                }
            }
        }

        // Commute Logger Form
        item {
            Text("COMMUTE LOGGER (REAL-TIME ADVISORY)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fromText,
                            onValueChange = { fromText = it },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = toText,
                            onValueChange = { toText = it },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = distanceKmText,
                            onValueChange = { distanceKmText = it },
                            label = { Text("Distance (km)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )

                        // Mode selection row
                        Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Tram", "S-Bahn", "Walk", "Cycling").forEach { mode ->
                                val isSelected = selectedMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f))
                                        .clickable { selectedMode = mode }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(mode, color = if (isSelected) Color.White else Color.Black, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val km = distanceKmText.toDoubleOrNull() ?: 1.0
                            viewModel.logCommuteManual(fromText, toText, selectedMode, km)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("log_commute_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Add to Travel History", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // History Log Lists
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("HISTORIC METRIC CHRONOLOGY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                TextButton(onClick = { viewModel.resetLogs() }) {
                    Text("Clear All Logs", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No recorded logs. Log your first trip from the Journey Planner or logger above.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, Color(0xFFE5E5E5)), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        when (log.mode) {
                                            "Walk" -> Color.LightGray
                                            "Cycling" -> Color(0xFF2E7D32)
                                            "S-Bahn" -> Color(0xFF0F52BA)
                                            else -> Color(0xFFE30613)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(log.mode.take(1), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(log.fromStation, style = MaterialTheme.typography.titleLarge)
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(10.dp).padding(horizontal = 2.dp))
                                    Text(log.toStation, style = MaterialTheme.typography.titleLarge)
                                }
                                Text("${log.distanceKm} km • ${log.durationMinutes} min via ${log.mode}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8E8E93))
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("+${"%.2f".format(log.carbonSavedKg)} kg", color = Color(0xFF2E7D32), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("CO₂ Saved", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8E8E93))
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Mini Language Switcher ---
@Composable
fun LanguageToggleMini(viewModel: ZurichFlowViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val currentLang by viewModel.currentLanguage.collectAsState()

    Box(
        modifier = Modifier
            .padding(12.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Text(currentLang.code, style = MaterialTheme.typography.labelSmall)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ZurichLanguage.values().forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.label) },
                    onClick = {
                        viewModel.setLanguage(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}
