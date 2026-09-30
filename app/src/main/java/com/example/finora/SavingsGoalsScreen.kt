
package com.example.finora

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finora.data.SavingsGoalEntity
import com.example.finora.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.util.Locale

private val SavingsGreen = Color(0xFF176B4D)
private val SavingsBackground = Color(0xFFF5F7F6)

@Composable
fun SavingsGoalsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel
) {
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGoal by remember {
        mutableStateOf<SavingsGoalEntity?>(null)
    }
    var goalToDelete by remember {
        mutableStateOf<SavingsGoalEntity?>(null)
    }
    var screenMessage by remember { mutableStateOf<String?>(null) }

    val totalSaved = goals.sumOf { it.savedAmount }
    val totalTarget = goals.sumOf { it.targetAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SavingsBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Savings Goals",
                    color = SavingsGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Small steps today, big goals tomorrow.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SavingsGreen
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Total saved",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )

                    Text(
                        text = formatSavingsINR(totalSaved),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Across ${goals.size} goal${if (goals.size == 1) "" else "s"}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )

                    if (totalTarget > 0.0) {
                        LinearProgressIndicator(
                            progress = {
                                (totalSaved / totalTarget)
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Text(
                            text = "${(totalSaved / totalTarget * 100)
                                .toInt()
                                .coerceIn(0, 100)}% of combined targets",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    screenMessage = null
                    showCreateDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavingsGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Spacer(Modifier.size(8.dp))
                Text("Create savings goal")
            }
        }

        if (screenMessage != null) {
            item {
                Text(
                    text = screenMessage.orEmpty(),
                    color = Color(0xFFD9534F),
                    fontSize = 13.sp
                )
            }
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No savings goals yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Create a goal for something you're saving for.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Your goals",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF252525)
                )
            }

            items(
                items = goals,
                key = { it.id }
            ) { goal ->
                SavingsGoalCard(
                    goal = goal,
                    onAddSavings = {
                        screenMessage = null
                        selectedGoal = goal
                    },
                    onDelete = {
                        screenMessage = null
                        goalToDelete = goal
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        CreateSavingsGoalDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { name, target ->
                viewModel.createSavingsGoal(
                    name = name,
                    targetAmount = target,
                    onSuccess = {
                        showCreateDialog = false
                        screenMessage = null
                    },
                    onError = { message ->
                        screenMessage = message
                    }
                )
            }
        )
    }

    selectedGoal?.let { goal ->
        AddSavingsDialog(
            goal = goal,
            onDismiss = { selectedGoal = null },
            onSave = { amount ->
                viewModel.addSavings(
                    goalId = goal.id,
                    amount = amount,
                    onSuccess = {
                        selectedGoal = null
                        screenMessage = null
                    },
                    onError = { message ->
                        screenMessage = message
                    }
                )
            }
        )
    }

    goalToDelete?.let { goal ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Delete savings goal?") },
            text = {
                Text("Delete \"${goal.name}\" and its saved progress?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavingsGoal(
                            goal = goal,
                            onSuccess = {
                                goalToDelete = null
                                screenMessage = null
                            },
                            onError = { message ->
                                goalToDelete = null
                                screenMessage = message
                            }
                        )
                    }
                ) {
                    Text(
                        "Delete",
                        color = Color(0xFFD9534F)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { goalToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    onAddSavings: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0.0) {
        (goal.savedAmount / goal.targetAmount)
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val remaining = (goal.targetAmount - goal.savedAmount)
        .coerceAtLeast(0.0)

    val completed = goal.savedAmount >= goal.targetAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = goal.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF252525)
                    )

                    Text(
                        text = if (completed) {
                            "Goal completed 🎉"
                        } else {
                            "${formatSavingsINR(remaining)} left to save"
                        },
                        color = if (completed) {
                            SavingsGreen
                        } else {
                            Color.Gray
                        },
                        fontSize = 13.sp
                    )
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        tint = Color(0xFFD9534F)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = SavingsGreen,
                trackColor = Color(0xFFE6EAE8)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% saved",
                    color = SavingsGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Text(
                    text = "Target ${formatSavingsINR(goal.targetAmount)}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Saved",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatSavingsINR(goal.savedAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onAddSavings,
                    enabled = !completed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavingsGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Add savings")
                }
            }
        }
    }
}

@Composable
private fun CreateSavingsGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create savings goal") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Goal name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = {
                        targetText = it
                        error = null
                    },
                    label = { Text("Target amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = Color(0xFFD9534F),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = targetText.toDoubleOrNull()

                    when {
                        name.isBlank() ->
                            error = "Please enter a goal name."

                        target == null ||
                                !target.isFinite() ||
                                target <= 0.0 ->
                            error = "Enter a valid target amount."

                        else -> onSave(name.trim(), target)
                    }
                }
            ) {
                Text("Create", color = SavingsGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddSavingsDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val remaining = (goal.targetAmount - goal.savedAmount)
        .coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add savings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${goal.name}\nRemaining: ${formatSavingsINR(remaining)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        error = null
                    },
                    label = { Text("Amount to add (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = Color(0xFFD9534F),
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()

                    when {
                        amount == null ||
                                !amount.isFinite() ||
                                amount <= 0.0 ->
                            error = "Enter a valid amount."

                        amount > remaining ->
                            error = "Amount cannot exceed the remaining balance."

                        else -> onSave(amount)
                    }
                }
            ) {
                Text("Add", color = SavingsGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatSavingsINR(amount: Double): String {
    val formatter =
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0

    return formatter.format(amount)
}
