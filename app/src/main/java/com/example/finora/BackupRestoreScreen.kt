
package com.example.finora

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.withTransaction
import com.example.finora.data.BudgetEntity
import com.example.finora.data.FinoraDatabase
import com.example.finora.data.RecurringTransactionEntity
import com.example.finora.data.SavingsGoalEntity
import com.example.finora.data.TransactionEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BACKUP_FORMAT = "finora-backup"
private const val BACKUP_VERSION = 1

// Leave some room below Firestore's 1 MiB document limit.
private const val MAX_CLOUD_BACKUP_BYTES = 900_000

@Composable
fun ProfileAndBackupScreen(
    modifier: Modifier = Modifier,
    database: FinoraDatabase,
    onOpenAppLock: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showCloudRestoreConfirmation by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isBusy = true
                val result = runCatching {
                    val json = withContext(Dispatchers.IO) {
                        createBackupJson(database)
                    }
                    context.contentResolver
                        .openOutputStream(uri)
                        ?.bufferedWriter(Charsets.UTF_8)
                        ?.use { it.write(json.toString(2)) }
                        ?: error("Could not open the selected backup file.")
                }
                isBusy = false
                showMessage(
                    if (result.isSuccess) {
                        "Backup created successfully"
                    } else {
                        "Backup failed: ${
                            result.exceptionOrNull()?.localizedMessage
                                ?: "Unknown error"
                        }"
                    }
                )
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmation = true
        }
    }

    fun uploadCloudBackup() {
        val user = auth.currentUser
        if (user == null) {
            showMessage("Please sign in to back up to the cloud.")
            return
        }

        scope.launch {
            isBusy = true
            val result = runCatching {
                val json = withContext(Dispatchers.IO) {
                    createBackupJson(database).toString()
                }

                val byteSize = json.toByteArray(Charsets.UTF_8).size
                require(byteSize <= MAX_CLOUD_BACKUP_BYTES) {
                    "This backup is too large for one Firestore document. " +
                            "Your local backup is still available."
                }

                // Re-check the active account before writing.
                val activeUser = auth.currentUser
                    ?: error("You have been signed out.")
                require(activeUser.uid == user.uid) {
                    "Your account changed. Please try again."
                }

                firestore.collection("users")
                    .document(user.uid)
                    .collection("backups")
                    .document("latest")
                    .set(
                        mapOf(
                            "format" to BACKUP_FORMAT,
                            "version" to BACKUP_VERSION,
                            "payload" to json,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
            }

            isBusy = false
            showMessage(
                if (result.isSuccess) {
                    "Cloud backup completed successfully."
                } else {
                    "Cloud backup failed: ${
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Unknown error"
                    }"
                }
            )
        }
    }

    fun restoreCloudBackup() {
        val user = auth.currentUser
        if (user == null) {
            showMessage("Please sign in to restore from the cloud.")
            return
        }

        scope.launch {
            isBusy = true
            val result = runCatching {
                val activeUser = auth.currentUser
                    ?: error("You have been signed out.")
                require(activeUser.uid == user.uid) {
                    "Your account changed. Please try again."
                }

                val snapshot = firestore.collection("users")
                    .document(user.uid)
                    .collection("backups")
                    .document("latest")
                    .get()
                    .await()

                require(snapshot.exists()) {
                    "No cloud backup found for this account."
                }

                val payload = snapshot.getString("payload")
                    ?: error("The cloud backup is missing its data.")

                val root = JSONObject(payload)
                validateBackup(root)

                withContext(Dispatchers.IO) {
                    restoreBackup(database, root)
                }
            }

            isBusy = false
            showMessage(
                if (result.isSuccess) {
                    "Cloud backup restored successfully."
                } else {
                    "Cloud restore failed: ${
                        result.exceptionOrNull()?.localizedMessage
                            ?: "Unknown error"
                    }"
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profile & Backup",
            color = Color(0xFF176B4D),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Keep a copy of your Finora data and restore it when changing phones.",
            color = Color.Gray,
            fontSize = 14.sp
        )

        FirebaseAuthScreen()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF176B4D)
                    )
                    Text(
                        "  Backup & Restore",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    "Your backup contains transactions, budgets, recurring " +
                            "transactions, and savings goals. Store backup files privately.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Button(
                    onClick = {
                        val stamp = SimpleDateFormat(
                            "yyyyMMdd_HHmm",
                            Locale.getDefault()
                        ).format(Date())
                        createBackupLauncher.launch(
                            "finora_backup_$stamp.json"
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null
                    )
                    Text("  Create Local Backup")
                }

                OutlinedButton(
                    onClick = {
                        restoreBackupLauncher.launch(
                            arrayOf("application/json", "text/*", "*/*")
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null
                    )
                    Text("  Restore Local Backup")
                }

                androidx.compose.material3.HorizontalDivider()

                Text(
                    "Cloud Backup",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Cloud backup requires a signed-in Firebase account. " +
                            "It saves one latest backup for this account.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Button(
                    onClick = { uploadCloudBackup() },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null
                    )
                    Text("  Back Up to Cloud")
                }

                OutlinedButton(
                    onClick = {
                        if (auth.currentUser == null) {
                            showMessage(
                                "Please sign in to restore from the cloud."
                            )
                        } else {
                            showCloudRestoreConfirmation = true
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null
                    )
                    Text("  Restore from Cloud")
                }

                if (isBusy) {
                    Text(
                        "Please wait…",
                        color = Color(0xFF176B4D)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onOpenAppLock,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("App Lock Settings")
        }

        Text(
            "Restoring replaces the current records in all four data tables. " +
                    "Create a backup first if you may need the existing data.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }

    if (showRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmation = false
                pendingRestoreUri = null
            },
            title = { Text("Replace current Finora data?") },
            text = {
                Text(
                    "This will replace your transactions, budgets, recurring " +
                            "transactions, and savings goals with the selected " +
                            "backup. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingRestoreUri
                        showRestoreConfirmation = false
                        pendingRestoreUri = null

                        if (uri != null) {
                            scope.launch {
                                isBusy = true
                                val result = runCatching {
                                    val raw = withContext(Dispatchers.IO) {
                                        context.contentResolver
                                            .openInputStream(uri)
                                            ?.bufferedReader(Charsets.UTF_8)
                                            ?.use { it.readText() }
                                            ?: error(
                                                "Could not read the selected backup."
                                            )
                                    }

                                    val root = JSONObject(raw)
                                    validateBackup(root)

                                    withContext(Dispatchers.IO) {
                                        restoreBackup(database, root)
                                    }
                                }

                                isBusy = false
                                showMessage(
                                    if (result.isSuccess) {
                                        "Backup restored successfully"
                                    } else {
                                        "Restore failed: ${
                                            result.exceptionOrNull()
                                                ?.localizedMessage
                                                ?: "Invalid backup"
                                        }"
                                    }
                                )
                            }
                        }
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmation = false
                        pendingRestoreUri = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    if (showCloudRestoreConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showCloudRestoreConfirmation = false
            },
            title = { Text("Restore cloud backup?") },
            text = {
                Text(
                    "This will replace your current transactions, budgets, " +
                            "recurring transactions, and savings goals with the " +
                            "backup saved in your signed-in account. " +
                            "Create a local backup first if you may need your current data."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloudRestoreConfirmation = false
                        restoreCloudBackup()
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCloudRestoreConfirmation = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun createBackupJson(
    database: FinoraDatabase
): JSONObject {
    val transactions =
        database.transactionDao().getAllTransactionsOnce()
    val budgets =
        database.budgetDao().getAllBudgetsOnce()
    val recurring =
        database.recurringTransactionDao().getAllRecurringOnce()
    val goals =
        database.savingsGoalDao().getAllGoalsOnce()

    return JSONObject().apply {
        put("format", BACKUP_FORMAT)
        put("version", BACKUP_VERSION)
        put("createdAt", System.currentTimeMillis())

        put("transactions", JSONArray().apply {
            transactions.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("amount", item.amount)
                    put("type", item.type)
                    put("category", item.category)
                    put("date", item.date)
                    put("note", item.note)
                })
            }
        })

        put("budgets", JSONArray().apply {
            budgets.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("category", item.category)
                    put("amount", item.amount)
                    put("month", item.month)
                    put("createdAt", item.createdAt)
                })
            }
        })

        put("recurringTransactions", JSONArray().apply {
            recurring.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("amount", item.amount)
                    put("type", item.type)
                    put("category", item.category)
                    put("note", item.note)
                    put("frequency", item.frequency)
                    put("nextRunDate", item.nextRunDate)
                    if (item.endDate == null) {
                        put("endDate", JSONObject.NULL)
                    } else {
                        put("endDate", item.endDate)
                    }
                    put("isActive", item.isActive)
                    put("createdAt", item.createdAt)
                })
            }
        })

        put("savingsGoals", JSONArray().apply {
            goals.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("targetAmount", item.targetAmount)
                    put("savedAmount", item.savedAmount)
                    if (item.targetDate == null) {
                        put("targetDate", JSONObject.NULL)
                    } else {
                        put("targetDate", item.targetDate)
                    }
                    put("createdAt", item.createdAt)
                })
            }
        })
    }
}

