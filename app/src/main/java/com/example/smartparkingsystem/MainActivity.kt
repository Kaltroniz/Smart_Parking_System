package com.example.smartparkingsystem

import android.os.Bundle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Custom image
        Image(
            painter = painterResource(id = R.drawable.parking_logo), // Make sure this matches your filename
            contentDescription = "App Logo",
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 16.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "Welcome to Smart Parking",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Your Reliable Parking Solution",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.Black), // Force black text
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.Black), // Force black text
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    // Trigger login when Enter is pressed
                    Firebase.auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onLoginSuccess() }
                        .addOnFailureListener {
                            Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                Firebase.auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { onLoginSuccess() }
                    .addOnFailureListener {
                        Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
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
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    Firebase.auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { onSignUpDone() }
                        .addOnFailureListener {
                            Toast.makeText(context, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                        }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                Firebase.auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { onSignUpDone() }
                    .addOnFailureListener {
                        Toast.makeText(context, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign Up")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Scanner", "Logout")
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 2) {
                                showLogoutDialog = true // Show confirmation dialog
                            } else {
                                selectedTab = index
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
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Confirm Logout") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    TextButton(onClick = {
                        Firebase.auth.signOut()
                        showLogoutDialog = false
                        onLogout()
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                    }) {
                        Text("No")
                    }
                }
            )
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
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedSlotIndex by remember { mutableStateOf(-1) }
    val slotStatuses = remember { mutableStateListOf(*Array(5) { "available" }) }
    val bookings = remember { mutableStateMapOf<Int, Booking>() }
    val timeLeftMap = remember { mutableStateMapOf<Int, Int>() }
    val jobs = remember { mutableStateMapOf<Int, Job>() }
    var showPaymentScreen by remember { mutableStateOf(false) }
    var bookingInProgress by remember { mutableStateOf(false) }

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
    Box(modifier = Modifier.fillMaxSize()) {
        if (showPaymentScreen) {
            PaymentScreen(
                slotIndex = selectedSlotIndex,
                onPaymentSuccess = {
                    currentUser?.let { user ->
                        bookingInProgress = true
                        FirebaseDatabase.getInstance().getReference().apply {
                            child("bookings/$selectedSlotIndex").setValue(
                                mapOf(
                                    "uid" to user.uid,
                                    "startTime" to System.currentTimeMillis()
                                )
                            )
                            child("gateControl/slotIndex").setValue(selectedSlotIndex)
                        }
                        showPaymentScreen = false
                        bookingInProgress = false
                    }
                },
                onCancel = {
                    showPaymentScreen = false
                    selectedSlotIndex = -1
                }
            )
        } else {

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "Parking Slots",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))

                // First row: 2 slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..1) {
                        ParkingSlotButton(
                            index = i,
                            status = slotStatuses[i],
                            isBooked = bookings.containsKey(i),
                            timeLeft = timeLeftMap[i],
                            enabled = slotStatuses[i] == "available" && !bookings.containsKey(i) && currentUser != null,
                            onClick = {
                                selectedSlotIndex = i
                                showBookingDialog = true
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Second row: 2 slots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 2..3) {
                        ParkingSlotButton(
                            index = i,
                            status = slotStatuses[i],
                            isBooked = bookings.containsKey(i),
                            timeLeft = timeLeftMap[i],
                            enabled = slotStatuses[i] == "available" && !bookings.containsKey(i) && currentUser != null,
                            onClick = {
                                selectedSlotIndex = i
                                showBookingDialog = true
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Third row: Single centered slot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ParkingSlotButton(
                        index = 4,
                        status = slotStatuses[4],
                        isBooked = bookings.containsKey(4),
                        timeLeft = timeLeftMap[4],
                        enabled = slotStatuses[4] == "available" && !bookings.containsKey(4) && currentUser != null,
                        onClick = {
                            selectedSlotIndex = 4
                            showBookingDialog = true
                        }
                    )
                }
            }
        }
    }
        if (bookingInProgress) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }


            if (showBookingDialog) {
        AlertDialog(
            onDismissRequest = { showBookingDialog = false },
            title = { Text("Confirm Booking") },
            text = { Text("Book parking slot ${selectedSlotIndex + 1} for 10 minutes?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBookingDialog = false
                        showPaymentScreen = true // Changed this line
                    }
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBookingDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}
@Composable
fun PaymentScreen(
    slotIndex: Int,
    onPaymentSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Payment,
            contentDescription = "Payment",
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Payment Required",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "₹60 will be charged for parking slot ${slotIndex + 1}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPaymentSuccess,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text("Pay Now", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel Booking")
        }
    }
}
@Composable
fun ParkingSlotButton(
    index: Int,
    status: String,
    isBooked: Boolean,
    timeLeft: Int?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = when {
        status == "occupied" -> Color(0xFFE53935) to Color.White
        isBooked -> Color(0xFFFFB300) to Color.Black
        else -> Color(0xFF43A047) to Color.White
    }

    Card(
        modifier = Modifier
            .size(140.dp)
            .clickable(enabled = enabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.first),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Slot number
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.second,
                    fontWeight = FontWeight.Bold
                )

                // Status icon
                Icon(
                    imageVector = when {
                        status == "occupied" -> Icons.Filled.DirectionsCar
                        isBooked -> Icons.Filled.LockClock
                        else -> Icons.Filled.LocalParking
                    },
                    contentDescription = "Status",
                    tint = colors.second,
                    modifier = Modifier.size(32.dp)
                )

                // Status text or timer
                if (isBooked && timeLeft != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Time Left",
                            color = colors.second,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "%02d:%02d".format(timeLeft / 60, timeLeft % 60),
                            color = if (timeLeft <= 60) Color.Red else colors.second,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = when {
                            status == "occupied" -> "Occupied"
                            else -> "Available"
                        },
                        color = colors.second,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}