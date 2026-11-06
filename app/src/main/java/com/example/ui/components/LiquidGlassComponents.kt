package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalPaperThemeConfig
import com.example.ui.theme.PaperAccentBlue
import com.example.ui.theme.PaperAccentPrimary
import com.example.ui.theme.PaperAccentSecondary
import com.example.ui.theme.PaperAccentViolet
import com.example.ui.theme.PaperBgMain
import com.example.ui.theme.PaperBgSecondary
import com.example.ui.theme.PaperGlassBorderL1
import com.example.ui.theme.PaperGlassBorderL2
import com.example.ui.theme.PaperGlassBorderL3
import com.example.ui.theme.PaperGlassSurfaceL1
import com.example.ui.theme.PaperGlassSurfaceL2
import com.example.ui.theme.PaperGlassSurfaceL3
import com.example.ui.theme.PaperHighlight
import com.example.ui.theme.PaperSuccess
import com.example.ui.theme.PaperSurfaceElevated
import com.example.ui.theme.PaperTextMuted
import com.example.ui.theme.PaperTextPrimary
import com.example.ui.theme.PaperTextSecondary
import com.example.ui.theme.PrimaryBrandGradient
import com.example.ui.theme.parseHexColor

/**
 * Three-Tier Liquid Glass Intensity System (Part 2.3):
 * - LEVEL_1_SUBTLE: Secondary controls, chips, badges, compact pills.
 * - LEVEL_2_STANDARD: Floating navigation, search bar, composer, pinned cards.
 * - LEVEL_3_ELEVATED: Modals, profile cards, thread panels, contextual overlays.
 */
enum class GlassIntensity {
    LEVEL_1_SUBTLE,
    LEVEL_2_STANDARD,
    LEVEL_3_ELEVATED
}

@Composable
fun AmbientLiquidBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val themeConfig = LocalPaperThemeConfig.current
    val bgBase = if (themeConfig.darkTheme) PaperBgMain else MaterialTheme.colorScheme.background
    val accentPrimary = parseHexColor(themeConfig.accentHex, PaperAccentPrimary)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
            .let { baseMod ->
                if (themeConfig.darkTheme) {
                    baseMod.drawWithCache {
                        val width = size.width
                        val height = size.height
                        val topLeftCenter = Offset(x = width * 0.18f, y = height * 0.14f)
                        val topLeftRadius = (width * 0.78f).coerceAtLeast(1f)
                        val topLeftBrush = Brush.radialGradient(
                            colors = listOf(
                                accentPrimary.copy(alpha = 0.14f),
                                PaperAccentViolet.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = topLeftCenter,
                            radius = topLeftRadius
                        )

                        val bottomRightCenter = Offset(x = width * 0.82f, y = height * 0.82f)
                        val bottomRightRadius = (width * 0.75f).coerceAtLeast(1f)
                        val bottomRightBrush = Brush.radialGradient(
                            colors = listOf(
                                PaperAccentBlue.copy(alpha = 0.12f),
                                PaperAccentSecondary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = bottomRightCenter,
                            radius = bottomRightRadius
                        )

                        onDrawBehind {
                            drawCircle(
                                brush = topLeftBrush,
                                center = topLeftCenter,
                                radius = topLeftRadius
                            )
                            drawCircle(
                                brush = bottomRightBrush,
                                center = bottomRightCenter,
                                radius = bottomRightRadius
                            )
                        }
                    }
                } else {
                    baseMod
                }
            }
    ) {
        content()
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.LEVEL_2_STANDARD,
    shape: Shape = RoundedCornerShape(20.dp),
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val themeConfig = LocalPaperThemeConfig.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && !themeConfig.reducedMotion) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_press_scale"
    )

    val baseFill = when (intensity) {
        GlassIntensity.LEVEL_1_SUBTLE -> if (themeConfig.darkTheme) {
            PaperBgSecondary.copy(alpha = 0.76f)
        } else {
            Color.White.copy(alpha = 0.78f)
        }
        GlassIntensity.LEVEL_2_STANDARD -> if (themeConfig.darkTheme) {
            PaperSurfaceElevated.copy(alpha = 0.84f)
        } else {
            Color.White.copy(alpha = 0.88f)
        }
        GlassIntensity.LEVEL_3_ELEVATED -> if (themeConfig.darkTheme) {
            Color(0xFF161C2B).copy(alpha = 0.94f)
        } else {
            Color.White.copy(alpha = 0.96f)
        }
    }

    val topSheen = when (intensity) {
        GlassIntensity.LEVEL_1_SUBTLE -> PaperGlassSurfaceL1
        GlassIntensity.LEVEL_2_STANDARD -> PaperGlassSurfaceL2
        GlassIntensity.LEVEL_3_ELEVATED -> PaperGlassSurfaceL3
    }

    val borderStrokeColor = when (intensity) {
        GlassIntensity.LEVEL_1_SUBTLE -> PaperGlassBorderL1
        GlassIntensity.LEVEL_2_STANDARD -> PaperGlassBorderL2
        GlassIntensity.LEVEL_3_ELEVATED -> PaperGlassBorderL3
    }

    val shadowElevation = when (intensity) {
        GlassIntensity.LEVEL_1_SUBTLE -> 2.dp
        GlassIntensity.LEVEL_2_STANDARD -> 8.dp
        GlassIntensity.LEVEL_3_ELEVATED -> 16.dp
    }

    val surfaceBrush = Brush.linearGradient(
        colors = listOf(
            (accentTint?.copy(alpha = 0.14f) ?: topSheen),
            Color.Transparent
        )
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            borderStrokeColor,
            borderStrokeColor.copy(alpha = 0.35f)
        )
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = (accentTint ?: PaperAccentPrimary).copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(baseFill)
            .background(surfaceBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(clickableModifier),
        content = content
    )
}

