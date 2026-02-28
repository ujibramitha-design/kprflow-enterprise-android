package com.kprflow.enterprise.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Responsive Layout that adapts to screen size
@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSize) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val windowSize = when {
        screenWidth < 600.dp -> WindowSize.Compact
        screenWidth < 840.dp -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
    
    content(windowSize)
}

// Window Size enum
enum class WindowSize {
    Compact,
    Medium,
    Expanded
}

// Responsive Grid that adapts columns based on screen size
@Composable
fun <T> ResponsiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    compactColumns: Int = 1,
    mediumColumns: Int = 2,
    expandedColumns: Int = 3,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable (T) -> Unit
) {
    ResponsiveLayout { windowSize ->
        val columns = when (windowSize) {
            WindowSize.Compact -> compactColumns
            WindowSize.Medium -> mediumColumns
            WindowSize.Expanded -> expandedColumns
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalArrangement = verticalArrangement
        ) {
            items(items) { item ->
                content(item)
            }
        }
    }
}

// Responsive Card that adapts size based on screen
@Composable
fun ResponsiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ResponsiveLayout { windowSize ->
        val cardModifier = when (windowSize) {
            WindowSize.Compact -> modifier.fillMaxWidth()
            WindowSize.Medium -> modifier.width(200.dp)
            WindowSize.Expanded -> modifier.width(250.dp)
        }
        
        Card(
            onClick = onClick ?: {},
            modifier = cardModifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

// Responsive Navigation that adapts layout
@Composable
fun ResponsiveNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ResponsiveLayout { windowSize ->
        when (windowSize) {
            WindowSize.Compact -> {
                // Bottom navigation for compact screens
                NavigationBar(
                    modifier = modifier
                ) {
                    NavigationItem(
                        icon = androidx.compose.material.icons.Icons.Default.Home,
                        label = "Home",
                        route = "home",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                    
                    NavigationItem(
                        icon = androidx.compose.material.icons.Icons.Default.Search,
                        label = "Search",
                        route = "search",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                    
                    NavigationItem(
                        icon = androidx.compose.material.icons.Icons.Default.Person,
                        label = "Profile",
                        route = "profile",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                }
            }
            
            WindowSize.Medium, WindowSize.Expanded -> {
                // Navigation rail for larger screens
                NavigationRail(
                    modifier = modifier
                ) {
                    NavigationRailItem(
                        icon = androidx.compose.material.icons.Icons.Default.Home,
                        label = "Home",
                        route = "home",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                    
                    NavigationRailItem(
                        icon = androidx.compose.material.icons.Icons.Default.Search,
                        label = "Search",
                        route = "search",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                    
                    NavigationRailItem(
                        icon = androidx.compose.material.icons.Icons.Default.Person,
                        label = "Profile",
                        route = "profile",
                        currentRoute = currentRoute,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

// Helper for Navigation Bar Item
@Composable
private fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    route: String,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBarItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = { Text(label) },
        selected = currentRoute == route,
        onClick = { onNavigate(route) }
    )
}

// Helper for Navigation Rail Item
@Composable
private fun NavigationRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    route: String,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationRailItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = { Text(label) },
        selected = currentRoute == route,
        onClick = { onNavigate(route) }
    )
}

// Responsive Row that adapts to screen size
@Composable
fun ResponsiveRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    ResponsiveLayout { windowSize ->
        when (windowSize) {
            WindowSize.Compact -> {
                // Use Column for compact screens
                Column(
                    modifier = modifier,
                    verticalArrangement = when (horizontalArrangement) {
                        Arrangement.Start -> Arrangement.Top
                        Arrangement.End -> Arrangement.Bottom
                        Arrangement.Center -> Arrangement.Center
                        else -> Arrangement.Top
                    }
                ) {
                    // Convert Row content to Column
                    content()
                }
            }
            
            WindowSize.Medium, WindowSize.Expanded -> {
                // Use Row for larger screens
                Row(
                    modifier = modifier,
                    horizontalArrangement = horizontalArrangement,
                    verticalAlignment = verticalAlignment
                ) {
                    content()
                }
            }
        }
    }
}

// Responsive Text that adapts font size
@Composable
fun ResponsiveText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.material3.Typography.() -> androidx.compose.ui.text.TextStyle = { bodyMedium },
    maxLines: Int = Int.MAX_VALUE
) {
    ResponsiveLayout { windowSize ->
        val fontSize = when (windowSize) {
            WindowSize.Compact -> 14.sp
            WindowSize.Medium -> 16.sp
            WindowSize.Expanded -> 18.sp
        }
        
        Text(
            text = text,
            modifier = modifier,
            fontSize = fontSize,
            maxLines = maxLines,
            style = style()
        )
    }
}

// Responsive Button that adapts size
@Composable
fun ResponsiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ResponsiveLayout { windowSize ->
        val buttonModifier = when (windowSize) {
            WindowSize.Compact -> modifier.fillMaxWidth()
            WindowSize.Medium -> modifier.widthIn(min = 120.dp, max = 200.dp)
            WindowSize.Expanded -> modifier.widthIn(min = 150.dp, max = 250.dp)
        }
        
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled
        ) {
            ResponsiveText(text = text)
        }
    }
}

// Responsive Scaffold that adapts layout structure
@Composable
fun ResponsiveScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    topBar: @Composable () -> Unit = {},
        floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    ResponsiveLayout { windowSize ->
        when (windowSize) {
            WindowSize.Compact -> {
                // Mobile layout with bottom navigation
                Scaffold(
                    topBar = topBar,
                    bottomBar = {
                        ResponsiveNavigation(
                            currentRoute = currentRoute,
                            onNavigate = onNavigate
                        )
                    },
                    floatingActionButton = floatingActionButton,
                    content = content
                )
            }
            
            WindowSize.Medium -> {
                // Tablet layout with navigation rail
                Scaffold(
                    topBar = topBar,
                    floatingActionButton = floatingActionButton,
                    content = { paddingValues ->
                        Row(modifier = Modifier.fillMaxSize()) {
                            ResponsiveNavigation(
                                currentRoute = currentRoute,
                                onNavigate = onNavigate,
                                modifier = Modifier.padding(vertical = paddingValues.calculateTopPadding())
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(paddingValues)
                            ) {
                                content(PaddingValues(0.dp))
                            }
                        }
                    }
                )
            }
            
            WindowSize.Expanded -> {
                // Desktop layout with permanent navigation drawer
                PermanentNavigationDrawer(
                    drawerContent = {
                        NavigationDrawer {
                            items.forEach { item ->
                                NavigationDrawerItem(
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.label) },
                                    selected = currentRoute == item.route,
                                    onClick = { onNavigate(item.route) }
                                )
                            }
                        }
                    }
                ) {
                    Scaffold(
                        topBar = topBar,
                        floatingActionButton = floatingActionButton,
                        content = content
                    )
                }
            }
        }
    }
}

// Navigation items for drawer
private data class NavigationItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val route: String
)

private val items = listOf(
    NavigationItem(
        icon = androidx.compose.material.icons.Icons.Default.Home,
        label = "Home",
        route = "home"
    ),
    NavigationItem(
        icon = androidx.compose.material.icons.Icons.Default.Search,
        label = "Search",
        route = "search"
    ),
    NavigationItem(
        icon = androidx.compose.material.icons.Icons.Default.Person,
        label = "Profile",
        route = "profile"
    )
)
