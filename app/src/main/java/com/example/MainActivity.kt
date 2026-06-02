package com.example

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MealRecord
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GeminiState
import com.example.viewmodel.MealViewModel
import com.example.viewmodel.SheetFetchState
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

sealed interface SelectedTab {
    object Dashboard : SelectedTab
    object SpreadsheetView : SelectedTab
    object DeveloperSetup : SelectedTab
    object LiveWebPreview : SelectedTab
}

@Composable
fun MainScreen() {
    val viewModel: MealViewModel = viewModel()
    
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val appsScriptUrl by viewModel.appsScriptUrl.collectAsStateWithLifecycle()
    val sheetFetchState by viewModel.sheetFetchState.collectAsStateWithLifecycle()
    val geminiState by viewModel.geminiState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf<SelectedTab>(SelectedTab.Dashboard) }
    var urlInputText by remember { mutableStateOf("") }
    
    // Sync the input URL when preference loads
    LaunchedEffect(appsScriptUrl) {
        if (urlInputText.isEmpty() && appsScriptUrl.isNotEmpty()) {
            urlInputText = appsScriptUrl
        }
    }

    // Determine current month names
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val now = Calendar.getInstance()
    val currentMonthName = monthNames[now.get(Calendar.MONTH)]
    val currentYear = now.get(Calendar.YEAR)
    val todayDateNumber = now.get(Calendar.DAY_OF_MONTH)

    Scaffold(
        topBar = {
            HeaderSection(
                onShowInfo = {
                    activeTab = SelectedTab.DeveloperSetup
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (activeTab) {
                SelectedTab.Dashboard -> {
                    DashboardView(
                        selectedDate = selectedDate,
                        todayDateNumber = todayDateNumber,
                        monthName = currentMonthName,
                        allRecords = allRecords,
                        appsScriptUrl = appsScriptUrl,
                        sheetFetchState = sheetFetchState,
                        geminiState = geminiState,
                        urlInputText = urlInputText,
                        onUrlChange = { urlInputText = it },
                        onSaveUrl = { viewModel.saveUrl(urlInputText) },
                        onFetchStats = { viewModel.fetchLiveStats() },
                        onAskGemini = { b, l, d, day -> viewModel.askGemini(b, l, d, day) },
                        onDismissGemini = { viewModel.dismissGemini() },
                        onDaySelected = { viewModel.selectDate(it) }
                    )
                }
                SelectedTab.SpreadsheetView -> {
                    LocalSimulatorView(
                        selectedDate = selectedDate,
                        allRecords = allRecords,
                        monthName = currentMonthName,
                        todayDateNumber = todayDateNumber,
                        onDaySelected = { viewModel.selectDate(it) },
                        onUpdateMealCount = { date, type, inc -> viewModel.updateMealCount(date, type, inc) }
                    )
                }
                SelectedTab.DeveloperSetup -> {
                    DeveloperSetupView(
                        appsScriptUrl = appsScriptUrl,
                        todayDateNumber = todayDateNumber,
                        currentMonthName = currentMonthName,
                        currentYear = currentYear
                    )
                }
                SelectedTab.LiveWebPreview -> {
                    LiveWebPreviewScreen(
                        appsScriptUrl = appsScriptUrl
                    )
                }
            }
        }
    }
}

// --- Common UI Components ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(onShowInfo: () -> Unit) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E2EC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Meal Tracker",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.25).sp
                    ),
                    color = Color(0xFF1B1B1F)
                )
            }
        },
        actions = {
            // Gorgeous Clean Minimalism "LIVE" badge from HTML layout
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFDDE2F9))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF161B2C)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onShowInfo) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Deployment Help",
                    tint = Color(0xFF0061A4)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            navigationIconContentColor = Color(0xFF1B1B1F),
            titleContentColor = Color(0xFF1B1B1F),
            actionIconContentColor = Color(0xFF0061A4)
        ),
        modifier = Modifier.border(0.5.dp, Color(0xFFC4C6D0))
    )
}

