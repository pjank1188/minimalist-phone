package com.pjank.minimalistphone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The link interstitial. Registered for http/https links, so tapping a URL in Signal,
 * Messages, etc. offers this alongside Chrome — a deliberate pause instead of a straight
 * shot into the browser. Three ways out: copy the link, ship it to the desktop via the
 * share sheet (read it at a real computer later), or consciously open it in Chrome.
 */
class LinkActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.data?.toString()
        if (url == null) {
            finish()
            return
        }
        setContent { LinkScreen(url) }
    }

    @Composable
    private fun LinkScreen(url: String) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 28.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = url,
                color = Color.Gray,
                fontFamily = Inter,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(48.dp))

            Option("copy") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("link", url))
                Toast.makeText(this@LinkActivity, "copied", Toast.LENGTH_SHORT).show()
                finish()
            }
            Option("send to desktop") {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                startActivity(Intent.createChooser(send, null))
                finish()
            }
            Option("open in chrome") {
                val view = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .setPackage("com.android.chrome")
                try {
                    startActivity(view)
                } catch (e: Exception) {
                    Toast.makeText(this@LinkActivity, "chrome isn't available", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }

    @Composable
    private fun Option(label: String, onClick: () -> Unit) {
        Text(
            text = label,
            color = Color.White,
            fontFamily = Inter,
            fontSize = 21.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 14.dp),
        )
    }
}
