package com.hasan.nisabwallet.ui.screens.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hasan.nisabwallet.data.Result
import com.hasan.nisabwallet.data.firebase.FirestorePaths
import com.hasan.nisabwallet.data.safeCall
import com.hasan.nisabwallet.ui.components.*
import com.hasan.nisabwallet.ui.theme.Emerald600
import com.hasan.nisabwallet.ui.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class FeedbackUiState(
    val feedbackType: String   = "general",
    val rating: Int            = 0,
    val message: String        = "",
    val isSaving: Boolean      = false,
    val submitted: Boolean     = false,
    val errorMessage: String?  = null
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState = _uiState.asStateFlow()
    private val userId get() = auth.currentUser?.uid ?: ""

    fun onTypeChange(v: String)    { _uiState.value = _uiState.value.copy(feedbackType = v) }
    fun onRatingChange(v: Int)     { _uiState.value = _uiState.value.copy(rating = v) }
    fun onMessageChange(v: String) { _uiState.value = _uiState.value.copy(message = v, errorMessage = null) }

    fun submit() {
        val s = _uiState.value
        if (s.message.isBlank()) { _uiState.value = s.copy(errorMessage = "Please write your feedback"); return }
        if (s.rating == 0) { _uiState.value = s.copy(errorMessage = "Please select a rating"); return }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result: Result<Unit> = safeCall {
                FirestorePaths.feedback(db, userId).add(mapOf(
                    "feedbackId"  to UUID.randomUUID().toString(),
                    "type"        to s.feedbackType,
                    "rating"      to s.rating,
                    "message"     to s.message.trim(),
                    "userId"      to userId,
                    "createdAt"   to FieldValue.serverTimestamp()
                )).await()
                Unit
            }
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(isSaving = false, submitted = true)
                is Result.Error   -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    navController: NavController,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Feedback", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {

            if (uiState.submitted) {
                // Success state
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(40.dp))
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(72.dp), tint = Emerald600)
                    Text("Thank you!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Your feedback has been submitted.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NisabButton("Go Back", onClick = { navController.popBackStack() })
                }
                return@Column
            }

            Text("Help us improve Nisab Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            // Feedback type
            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("general" to "General", "bug" to "Bug", "feature" to "Feature").forEach { (k, l) ->
                    val sel = uiState.feedbackType == k
                    FilterChip(selected = sel, onClick = { viewModel.onTypeChange(k) }, label = { Text(l) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                }
            }

            // Star rating
            Text("Rating", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { viewModel.onRatingChange(star) }) {
                        Icon(if (star <= uiState.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            null, Modifier.size(32.dp), tint = if (star <= uiState.rating) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            uiState.errorMessage?.let { ErrorCard(it) }

            // Message
            OutlinedTextField(
                value = uiState.message, onValueChange = viewModel::onMessageChange,
                label = { Text("Your feedback") }, modifier = Modifier.fillMaxWidth().height(160.dp),
                shape = RoundedCornerShape(12.dp), maxLines = 8,
                placeholder = { Text("Tell us what you think, report a bug, or suggest a feature…", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) }
            )

            NisabButton("Submit Feedback", viewModel::submit, isLoading = uiState.isSaving)
        }
    }
}