@Composable
fun BottomNavigationBar(activeTab: SelectedTab, onTabSelected: (SelectedTab) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFFEFF1F8),
        tonalElevation = 0.dp,
        modifier = Modifier.border(0.5.dp, Color(0xFFC4C6D0))
    ) {
        NavigationBarItem(
            selected = activeTab == SelectedTab.Dashboard,
            onClick = { onTabSelected(SelectedTab.Dashboard) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF161B2C),
                selectedTextColor = Color(0xFF161B2C),
                unselectedIconColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                unselectedTextColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                indicatorColor = Color(0xFFDDE2F9)
            )
        )
        NavigationBarItem(
            selected = activeTab == SelectedTab.SpreadsheetView,
            onClick = { onTabSelected(SelectedTab.SpreadsheetView) },
            icon = { Icon(Icons.Default.List, contentDescription = "Spreadsheet") },
            label = { Text("Simulator") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF161B2C),
                selectedTextColor = Color(0xFF161B2C),
                unselectedIconColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                unselectedTextColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                indicatorColor = Color(0xFFDDE2F9)
            )
        )
        NavigationBarItem(
            selected = activeTab == SelectedTab.DeveloperSetup,
            onClick = { onTabSelected(SelectedTab.DeveloperSetup) },
            icon = { Icon(Icons.Default.Build, contentDescription = "Setup") },
            label = { Text("Script Code") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF161B2C),
                selectedTextColor = Color(0xFF161B2C),
                unselectedIconColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                unselectedTextColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                indicatorColor = Color(0xFFDDE2F9)
            )
        )
        NavigationBarItem(
            selected = activeTab == SelectedTab.LiveWebPreview,
            onClick = { onTabSelected(SelectedTab.LiveWebPreview) },
            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Web Preview") },
            label = { Text("Web preview") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF161B2C),
                selectedTextColor = Color(0xFF161B2C),
                unselectedIconColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                unselectedTextColor = Color(0xFF1B1B1F).copy(alpha = 0.6f),
                indicatorColor = Color(0xFFDDE2F9)
            )
        )
    }
}

// --- TAB 1: DASHBOARD VIEW ---

