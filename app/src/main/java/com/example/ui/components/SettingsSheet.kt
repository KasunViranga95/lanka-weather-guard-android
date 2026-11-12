package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.AdaptiveBorderColor
import com.example.ui.theme.AdaptiveCyanAccent
import com.example.ui.theme.AdaptivePrimaryText
import com.example.ui.theme.AdaptiveSecondaryText
import com.example.ui.theme.AdaptiveSurfaceColor

const val PRIVACY_POLICY_URL =
    "https://github.com/KasunViranga95/lanka-weather-guard-android/blob/main/docs/PRIVACY.md"

/** Language, theme, alert notifications and about/privacy in one sheet. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    language: String,
    onLanguageChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    alertsEnabled: Boolean,
    onAlertsEnabledChange: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AdaptiveSurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .testTag("settings_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, color = AdaptivePrimaryText, fontWeight = FontWeight.SemiBold)

            Text(stringResource(R.string.language), style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "" to stringResource(R.string.language_phone),
                    "en" to "English",
                    "si" to "සිංහල",
                    "ta" to "தமிழ்"
                ).forEach { (tag, label) ->
                    FilterChip(
                        selected = language == tag,
                        onClick = { if (language != tag) onLanguageChange(tag) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("language_${tag.ifEmpty { "system" }}")
                    )
                }
            }

            Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isDarkTheme, onClick = { onDarkThemeChange(false) }, label = { Text(stringResource(R.string.theme_light)) })
                FilterChip(selected = isDarkTheme, onClick = { onDarkThemeChange(true) }, label = { Text(stringResource(R.string.theme_dark)) })
            }

            HorizontalDivider(color = AdaptiveBorderColor)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AdaptiveCyanAccent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.alert_notifications), style = MaterialTheme.typography.bodyLarge, color = AdaptivePrimaryText)
                    Text(stringResource(R.string.alert_notifications_summary), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
                }
                Switch(checked = alertsEnabled, onCheckedChange = onAlertsEnabledChange, modifier = Modifier.testTag("settings_alerts_switch"))
            }

            HorizontalDivider(color = AdaptiveBorderColor)
            Text(stringResource(R.string.about), style = MaterialTheme.typography.labelLarge, color = AdaptiveSecondaryText)
            Text(stringResource(R.string.about_data), style = MaterialTheme.typography.bodySmall, color = AdaptivePrimaryText)
            Text(stringResource(R.string.alerts_disclaimer), style = MaterialTheme.typography.bodySmall, color = AdaptiveSecondaryText)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenUrl(PRIVACY_POLICY_URL) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.privacy_policy), style = MaterialTheme.typography.bodyLarge, color = AdaptiveCyanAccent, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AdaptiveCyanAccent, modifier = Modifier.size(18.dp))
            }
            Text(stringResource(R.string.version_n, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelSmall, color = AdaptiveSecondaryText)
        }
    }
}
