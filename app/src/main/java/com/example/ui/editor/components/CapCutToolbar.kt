package com.example.ui.editor.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TextAlignment
import com.example.data.model.TextBlock
import com.example.ui.editor.DrawToolState
import com.example.ui.editor.DrawToolType
import com.example.ui.editor.ToolbarMode
import com.example.ui.theme.DocumentFonts

enum class TextSubTab(val label: String) {
    TEXT("Text"),
    STYLE("Style"),
    FONT("Font"),
    SIZE("Size"),
    ALIGN("Align"),
    SPACING("Spacing")
}

@Composable
fun CapCutToolbar(
    mode: ToolbarMode,
    selectedTextBlock: TextBlock?,
    drawToolState: DrawToolState,
    onModeChange: (ToolbarMode) -> Unit,
    onOpenInsertSheet: () -> Unit,
    onOpenPageManager: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onFormatText: (
        isBold: Boolean?,
        isItalic: Boolean?,
        isUnderline: Boolean?,
        fontSizeDelta: Float?,
        fontFamily: String?,
        textColorHex: String?,
        highlightColorHex: String?,
        clearHighlight: Boolean,
        alignment: TextAlignment?,
        lineSpacingMultiplier: Float?
    ) -> Unit,
    onImageScale: (Float) -> Unit,
    onImageRotate: (Float) -> Unit,
    onDeleteImage: () -> Unit,
    onAddTableRow: () -> Unit,
    onDeleteTableRow: () -> Unit,
    onAddTableCol: () -> Unit,
    onDeleteTableCol: () -> Unit,
    onDeleteTable: () -> Unit,
    onUpdateDrawTool: (DrawToolState) -> Unit,
    onClearDrawStrokes: () -> Unit,
    onDoneSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    // New toolbar slides up from the bottom edge while the old
                    // one fades and slides away — CapCut-style contextual swap.
                    (slideInVertically(
                        animationSpec = tween(260, easing = FastOutSlowInEasing)
                    ) { it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(
                            animationSpec = tween(180, easing = FastOutLinearInEasing)
                        ) { -it / 4 } + fadeOut(tween(140))) using
                        SizeTransform(clip = false)
                },
                label = "ToolbarTransition"
            ) { targetMode ->
                when (targetMode) {
                    ToolbarMode.DEFAULT -> DefaultToolbar(
                        onTextClick = { onModeChange(ToolbarMode.TEXT) },
                        onInsertClick = onOpenInsertSheet,
                        onDrawClick = { onModeChange(ToolbarMode.DRAW) },
                        onPageClick = onOpenPageManager,
                        onMoreClick = onOpenMoreMenu
                    )

                    ToolbarMode.TEXT -> TextContextualToolbar(
                        textBlock = selectedTextBlock,
                        onFormat = onFormatText,
                        onDone = onDoneSelection
                    )

                    ToolbarMode.IMAGE -> ImageContextualToolbar(
                        onScale = onImageScale,
                        onRotate = onImageRotate,
                        onDelete = onDeleteImage,
                        onDone = onDoneSelection
                    )

                    ToolbarMode.TABLE -> TableContextualToolbar(
                        onAddRow = onAddTableRow,
                        onDeleteRow = onDeleteTableRow,
                        onAddCol = onAddTableCol,
                        onDeleteCol = onDeleteTableCol,
                        onDeleteTable = onDeleteTable,
                        onDone = onDoneSelection
                    )

                    ToolbarMode.DRAW -> DrawContextualToolbar(
                        drawToolState = drawToolState,
                        onUpdateDrawTool = onUpdateDrawTool,
                        onClearCanvas = onClearDrawStrokes,
                        onDone = {
                            onModeChange(ToolbarMode.DEFAULT)
                        }
                    )

                    ToolbarMode.PAGE_MANAGER -> DefaultToolbar(
                        onTextClick = { onModeChange(ToolbarMode.TEXT) },
                        onInsertClick = onOpenInsertSheet,
                        onDrawClick = { onModeChange(ToolbarMode.DRAW) },
                        onPageClick = onOpenPageManager,
                        onMoreClick = onOpenMoreMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultToolbar(
    onTextClick: () -> Unit,
    onInsertClick: () -> Unit,
    onDrawClick: () -> Unit,
    onPageClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarItemCustom(
            title = "Text",
            testTag = "toolbar_text_button",
            onClick = onTextClick,
            iconContent = {
                Text(
                    text = "Aa",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        )

        ToolbarItemCustom(
            title = "Insert",
            testTag = "toolbar_insert_button",
            onClick = onInsertClick,
            iconContent = {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Insert",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )

        ToolbarItemCustom(
            title = "Draw",
            testTag = "toolbar_draw_button",
            onClick = onDrawClick,
            iconContent = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Draw",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )

        ToolbarItemCustom(
            title = "Page",
            testTag = "toolbar_page_button",
            onClick = onPageClick,
            iconContent = {
                Icon(
                    Icons.Default.Description,
                    contentDescription = "Page",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )

        ToolbarItemCustom(
            title = "More",
            testTag = "toolbar_more_button",
            onClick = onMoreClick,
            iconContent = {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "More",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}

@Composable
private fun TextContextualToolbar(
    textBlock: TextBlock?,
    onFormat: (
        isBold: Boolean?,
        isItalic: Boolean?,
        isUnderline: Boolean?,
        fontSizeDelta: Float?,
        fontFamily: String?,
        textColorHex: String?,
        highlightColorHex: String?,
        clearHighlight: Boolean,
        alignment: TextAlignment?,
        lineSpacingMultiplier: Float?
    ) -> Unit,
    onDone: () -> Unit
) {
    var activeSubTab by remember { mutableStateOf(TextSubTab.TEXT) }
    var showColorDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        // Tab Header: Text | Style | Font | Size | Align | Spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextSubTab.values().forEach { tab ->
                val isSelected = activeSubTab == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(28.dp)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Spacer(modifier = Modifier.height(8.dp))

        // Content per active sub-tab, with a smooth slide/fade between tabs
        AnimatedContent(
            targetState = activeSubTab,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(240)) { it / 10 }) togetherWith
                    fadeOut(tween(120))
            },
            label = "TextSubTabTransition"
        ) { subTab ->
            when (subTab) {
            TextSubTab.TEXT -> {
                val isBold = textBlock?.isBold == true
                val isItalic = textBlock?.isItalic == true
                val isUnderline = textBlock?.isUnderline == true
                val currentAlign = textBlock?.alignment ?: TextAlignment.LEFT
                val currentSize = (textBlock?.fontSize ?: 16f).toInt()
                val currentColorHex = textBlock?.textColorHex ?: "#000000"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: B, I, U, Left, Center, Right, A_ (Text color)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bold
                        FormatSquareButton(
                            selected = isBold,
                            onClick = { onFormat(!isBold, null, null, null, null, null, null, false, null, null) }
                        ) {
                            Text("B", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        // Italic
                        FormatSquareButton(
                            selected = isItalic,
                            onClick = { onFormat(null, !isItalic, null, null, null, null, null, false, null, null) }
                        ) {
                            Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        }

                        // Underline
                        FormatSquareButton(
                            selected = isUnderline,
                            onClick = { onFormat(null, null, !isUnderline, null, null, null, null, false, null, null) }
                        ) {
                            Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        }

                        // Left Align
                        FormatSquareButton(
                            selected = currentAlign == TextAlignment.LEFT,
                            onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.LEFT, null) }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = "Left", modifier = Modifier.size(20.dp))
                        }

                        // Center Align
                        FormatSquareButton(
                            selected = currentAlign == TextAlignment.CENTER,
                            onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.CENTER, null) }
                        ) {
                            Icon(Icons.Default.FormatAlignCenter, contentDescription = "Center", modifier = Modifier.size(20.dp))
                        }

                        // Right Align
                        FormatSquareButton(
                            selected = currentAlign == TextAlignment.RIGHT,
                            onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.RIGHT, null) }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = "Right", modifier = Modifier.size(20.dp))
                        }

                        // Text Color Button
                        FormatSquareButton(
                            selected = false,
                            onClick = { showColorDialog = true }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("A", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .background(parseColorSafely(currentColorHex, Color.Black), RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }

                    // Row 2: [Aa] (Font picker) | [ - | 16 | + ] (Size Stepper) | [ ■ ] (Color swatch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Aa Font shortcut button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .height(46.dp)
                                .weight(1.2f)
                                .clickable { activeSubTab = TextSubTab.FONT }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Aa", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        // Segmented Size Stepper [ - | 16 | + ]
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .height(46.dp)
                                .weight(2.4f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(46.dp)
                                        .clickable { onFormat(null, null, null, -1f, null, null, null, false, null, null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("−", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Text(
                                    text = "$currentSize",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(46.dp)
                                        .clickable { onFormat(null, null, null, 1f, null, null, null, false, null, null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Color swatch button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .height(46.dp)
                                .width(52.dp)
                                .clickable { showColorDialog = true }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(parseColorSafely(currentColorHex, Color.Black), RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }
            }

            TextSubTab.STYLE -> {
                val isBold = textBlock?.isBold == true
                val isItalic = textBlock?.isItalic == true
                val isUnderline = textBlock?.isUnderline == true

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StyleActionItem(
                            label = "Bold",
                            selected = isBold,
                            onClick = { onFormat(!isBold, null, null, null, null, null, null, false, null, null) }
                        ) {
                            Text("B", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }

                        StyleActionItem(
                            label = "Italic",
                            selected = isItalic,
                            onClick = { onFormat(null, !isItalic, null, null, null, null, null, false, null, null) }
                        ) {
                            Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }

                        StyleActionItem(
                            label = "Underline",
                            selected = isUnderline,
                            onClick = { onFormat(null, null, !isUnderline, null, null, null, null, false, null, null) }
                        ) {
                            Text("U", textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StyleActionItem(
                            label = "Text Color",
                            selected = false,
                            onClick = { showColorDialog = true }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("A", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .background(parseColorSafely(textBlock?.textColorHex ?: "#000000", Color.Black))
                                )
                            }
                        }

                        StyleActionItem(
                            label = "Highlight",
                            selected = textBlock?.highlightColorHex != null,
                            onClick = {
                                if (textBlock?.highlightColorHex != null) {
                                    onFormat(null, null, null, null, null, null, null, true, null, null)
                                } else {
                                    onFormat(null, null, null, null, null, null, "#FEF08A", false, null, null)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Highlight", modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            TextSubTab.FONT -> {
                val currentFont = DocumentFonts.byId(textBlock?.fontFamily)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    DocumentFonts.all.forEach { font ->
                        val isSelected = currentFont == font
                        val rowBg by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else Color.Transparent,
                            animationSpec = tween(180),
                            label = "fontRowBg"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(rowBg)
                                .clickable {
                                    onFormat(null, null, null, null, font.id, null, null, false, null, null)
                                }
                                .padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = font.displayName,
                                    fontFamily = font.composeFamily,
                                    fontSize = 17.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = font.sample,
                                    fontFamily = font.composeFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            TextSubTab.SIZE -> {
                val currentSize = (textBlock?.fontSize ?: 16f).toInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$currentSize pt",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { onFormat(null, null, null, -1f, null, null, null, false, null, null) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = currentSize.toFloat(),
                            onValueChange = { newVal ->
                                val delta = newVal - currentSize
                                onFormat(null, null, null, delta, null, null, null, false, null, null)
                            },
                            valueRange = 8f..40f,
                            steps = 31,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(
                            onClick = { onFormat(null, null, null, 1f, null, null, null, false, null, null) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(8, 16, 24, 32, 40).forEach { tick ->
                            Text(
                                text = "$tick",
                                fontSize = 12.sp,
                                color = if (tick == currentSize) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = if (tick == currentSize) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            TextSubTab.ALIGN -> {
                val currentAlign = textBlock?.alignment ?: TextAlignment.LEFT

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AlignCard(
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        label = "Left",
                        selected = currentAlign == TextAlignment.LEFT,
                        modifier = Modifier.weight(1f),
                        onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.LEFT, null) }
                    )
                    AlignCard(
                        icon = Icons.Default.FormatAlignCenter,
                        label = "Center",
                        selected = currentAlign == TextAlignment.CENTER,
                        modifier = Modifier.weight(1f),
                        onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.CENTER, null) }
                    )
                    AlignCard(
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                        label = "Right",
                        selected = currentAlign == TextAlignment.RIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.RIGHT, null) }
                    )
                    AlignCard(
                        icon = Icons.Default.FormatAlignJustify,
                        label = "Justify",
                        selected = currentAlign == TextAlignment.JUSTIFY,
                        modifier = Modifier.weight(1f),
                        onClick = { onFormat(null, null, null, null, null, null, null, false, TextAlignment.JUSTIFY, null) }
                    )
                }
            }

            TextSubTab.SPACING -> {
                val currentSpacing = textBlock?.lineSpacingMultiplier ?: 1.35f
                val spacingOptions = listOf(1.0f, 1.15f, 1.35f, 1.5f, 2.0f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    spacingOptions.forEach { sp ->
                        val isSelected = Math.abs(currentSpacing - sp) < 0.05f
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { onFormat(null, null, null, null, null, null, null, false, null, sp) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${sp}x",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    // Color picker dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("Choose Color", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Palette", style = MaterialTheme.typography.labelMedium)
                    val colors = listOf(
                        "#000000", "#374151", "#6B7280", "#2563EB",
                        "#1D4ED8", "#DC2626", "#B91C1C", "#16A34A",
                        "#D97706", "#9333EA", "#0D9488", "#E11D48"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.take(6).forEach { hex ->
                            ColorDot(hex = hex) {
                                onFormat(null, null, null, null, null, hex, null, false, null, null)
                                showColorDialog = false
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.drop(6).forEach { hex ->
                            ColorDot(hex = hex) {
                                onFormat(null, null, null, null, null, hex, null, false, null, null)
                                showColorDialog = false
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ColorDot(hex: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(parseColorSafely(hex, Color.Black))
            .border(1.dp, Color.LightGray, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FormatSquareButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "FormatBtnBg"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "FormatBtnFg"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        contentColor = contentColor,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun StyleActionItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    val containerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "StyleActionBg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                iconContent()
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AlignCard(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "AlignCardBg"
    )
    val cardTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "AlignCardTint"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .height(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = cardTint
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = cardTint,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ToolbarItemCustom(
    title: String,
    testTag: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImageContextualToolbar(
    onScale: (Float) -> Unit,
    onRotate: (Float) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onScale(1.1f) }) {
                Text("+ Scale")
            }
            FilledTonalButton(onClick = { onScale(0.9f) }) {
                Text("− Scale")
            }
            FilledTonalButton(onClick = { onRotate(90f) }) {
                Icon(Icons.Default.RotateRight, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("90°")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Image", tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onDone) {
                Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TableContextualToolbar(
    onAddRow: () -> Unit,
    onDeleteRow: () -> Unit,
    onAddCol: () -> Unit,
    onDeleteCol: () -> Unit,
    onDeleteTable: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(onClick = onAddRow) {
            Text("+ Row")
        }
        FilledTonalButton(onClick = onDeleteRow) {
            Text("− Row")
        }
        FilledTonalButton(onClick = onAddCol) {
            Text("+ Col")
        }
        FilledTonalButton(onClick = onDeleteCol) {
            Text("− Col")
        }
        OutlinedButton(
            onClick = onDeleteTable,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Delete Table")
        }
        IconButton(onClick = onDone) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DrawContextualToolbar(
    drawToolState: DrawToolState,
    onUpdateDrawTool: (DrawToolState) -> Unit,
    onClearCanvas: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconToggleButton(
                checked = drawToolState.type == DrawToolType.PEN,
                onCheckedChange = { onUpdateDrawTool(drawToolState.copy(type = DrawToolType.PEN)) }
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Pen")
            }
            FilledIconToggleButton(
                checked = drawToolState.type == DrawToolType.HIGHLIGHTER,
                onCheckedChange = { onUpdateDrawTool(drawToolState.copy(type = DrawToolType.HIGHLIGHTER)) }
            ) {
                Icon(Icons.Default.Brush, contentDescription = "Highlighter")
            }
            FilledIconToggleButton(
                checked = drawToolState.type == DrawToolType.ERASER,
                onCheckedChange = { onUpdateDrawTool(drawToolState.copy(type = DrawToolType.ERASER)) }
            ) {
                Icon(Icons.Default.AutoFixNormal, contentDescription = "Eraser")
            }
            IconButton(onClick = onClearCanvas) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Canvas")
            }
        }

        IconButton(onClick = onDone) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun parseColorSafely(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        fallback
    }
}
