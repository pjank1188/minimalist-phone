package com.pjank.minimalistphone

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    val apps = remember { loadApps(context) }

    // Ticking clock — re-reads the time once a second.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE, d MMMM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 64.dp),
        verticalArrangement = Arrangement.Center
    ) {
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

        Spacer(Modifier.height(48.dp))

        apps.forEach { app ->
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
