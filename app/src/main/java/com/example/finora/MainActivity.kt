@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.finora

import androidx.compose.material.icons.filled.CalendarMonth
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.finora.data.FinoraDatabase
import com.example.finora.data.BudgetEntity
import com.example.finora.data.TransactionEntity
import com.example.finora.data.TransactionRepository
import com.example.finora.viewmodel.TransactionViewModel
import com.example.finora.ui.theme.FinoraTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.YearMonth

private val FinoraGreen = Color(0xFF176B4D)
private val FinoraBackground = Color(0xFFF5F7F6)
private val IncomeGreen = Color(0xFF21865B)
private val ExpenseRed = Color(0xFFD9534F)

private val ChartColors = listOf(
    Color(0xFF176B4D),
    Color(0xFF4C9A80),
    Color(0xFFF2B544),
    Color(0xFF5B8DEF),
    Color(0xFFE17B5F),
    Color(0xFF9B79C9),
    Color(0xFF67B7C7),
    Color(0xFFB0A08A)
)

class MainActivity : ComponentActivity() {

    private val database by lazy {
        FinoraDatabase.getDatabase(applicationContext)
    }

    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModel.Factory(
            TransactionRepository(
                transactionDao = database.transactionDao(),
                budgetDao = database.budgetDao(),
                recurringTransactionDao = database.recurringTransactionDao(),
                database = database
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FinoraTheme {
                FinoraApp(viewModel)
            }
        }
    }
}

@Composable
private fun FinoraApp(viewModel: TransactionViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val transactions by viewModel.transactions
        .collectAsStateWithLifecycle()

    val totalIncome by viewModel.totalIncome
        .collectAsStateWithLifecycle()

    val totalExpenses by viewModel.totalExpenses
        .collectAsStateWithLifecycle()

    val balance = totalIncome - totalExpenses

    Scaffold(
        containerColor = FinoraBackground,
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = FinoraGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add transaction"
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null
                        )
                    },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null
                        )
                    },
                    label = { Text("Transactions") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = null
                        )
                    },
                    label = { Text("Analytics") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Default.Category,
                            contentDescription = null
                        )
                    },
                    label = { Text("Budgets") }
                )

                // NEW: Recurring Transactions tab
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null
                        )
                    },
                    label = { Text("Recurring") }
                )

                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    label = { Text("Profile") }
                )
            }
        }
    ) { innerPadding ->

        when (selectedTab) {

            0 -> DashboardScreen(
                modifier = Modifier.padding(innerPadding),
                balance = balance,
                income = totalIncome,
                expenses = totalExpenses,
                transactions = transactions,
                onAddTransaction = { showAddDialog = true }
            )

            1 -> TransactionsScreen(
                modifier = Modifier.padding(innerPadding),
                transactions = transactions,
                onDelete = viewModel::deleteTransaction
            )

            2 -> AnalyticsScreen(
                modifier = Modifier.padding(innerPadding),
                transactions = transactions
            )

            3 -> BudgetsScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )

            // NEW: Recurring Transactions screen
            4 -> RecurringTransactionsScreen(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel
            )

            5 -> PlaceholderScreen(
                modifier = Modifier.padding(innerPadding),
                title = "Profile",
                message = "Your profile and settings will appear here."
            )
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, type, category, note ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    note = note
                )
                showAddDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// DASHBOARD
// -----------------------------------------------------------------------------