private fun validateBackup(root: JSONObject) {
    require(root.optString("format") == BACKUP_FORMAT) {
        "This is not a Finora backup file."
    }
    require(root.optInt("version", -1) == BACKUP_VERSION) {
        "This backup version is not supported."
    }

    listOf(
        "transactions",
        "budgets",
        "recurringTransactions",
        "savingsGoals"
    ).forEach { key ->
        require(root.opt(key) is JSONArray) {
            "Backup is missing the '$key' list."
        }
    }
}

private suspend fun restoreBackup(
    database: FinoraDatabase,
    root: JSONObject
) {
    val transactions =
        root.getJSONArray("transactions").let { array ->
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                TransactionEntity(
                    id = item.getInt("id"),
                    title = item.getString("title"),
                    amount = item.getDouble("amount"),
                    type = item.getString("type"),
                    category = item.getString("category"),
                    date = item.getLong("date"),
                    note = item.optString("note", "")
                )
            }
        }

    val budgets =
        root.getJSONArray("budgets").let { array ->
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                BudgetEntity(
                    id = item.getInt("id"),
                    category = item.getString("category"),
                    amount = item.getDouble("amount"),
                    month = item.getString("month"),
                    createdAt = item.getLong("createdAt")
                )
            }
        }

    val recurring =
        root.getJSONArray("recurringTransactions").let { array ->
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                RecurringTransactionEntity(
                    id = item.getInt("id"),
                    title = item.getString("title"),
                    amount = item.getDouble("amount"),
                    type = item.getString("type"),
                    category = item.getString("category"),
                    note = item.optString("note", ""),
                    frequency = item.getString("frequency"),
                    nextRunDate = item.getLong("nextRunDate"),
                    endDate = if (item.isNull("endDate")) {
                        null
                    } else {
                        item.getLong("endDate")
                    },
                    isActive = item.optBoolean("isActive", true),
                    createdAt = item.getLong("createdAt")
                )
            }
        }

    val goals =
        root.getJSONArray("savingsGoals").let { array ->
            (0 until array.length()).map { i ->
                val item = array.getJSONObject(i)
                SavingsGoalEntity(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    targetAmount = item.getDouble("targetAmount"),
                    savedAmount = item.getDouble("savedAmount"),
                    targetDate = if (item.isNull("targetDate")) {
                        null
                    } else {
                        item.getLong("targetDate")
                    },
                    createdAt = item.getLong("createdAt")
                )
            }
        }

    database.withTransaction {
        database.transactionDao().deleteAllTransactions()
        database.budgetDao().deleteAllBudgets()
        database.recurringTransactionDao().deleteAllRecurring()
        database.savingsGoalDao().deleteAllGoals()

        database.transactionDao().insertTransactions(transactions)
        database.budgetDao().insertBudgets(budgets)
        database.recurringTransactionDao().insertRecurring(recurring)
        database.savingsGoalDao().insertGoals(goals)
    }
}
