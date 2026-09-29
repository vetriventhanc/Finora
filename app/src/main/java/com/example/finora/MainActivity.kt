@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.finora

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
                budgetDao = database.budgetDao()
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

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
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

            else -> PlaceholderScreen(
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
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    "Hello, welcome back 👋",
                    color = Color.Gray
                )
                Text(
                    "Finora",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FinoraGreen
                )
            }
        }

        item {
            BalanceCard(balance)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Transactions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onAddTransaction) {
                    Text(
                        "Add new",
                        color = FinoraGreen
                    )
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                EmptyTransactions()
            }
        } else {
            items(
                transactions.take(5),
                key = { it.id }
            ) {
                TransactionRow(it)
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

@Composable
private fun AnalyticsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
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
    val maxValue = (data.maxOfOrNull {
        maxOf(it.income, it.expenses)
    } ?: 0.0).coerceAtLeast(1.0)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val groupWidth = size.width / data.size
            val barWidth = groupWidth * 0.25f
            val chartHeight = size.height - 8.dp.toPx()

            data.forEachIndexed { index, month ->
                val centerX =
                    groupWidth * index + groupWidth / 2f

                val incomeHeight =
                    (month.income / maxValue * chartHeight).toFloat()

                val expenseHeight =
                    (month.expenses / maxValue * chartHeight).toFloat()

                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(
                        centerX - barWidth - 2.dp.toPx(),
                        chartHeight - incomeHeight
                    ),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            5.dp.toPx()
                        )
                )

                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(
                        centerX + 2.dp.toPx(),
                        chartHeight - expenseHeight
                    ),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            5.dp.toPx()
                        )
                )

                if (month.key == selectedMonth) {
                    drawLine(
                        color = FinoraGreen.copy(alpha = 0.35f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            data.forEach { month ->
                Text(
                    text = month.label,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    color = if (month.key == selectedMonth) {
                        FinoraGreen
                    } else {
                        Color.Gray
                    },
                    fontWeight = if (month.key == selectedMonth) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    textAlign =
                        androidx.compose.ui.text.style.TextAlign.Center
                )
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

        val strokeWidth = 26.dp.toPx()
        var startAngle = -90f

        val diameter = minOf(size.width, size.height)
        val arcSize = Size(diameter, diameter)

        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )

        data.forEachIndexed { index, item ->
            val sweep =
                (item.amount / total * 360.0).toFloat()

            drawArc(
                color = ChartColors[index % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Butt
                )
            )

            startAngle += sweep
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
            Modifier
                .size(10.dp)
                .background(
                    color,
                    RoundedCornerShape(3.dp)
                )
        )

        Spacer(Modifier.width(6.dp))

        Text(
            label,
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
                        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
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

    val progress = if (budget.amount > 0.0) {
        (spending / budget.amount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    val exceeded = spending > budget.amount
    val progressColor = if (exceeded) ExpenseRed else FinoraGreen
    val remaining = (budget.amount - spending).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

                Column(Modifier.weight(1f)) {
                    Text(
                        budget.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (exceeded) "Budget exceeded" else "Monthly limit",
                        color = if (exceeded) ExpenseRed else Color.Gray,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spent", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        formatINR(spending),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (exceeded) ExpenseRed else Color(0xFF252525)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        formatINR(budget.amount),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = FinoraGreen.copy(alpha = 0.10f),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${((spending / budget.amount) * 100).toInt()}% used",
                    color = if (exceeded) ExpenseRed else Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    if (exceeded) "${formatINR(spending - budget.amount)} over budget"
                    else "${formatINR(remaining)} left",
                    color = if (exceeded) ExpenseRed else FinoraGreen,
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (budget == null) "Create budget" else "Edit budget",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    YearMonth.parse(month).format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
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
                    placeholder = { Text("e.g. Food, Travel, Bills") },
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
                        amount == null || !amount.isFinite() || amount <= 0.0 ->
                            errorMessage = "Enter a valid amount greater than zero."
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        Text(
            "Transactions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        if (transactions.isEmpty()) {
            EmptyTransactions()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    transactions,
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
// ADD TRANSACTION
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