@Composable
private fun DashboardScreen(
    modifier: Modifier = Modifier,
    balance: Double,
    income: Double,
    expenses: Double,
    transactions: List<TransactionEntity>,
    onAddTransaction: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FinoraBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Welcome back 👋",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Text(
                    text = "Your finances",
                    color = FinoraGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Balance
        item {
            BalanceCard(balance = balance)
        }

        // Income and expense summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = income,
                    color = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Expenses",
                    amount = expenses,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent transactions heading
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent transactions",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF252525)
                )

                TextButton(onClick = onAddTransaction) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FinoraGreen
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        text = "Add",
                        color = FinoraGreen
                    )
                }
            }
        }

        // Recent transactions list
        if (transactions.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(42.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "No transactions yet",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Add your first transaction to get started.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = onAddTransaction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinoraGreen
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(6.dp))

                            Text("Add transaction")
                        }
                    }
                }
            }
        } else {
            items(
                items = transactions
                    .sortedByDescending { it.date }
                    .take(5),
                key = { it.id }
            ) { transaction ->
                TransactionRow(
                    transaction = transaction
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Double) {
    val animatedBalance by animateFloatAsState(
        targetValue = balance.toFloat(),
        animationSpec = tween(durationMillis = 850),
        label = "balanceAmount"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF16A34A),
                            Color(0xFF087443),
                            Color(0xFF064E3B)
                        )
                    )
                )
        ) {
            // Decorative background circles
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .offset(x = 230.dp, y = (-80).dp)
                    .background(
                        Color.White.copy(alpha = 0.07f),
                        RoundedCornerShape(100.dp)
                    )
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = 275.dp, y = 115.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(100.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(15.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "TOTAL BALANCE",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "Your financial overview",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                AnimatedContent(
                    targetState = formatINR(animatedBalance.toDouble()),
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(250)
                        ) togetherWith fadeOut(
                            animationSpec = tween(150)
                        )
                    },
                    label = "balanceText"
                ) { value ->
                    Text(
                        text = value,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = "Your money, at a glance",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 750),
        label = "${title}Amount"
    )

    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            color,
                            RoundedCornerShape(50)
                        )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = title,
                    color = Color(0xFF647067),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = formatINR(animatedAmount.toDouble()),
                color = color,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ANALYTICS
// -----------------------------------------------------------------------------

private data class MonthData(
    val key: String,
    val label: String,
    val income: Double,
    val expenses: Double
)

private data class CategoryData(
    val category: String,
    val amount: Double
)

