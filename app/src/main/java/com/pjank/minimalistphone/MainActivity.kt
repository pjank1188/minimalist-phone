package com.pjank.minimalistphone

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

/**
 * One launchable app: what to show, which component to start, and which profile (user)
 * it lives in. The user handle lets us launch work-profile apps as well as personal ones.
 */
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
)

/** The resolved allow-list, split into the home screen's three sections. */
data class LoadedApps(
    val primary: List<AppEntry>,
    val work: List<AppEntry>,
    val utilities: List<AppEntry>,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBlackWallpaper(this)
        setContent { HomeScreen() }
    }
}

/**
 * One-shot: paints both the system and lock-screen wallpapers solid black, so the lock
 * screen matches the launcher and Material You derives a neutral (not blue) palette for
 * the lock clock. Guarded by a pref so we don't re-set it on every launch — clear the
 * "black_wallpaper_set" pref (or app data) to re-apply after changing wallpaper manually.
 */
private fun ensureBlackWallpaper(context: Context) {
    val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    if (prefs.getBoolean("black_wallpaper_set", false)) return
    try {
        val black = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        black.eraseColor(android.graphics.Color.BLACK)
        val wm = android.app.WallpaperManager.getInstance(context)
        wm.setBitmap(black, null, true, android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK)
        prefs.edit().putBoolean("black_wallpaper_set", true).apply()
    } catch (e: Exception) {
        // Wallpaper is cosmetic — never let it break the launcher.
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val weather = rememberWeather()

    // Reload the app list whenever we return to the home screen, so newly installed or
    // removed apps show up without restarting the launcher.
    var allApps by remember { mutableStateOf(loadApps(context)) }
    var pickups by remember { mutableStateOf(Pickups.today(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                allApps = loadApps(context)
                pickups = Pickups.today(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Live-update the pickup count when WorkHoursService records an unlock (same process,
    // so a SharedPreferences listener is enough).
    DisposableEffect(Unit) {
        val prefs = Pickups.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            pickups = Pickups.today(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    // Ticking clock — re-reads the time once a second.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("h:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, d MMMM") }

    // Recomputed every tick — the clock's read of `now` drives recomposition, so this
    // re-evaluates each second and drops work apps once we're outside work hours.
    val visibleApps = allApps.primary.filterNot { Schedule.isRestrictedNow(it.component.packageName) }
    val visibleWork = allApps.work.filterNot { Schedule.isRestrictedNow(it.component.packageName) }
    val visibleUtilities = allApps.utilities.filterNot { Schedule.isRestrictedNow(it.component.packageName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Design B: clock, date, and weather share the same center axis as the app list.
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = now.format(timeFmt),
                color = Color.White,
                fontFamily = Inter,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 1.sp,
            )
            val dateLine = buildString {
                append(now.format(dateFmt).lowercase())
                weather?.let { w -> append("  ·  ${w.tempF}° ${w.condition.lowercase()}") }
            }
            Text(
                text = dateLine,
                color = Color.Gray,
                fontFamily = Inter,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (pickups > 0) {
                Text(
                    text = if (pickups == 1) "1 pickup" else "$pickups pickups",
                    color = Color.DarkGray,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(48.dp))

        visibleApps.forEach { app ->
            Text(
                text = app.label.lowercase(),
                color = Color.White,
                fontFamily = Inter,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchApp(context, app) }
                    .padding(vertical = 14.dp)
            )
        }

        if (visibleWork.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            visibleWork.forEach { app ->
                Text(
                    text = app.label.lowercase(),
                    color = Color.White,
                    fontFamily = Inter,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchApp(context, app) }
                        .padding(vertical = 14.dp)
                )
            }
        }

        if (visibleUtilities.isNotEmpty()) {
            Spacer(Modifier.height(40.dp))
            visibleUtilities.forEach { app ->
                Text(
                    text = app.label.lowercase(),
                    color = Color.Gray,
                    fontFamily = Inter,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { launchApp(context, app) }
                        .padding(vertical = 10.dp)
                )
            }
        }

        val torch = rememberTorch()
        if (torch.available) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (torch.on) "torch · on" else "torch",
                color = if (torch.on) Color.White else Color.DarkGray,
                fontFamily = Inter,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { torch.toggle() }
                    .padding(vertical = 10.dp)
            )
        }
    }
}

/** Torch state for the home-screen row: whether the device has one, whether it's lit. */
data class TorchState(val available: Boolean, val on: Boolean, val toggle: () -> Unit)

/**
 * Tracks and toggles the camera flash as a flashlight via [CameraManager.setTorchMode] —
 * no camera permission needed. State comes from a TorchCallback, so the row stays right
 * even when the torch is switched from quick settings or turned off by the camera app.
 */
@Composable
private fun rememberTorch(): TorchState {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val torchId = remember {
        try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }
    var on by remember { mutableStateOf(false) }

    DisposableEffect(torchId) {
        val callback = torchId?.let {
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    if (cameraId == torchId) on = enabled
                }
            }.also { cameraManager.registerTorchCallback(it, null) }
        }
        onDispose { callback?.let { cameraManager.unregisterTorchCallback(it) } }
    }

    return TorchState(
        available = torchId != null,
        on = on,
        toggle = {
            torchId?.let {
                try {
                    cameraManager.setTorchMode(it, !on)
                } catch (e: Exception) {
                    // Torch briefly unavailable (camera in use) — ignore.
                }
            }
        },
    )
}

/**
 * Requests coarse-location permission once, then fetches current weather and refreshes it
 * every 15 minutes. Returns null until a reading is available (or if permission is denied).
 */
@Composable
private fun rememberWeather(): WeatherInfo? {
    val context = LocalContext.current
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        while (true) {
            val loc = currentLocation(context)
            if (loc != null) {
                val fetched = withContext(Dispatchers.IO) { Weather.fetch(loc.first, loc.second) }
                if (fetched != null) weather = fetched
            }
            delay(15 * 60 * 1000L) // refresh every 15 minutes
        }
    }
    return weather
}

/** Best-effort coarse location: last known fix if available, otherwise a fresh one-shot. */
@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(
        LocationManager.FUSED_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
    )
    for (p in providers) {
        if (!lm.isProviderEnabled(p)) continue
        val last = try { lm.getLastKnownLocation(p) } catch (e: SecurityException) { null }
        if (last != null) return last.latitude to last.longitude
    }
    val provider = providers.firstOrNull { lm.isProviderEnabled(it) } ?: return null
    return suspendCancellableCoroutine { cont ->
        try {
            lm.getCurrentLocation(provider, null, context.mainExecutor) { loc ->
                cont.resume(loc?.let { it.latitude to it.longitude })
            }
        } catch (e: SecurityException) {
            cont.resume(null)
        }
    }
}

/**
 * Resolves the apps to show across ALL profiles (personal + managed work profile), so
 * work apps like Teams/Outlook appear too. In discovery mode ([AllowList.SHOW_ALL]) it
 * returns every launchable app annotated with its package name; otherwise just the
 * allow-listed ones, in the order they're declared.
 */
private fun loadApps(context: Context): LoadedApps {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    // packageName -> first launchable activity found, walking every profile.
    val byPackage = LinkedHashMap<String, AppEntry>()
    for (profile in userManager.userProfiles) {
        for (info in launcherApps.getActivityList(null, profile)) {
            val pkg = info.applicationInfo.packageName
            if (!byPackage.containsKey(pkg)) {
                byPackage[pkg] = AppEntry(info.label.toString(), info.componentName, profile)
            }
        }
    }

    if (AllowList.SHOW_ALL) {
        val everything = byPackage
            .map { (pkg, entry) -> entry.copy(label = "${entry.label}  —  $pkg") }
            .sortedBy { it.label.lowercase() }
        return LoadedApps(primary = everything, work = emptyList(), utilities = emptyList())
    }

    return LoadedApps(
        primary = AllowList.PACKAGES.mapNotNull { pkg -> byPackage[pkg] },
        work = AllowList.WORK.mapNotNull { pkg -> byPackage[pkg] },
        utilities = AllowList.UTILITIES.mapNotNull { pkg -> byPackage[pkg] },
    )
}

private fun launchApp(context: Context, app: AppEntry) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    launcherApps.startMainActivity(app.component, app.user, null, null)
}
