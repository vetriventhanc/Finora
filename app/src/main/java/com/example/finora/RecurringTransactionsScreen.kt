
package com.example.finora

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finora.data.RecurringTransactionEntity
import com.example.finora.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RecurringGreen = Color(0xFF176B4D)
private val RecurringRed = Color(0xFFD9534F)

@Composable
fun RecurringTransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel
) {
    val recurringItems by viewModel.recurringTransactions
        .collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Recurring",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Manage scheduled income and expenses",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            FilledIconButton(
                onClick = { showAddDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = RecurringGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add recurring transaction"
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        errorMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = RecurringRed.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    message,
                    color = RecurringRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (recurringItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "No recurring transactions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Add a scheduled income or expense.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RecurringGreen
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add recurring")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = recurringItems,
                    key = { it.id }
                ) { item ->
                    RecurringTransactionCard(
                        item = item,
                        onToggle = { active ->
                            viewModel.setRecurringTransactionActive(
                                id = item.id,
                                isActive = active,
                                onError = { errorMessage = it }
                            )
                        },
                        onDelete = {
                            viewModel.deleteRecurringTransaction(
                                recurring = item,
                                onSuccess = {
                                    errorMessage = null
                                },
                                onError = {
                                    errorMessage = it
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, category, note, frequency, startDate ->
                viewModel.addRecurringTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note,
                    frequency = frequency,
                    nextRunDate = startDate,
                    onSuccess = {
                        errorMessage = null
                        showAddDialog = false
                    },
                    onError = {
                        errorMessage = it
                    }
                )
            }
        )
    }
}

@Composable
private fun RecurringTransactionCard(
    item: RecurringTransactionEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = item.type == "INCOME"
    val amountColor = if (isIncome) RecurringGreen else RecurringRed
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${item.category} • ${item.frequency.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Text(
                    (if (isIncome) "+" else "−") + formatRecurringINR(item.amount),
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Next: ${formatRecurringDate(item.nextRunDate)}",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        if (item.isActive) "Active" else "Paused",
                        fontSize = 12.sp,
                        color = if (item.isActive) RecurringGreen else Color.Gray
                    )
                }

                IconButton(
                    onClick = { onToggle(!item.isActive) }
                ) {
                    Icon(
                        if (item.isActive) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = if (item.isActive) {
                            "Pause recurring transaction"
                        } else {
                            "Resume recurring transaction"
                        },
                        tint = RecurringGreen
                    )
                }

                IconButton(
                    onClick = { confirmDelete = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete recurring transaction",
                        tint = RecurringRed
                    )
                }
            }

            if (item.note.isNotBlank()) {
                Text(
                    item.note,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete recurring transaction?") },
            text = {
                Text("This removes the schedule. Transactions already created will remain.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = RecurringRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDelete = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        note: String,
        frequency: String,
        startDate: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var category by remember { mutableStateOf("General") }
    var note by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val selectedDate = datePickerState.selectedDateMillis
        ?: System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New recurring transaction",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Income") }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Frequency", fontWeight = FontWeight.SemiBold)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("DAILY", "WEEKLY").forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = {
                                Text(option.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("MONTHLY", "YEARLY").forEach { option ->
                        FilterChip(
                            selected = frequency == option,
                            onClick = { frequency = option },
                            label = {
                                Text(option.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Starts: ${formatRecurringDate(selectedDate)}")
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()

                    when {
                        title.isBlank() ->
                            error = "Enter a title."

                        amount == null || !amount.isFinite() || amount <= 0.0 ->
                            error = "Enter a valid amount greater than zero."

                        category.isBlank() ->
                            error = "Enter a category."

                        else -> onSave(
                            title.trim(),
                            amount,
                            type,
                            category.trim(),
                            note.trim(),
                            frequency,
                            selectedDate
                        )
                    }
                }
            ) {
                Text("Save", color = RecurringGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatRecurringINR(amount: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale("en", "IN")
    ).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }.format(amount)
}

private fun formatRecurringDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))
}