@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val themeConfig = LocalPaperThemeConfig.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !themeConfig.reducedMotion) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    val shape = RoundedCornerShape(16.dp)
    val accentColor = parseHexColor(themeConfig.accentHex, PaperAccentPrimary)
    val gradient = if (isPrimary) {
        Brush.linearGradient(listOf(accentColor, PaperAccentBlue))
    } else {
        Brush.linearGradient(listOf(PaperSurfaceElevated, PaperBgSecondary))
    }

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .minimumInteractiveComponentSize()
            .scale(scale)
            .height(48.dp)
            .clip(shape)
            .background(if (enabled) gradient else Brush.linearGradient(listOf(PaperSurfaceElevated, PaperSurfaceElevated)))
            .border(
                width = 1.dp,
                color = if (isPrimary) PaperHighlight.copy(alpha = 0.35f) else PaperGlassBorderL2,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = if (enabled) Color.White else PaperTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Color.White else PaperTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PaperAvatar(
    name: String,
    avatarHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    isOnline: Boolean = false,
    isGroup: Boolean = false
) {
    val baseColor = parseHexColor(avatarHex, PaperAccentPrimary)
    val initials = remember(name) {
        name.trim()
            .split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "PM" }
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(if (isGroup) RoundedCornerShape(size * 0.34f) else CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.88f),
                            baseColor.copy(alpha = 0.48f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = if (isGroup) RoundedCornerShape(size * 0.34f) else CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.35f).sp
                ),
                color = Color.White
            )
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.28f).coerceAtLeast(10f).dp)
                    .clip(CircleShape)
                    .background(PaperBgMain)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(PaperSuccess)
            )
        }
    }
}

enum class MainNavSection(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    CHATS("Chats", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chats_tab"),
    COMMUNITIES("Communities", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_communities_tab"),
    CALLS("Calls", Icons.Filled.Call, Icons.Outlined.Call, "nav_calls_tab"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_profile_tab")
}

@Composable
fun FloatingLiquidGlassNavBar(
    currentSection: MainNavSection,
    unreadChatsCount: Int,
    unreadCommunitiesCount: Int,
    onSelectSection: (MainNavSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeConfig = LocalPaperThemeConfig.current
    val accentColor = parseHexColor(themeConfig.accentHex, PaperAccentPrimary)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            intensity = GlassIntensity.LEVEL_2_STANDARD,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .testTag("floating_glass_nav_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainNavSection.entries.forEach { section ->
                    val isSelected = section == currentSection
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) Color.White else PaperTextSecondary,
                        label = "nav_icon_tint"
                    )
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 0.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "nav_indicator_width"
                    )
                    val badgeCount = when (section) {
                        MainNavSection.CHATS -> unreadChatsCount
                        MainNavSection.COMMUNITIES -> unreadCommunitiesCount
                        else -> 0
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSelectSection(section) }
                            .testTag(section.testTag)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.linearGradient(
                                                    listOf(
                                                        accentColor.copy(alpha = 0.35f),
                                                        PaperAccentBlue.copy(alpha = 0.25f)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                            }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) section.selectedIcon else section.unselectedIcon,
                                        contentDescription = section.label,
                                        tint = iconTint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                if (badgeCount > 0) {
                                    Surface(
                                        color = accentColor,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 2.dp)
                                    ) {
                                        Text(
                                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) PaperTextPrimary else PaperTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (indicatorWidth > 0.dp) {
                                Box(
                                    modifier = Modifier
                                        .width(indicatorWidth)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBrandGradient)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(3.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
