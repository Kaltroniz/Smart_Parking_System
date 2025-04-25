package com.example.smartparkingsystem

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.smartparkingsystem.ui.theme.SmartParkingSystemTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.*

data class Booking(val uid: String, val startTime: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        val database = FirebaseDatabase.getInstance()
        database.getReference("gateControl").apply {
            child("open").setValue(false)
            child("slotIndex").setValue(-1)
        }
        setContent {
            SmartParkingSystemTheme {
                var currentScreen by remember { mutableStateOf("login") }
                when (currentScreen) {
                    "login"     -> LoginScreen(
                        onLoginSuccess = { currentScreen = "main" },
                        onSignUpClick = { currentScreen = "signup" }
                    )
                    "signup"    -> SignUpScreen(onSignUpDone = { currentScreen = "login" })
                    "main"      -> MainScreen(onLogout = { currentScreen = "login" })
                }
            }
        }
    }
}

// ---------------- Login Screen ----------------
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSignUpClick: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { onLoginSuccess() }
                .addOnFailureListener {
                    Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignUpClick) {
            Text("Don't have an account? Sign Up")
        }
    }
}

// ---------------- Sign-Up Screen ----------------
@Composable
fun SignUpScreen(onSignUpDone: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            Firebase.auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { onSignUpDone() }
                .addOnFailureListener {
                    Toast.makeText(context, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Up")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Scanner", "Logout")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 2) { // Logout tab
                                Firebase.auth.signOut()
                                onLogout()
                            }
                        },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Filled.Home, contentDescription = "Home")
                                1 -> Icon(Icons.Filled.QrCodeScanner, contentDescription = "Scanner")
                                2 -> Icon(Icons.Filled.Logout, contentDescription = "Logout")
                                else -> Icon(Icons.Filled.Home, contentDescription = "Home")
                            }
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ParkingSlotScreen()
                1 -> QRScannerScreen()
                // Logout is handled in onClick
            }
        }
    }
}

@Composable
fun QRScannerScreen() {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val gateControlRef = FirebaseDatabase.getInstance().getReference("gateControl")
    val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")

    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            bookingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var bookedSlotIndex = -1

                    // 1. Find the user's active booking
                    snapshot.children.forEach { child ->
                        val uid = child.child("uid").getValue(String::class.java)
                        val slotIndex = child.key?.toIntOrNull() ?: -1

                        if (uid == currentUser?.uid && slotIndex != -1) {
                            bookedSlotIndex = slotIndex
                        }
                    }

                    // 2. Handle gate control based on found slot
                    if (bookedSlotIndex != -1) {
                        gateControlRef.apply {
                            child("open").setValue(true)
                            child("slotIndex").setValue(bookedSlotIndex)
                        }
                        Toast.makeText(context, "Gate opened for slot $bookedSlotIndex", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No active booking found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) { launcher.launch(ScanOptions()) }
}

@Composable
fun ParkingSlotScreen() {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val slotRef = FirebaseDatabase.getInstance().getReference("parking_slots")
    val bookingRef = FirebaseDatabase.getInstance().getReference("bookings")
    val gateControlRef = FirebaseDatabase.getInstance().getReference("gateControl")
    val scope = rememberCoroutineScope()

    val slotStatuses = remember { mutableStateListOf(*Array(6) { "available" }) }
    val bookings = remember { mutableStateMapOf<Int, Booking>() }
    val timeLeftMap = remember { mutableStateMapOf<Int, Int>() }
    val jobs = remember { mutableStateMapOf<Int, Job>() }

    // Listen for IR updates
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEachIndexed { index, child ->
                    val status = child.getValue(String::class.java) ?: "available"
                    if (status in listOf("available", "occupied")) {
                        slotStatuses[index] = status
                        if (status == "occupied" && bookings.containsKey(index)) {
                            bookingRef.child(index.toString()).removeValue()
                            jobs[index]?.cancel()
                            jobs.remove(index)
                            bookings.remove(index)
                            timeLeftMap.remove(index)
                            gateControlRef.child("open").setValue(true)
                            scope.launch {
                                delay(10_000)
                                gateControlRef.child("open").setValue(false)
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        slotRef.addValueEventListener(listener)
        onDispose { slotRef.removeEventListener(listener) }
    }

    // Listen for booking changes
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val seen = mutableSetOf<Int>()
                snapshot.children.forEach { child ->
                    val idx = child.key?.toIntOrNull() ?: return@forEach
                    if (slotStatuses[idx] == "occupied") {
                        bookingRef.child(idx.toString()).removeValue()
                        return@forEach
                    }
                    val uid = child.child("uid").getValue(String::class.java)
                    val start = child.child("startTime").getValue(Long::class.java)
                    if (uid != null && start != null) {
                        seen += idx
                        bookings[idx] = Booking(uid, start)
                        val rem = 600 - ((System.currentTimeMillis() - start) / 1000).toInt()
                        if (rem > 0) {
                            timeLeftMap[idx] = rem
                            if (jobs[idx] == null) {
                                jobs[idx] = scope.launch {
                                    var t = rem
                                    while (t > 0) {
                                        delay(1000)
                                        t--
                                        timeLeftMap[idx] = t
                                    }
                                    bookingRef.child(idx.toString()).removeValue()
                                    bookings.remove(idx)
                                    timeLeftMap.remove(idx)
                                    jobs.remove(idx)
                                    Toast.makeText(context, "Booking expired for slot ${idx+1}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            bookingRef.child(idx.toString()).removeValue()
                        }
                    }
                }
                (bookings.keys - seen).forEach { idx ->
                    jobs[idx]?.cancel()
                    jobs.remove(idx)
                    bookings.remove(idx)
                    timeLeftMap.remove(idx)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        bookingRef.addValueEventListener(listener)
        onDispose { bookingRef.removeEventListener(listener) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Parking Slots", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        // 3×2 grid
        for (row in 0..1) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val status = slotStatuses[idx]
                    val isBooked = bookings.containsKey(idx)
                    val color = when {
                        status == "occupied" -> Color.Red
                        isBooked -> Color.Yellow
                        else -> Color.Green
                    }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(color)
                            .clickable(enabled = status == "available" && !isBooked && currentUser != null) {
                                currentUser?.let {
                                    // Update both bookings and gateControl
                                    FirebaseDatabase.getInstance().getReference().apply {
                                        child("bookings/$idx").setValue(
                                            mapOf("uid" to it.uid, "startTime" to System.currentTimeMillis())
                                        )
                                        child("gateControl/slotIndex").setValue(idx) // Add this line
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when {
                                    status == "occupied" -> "Occupied"
                                    isBooked -> "Booked"
                                    else -> "Available"
                                },
                                color = Color.White
                            )
                            timeLeftMap[idx]?.let { t ->
                                Text(
                                    text = "%02d:%02d".format(t / 60, t % 60),
                                    color = if (t <= 60) Color.Red else Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