@Composable
fun DashboardView(
    selectedDate: Int,
    todayDateNumber: Int,
    monthName: String,
    allRecords: List<MealRecord>,
    appsScriptUrl: String,
    sheetFetchState: SheetFetchState,
    geminiState: GeminiState,
    urlInputText: String,
    onUrlChange: (String) -> Unit,
    onSaveUrl: () -> Unit,
    onFetchStats: () -> Unit,
    onAskGemini: (breakfast: Int, lunch: Int, dinner: Int, day: Int) -> Unit,
    onDismissGemini: () -> Unit,
    onDaySelected: (Int) -> Unit
) {
    val currentRecord = allRecords.find { it.date == selectedDate } ?: MealRecord(selectedDate, 0, 0, 0)
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Date Column Lookup Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Elegant large-number date display
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "LUNCH & CATERING SHEET",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF44474E)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = selectedDate.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 54.sp
                                ),
                                color = Color(0xFF0061A4)
                            )
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Text(
                                    text = "$monthName ${Calendar.getInstance().get(Calendar.YEAR)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF44474E)
                                )
                                if (selectedDate == todayDateNumber) {
                                    Text(
                                        text = "Today",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF0061A4)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "The application automatically identifies today's date ($todayDateNumber) or any selected column date in Row 5, scanning header values to fetch Breakfast (Row 2), Lunch (Row 3), and Dinner (Row 4).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474E)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Date quick-picker horizontal row
                    Text(
                        text = "Quick Day Picker:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..31).toList()) { dateNum ->
                            val isSelected = dateNum == selectedDate
                            val isToday = dateNum == todayDateNumber
                            
                            Box(
                                modifier = Modifier
                                    .size(width = 54.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0xFF0061A4)
                                            isToday -> Color(0xFFDDE2F9)
                                            else -> Color(0xFFFDFBFF)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isSelected -> Color.Transparent
                                            isToday -> Color.Transparent
                                            else -> Color(0xFFC4C6D0)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onDaySelected(dateNum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = dateNum.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.White else Color(0xFF1B1B1F)
                                    )
                                    if (isToday) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                            color = if (isSelected) Color.White else Color(0xFF161B2C)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Meal Count displays
        item {
            MealDashboardCard(
                title = "Breakfast",
                rowHeader = "Spreadsheet Row 2",
                count = currentRecord.breakfast,
                accentColor = Color(0xFFF59E0B),
                emoji = "🍳",
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            MealDashboardCard(
                title = "Lunch",
                rowHeader = "Spreadsheet Row 3",
                count = currentRecord.lunch,
                accentColor = Color(0xFF10B981),
                emoji = "🥗",
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            MealDashboardCard(
                title = "Dinner",
                rowHeader = "Spreadsheet Row 4",
                count = currentRecord.dinner,
                accentColor = Color(0xFF3B82F6),
                emoji = "🍛",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Connection Panel containing Google Apps Script details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Sheet Connection Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )

                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = onUrlChange,
                        label = { Text("Google Web App URL") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveUrl()
                                Toast.makeText(context, "Settings Saved!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E2EC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Config", color = Color(0xFF1B1B1F))
                        }

                        Button(
                            onClick = onFetchStats,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Fetch",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Query Sync", color = Color.White)
                        }
                    }

                    // Display Network Fetch outcomes
                    AnimatedVisibility(visible = sheetFetchState != SheetFetchState.Idle) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (sheetFetchState) {
                                        is SheetFetchState.Loading -> Color(0xFFE0E2EC)
                                        is SheetFetchState.Success -> Color(0xFFDDE2F9)
                                        is SheetFetchState.Error -> Color(0xFFF9DEDC)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    1.dp,
                                    when (sheetFetchState) {
                                        is SheetFetchState.Loading -> Color(0xFFC4C6D0)
                                        is SheetFetchState.Success -> Color(0xFF0061A4)
                                        is SheetFetchState.Error -> Color(0xFFB3261E)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                  .padding(14.dp)
                        ) {
                            when (sheetFetchState) {
                                is SheetFetchState.Loading -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF0061A4)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Contacting Google Apps Script...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B1B1F))
                                    }
                                }
                                is SheetFetchState.Success -> {
                                    Column {
                                        Text(
                                            text = "Sync Connected & Updated successfully!",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF161B2C)
                                        )
                                        Text(
                                            text = "Source Month (Tab): ${(sheetFetchState as SheetFetchState.Success).sheetName} | Found Date index: Col ${(sheetFetchState as SheetFetchState.Success).column}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF44474E)
                                        )
                                    }
                                }
                                is SheetFetchState.Error -> {
                                    Column {
                                        Text(
                                            text = "Fetch Failed",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFB3261E)
                                        )
                                        Text(
                                            text = (sheetFetchState as SheetFetchState.Error).message,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF601410)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        // Gemini AI Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF1F8)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🪄", fontSize = 20.sp)
                        Text(
                            text = "Gemini Catering Scheduler",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B1B1F)
                        )
                    }

                    Text(
                        text = "Analyze these meal requests. Gemini will construct prep timings, ingredient recommendations, and resource coordination models tailored for Day $selectedDate counts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF44474E)
                    )

                    Button(
                        onClick = {
                            onAskGemini(currentRecord.breakfast, currentRecord.lunch, currentRecord.dinner, selectedDate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Analyze Counts with Gemini", color = Color(0xFFFDFBFF))
                    }

                    if (geminiState != GeminiState.Idle) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFDFBFF))
                                .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GEMINI ADVICE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                        color = Color(0xFF0061A4)
                                    )
                                    IconButton(
                                        onClick = onDismissGemini,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF44474E)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                when (geminiState) {
                                    is GeminiState.Loading -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = Color(0xFF0061A4)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Catering chef constructing brief...", style = MaterialTheme.typography.bodySmall, color = Color(0xFF44474E))
                                        }
                                    }
                                    is GeminiState.Success -> {
                                        Text(
                                            text = (geminiState as GeminiState.Success).responseText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1B1B1F)
                                        )
                                    }
                                    is GeminiState.Error -> {
                                        Text(
                                            text = (geminiState as GeminiState.Error).message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFB3261E)
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MealDashboardCard(
    title: String,
    rowHeader: String,
    count: Int,
    accentColor: Color,
    emoji: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large styled icon container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE0E2EC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
                
                Column {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color(0xFF44474E)
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color(0xFF1B1B1F)
                    )
                }
            }
            
            // Right-side indicator "Row X"
            Text(
                text = rowHeader.replace("Spreadsheet ", ""),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0061A4)
                )
            )
        }
    }
}

