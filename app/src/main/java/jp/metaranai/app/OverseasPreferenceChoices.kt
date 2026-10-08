package jp.metaranai.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OverseasPreferenceChoices(selected: OverseasPreference?, enabled: Boolean = true,
                              onSelect: (OverseasPreference) -> Unit) {
    OverseasPreference.entries.forEach { choice ->
        OutlinedButton(onClick = { onSelect(choice) }, enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text(if (selected == choice) "✓ ${choice.answer}" else choice.answer)
                Text(choice.detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