@Composable
private fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>
) {
    val calendar = remember {
        Calendar.getInstance()
    }

    var selectedMonth by remember {
        mutableStateOf(monthKey(calendar))
    }

    val availableMonths = remember {
        (0..11).map { offset ->
            Calendar.getInstance().apply {
                add(Calendar.MONTH, -offset)
            }
        }.map {
            monthKey(it) to monthLabel(it)
        }
    }

    val selectedTransactions = remember(
        transactions,
        selectedMonth
    ) {
        transactions.filter {
            monthKey(it.date) == selectedMonth
        }
    }

    val income = selectedTransactions
        .filter { it.type == "INCOME" }
        .sumOf { it.amount }

    val expenses = selectedTransactions
        .filter { it.type == "EXPENSE" }
        .sumOf { it.amount }

    val savings = income - expenses

    val savingsRate = if (income > 0.0) {
        (savings / income) * 100.0
    } else {
        0.0
    }

    val monthlyData = remember(transactions) {
        (5 downTo 0).map { offset ->
            val month = Calendar.getInstance().apply {
                add(Calendar.MONTH, -offset)
            }

            val key = monthKey(month)

            val monthTransactions = transactions.filter {
                monthKey(it.date) == key
            }

            MonthData(
                key = key,
                label = SimpleDateFormat(
                    "MMM",
                    Locale.getDefault()
                ).format(month.time),
                income = monthTransactions
                    .filter { it.type == "INCOME" }
                    .sumOf { it.amount },
                expenses = monthTransactions
                    .filter { it.type == "EXPENSE" }
                    .sumOf { it.amount }
            )
        }
    }

    val categoryData = remember(selectedTransactions) {
        selectedTransactions
            .filter { it.type == "EXPENSE" }
            .groupBy {
                it.category.ifBlank { "Other" }
            }
            .map { (category, items) ->
                CategoryData(
                    category,
                    items.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    "Your money, clearly.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Text(
                    "Analytics",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinoraGreen
                )
            }
        }

        item {
            var expanded by remember {
                mutableStateOf(false)
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = it
                }
            ) {
                OutlinedTextField(
                    value = availableMonths.firstOrNull {
                        it.first == selectedMonth
                    }?.second ?: selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Selected month")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    availableMonths.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = {
                                Text(label)
                            },
                            onClick = {
                                selectedMonth = key
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    "Income",
                    income,
                    IncomeGreen,
                    Modifier.weight(1f)
                )

                SummaryCard(
                    "Expenses",
                    expenses,
                    ExpenseRed,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Net savings",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Text(
                        formatINR(savings),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (savings >= 0) {
                            IncomeGreen
                        } else {
                            ExpenseRed
                        }
                    )

                    Text(
                        if (income > 0) {
                            "Savings rate: ${
                                "%.1f".format(
                                    Locale.getDefault(),
                                    savingsRate
                                )
                            }%"
                        } else {
                            "Add income to calculate your savings rate."
                        },
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            AnalyticsCard(title = "Income vs. expenses") {
                Text(
                    "Last 6 months",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(14.dp))

                MonthlyBarChart(
                    data = monthlyData,
                    selectedMonth = selectedMonth
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChartLegend(IncomeGreen, "Income")
                    ChartLegend(ExpenseRed, "Expenses")
                }
            }
        }

        item {
            AnalyticsCard(title = "Expenses by category") {
                if (categoryData.isEmpty()) {
                    Text(
                        "No expenses recorded for this month.",
                        color = Color.Gray
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            data = categoryData,
                            modifier = Modifier
                                .weight(1f)
                                .height(190.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            categoryData.take(8)
                                .forEachIndexed { index, item ->
                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .background(
                                                    ChartColors[
                                                        index % ChartColors.size
                                                    ],
                                                    RoundedCornerShape(3.dp)
                                                )
                                        )

                                        Spacer(Modifier.width(7.dp))

                                        Column {
                                            Text(
                                                item.category,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Text(
                                                formatINR(item.amount),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Analytics are calculated from your saved transactions.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ANALYTICS CHART COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun AnalyticsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF26352D)
            )

            content()
        }
    }
}

@Composable
private fun MonthlyBarChart(
    data: List<MonthData>,
    selectedMonth: String
) {
    val maxAmount = data.maxOfOrNull {
        maxOf(it.income, it.expenses)
    }?.coerceAtLeast(1.0) ?: 1.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEach { month ->
                val incomeHeight =
                    (month.income / maxAmount).toFloat()
                        .coerceIn(0f, 1f)

                val expenseHeight =
                    (month.expenses / maxAmount).toFloat()
                        .coerceIn(0f, 1f)

                val isSelected = month.key == selectedMonth

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(incomeHeight)
                                .background(
                                    color = IncomeGreen.copy(
                                        alpha = if (isSelected) 1f else 0.75f
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 5.dp,
                                        topEnd = 5.dp
                                    )
                                )
                        )

                        Spacer(Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(expenseHeight)
                                .background(
                                    color = ExpenseRed.copy(
                                        alpha = if (isSelected) 1f else 0.75f
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 5.dp,
                                        topEnd = 5.dp
                                    )
                                )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = month.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (isSelected) {
                            FinoraGreen
                        } else {
                            Color.Gray
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.amount }

    Canvas(modifier = modifier) {
        if (total <= 0.0) return@Canvas

        val strokeWidth = size.minDimension * 0.18f

        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )

        var startAngle = -90f

        data.forEachIndexed { index, item ->
            val sweepAngle =
                (item.amount / total * 360.0).toFloat()

            drawArc(
                color = ChartColors[index % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )

            startAngle += sweepAngle
        }
    }
}

@Composable
private fun ChartLegend(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color,
                    RoundedCornerShape(3.dp)
                )
        )

        Spacer(Modifier.width(7.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// -----------------------------------------------------------------------------
// BUDGETS
// -----------------------------------------------------------------------------

@Composable
private fun BudgetsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }

    val monthKey = selectedMonth.toString()
    val budgets by remember(monthKey) {
        viewModel.getBudgetsForMonth(monthKey)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val totalBudget = budgets.sumOf { it.amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Budgets",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Plan your monthly spending",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            FilledTonalButton(
                onClick = {
                    editingBudget = null
                    showBudgetDialog = true
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = FinoraGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FinoraGreen)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Total planned budget",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
                Text(
                    formatINR(totalBudget),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${budgets.size} ${if (budgets.size == 1) "category" else "categories"}",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { selectedMonth = selectedMonth.minusMonths(1) }
                ) {
                    Text("‹", fontSize = 28.sp, color = FinoraGreen)
                }

                Text(
                    selectedMonth.format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                IconButton(
                    onClick = { selectedMonth = selectedMonth.plusMonths(1) }
                ) {
                    Text("›", fontSize = 28.sp, color = FinoraGreen)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No budgets for this month",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add a category budget to start tracking.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            editingBudget = null
                            showBudgetDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FinoraGreen
                        )
                    ) {
                        Text("Create budget")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgets, key = { it.id }) { budget ->
                    BudgetCard(
                        budget = budget,
                        onEdit = {
                            editingBudget = budget
                            showBudgetDialog = true
                        },
                        onDelete = {
                            viewModel.deleteBudget(budget)
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (showBudgetDialog) {
        BudgetEditorDialog(
            month = monthKey,
            budget = editingBudget,
            onDismiss = { showBudgetDialog = false },
            onSave = { category, amount ->
                if (editingBudget == null) {
                    viewModel.saveBudget(
                        category = category,
                        amount = amount,
                        month = monthKey
                    )
                } else {
                    viewModel.updateBudget(
                        editingBudget!!.copy(
                            category = category,
                            amount = amount,
                            month = monthKey
                        )
                    )
                }
                showBudgetDialog = false
            }
        )
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    viewModel: TransactionViewModel
) {
    val spending by viewModel
        .getCategorySpending(budget.category, budget.month)
        .collectAsStateWithLifecycle(initialValue = 0.0)

    val budgetAmount = budget.amount.coerceAtLeast(0.0)
    val percentage = if (budgetAmount > 0.0) {
        spending / budgetAmount
    } else {
        0.0
    }

    val progress = percentage
        .toFloat()
        .coerceIn(0f, 1f)

    val exceeded = spending > budgetAmount
    val limitReached = spending >= budgetAmount
    val approachingLimit = percentage >= 0.80 && !limitReached

    val remaining = (budgetAmount - spending).coerceAtLeast(0.0)
    val exceededAmount = (spending - budgetAmount).coerceAtLeast(0.0)

    val statusColor = when {
        exceeded -> ExpenseRed
        limitReached -> Color(0xFFF97316)
        approachingLimit -> Color(0xFFF59E0B)
        else -> FinoraGreen
    }

    val statusTitle = when {
        exceeded -> "Budget exceeded"
        limitReached -> "Budget limit reached"
        approachingLimit -> "Approaching budget limit"
        else -> "Budget on track"
    }

    val statusMessage = when {
        exceeded ->
            "You have spent ${formatINR(exceededAmount)} over your limit."

        limitReached ->
            "You have used 100% of this month's budget."

        approachingLimit ->
            "${formatINR(remaining)} remaining. You're getting close to your limit."

        else ->
            "${formatINR(remaining)} remaining for this month."
    }

    val progressColor = statusColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category and actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            FinoraGreen.copy(alpha = 0.10f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = FinoraGreen
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = budget.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "Monthly budget",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        tint = FinoraGreen
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete budget",
                        tint = ExpenseRed
                    )
                }
            }

            // Budget alert banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = statusColor.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = when {
                        exceeded -> "⚠"
                        limitReached -> "!"
                        approachingLimit -> "!"
                        else -> "✓"
                    },
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        text = statusTitle,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = statusMessage,
                        color = Color(0xFF555555),
                        fontSize = 12.sp
                    )
                }
            }

            // Spent and budget amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Text(
                        text = formatINR(spending),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (limitReached) {
                            statusColor
                        } else {
                            Color(0xFF252525)
                        }
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Budget",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )

                    Text(
                        text = formatINR(budgetAmount),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            // Spending progress
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = FinoraGreen.copy(alpha = 0.10f),
                strokeCap = StrokeCap.Round
            )

            // Percentage and remaining amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${kotlin.math.ceil(percentage * 100).toInt()}% used",
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (exceeded) {
                        "${formatINR(exceededAmount)} over"
                    } else {
                        "${formatINR(remaining)} left"
                    },
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
@Composable
private fun BudgetEditorDialog(
    month: String,
    budget: BudgetEntity?,
    onDismiss: () -> Unit,
    onSave: (category: String, amount: Double) -> Unit
) {
    var category by remember(budget?.id) {
        mutableStateOf(budget?.category ?: "")
    }

    var amountText by remember(budget?.id) {
        mutableStateOf(budget?.amount?.toString() ?: "")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (budget == null) "Create budget" else "Edit budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    YearMonth.parse(month).format(
                        java.time.format.DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.getDefault()
                        )
                    ),
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        errorMessage = null
                    },
                    label = { Text("Category") },
                    placeholder = {
                        Text("e.g. Food, Travel, Bills")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Monthly limit (₹)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
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
                        category.isBlank() ->
                            errorMessage = "Enter a category."

                        amount == null ||
                                !amount.isFinite() ||
                                amount <= 0.0 ->
                            errorMessage =
                                "Enter a valid amount greater than zero."

                        else -> onSave(category.trim(), amount)
                    }
                }
            ) {
                Text("Save", color = FinoraGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -----------------------------------------------------------------------------
// TRANSACTIONS
// -----------------------------------------------------------------------------

@Composable
private fun TransactionsScreen(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>,
    onDelete: (TransactionEntity) -> Unit
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf("ALL")
    }

    var selectedCategory by remember {
        mutableStateOf("All categories")
    }

    var selectedDateFilter by remember {
        mutableStateOf("Any time")
    }

    var categoryMenuExpanded by remember {
        mutableStateOf(false)
    }

    var dateMenuExpanded by remember {
        mutableStateOf(false)
    }

    val categories = remember(transactions) {
        transactions
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedType,
        selectedCategory,
        selectedDateFilter
    ) {
        val now = System.currentTimeMillis()

        val startOfCurrentMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val last30Days = now - (30L * 24 * 60 * 60 * 1000)

        transactions.filter { transaction ->

            val query = searchQuery.trim()

            val matchesSearch =
                query.isBlank() ||
                        transaction.title.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        transaction.category.contains(
                            query,
                            ignoreCase = true
                        ) ||
                        transaction.note.contains(
                            query,
                            ignoreCase = true
                        )

            val matchesType =
                selectedType == "ALL" ||
                        transaction.type == selectedType

            val matchesCategory =
                selectedCategory == "All categories" ||
                        transaction.category == selectedCategory

            val matchesDate = when (selectedDateFilter) {
                "This month" ->
                    transaction.date >= startOfCurrentMonth

                "Last 30 days" ->
                    transaction.date >= last30Days &&
                            transaction.date <= now

                else -> true
            }

            matchesSearch &&
                    matchesType &&
                    matchesCategory &&
                    matchesDate
        }
    }

    val hasActiveFilters =
        searchQuery.isNotBlank() ||
                selectedType != "ALL" ||
                selectedCategory != "All categories" ||
                selectedDateFilter != "Any time"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            text = "Transactions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // SEARCH
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search transactions")
            },
            placeholder = {
                Text("Title, category, or note")
            },
            singleLine = true,
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        // TRANSACTION TYPE FILTER
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedType == "ALL",
                onClick = {
                    selectedType = "ALL"
                },
                label = {
                    Text("All")
                }
            )

            FilterChip(
                selected = selectedType == "INCOME",
                onClick = {
                    selectedType = "INCOME"
                },
                label = {
                    Text("Income")
                }
            )

            FilterChip(
                selected = selectedType == "EXPENSE",
                onClick = {
                    selectedType = "EXPENSE"
                },
                label = {
                    Text("Expense")
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // CATEGORY AND DATE FILTERS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = {
                        categoryMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedCategory,
                        maxLines = 1
                    )
                }

                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = {
                        categoryMenuExpanded = false
                    }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("All categories")
                        },
                        onClick = {
                            selectedCategory = "All categories"
                            categoryMenuExpanded = false
                        }
                    )

                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(category)
                            },
                            onClick = {
                                selectedCategory = category
                                categoryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedButton(
                    onClick = {
                        dateMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedDateFilter,
                        maxLines = 1
                    )
                }

                DropdownMenu(
                    expanded = dateMenuExpanded,
                    onDismissRequest = {
                        dateMenuExpanded = false
                    }
                ) {
                    listOf(
                        "Any time",
                        "This month",
                        "Last 30 days"
                    ).forEach { dateFilter ->
                        DropdownMenuItem(
                            text = {
                                Text(dateFilter)
                            },
                            onClick = {
                                selectedDateFilter = dateFilter
                                dateMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // RESULTS COUNT AND RESET
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTransactions.size} transactions",
                color = Color.Gray,
                fontSize = 13.sp
            )

            if (hasActiveFilters) {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedType = "ALL"
                        selectedCategory = "All categories"
                        selectedDateFilter = "Any time"
                    }
                ) {
                    Text(
                        text = "Clear filters",
                        color = FinoraGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // FILTERED TRANSACTION LIST
        if (filteredTransactions.isEmpty()) {
            if (transactions.isEmpty()) {
                EmptyTransactions()
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No matching transactions",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Try changing your search or filters.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                        if (hasActiveFilters) {
                            Spacer(Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedType = "ALL"
                                    selectedCategory = "All categories"
                                    selectedDateFilter = "Any time"
                                }
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onDelete = {
                            onDelete(transaction)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onDelete: (() -> Unit)? = null
) {
    val isIncome = transaction.type == "INCOME"
    val amountColor = if (isIncome) {
        IncomeGreen
    } else {
        ExpenseRed
    }

    val prefix = if (isIncome) "+" else "−"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    transaction.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "${transaction.category} • ${
                        formatDate(transaction.date)
                    }",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                if (transaction.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        transaction.note,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "$prefix${formatINR(transaction.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete transaction",
                            tint = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactions() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = Color.LightGray,
                modifier = Modifier.size(48.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "No transactions yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                "Tap + to add your first transaction.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// ADD TRANSACTION DIALOG
// -----------------------------------------------------------------------------

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: String,
        category: String,
        note: String
    ) -> Unit
) {
    var title by remember {
        mutableStateOf("")
    }

    var amountText by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("General")
    }

    var note by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf("EXPENSE")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Transaction",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = {
                            type = "EXPENSE"
                        },
                        label = {
                            Text("Expense")
                        }
                    )

                    FilterChip(
                        selected = type == "INCOME",
                        onClick = {
                            type = "INCOME"
                        },
                        label = {
                            Text("Income")
                        }
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = {
                        Text("Title")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = {
                        Text("Amount (₹)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                    },
                    label = {
                        Text("Category")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("Note (optional)")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                errorMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
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
                        title.isBlank() -> {
                            errorMessage = "Please enter a title."
                        }

                        amount == null ||
                                !amount.isFinite() ||
                                amount <= 0.0 -> {
                            errorMessage = "Enter a valid amount."
                        }

                        category.isBlank() -> {
                            errorMessage = "Please enter a category."
                        }

                        else -> onSave(
                            title.trim(),
                            amount,
                            type,
                            category.trim(),
                            note.trim()
                        )
                    }
                }
            ) {
                Text(
                    "Save",
                    color = FinoraGreen
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -----------------------------------------------------------------------------
// PLACEHOLDERS AND HELPERS
// -----------------------------------------------------------------------------

@Composable
private fun PlaceholderScreen(
    modifier: Modifier = Modifier,
    title: String,
    message: String
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Text(
                message,
                color = Color.Gray
            )
        }
    }
}

private fun formatINR(amount: Double): String {
    val formatter =
        NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 0

    return formatter.format(amount)
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(timestamp))
}

private fun monthKey(calendar: Calendar): String {
    return SimpleDateFormat(
        "yyyy-MM",
        Locale.US
    ).format(calendar.time)
}

private fun monthKey(timestamp: Long): String {
    return monthKey(
        Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
    )
}

private fun monthLabel(calendar: Calendar): String {
    return SimpleDateFormat(
        "MMMM yyyy",
        Locale.getDefault()
    ).format(calendar.time)
}