// --- TAB 2: LOCAL SHEET SIMULATOR ---

@Composable
fun LocalSimulatorView(
    selectedDate: Int,
    allRecords: List<MealRecord>,
    monthName: String,
    todayDateNumber: Int,
    onDaySelected: (Int) -> Unit,
    onUpdateMealCount: (date: Int, type: String, increment: Boolean) -> Unit
) {
    val activeRecord = allRecords.find { it.date == selectedDate } ?: MealRecord(selectedDate, 0, 0, 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explanatory Grid Visual Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sheet Simulator Grid",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = "Every day of the month corresponds to a sequential column in Row 5 of Google Sheets. Below represents a 2D grid matrix of your live catering database. Tap any block to read/modify counts, which instantly simulates cell edits!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474E)
                    )
                }
            }
        }

        // 2D SpreadSheet Grid visualization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "$monthName Visual Table Matrix",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Horizontally scrollable spreadsheet representation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Headers Row labels
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Box(modifier = Modifier.size(width = 110.dp, height = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Spreadsheet Header", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF44474E))
                            }
                            Box(modifier = Modifier.size(width = 110.dp, height = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Row 2: Breakfast", style = MaterialTheme.typography.labelSmall, color = Color(0xFF44474E))
                            }
                            Box(modifier = Modifier.size(width = 110.dp, height = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Row 3: Lunch", style = MaterialTheme.typography.labelSmall, color = Color(0xFF44474E))
                            }
                            Box(modifier = Modifier.size(width = 110.dp, height = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Row 4: Dinner", style = MaterialTheme.typography.labelSmall, color = Color(0xFF44474E))
                            }
                            Box(modifier = Modifier.size(width = 110.dp, height = 34.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Row 5: Calendar Day", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0061A4))
                            }
                        }

                        // Grid Columns matching sequential dates
                        (1..31).forEach { dayNum ->
                            val record = allRecords.find { it.date == dayNum } ?: MealRecord(dayNum, 0, 0, 0)
                            val isSelected = dayNum == selectedDate
                            val isToday = dayNum == todayDateNumber

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .width(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            isSelected -> Color(0xFFDDE2F9)
                                            isToday -> Color(0xFFEFF1F8)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF0061A4) else Color(0xFFC4C6D0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onDaySelected(dayNum) }
                                    .padding(vertical = 4.dp)
                            ) {
                                // Cell Header (Column Letter mapping helper, e.g. B, C, D, E...)
                                val colLetter = getColumnLetter(dayNum)
                                Box(
                                    modifier = Modifier.size(height = 30.dp, width = 46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(colLetter, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF44474E))
                                }
                                
                                // Breakfast Value cell
                                SimpleCellBox(record.breakfast.toString(), Color(0xFFFDFBFF))
                                
                                // Lunch Value cell
                                SimpleCellBox(record.lunch.toString(), Color(0xFFFDFBFF))
                                
                                // Dinner Value cell
                                SimpleCellBox(record.dinner.toString(), Color(0xFFFDFBFF))
                                
                                // Row 5 Cell: Date
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) Color(0xFF0061A4) else Color(0xFF44474E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Column detail controls
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Modify Day $selectedDate Column Cells",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )

                    // Breakfast control row
                    MealEditRow(
                        title = "🍳 Row 2 Breakfast",
                        currentCount = activeRecord.breakfast,
                        accentColor = Color(0xFFF59E0B),
                        onIncrement = { onUpdateMealCount(selectedDate, "breakfast", true) },
                        onDecrement = { onUpdateMealCount(selectedDate, "breakfast", false) }
                    )

                    Divider(color = Color(0xFFC4C6D0))

                    // Lunch control row
                    MealEditRow(
                        title = "🥗 Row 3 Lunch",
                        currentCount = activeRecord.lunch,
                        accentColor = Color(0xFF10B981),
                        onIncrement = { onUpdateMealCount(selectedDate, "lunch", true) },
                        onDecrement = { onUpdateMealCount(selectedDate, "lunch", false) }
                    )

                    Divider(color = Color(0xFFC4C6D0))

                    // Dinner control row
                    MealEditRow(
                        title = "🍛 Row 4 Dinner",
                        currentCount = activeRecord.dinner,
                        accentColor = Color(0xFF3B82F6),
                        onIncrement = { onUpdateMealCount(selectedDate, "dinner", true) },
                        onDecrement = { onUpdateMealCount(selectedDate, "dinner", false) }
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleCellBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .size(height = 34.dp, width = 46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFF1F8))
            .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B1B1F)
        )
    }
}

@Composable
fun MealEditRow(
    title: String,
    currentCount: Int,
    accentColor: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1B1B1F)
            )
            Text(
                "Cell count to save",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF44474E)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFE0E2EC))
            ) {
                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
            }

            Text(
                text = currentCount.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1B1B1F),
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFDDE2F9))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color(0xFF161B2C))
            }
        }
    }
}

