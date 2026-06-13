package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StockNews
import com.example.data.SyncError
import com.example.data.SyncConfig
import com.example.api.StockSyncCoordinator
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MarketPulseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val vm: MarketPulseViewModel = viewModel()
                val currentScreenState by vm.currentScreen.collectAsState()
                val isLoggedInState by vm.isLoggedIn.collectAsState()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreenState) {
                        "AUTH_LOGIN" -> AuthScreen(vm = vm, isSignUp = false)
                        "AUTH_SIGNUP" -> AuthScreen(vm = vm, isSignUp = true)
                        else -> MainAppShell(vm = vm)
                    }
                }
            }
        }
    }
}

// ==========================================
// AUTHENTICATION SCREEN FOR THE APP
// ==========================================
@Composable
fun AuthScreen(vm: MarketPulseViewModel, isSignUp: Boolean) {
    val email by vm.authEmailInput.collectAsState()
    val password by vm.authPasswordInput.collectAsState()
    val name by vm.authNameInput.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131215),
                        Color(0xFF1C1B1F)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Brand Mark
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFD0BCFF))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync icon",
                    tint = Color(0xFF381E72),
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "MarketPulse Sync",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E9),
                fontFamily = FontFamily.SansSerif
            )
            
            Text(
                text = "AUTOMATED STOCK ANALYZER & SHEETS LINKER",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = Color(0xFFD0BCFF),
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )
            
            // Credentials Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2B2930)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Register Account" else "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E9),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (isSignUp) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { vm.authNameInput.value = it },
                            label = { Text("Full Name", color = Color(0xFFCAC4D0)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name icon", tint = Color(0xFFD0BCFF)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("name_input")
                        )
                    }
                    
                    OutlinedTextField(
                        value = email,
                        onValueChange = { vm.authEmailInput.value = it },
                        label = { Text("Email Address", color = Color(0xFFCAC4D0)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email icon", tint = Color(0xFFD0BCFF)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("email_input")
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { vm.authPasswordInput.value = it },
                        label = { Text("Password", color = Color(0xFFCAC4D0)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Lock icon", tint = Color(0xFFD0BCFF)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color(0xFF49454F),
                            focusedLabelColor = Color(0xFFD0BCFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("password_input")
                    )
                    
                    // Main Email Login CTA
                    Button(
                        onClick = {
                            if (isSignUp) vm.signUp() else vm.login("Email")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEADDFF),
                            contentColor = Color(0xFF21005D)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("email_submit_button")
                    ) {
                        Text(
                            text = if (isSignUp) "Register with Email" else "Sign In with Email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Option Separator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF49454F))
                        Text(
                            text = "OR CONTINUE WITH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF938F99),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF49454F))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Providers Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // GOOGLE OPTION
                        OutlinedButton(
                            onClick = { vm.login("Google") },
                            shape = RoundedCornerShape(14.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("google_login_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE6E1E9))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Dynamic styled G brand
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(2.dp, Color(0xFFD0BCFF), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // GITHUB OPTION
                        OutlinedButton(
                            onClick = { vm.login("GitHub") },
                            shape = RoundedCornerShape(14.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("github_login_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE6E1E9))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFFCAC4D0), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GitHub", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Authentication toggle text
                    Text(
                        text = if (isSignUp) "Already have an account? Sign In" else "New to MarketPulse? Sign Up",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier
                            .clickable {
                                if (isSignUp) vm.navigateTo("AUTH_LOGIN") else vm.navigateTo("AUTH_SIGNUP")
                            }
                            .testTag("auth_toggle_link")
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Demo quick access bypass
            Text(
                text = "⚡ QUICK ACCESS DEMO BYPASS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEADDFF).copy(alpha = 0.6f),
                modifier = Modifier
                    .clickable { vm.login("Demo", "inflexlabs@gmail.com") }
                    .padding(8.dp)
            )
        }
    }
}


// ==========================================
// CORE SHELL FOR THE AUTHENTICATED SYSTEM
// ==========================================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppShell(vm: MarketPulseViewModel) {
    val currentScreenState by vm.currentScreen.collectAsState()
    val loggedInEmailState by vm.loggedInEmail.collectAsState()
    val activeAlertMessage by vm.activeAlertMessage.collectAsState()
    val activeAlertDetails by vm.activeAlertDetails.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(Color(0xFF1C1B1F))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF49454F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "App logo symbol",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "MarketPulse Sync",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE6E1E9)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = loggedInEmailState,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { vm.logout() },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log out button",
                            tint = Color(0xFFF2B8B5)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Elegant bottom navigation designed exactly like the Sophisticated Dark concept
            NavigationBar(
                containerColor = Color(0xFF1C1B1F),
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentScreenState == "DASHBOARD",
                    onClick = { vm.navigateTo("DASHBOARD") },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFFD0BCFF),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFF938F99)
                    )
                )
                
                NavigationBarItem(
                    selected = currentScreenState == "SIMULATED_EMAILS",
                    onClick = { vm.navigateTo("SIMULATED_EMAILS") },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Sent emails log") },
                    label = { Text("Inboxes", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFFD0BCFF),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFF938F99)
                    )
                )

                NavigationBarItem(
                    selected = currentScreenState == "SIMULATED_SHEETS",
                    onClick = { vm.navigateTo("SIMULATED_SHEETS") },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "View Sheets Rows") },
                    label = { Text("Sheets", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFFD0BCFF),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFF938F99)
                    )
                )
                
                NavigationBarItem(
                    selected = currentScreenState == "SETTINGS" || currentScreenState == "HISTORY",
                    onClick = { vm.navigateTo("SETTINGS") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings config") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF21005D),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFFD0BCFF),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFF938F99)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1B1F))
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreenState,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) { screen ->
                when (screen) {
                    "DASHBOARD" -> DashboardScreen(vm = vm)
                    "SIMULATED_EMAILS" -> InboxesScreen(vm = vm)
                    "SIMULATED_SHEETS" -> SheetsScreen(vm = vm)
                    "SETTINGS" -> SettingsScreen(vm = vm)
                    "HISTORY" -> SettingsScreen(vm = vm) // Unified under config settings block
                    else -> DashboardScreen(vm = vm)
                }
            }
            
            // ERROR CRITICAL DIALOG - SENDS ALERTS INSTANTLY
            if (activeAlertMessage != null) {
                AlertDialog(
                    onDismissRequest = { vm.dismissAlert() },
                    icon = { Icon(Icons.Default.Warning, contentDescription = "Warning logo", tint = Color(0xFFF2B8B5)) },
                    title = { Text("System Sync Interrupted", color = Color(0xFFE6E1E9), fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text(
                                text = activeAlertMessage ?: "",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF2B8B5),
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = activeAlertDetails ?: "",
                                color = Color(0xFFCAC4D0),
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF601410).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF601410), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "📪 SMTP Dispatcher automatically sent a breakdown report of this crash to inflexlabs@gmail.com detailing this failure.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFF2B8B5),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { vm.dismissAlert() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD0BCFF))
                        ) {
                            Text("Acknowledge")
                        }
                    },
                    containerColor = Color(0xFF2B2930),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}


// ==========================================
// DASHBOARD VIEW
// ==========================================
@Composable
fun DashboardScreen(vm: MarketPulseViewModel) {
    val allNewsState by vm.allNews.collectAsState()
    val isSyncingState by vm.isSyncing.collectAsState()
    val lastSyncStatusMessage by vm.lastSyncStatusMessage.collectAsState()
    val configState by vm.currentConfig.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Primary Card with Status details
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_primary_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF381E72), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSyncingState) Icons.Default.Refresh else Icons.Default.Check,
                                contentDescription = "Active Check Icon",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF381E72), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ACTIVE SYNC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Text(
                        text = if (isSyncingState) "Syncing..." else "Sync Automated",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF381E72)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = lastSyncStatusMessage ?: "Inbox watcher looking for stock news updates.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF381E72).copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        // Two connection nodes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Node 1: Destination
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF49454F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Inbox Node",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Destination", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                        Text(
                            text = configState?.userEmail ?: "inflexlabs@gmail.com",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Node 2: Google Sheets target
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF49454F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Storage Log",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Storage Sheet", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                        Text(
                            text = configState?.googleSheetName ?: "MarketPulse_Log_2026",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E9),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Email manual dispatch trigger simulator
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📥 SIMULATE STOCK EMAIL FILTER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    var expandedSelect by remember { mutableStateOf(false) }
                    val sampleEmails = listOf(
                        "NVDA Boost: Nvidia hits record revenues amid intense data center upgrades." to "NVIDIA Spike",
                        "AAPL Upgrade: Apple Intelligence sets incredible consumer demand targets." to "APPLE Surge",
                        "TSLA Deliveries: Tesla exceeds quarterly expectations as volume rises." to "TESLA Updates"
                    )

                    Button(
                        onClick = { expandedSelect = !expandedSelect },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49454F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Stock Template News", fontSize = 12.sp)
                    }

                    if (expandedSelect) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            sampleEmails.forEach { (text, label) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF1C1B1F), RoundedCornerShape(6.dp))
                                        .clickable {
                                            vm.customEmailBodyInput.value = text
                                            vm.customEmailSubjectInput.value = "STOCK ALERT: " + label
                                            expandedSelect = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(text = label, color = Color(0xFFE6E1E9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Raw text field input option
                    OutlinedTextField(
                        value = vm.customEmailBodyInput.collectAsState().value,
                        onValueChange = { vm.customEmailBodyInput.value = it },
                        label = { Text("Write Custom Email Body to pass filter...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(84.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Or type custom company triggers like 'AAPL Surge' or 'NVDA Growth'.",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { vm.triggerSync() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEADDFF),
                                contentColor = Color(0xFF21005D)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("run_now_action")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Run Icon", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Now")
                            }
                        }
                    }
                }
            }
        }
        
        // Latest Activity preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Recent Activity Log",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD0BCFF),
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Text(
                    text = "View History Tab",
                    fontSize = 11.sp,
                    color = Color(0xFF938F99),
                    modifier = Modifier
                        .clickable { vm.navigateTo("SETTINGS") }
                )
            }
        }
        
        if (allNewsState.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty Log",
                            tint = Color(0xFF938F99),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No stock transactions synced yet",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6E1E9),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Run a test email sync using the simulator options above to watch columns write and emails trigger!",
                            color = Color(0xFF938F99),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(allNewsState.take(5)) { news ->
                ActivityRowItem(news)
            }
        }
    }
}

