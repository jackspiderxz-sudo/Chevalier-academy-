package com.example.ui.screens.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Lightweight, permission-aware destination launcher item used by Admin Dashboard
 * section hubs to route directly into existing Management editors without duplicating
 * or rebuilding internal card contents.
 */
data class AdminHubRouteItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isAllowed: Boolean,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun AdminSectionHubScreen(
    hubTestTag: String,
    subtitle: String,
    items: List<AdminHubRouteItem>
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminDashboardBlueprint.PageBackgroundBrush),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .testTag(hubTestTag),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (subtitle.isNotBlank()) {
                item {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AdminDashboardBlueprint.TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            items(items = items, key = { it.id }) { item ->
                val shape = RoundedCornerShape(16.dp)
                Card(
                    shape = shape,
                    border = BorderStroke(
                        1.2.dp,
                        if (item.isAllowed) {
                            AdminDashboardBlueprint.CardOutlineBrush
                        } else {
                            AdminDashboardBlueprint.LockedOutlineBrush
                        }
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.isAllowed) {
                            AdminDashboardBlueprint.NavyCardSurface
                        } else {
                            AdminDashboardBlueprint.NavyCardSurfaceLocked
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 76.dp)
                        .clip(shape)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = item.title,
                            onClick = item.onClick
                        )
                        .testTag(item.testTag)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (item.isAllowed) {
                                        AdminDashboardBlueprint.NavyIconBadge
                                    } else {
                                        AdminDashboardBlueprint.NavyIconBadgeLocked
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (item.isAllowed) {
                                    AdminDashboardBlueprint.AccentLavender
                                } else {
                                    AdminDashboardBlueprint.TextMuted
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isAllowed) {
                                    AdminDashboardBlueprint.TextPrimary
                                } else {
                                    AdminDashboardBlueprint.TextMuted
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (item.isAllowed) {
                                    AdminDashboardBlueprint.TextSecondary
                                } else {
                                    AdminDashboardBlueprint.TextMuted
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = if (item.isAllowed) {
                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                            } else {
                                Icons.Filled.Lock
                            },
                            contentDescription = if (item.isAllowed) "Open ${item.title}" else "Restricted",
                            tint = if (item.isAllowed) {
                                AdminDashboardBlueprint.AccentLavender
                            } else {
                                AdminDashboardBlueprint.TextMuted
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAccessDeniedScreen(
    sectionTitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AdminDashboardBlueprint.PageBackgroundBrush)
            .padding(24.dp)
            .testTag("admin_access_denied_screen"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = AdminDashboardBlueprint.NavyCardSurface,
            border = BorderStroke(1.25.dp, AdminDashboardBlueprint.LockedOutlineBrush),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Access Restricted",
                    tint = AdminDashboardBlueprint.AccentLavender,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Access Restricted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AdminDashboardBlueprint.TextPrimary
                )
                Text(
                    text = "Your account does not have permission to access $sectionTitle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdminDashboardBlueprint.TextSecondary
                )
            }
        }
    }
}
