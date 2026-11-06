package com.example.focuslock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NavySurfaceCard
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.random.Random

val FOCUS_QUOTES = listOf(
    "Starve your distractions, feed your focus." to "Anonymous",
    "Concentration is the secret of strength." to "Ralph Waldo Emerson",
    "Where attention goes, energy flows and results show." to "T. Harv Eker",
    "Discipline is choosing between what you want now and what you want most." to "Abraham Lincoln",
    "Deep work is the superpower of the 21st century." to "Cal Newport",
    "You do not rise to the level of your goals. You fall to the level of your systems." to "James Clear",
    "Focus is a muscle. Every time you resist impulse, it grows stronger." to "FocusLock Wisdom"
)

@Composable
fun QuoteCard(
    modifier: Modifier = Modifier
) {
    val quote = remember {
        FOCUS_QUOTES[Random.nextInt(FOCUS_QUOTES.size)]
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        color = NavySurfaceCard,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "“${quote.first}”",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "— ${quote.second}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = PurpleLight
            )
        }
    }
}