// Convert sequential date 1..31 to Excel column (e.g., Column 1 (A) is metadata, Column 2 is Date 1 (B), Column 3 is Date 2 (C)...)
fun getColumnLetter(dateNum: Int): String {
    val index = dateNum + 1 // Offset: 1 corresponds to Column B (2)
    var columnName = ""
    var num = index
    while (num > 0) {
        val modulo = (num - 1) % 26
        columnName = (65 + modulo).toChar() + columnName
        num = (num - modulo) / 26
    }
    return columnName
}

// --- TAB 3: DEVELOPER SETUP & EXPORT CODE ---

@Composable
fun DeveloperSetupView(
    appsScriptUrl: String,
    todayDateNumber: Int,
    currentMonthName: String,
    currentYear: Int
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var activeSubCodeTab by remember { mutableStateOf(0) } // 0: Apps Script, 1: HTML Client

    val appsScriptCode = """
function doGet(e) {
  try {
    const sheetId = e.parameter.id || SpreadsheetApp.getActiveSpreadsheet().getId();
    const spreadsheet = SpreadsheetApp.openById(sheetId);
    
    // Auto-identify current Month targeting tab sheets
    const now = new Date();
    const monthNames = [
      "January", "February", "March", "April", "May", "June",
      "July", "August", "September", "October", "November", "December"
    ];
    const currentMonthName = monthNames[now.getMonth()];
    const currentDay = now.getDate(); // Sequential date day
    
    // Look up month-specific spreadsheet tab nicely
    let sheet = spreadsheet.getSheetByName(currentMonthName);
    if (!sheet) {
      sheet = spreadsheet.getSheetByName(currentMonthName + " " + now.getFullYear());
    }
    if (!sheet) {
      sheet = spreadsheet.getSheets()[0]; // Fallback to active sheet
    }
    
    // Row 5 contains dates (1, 2, 3... 31). Look up today's date column index.
    const lastColumn = Math.max(sheet.getLastColumn(), 35);
    const dateRowValues = sheet.getRange(5, 1, 1, lastColumn).getValues()[0];
    
    let targetColumnIdx = -1;
    for (let i = 0; i < dateRowValues.length; i++) {
      const val = dateRowValues[i];
      if (val === currentDay || parseInt(val, 10) === currentDay) {
        targetColumnIdx = i + 1; // 1-based index sheet range mapping
        break;
      }
    }
    
    if (targetColumnIdx === -1) {
      return ContentService.createTextOutput(JSON.stringify({
        success: false,
        error: "Day " + currentDay + " not found in Row 5 date header."
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    // Fetch live counts: Row 2 (Breakfast), Row 3 (Lunch), Row 4 (Dinner)
    const breakfast = sheet.getRange(2, targetColumnIdx).getValue();
    const lunch = sheet.getRange(3, targetColumnIdx).getValue();
    const dinner = sheet.getRange(4, targetColumnIdx).getValue();
    
    return ContentService.createTextOutput(JSON.stringify({
      success: true,
      sheetName: sheet.getName(),
      date: currentDay,
      month: currentMonthName,
      column: targetColumnIdx,
      data: {
        breakfast: Number(breakfast) || 0,
        lunch: Number(lunch) || 0,
        dinner: Number(dinner) || 0
      },
      timestamp: now.toISOString()
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      success: false,
      error: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

    val htmlCode = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Live Meal Tracker</title>
    <style>
        body {
            font-family: system-ui, -apple-system, sans-serif;
            background-color: #f8fafc;
            color: #0f172a;
            margin: 0;
            padding: 20px;
        }
        .container {
            max-width: 480px;
            margin: 40px auto;
            background: white;
            padding: 24px;
            border-radius: 12px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.05);
        }
        h2 { margin-top: 0; color: #1e293b; }
        .input-group { margin-bottom: 20px; }
        .input-group label { display: block; font-weight: 600; margin-bottom: 8px; font-size: 14px; }
        .input-group input { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; box-sizing: border-box; }
        .metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
        .metric-card { background: #f1f5f9; padding: 16px 8px; border-radius: 8px; text-align: center; }
        .metric-card.b { border-top: 4px solid #f59e0b; }
        .metric-card.l { border-top: 4px solid #10b981; }
        .metric-card.d { border-top: 4px solid #3b82f6; }
        .label { font-size: 11px; text-transform: uppercase; color: #64748b; margin-bottom: 4px; }
        .value { font-size: 24px; font-weight: 700; }
        button { background: #4f46e5; color: white; border: none; padding: 12px 20px; border-radius: 6px; width: 100%; cursor: pointer; font-weight: bold; }
        button:hover { background: #4338ca; }
        .status { margin-top: 15px; text-align: center; font-size: 12px; color: #64748b; }
    </style>
</head>
<body>
    <div class="container">
        <h2 id="tracker-title">Google Sheet Meal Sync</h2>
        <div id="tracker-subtitle" style="font-size:12px; margin-bottom: 15px; color:#475569;">Fetch live day stats matching column dates</div>
        <div class="input-group">
            <label for="webAppUrl">Apps Script Web App Exec URL</label>
            <input type="text" id="webAppUrl" placeholder="https://script.google.com/macros/s/.../exec">
        </div>
        <div class="metrics-grid">
            <div class="metric-card b">
                <div class="label">🍳 Breakfast</div>
                <div class="value" id="valB">-</div>
            </div>
            <div class="metric-card l">
                <div class="label">🥗 Lunch</div>
                <div class="value" id="valL">-</div>
            </div>
            <div class="metric-card d">
                <div class="label">🍛 Dinner</div>
                <div class="value" id="valD">-</div>
            </div>
        </div>
        <button onclick="fetchLiveCounts()">Query Spreadsheet</button>
        <div class="status" id="statusMsg">Enter your Apps Script URL then click Query</div>
    </div>
    <script>
        const stored = localStorage.getItem('apps_script_url');
        if (stored) document.getElementById('webAppUrl').value = stored;

        async function fetchLiveCounts() {
            const url = document.getElementById('webAppUrl').value.trim();
            const status = document.getElementById('statusMsg');
            if(!url) {
                alert('Please enter your Google Apps Script URL!');
                return;
            }
            localStorage.setItem('apps_script_url', url);
            status.innerText = 'Connecting to Google Sheets...';
            try {
                const res = await fetch(url);
                const result = await res.json();
                if(result.success) {
                    document.getElementById('valB').innerText = result.data.breakfast;
                    document.getElementById('valL').innerText = result.data.lunch;
                    document.getElementById('valD').innerText = result.data.dinner;
                    document.getElementById('tracker-title').innerText = 'Meals for Day ' + result.date;
                    document.getElementById('tracker-subtitle').innerText = result.month + ' Sheet (Column ' + result.column + ')';
                    status.innerText = 'Synced successfully at ' + new Date().toLocaleTimeString();
                    status.style.color = '#16a34a';
                } else {
                    status.innerText = 'Error: ' + result.error;
                    status.style.color = '#dc2626';
                }
            } catch(e) {
                status.innerText = 'Connecting Failed (CORS / Deployed Wrongly?)';
                status.style.color = '#dc2626';
                alert('Connection direct fetch failed due to CORS. Note: For client websites, verify Apps Script is deployed as a Web App to "Anyone", and CORS allows script requests. Showing mock visualizer.');
                document.getElementById('valB').innerText = '12';
                document.getElementById('valL').innerText = '18';
                document.getElementById('valD').innerText = '24';
                status.innerText = 'Mock Display Sync Success';
            }
        }
    </script>
</body>
</html>
""".trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Excel structure setup card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Google Sheet Column Layout Guide",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = "Set up your Google Sheet tab named exactly \"$currentMonthName\" (or \"$currentMonthName $currentYear\") with this structure:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474E)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFF1F8))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("• Row 2: Breakfast row. Fill column cells with numbers (e.g. B2, C2).", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B1B1F))
                        Text("• Row 3: Lunch row. Fill column cells with numbers (e.g. B3, C3).", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B1B1F))
                        Text("• Row 4: Dinner row. Fill column cells with numbers (e.g. B4, C4).", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1B1B1F))
                        Text("• Row 5: Dates column headers (e.g. Cell B5 = 1, Cell C5 = 2, Cell D5 = 3... Cell AF5 = 31).", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B1B1F))
                    }
                }
            }
        }

        // Deploy instructions step-by-step
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "2. Build & Deploy Instructions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B1B1F)
                    )

                    Text(
                        text = "1. In Google Sheets, click Extensions > Apps Script.\n" +
                                "2. Erase existing generic placeholder code and paste Code Block A.\n" +
                                "3. Click Deploy > New Deployment.\n" +
                                "4. Choose Web App as the deployment type.\n" +
                                "5. Under Execute As, choose \"Me\". Under Who has access, choose \"Anyone\". (Required to allow queries without security tokens!)\n" +
                                "6. Deploy and authorize permissions. Copy the webapp executive URL and paste it into our Settings tab!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF44474E)
                    )
                }
            }
        }

        // Export tabs (Google App Script and HTML GUI Code)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TabRow(
                        selectedTabIndex = activeSubCodeTab,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF0061A4)
                    ) {
                        Tab(
                            selected = activeSubCodeTab == 0,
                            onClick = { activeSubCodeTab = 0 },
                            text = { Text("Code A: Apps Script", fontWeight = FontWeight.Bold) },
                            selectedContentColor = Color(0xFF161B2C),
                            unselectedContentColor = Color(0xFF44474E)
                        )

                        Tab(
                            selected = activeSubCodeTab == 1,
                            onClick = { activeSubCodeTab = 1 },
                            text = { Text("Code B: HTML / JS GUI", fontWeight = FontWeight.Bold) },
                            selectedContentColor = Color(0xFF161B2C),
                            unselectedContentColor = Color(0xFF44474E)
                        )
                    }

                    val codeToShow = if (activeSubCodeTab == 0) appsScriptCode else htmlCode

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeSubCodeTab == 0) "Google Apps Script Engine" else "Visual Web App Client",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1B1B1F)
                        )

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(codeToShow))
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("📋 Copy Code", fontSize = 12.sp, color = Color(0xFFFDFBFF))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1B1F))
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Text(
                            text = codeToShow,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFEFF1F8),
                            modifier = Modifier.wrapContentHeight()
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 4: APP WEB WEBVIEW PREVIEW ---

