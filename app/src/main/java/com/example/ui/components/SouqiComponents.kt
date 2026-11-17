package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.SouqiAmber
import com.example.ui.theme.SouqiAmberContainer
import com.example.ui.theme.SouqiEmerald
import com.example.ui.theme.SouqiEmeraldContainer
import com.example.ui.theme.SouqiTeal
import com.example.ui.theme.SouqiTealContainer
import com.example.ui.theme.SouqiTerracotta
import com.example.ui.theme.SouqiTerracottaContainer

fun coverDrawableRes(coverPreset: String): Int = when (coverPreset) {
    "honey" -> R.drawable.img_store_honey
    "fashion" -> R.drawable.img_store_fashion
    "electronics" -> R.drawable.img_store_electronics
    else -> R.drawable.img_souqi_hero
}

fun accentColorForKey(accentKey: String): Color = when (accentKey) {
    "amber" -> SouqiAmber
    "emerald" -> SouqiEmerald
    "terracotta" -> SouqiTerracotta
    else -> SouqiTeal
}

@Composable
fun StoreCoverImage(
    coverPreset: String,
    logoUri: String?,
    storeName: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (!logoUri.isNullOrBlank()) {
            AsyncImage(
                model = logoUri,
                contentDescription = storeName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(id = coverDrawableRes(coverPreset)),
                contentDescription = storeName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.58f))
                    )
                )
        )
    }
}

@Composable
fun ProductVisual(
    imagePreset: String,
    imageUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val (bgColor, iconTint, iconVector) = when (imagePreset) {
        "honey" -> Triple(SouqiAmberContainer, Color(0xFF7A4B00), Icons.Filled.Spa)
        "coffee" -> Triple(Color(0xFFEDE0D4), Color(0xFF5C3A21), Icons.Filled.LocalCafe)
        "abaya" -> Triple(SouqiTerracottaContainer, SouqiTerracotta, Icons.Filled.Checkroom)
        "earbuds" -> Triple(SouqiTealContainer, SouqiTeal, Icons.Filled.Headphones)
        "watch" -> Triple(SouqiEmeraldContainer, SouqiEmerald, Icons.Filled.Watch)
        "electronics" -> Triple(SouqiTealContainer, SouqiTeal, Icons.Filled.Devices)
        else -> Triple(SouqiTealContainer, SouqiTeal, Icons.Filled.LocalMall)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUri.isNullOrBlank()) {
            AsyncImage(
                model = imageUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = iconVector,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

object ExternalIntentHelper {
    fun openWhatsAppUrl(context: Context, uriString: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun shareStoreLink(context: Context, storeName: String, slug: String, isAr: Boolean) {
        val url = "https://souqi.app/s/$slug"
        val shareText = if (isAr) {
            "تصفح متجر $storeName على منصة سوقي واطلب عبر واتساب:\n$url"
        } else {
            "Browse $storeName on Souqi and order via WhatsApp:\n$url"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, storeName))
    }
}