@Composable
fun ActivityRowItem(news: StockNews) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Colored sentiment indicator dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = when (news.sentiment) {
                                "Bullish" -> Color(0xFF81C784)
                                "Bearish" -> Color(0xFFE57373)
                                else -> Color(0xFFFFD54F)
                            },
                            shape = CircleShape
                        )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "${news.companyName} (${news.ticker})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E9)
                    )
                    Text(
                        text = "Sheet Row index: " + (if (news.sheetRowIndex == -1) "FAILED" else news.sheetRowIndex),
                        fontSize = 11.sp,
                        color = Color(0xFF938F99)
                    )
                    Text(
                        text = "Price: $${news.price} | Sentiment: ${news.sentiment}",
                        fontSize = 11.sp,
                        color = Color(0xFFD0BCFF)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(news.timestamp)),
                    fontSize = 10.sp,
                    color = Color(0xFF938F99)
                )
                
                Box(
                    modifier = Modifier
                        .background(
                            if (news.syncStatus == "SUCCESS") Color(0xFF381E72) else Color(0xFF601410),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = news.syncStatus,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}


// ==========================================
// INBOX SIMULATED SENT EMAILS VIEW
// ==========================================
@Composable
fun InboxesScreen(vm: MarketPulseViewModel) {
    var expandedEmailId by remember { mutableStateOf<Int?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📪 SMTP Dispatch Outbox",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E9)
                )
                Text(
                    text = "Live dispatches sent to inflexlabs@gmail.com",
                    fontSize = 12.sp,
                    color = Color(0xFF938F99)
                )
            }
            
            IconButton(onClick = { expandedEmailId = null }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh inbox", tint = Color(0xFFD0BCFF))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (StockSyncCoordinator.simulatedSentEmails.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
                    ) {
                        Text(
                            text = "No dispatches logged",
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF938F99)
                        )
                    }
                }
            } else {
                items(StockSyncCoordinator.simulatedSentEmails) { email ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedEmailId = if (expandedEmailId == email.id) null else email.id
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (email.isErrorNotification) Color(0xFF601410).copy(alpha = 0.25f) else Color(0xFF2B2930)
                        ),
                        border = if (email.isErrorNotification) BorderStroke(1.dp, Color(0xFFF2B8B5)) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (email.isErrorNotification) Icons.Default.Warning else Icons.Default.MailOutline,
                                        contentDescription = "mail icon indication",
                                        tint = if (email.isErrorNotification) Color(0xFFF2B8B5) else Color(0xFFD0BCFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = email.sender,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD0BCFF)
                                    )
                                }
                                
                                Text(
                                    text = email.timestamp,
                                    fontSize = 10.sp,
                                    color = Color(0xFF938F99)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = email.subject,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE6E1E9)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (expandedEmailId == email.id) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFF49454F))
                                
                                Text(
                                    text = "To: ${email.recipient}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCAC4D0),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = email.body,
                                    fontSize = 13.sp,
                                    color = Color(0xFFCAC4D0),
                                    modifier = Modifier.padding(top = 8.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                Text(
                                    text = email.body,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// SIMULATED GOOGLE SHEETS VIEW SCREEN
// ==========================================
@Composable
fun SheetsScreen(vm: MarketPulseViewModel) {
    val configState by vm.currentConfig.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📊 Google Sheet Link preview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E9)
                )
                Text(
                    text = "File Name: " + (configState?.googleSheetName ?: "MarketPulse_Log_2026"),
                    fontSize = 12.sp,
                    color = Color(0xFF938F99)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Row Table Grid Columns Headers
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF49454F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Row", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(32.dp))
                Text("Ticker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(50.dp))
                Text("Price", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(54.dp))
                Text("Summary / Analysis", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(StockSyncCoordinator.simulatedSheetRows) { row ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${row.rowIndex}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD0BCFF),
                            modifier = Modifier.width(32.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .background(Color(0xFF381E72), RoundedCornerShape(4.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.ticker,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD0BCFF),
                                maxLines = 1
                            )
                        }

                        Text(
                            text = row.price,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE6E1E9),
                            modifier = Modifier.width(54.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.summary,
                                fontSize = 11.sp,
                                color = Color(0xFFCAC4D0),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Vol: ${row.volume} | ${row.timestamp}",
                                fontSize = 9.sp,
                                color = Color(0xFF938F99)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// CONFIGURATION SETTINGS & ERROR TEST SCREEN
// ==========================================
@Composable
fun SettingsScreen(vm: MarketPulseViewModel) {
    val configState by vm.currentConfig.collectAsState()
    val allErrorsState by vm.allErrors.collectAsState()

    var userEmailInput by remember { mutableStateOf("") }
    var sheetNameInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var errSheetsToggle by remember { mutableStateOf(false) }
    var errEmailToggle by remember { mutableStateOf(false) }

    // Initialize state fields
    LaunchedEffect(configState) {
        configState?.let {
            userEmailInput = it.userEmail
            sheetNameInput = it.googleSheetName
            apiKeyInput = it.financialApiKey
            errSheetsToggle = it.isSimulationErrorSheets
            errEmailToggle = it.isSimulationErrorEmail
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "⚙️ Connection Parameters",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E9)
            )
            Text(
                text = "Modify destination, storage links and keys.",
                fontSize = 12.sp,
                color = Color(0xFF938F99),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Connection detail inputs
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = userEmailInput,
                        onValueChange = { userEmailInput = it },
                        label = { Text("Notification Email Address") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = sheetNameInput,
                        onValueChange = { sheetNameInput = it },
                        label = { Text("Google Sheet Document Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Financial Data API Key (FMP)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFD0BCFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            vm.updateConfig(
                                userEmail = userEmailInput,
                                googleSheetName = sheetNameInput,
                                financialApiKey = apiKeyInput,
                                isSimulationErrorSheets = errSheetsToggle,
                                isSimulationErrorEmail = errEmailToggle
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Configurations", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ERROR SIMULATION CONTROLS (TESTBED)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFFF2B8B5).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Test errors", tint = Color(0xFFF2B8B5))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Error Simulation testbed",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF2B8B5),
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "Toggle these options to trigger real network exception dispatches. A notification crash log will instantly send to inflexlabs@gmail.com on next Sync calculation!",
                        color = Color(0xFF938F99),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate Google Sheets Access Error", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Throws Sheets API Forbidden Error 403 on write", fontSize = 11.sp, color = Color(0xFF938F99))
                        }
                        Switch(
                            checked = errSheetsToggle,
                            onCheckedChange = {
                                errSheetsToggle = it
                                vm.updateConfig(
                                    userEmail = userEmailInput,
                                    googleSheetName = sheetNameInput,
                                    financialApiKey = apiKeyInput,
                                    isSimulationErrorSheets = errSheetsToggle,
                                    isSimulationErrorEmail = errEmailToggle
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF2B8B5))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulate IMAP/SMTP Connection Failure", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Throws Server Socket Authentication Timeout", fontSize = 11.sp, color = Color(0xFF938F99))
                        }
                        Switch(
                            checked = errEmailToggle,
                            onCheckedChange = {
                                errEmailToggle = it
                                vm.updateConfig(
                                    userEmail = userEmailInput,
                                    googleSheetName = sheetNameInput,
                                    financialApiKey = apiKeyInput,
                                    isSimulationErrorSheets = errSheetsToggle,
                                    isSimulationErrorEmail = errEmailToggle
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFF2B8B5))
                        )
                    }
                }
            }
        }

        // HISTORICAL CRASH REVIEWS & CLEAR LOGS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historical Crash Logs",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E9),
                    fontSize = 15.sp
                )

                TextButton(
                    onClick = { vm.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF2B8B5))
                ) {
                    Text("Clear All Data")
                }
            }
        }

        if (allErrorsState.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No system crash events recorded (Optimal health Status)",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = Color(0xFF938F99)
                    )
                }
            }
        } else {
            items(allErrorsState) { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF601410).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF601410))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ SYSTEM ERROR TRIGGERED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1B8B5)
                            )
                            Text(
                                text = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(Date(error.timestamp)),
                                fontSize = 10.sp,
                                color = Color(0xFF938F99)
                            )
                        }
                        
                        Text(
                            text = error.errorMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Text(
                            text = error.errorDetails,
                            fontSize = 11.sp,
                            color = Color(0xFFCAC4D0),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
