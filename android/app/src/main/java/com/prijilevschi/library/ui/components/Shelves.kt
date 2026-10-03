package com.prijilevschi.library.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prijilevschi.library.R
import com.prijilevschi.library.data.Book
import com.prijilevschi.library.data.ShelfOrientation
import com.prijilevschi.library.ui.theme.SpineColors
import com.prijilevschi.library.ui.theme.Wood
import kotlin.math.abs

private fun spineColor(book: Book): Color = SpineColors[abs(book.name.hashCode()) % SpineColors.size]

private fun Color.readableText(): Color = if (luminance() > 0.45f) Color(0xFF1B1B1B) else Color(0xFFFDF8F0)

/** Lays the content out rotated by -90°, so a horizontal Text reads bottom-to-top like a book spine. */
private fun Modifier.vertical(): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
            ),
        )
        layout(placeable.height, placeable.width) {
            placeable.place(
                x = (placeable.height - placeable.width) / 2,
                y = (placeable.width - placeable.height) / 2,
            )
        }
    }
    .rotate(-90f)

/**
 * One shelf: a dark back panel with one lane per depth row (back rows on top, dimmed),
 * standing on a wooden plank.
 *
 * @param highlighted ids of books matching the current search; null means "no search active".
 */
@Composable
fun ShelfView(
    title: String,
    subtitle: String?,
    orientation: ShelfOrientation,
    books: List<Book>,
    highlighted: Set<Long>?,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(Modifier.width(8.dp))
                Text(subtitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Wood.Back, Wood.Dark)),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                )
                .padding(top = 8.dp, start = 6.dp, end = 6.dp),
        ) {
            val lanes = books.groupBy { it.depthRow }.toSortedMap(compareByDescending { it })
            if (lanes.isEmpty()) {
                Text(
                    "Empty shelf",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 28.dp, horizontal = 8.dp),
                )
            }
            lanes.forEach { (depth, laneBooks) ->
                val back = depth > 1
                Column(Modifier.alpha(if (back) 0.75f else 1f)) {
                    if (lanes.size > 1) {
                        Text(
                            if (back) "Back row $depth" else "Front row",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                        )
                    }
                    val sorted = laneBooks.sortedWith(compareBy(nullsLast()) { it.positionNumber })
                    when (orientation) {
                        ShelfOrientation.VERTICAL -> SpineLane(sorted, highlighted, back, onBookClick)
                        ShelfOrientation.HORIZONTAL -> StackLane(sorted, highlighted, onBookClick)
                    }
                }
            }
        }
        // the plank
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .shadow(4.dp, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(Brush.verticalGradient(listOf(Wood.Light, Wood.Mid, Wood.Dark))),
        )
    }
}

@Composable
private fun SpineLane(books: List<Book>, highlighted: Set<Long>?, back: Boolean, onBookClick: (Book) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).heightIn(min = if (back) 130.dp else 160.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        books.forEach { book -> Spine(book, highlighted, if (back) 0.82f else 1f, onBookClick) }
    }
}

@Composable
private fun Spine(book: Book, highlighted: Set<Long>?, scale: Float, onBookClick: (Book) -> Unit) {
    val color = spineColor(book)
    val pages = book.pages ?: 250
    val width = (22 + pages / 18).coerceIn(26, 54).dp * scale
    val height = (128 + abs(book.name.hashCode() / 7) % 32).dp * scale
    val match = highlighted == null || book.id in highlighted
    Box(
        Modifier
            .alpha(if (match) 1f else 0.25f)
            .size(width, height)
            .background(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.85f), color, color.copy(alpha = 0.7f))),
                RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
            )
            .then(
                if (highlighted != null && match) Modifier.border(BorderStroke(2.dp, Wood.Highlight), RoundedCornerShape(3.dp))
                else Modifier,
            )
            .clickable { onBookClick(book) },
    ) {
        Text(
            book.name,
            color = color.readableText(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).vertical().padding(horizontal = 8.dp),
        )
        if (book.positionNumber != null) {
            Text(
                "${book.positionNumber}",
                color = color.readableText().copy(alpha = 0.7f),
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
            )
        }
        if (book.read) ReadBadge(Modifier.align(Alignment.TopCenter).padding(top = 4.dp))
    }
}

/** Books lying flat: a pile, position 1 at the bottom. */
@Composable
private fun StackLane(books: List<Book>, highlighted: Set<Long>?, onBookClick: (Book) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        books.asReversed().forEach { book ->
            val color = spineColor(book)
            val match = highlighted == null || book.id in highlighted
            val thickness = ((book.pages ?: 250) / 25).coerceIn(18, 34).dp
            val width = (0.55f + (abs(book.name.hashCode()) % 30) / 100f).coerceAtMost(0.9f)
            Row(
                Modifier
                    .alpha(if (match) 1f else 0.25f)
                    .fillMaxWidth(width)
                    .height(thickness)
                    .background(color, RoundedCornerShape(2.dp))
                    .then(
                        if (highlighted != null && match) Modifier.border(BorderStroke(2.dp, Wood.Highlight), RoundedCornerShape(2.dp))
                        else Modifier,
                    )
                    .clickable { onBookClick(book) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    book.name,
                    color = color.readableText(),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (book.read) ReadBadge()
            }
        }
    }
}

@Composable
fun ReadBadge(modifier: Modifier = Modifier) {
    Surface(modifier.size(16.dp), shape = CircleShape, color = Color(0xFF2E7D32), contentColor = Color.White) {
        AppIcon(R.drawable.ic_check, "Read", Modifier.padding(2.dp))
    }
}
