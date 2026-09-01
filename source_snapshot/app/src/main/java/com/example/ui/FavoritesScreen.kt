package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.LessonPlanListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: LessonPlanListViewModel = viewModel(),
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        viewModel.loadFavoritePlans()
    }
    
    val plans by viewModel.plans.collectAsState()
    val df = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        com.example.ui.components.SoftCard(
            containerColor = com.example.ui.theme.PlannerColors.SecondarySoft
        ) {
            com.example.ui.components.SectionTitle(
                title = "Избранные планы",
                subtitle = "Всё важное — под рукой"
            )
        }

        if (plans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.example.ui.components.mascot.PlannerMascot(
                        com.example.ui.components.mascot.MascotState.FavoritesEmpty,
                        modifier = Modifier.size(142.dp)
                    )
                    Text(
                        "Здесь пока нет избранного",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Нажмите на сердечко у нужного плана",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(plans, key = { it.id }) { plan ->
                    com.example.ui.components.PlanCard(
                        plan = plan,
                        onOpen = { navController.navigate("plan_detail/${plan.id}") },
                        onToggleFavorite = {
                            viewModel.toggleFavorite(plan.id, false)
                            viewModel.loadFavoritePlans()
                        }
                    )
                }
            }
        }
    }
}
