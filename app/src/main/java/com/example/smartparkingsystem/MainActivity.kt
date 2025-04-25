// MainActivity.kt
package com.example.smartparkingsystem

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
        setContent {
            SmartParkingSystemTheme {
                var currentScreen by remember { mutableStateOf("login") }
                when (currentScreen) {
                    "login"     -> LoginScreen(onLoginSuccess = { currentScreen = "dashboard" }, onSignUpClick = { currentScreen = "signup" })
                    "signup"    -> SignUpScreen(onSignUpDone = { currentScreen = "login" })
                    "dashboard" -> DashboardScreen(
                        onNavigateToSlots = { currentScreen = "slots" },
                        onNavigateToScanner = { currentScreen = "scanner" },
                        onLogout = { currentScreen = "login" }
                    )
                    "slots"     -> ParkingSlotScreen(onBack = { currentScreen = "dashboard" })
                    "scanner"   -> QRScannerScreen(onBack = { currentScreen = "dashboard" })
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

// ---------------- Dashboard ----------------
@Composable
fun DashboardScreen(onNavigateToSlots: () -> Unit, onNavigateToScanner: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNavigateToSlots, modifier = Modifier.fillMaxWidth()) {
            Text("View Parking Slots")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToScanner, modifier = Modifier.fillMaxWidth()) {
            Text("Scan QR at Gate")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            Firebase.auth.signOut()
            onLogout()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
    }
}

// ---------------- QR Scanner ----------------
@Composable
fun QRScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val gateControlRef = FirebaseDatabase.getInstance().getReference("gateControl")
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val bookingsRef = FirebaseDatabase.getInstance().getReference("bookings")
            bookingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var userHasBooking = false
                    snapshot.children.forEach { child ->
                        val uid = child.child("uid").getValue(String::class.java)
                        if (uid == currentUser?.uid) userHasBooking = true
                    }
                    if (userHasBooking) {
                        FirebaseDatabase.getInstance().getReference("gateControl/open").setValue(true)

                        Toast.makeText(context, "Gate opened for booking.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No booking found.", Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error checking booking.", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "Scan cancelled", Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
    LaunchedEffect(Unit) { launcher.launch(ScanOptions()) }
    BackHandler { onBack() }
}

// ---------------- Parking Slot Screen ----------------
@Composable
fun ParkingSlotScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser
    val slotRef        = FirebaseDatabase.getInstance().getReference("parking_slots")
    val bookingRef     = FirebaseDatabase.getInstance().getReference("bookings")
    val gateControlRef = FirebaseDatabase.getInstance().getReference("gate_control")
    val scope          = rememberCoroutineScope()

    // slot statuses from IR sensor (occupied/available)
    val slotStatuses = remember { mutableStateListOf(*Array(6) { "available" }) }
    // current bookings (persistent)
    val bookings     = remember { mutableStateMapOf<Int, Booking>() }
    // countdown in seconds
    val timeLeftMap  = remember { mutableStateMapOf<Int, Int>() }
    // coroutine jobs for each countdown
    val jobs         = remember { mutableStateMapOf<Int, Job>() }

    // Listen for IR updates
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEachIndexed { index, child ->
                    val status = child.getValue(String::class.java) ?: "available"
                    if (status != null && (status == "available" || status == "occupied")){
                    slotStatuses[index] = status
                    // if slot became occupied, cancel any booking
                    if (status == "occupied" && bookings.containsKey(index)) {
                        // remove booking
                        bookingRef.child(index.toString()).removeValue()
                        jobs[index]?.cancel()
                        jobs.remove(index)
                        bookings.remove(index)
                        timeLeftMap.remove(index)
                        // open gate
                        gateControlRef.child("open").setValue(true)
                        scope.launch {
                            delay(10_000)
                            gateControlRef.child("open").setValue(false)
                        }
                    }
                }}
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
                // track new and removed bookings
                val seen = mutableSetOf<Int>()
                snapshot.children.forEach { child ->
                    val idx = child.key?.toIntOrNull() ?: return@forEach

                    if (slotStatuses[idx] == "occupied") {
                        // and just make sure the booking node really is gone:
                        bookingRef.child(idx.toString()).removeValue()
                        return@forEach
                    }
                    val uid = child.child("uid").getValue(String::class.java) ?: return@forEach
                    val start = child.child("startTime").getValue(Long::class.java) ?: return@forEach
                    seen += idx
                    bookings[idx] = Booking(uid, start)
                    // compute remaining
                    val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
                    val rem = 600 - elapsed
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
                                // expired
                                bookingRef.child(idx.toString()).removeValue()
                                bookings.remove(idx)
                                timeLeftMap.remove(idx)
                                jobs.remove(idx)
                                Toast.makeText(context, "Booking expired for slot ${idx+1}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // already expired
                        bookingRef.child(idx.toString()).removeValue()
                    }
                }
                // clean up removed
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

    BackHandler {
        jobs.values.forEach { it.cancel() }
        onBack()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Parking Slots", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        // 3×2 grid
        for (row in 0..1) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val status = slotStatuses[idx]
                    val isBooked = bookings.containsKey(idx)
                    val color = when {
                        status == "occupied" -> Color.Red
                        isBooked             -> Color.Yellow
                        else                 -> Color.Green
                    }
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(color)
                            .clickable(enabled = status=="available" && !isBooked) {
                                // place booking
                                val now = System.currentTimeMillis()
                                bookingRef.child(idx.toString()).setValue(
                                    mapOf("uid" to currentUser?.uid, "startTime" to now)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when {
                                    status=="occupied" -> "Occupied"
                                    isBooked          -> "Booked"
                                    else              -> "Available"
                                },
                                color = Color.White
                            )
                            timeLeftMap[idx]?.let { t ->
                                val m = t/60; val s = t%60
                                Text(
                                    text = "%02d:%02d".format(m,s),
                                    color = if (t<=60) Color.Red else Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            jobs.values.forEach { it.cancel() }
            onBack()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
