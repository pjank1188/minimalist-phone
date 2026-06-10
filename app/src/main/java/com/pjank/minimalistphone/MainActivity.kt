package com.pjank.minimalistphone

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

/** One launchable app: what to show and what to open. */
data class AppEntry(val label: String, val packageName: String)

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
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }

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
            fontSize = 64.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = now.format(dateFmt),
            color = Color.Gray,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(48.dp))

        apps.forEach { app ->
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { launchApp(context, app.packageName) }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

/**
 * Resolves the apps to show. In discovery mode ([AllowList.SHOW_ALL]) it returns every
 * launchable app annotated with its package name; otherwise just the allow-listed ones,
 * in the order they're declared.
 */
private fun loadApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val mainLauncher = Intent(Intent.ACTION_MAIN, null)
        .apply { addCategory(Intent.CATEGORY_LAUNCHER) }

    val installed: Map<String, String> = pm.queryIntentActivities(mainLauncher, 0)
        .associate { ri -> ri.activityInfo.packageName to ri.loadLabel(pm).toString() }

    if (AllowList.SHOW_ALL) {
        return installed
            .map { (pkg, label) -> AppEntry("$label  —  $pkg", pkg) }
            .sortedBy { it.label.lowercase() }
    }

    return AllowList.PACKAGES.mapNotNull { pkg ->
        installed[pkg]?.let { label -> AppEntry(label, pkg) }
    }
}

private fun launchApp(context: Context, packageName: String) {
    val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(launch)
}
