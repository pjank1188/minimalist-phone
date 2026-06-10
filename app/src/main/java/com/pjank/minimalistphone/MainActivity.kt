package com.pjank.minimalistphone

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Row
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HomeScreen() }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val weather = rememberWeather()

    // Reload the app list whenever we return to the home screen, so newly installed or
    // removed apps show up without restarting the launcher.
    var allApps by remember { mutableStateOf(loadApps(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allApps = loadApps(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    val visibleApps = allApps.filterNot { Schedule.isRestrictedNow(it.component.packageName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = now.format(timeFmt),
                    color = Color.White,
                    fontFamily = Inter,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = now.format(dateFmt).lowercase(),
                    color = Color.Gray,
                    fontFamily = Inter,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraLight,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            weather?.let { w ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${w.tempF}°",
                        color = Color.White,
                        fontFamily = Inter,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Light,
                    )
                    Text(
                        text = w.condition,
                        color = Color.Gray,
                        fontFamily = Inter,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                    )
                }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchApp(context, app) }
                    .padding(vertical = 14.dp)
            )
        }
    }
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
private fun loadApps(context: Context): List<AppEntry> {
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
        return byPackage
            .map { (pkg, entry) -> entry.copy(label = "${entry.label}  —  $pkg") }
            .sortedBy { it.label.lowercase() }
    }

    return AllowList.PACKAGES.mapNotNull { pkg -> byPackage[pkg] }
}

private fun launchApp(context: Context, app: AppEntry) {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    launcherApps.startMainActivity(app.component, app.user, null, null)
}
