package com.kinetix.pro.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ContentPlaceholder
import com.kinetix.pro.core.ui.theme.ObsidianInputStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.PlusJakartaSans
import com.kinetix.pro.core.ui.theme.PrimarySunset

@Composable
fun ObsidianUrlInput(
    value: String,
    onValueChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Paste YouTube, TikTok, Instagram or direct media link...",
    onClearClick: () -> Unit = { onValueChange("") },
    onSubmit: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused || value.isNotEmpty()) PrimarySunset else ObsidianInputStroke,
        label = "urlInputBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(ObsidianSurface1)
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = if (isFocused || value.isNotEmpty()) PrimarySunset else ContentMedium,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(22.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = ContentPlaceholder,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = ContentHigh,
                        fontSize = 14.sp,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
                    cursorBrush = SolidColor(PrimarySunset)
                )
            }

            if (value.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear URL",
                        tint = ContentMedium,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                SunsetOutlinedButton(
                    text = "Paste",
                    onClick = onPasteClick,
                    icon = Icons.Default.ContentPaste,
                    modifier = Modifier.height(36.dp)
                )
            }
        }
    }
}

@Composable
fun ObsidianSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search downloads, projects & templates..."
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused || query.isNotEmpty()) PrimarySunset else ObsidianInputStroke,
        label = "searchBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(ObsidianSurface1)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (isFocused) PrimarySunset else ContentMedium,
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(18.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = ContentPlaceholder,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = ContentHigh,
                        fontSize = 13.sp,
                        fontFamily = PlusJakartaSans
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(PrimarySunset)
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = ContentMedium,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
