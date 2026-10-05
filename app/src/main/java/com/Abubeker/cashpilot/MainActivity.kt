package com.Abubeker.cashpilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.Abubeker.cashpilot.ui.BusinessViewModel
import com.Abubeker.cashpilot.ui.screens.*
import com.Abubeker.cashpilot.ui.theme.CashPilotTheme

sealed class Screen(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object Dashboard : Screen("dashboard", "Home", { Icon(Icons.Default.Dashboard, contentDescription = null) })
    object Customers : Screen("customers", "People", { Icon(Icons.Default.People, contentDescription = null) })
    object Suppliers : Screen("suppliers", "Suppliers", { Icon(Icons.Default.Business, contentDescription = null) })
    object Inventory : Screen("inventory", "Stock", { Icon(Icons.Default.Inventory, contentDescription = null) })
    object Cash : Screen("cash", "Ledger", { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) })
    object Reports : Screen("reports", "Insights", { Icon(Icons.Default.Assessment, contentDescription = null) })
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CashPilotTheme {
                val viewModel: BusinessViewModel = viewModel()
                val navController = rememberNavController()
                val items = listOf(
                    Screen.Dashboard,
                    Screen.Customers,
                    Screen.Suppliers,
                    Screen.Inventory,
                    Screen.Cash,
                    Screen.Reports
                )

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            items.forEach { screen ->
                                NavigationBarItem(
                                    icon = screen.icon,
                                    label = { Text(screen.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Dashboard.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScreen = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(Screen.Customers.route) { CustomersScreen(viewModel) }
                        composable(Screen.Suppliers.route) { SuppliersScreen(viewModel) }
                        composable(Screen.Inventory.route) { InventoryScreen(viewModel) }
                        composable(Screen.Cash.route) { CashScreen(viewModel) }
                        composable(Screen.Reports.route) { ReportsScreen(viewModel) }
                    }
                }
            }
        }
    }
}