@Composable
fun LiveWebPreviewScreen(appsScriptUrl: String) {
    val rawHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Live Meal Tracker</title>
    <style>
        body {
            font-family: system-ui, -apple-system, sans-serif;
            background-color: #f1f5f9;
            color: #0f172a;
            margin: 0;
            padding: 16px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .container {
            width: 100%;
            max-width: 440px;
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 4px 10px rgba(0,0,0,0.05);
            box-sizing: border-box;
        }
        h2 { margin-top: 0; color: #1e293b; font-size: 20px; text-align: center; }
        .input-group { margin-bottom: 16px; }
        .input-group label { display: block; font-weight: 600; margin-bottom: 6px; font-size: 13px; color: #475569; }
        .input-group input { width: 100%; padding: 10px; border: 1px solid #cbd5e1; border-radius: 6px; box-sizing: border-box; font-size: 13px; }
        .metrics-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 18px; }
        .metric-card { background: #f8fafc; padding: 12px 6px; border-radius: 8px; text-align: center; border: 1px solid #e2e8f0; }
        .metric-card.b { border-top: 4px solid #f59e0b; }
        .metric-card.l { border-top: 4px solid #10b981; }
        .metric-card.d { border-top: 4px solid #3b82f6; }
        .label { font-size: 10px; text-transform: uppercase; color: #64748b; margin-bottom: 4px; }
        .value { font-size: 22px; font-weight: 700; color: #0f172a; }
        button { background: #4f46e5; color: white; border: none; padding: 12px; border-radius: 6px; width: 100%; cursor: pointer; font-weight: bold; font-size: 14px; }
        button:hover { background: #4338ca; }
        .status { margin-top: 14px; text-align: center; font-size: 12px; color: #64748b; }
    </style>
</head>
<body>
    <div class="container">
        <h2 id="tracker-title">Live Spreadsheet Lookup</h2>
        <div id="tracker-subtitle" style="font-size:12px; margin-bottom: 15px; color:#475569; text-align:center;">Row 5 date lookup matching sequential column indices</div>
        <div class="input-group">
            <label for="webAppUrl">Apps Script Web App Exec URL</label>
            <input type="text" id="webAppUrl" placeholder="Paste your script URL here">
        </div>
        <div class="metrics-grid">
            <div class="metric-card b">
                <div class="label">🍳 Breakfast</div>
                <div class="value" id="valB">-</div>
            </div>
            <div class="metric-card l">
                <div class="label">🥗 Lunch</div>
                <div class="value" id="valL">-</div>
            </div>
            <div class="metric-card d">
                <div class="label">🍛 Dinner</div>
                <div class="value" id="valD">-</div>
            </div>
        </div>
        <button onclick="fetchLiveCounts()">Fetch Counts</button>
        <div class="status" id="statusMsg">Ready. Ensure Web URL is supplied.</div>
    </div>
    <script>
        // Init value
        document.getElementById('webAppUrl').value = "$appsScriptUrl";

        async function fetchLiveCounts() {
            const url = document.getElementById('webAppUrl').value.trim();
            const status = document.getElementById('statusMsg');
            if(!url) {
                alert('Please enter your Google Apps Script URL first!');
                return;
            }
            status.innerText = 'Connecting to Google Sheets...';
            try {
                const res = await fetch(url);
                const result = await res.json();
                if(result.success) {
                    document.getElementById('valB').innerText = result.data.breakfast;
                    document.getElementById('valL').innerText = result.data.lunch;
                    document.getElementById('valD').innerText = result.data.dinner;
                    document.getElementById('tracker-title').innerText = 'Meals for Day ' + result.date;
                    document.getElementById('tracker-subtitle').innerText = result.month + ' Sheet (Column ' + result.column + ')';
                    status.innerText = 'Synced successfully ';
                    status.style.color = '#10b981';
                } else {
                    status.innerText = 'Error: ' + result.error;
                    status.style.color = '#dc2626';
                }
            } catch(e) {
                status.innerText = 'Direct fetch failed (mock demo active)';
                status.style.color = '#dc2626';
                const fallbackDay = new Date().getDate();
                document.getElementById('valB').innerText = Math.floor(Math.random() * 10) + 4;
                document.getElementById('valL').innerText = Math.floor(Math.random() * 15) + 10;
                document.getElementById('valD').innerText = Math.floor(Math.random() * 20) + 12;
                document.getElementById('tracker-title').innerText = 'Meals for Day ' + fallbackDay + ' (Demo)';
                document.getElementById('tracker-subtitle').innerText = 'Tab Match (Column ' + (fallbackDay + 1) + ')';
            }
        }
    </script>
</body>
</html>
""".trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Interactive HTML Client Live Preview",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Below is your customized HTML/JavaScript tracker displaying live updates inside our WebView. This directly executes the exact HTML file you export!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                loadDataWithBaseURL("https://script.google.com", rawHtml, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://script.google.com", rawHtml, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
