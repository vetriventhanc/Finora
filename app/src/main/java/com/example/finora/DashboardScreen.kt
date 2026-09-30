
package com.example.finora

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finora.data.BudgetEntity
import com.example.finora.data.SavingsGoalEntity
import com.example.finora.data.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private val DashboardGreen = Color(0xFF176B4D)
private val DashboardBackground = Color(0xFFF5F7F6)
private val DashboardIncome = Color(0xFF21865B)
private val DashboardExpense = Color(0xFFD9534F)
private val DashboardText = Color(0xFF26332C)
private val DashboardMuted = Color(0xFF78847C)

private fun dashboardINR(amount: Double): String {
    return NumberFormat.getCurrencyInstance(
        Locale("en", "IN")
    ).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }.format(amount)
}

private fun isInCurrentMonth(
    timestamp: Long,
    month: YearMonth,
    zone: ZoneId
): Boolean {
    val date = Instant.ofEpochMilli(timestamp)
        .atZone(zone)

    return YearMonth.from(date) == month
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    savingsGoals: List<SavingsGoalEntity>,
    onAddTransaction: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val currentMonth = YearMonth.now(zone)

    val monthTransactions = transactions.filter {
        isInCurrentMonth(it.date, currentMonth, zone)
    }

    val monthlyIncome = monthTransactions
        .filter { it.type.equals("INCOME", ignoreCase = true) }
        .sumOf { it.amount }

    val monthlyExpenses = monthTransactions
        .filter { it.type.equals("EXPENSE", ignoreCase = true) }
        .sumOf { it.amount }

    val netCashFlow = monthlyIncome - monthlyExpenses

    val totalBudget = budgets
        .filter { it.month == currentMonth.toString() }
        .sumOf { it.amount }

    val budgetedCategories = budgets
        .filter { it.month == currentMonth.toString() }
        .map { it.category.trim().lowercase() }
        .toSet()

    val budgetedSpending = monthTransactions
        .filter {
            it.type.equals("EXPENSE", ignoreCase = true) &&
                    it.category.trim().lowercase() in budgetedCategories
        }
        .sumOf { it.amount }

    val remainingBudget = totalBudget - budgetedSpending

    val totalSaved = savingsGoals.sumOf {
        it.savedAmount.coerceAtLeast(0.0)
    }

    val totalSavingsTarget = savingsGoals.sumOf {
        it.targetAmount.coerceAtLeast(0.0)
    }

    val savingsProgress = if (totalSavingsTarget > 0.0) {
        (totalSaved / totalSavingsTarget).toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground)
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
                    text = "Welcome back 👋",
                    color = DashboardMuted,
                    fontSize = 14.sp
                )

                Text(
                    text = "Your finances",
                    color = DashboardGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = currentMonth.month.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() } +
                            " " + currentMonth.year,
                    color = DashboardMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            CashFlowCard(
                income = monthlyIncome,
                expenses = monthlyExpenses,
                net = netCashFlow
            )
        }

        item {
            SectionHeading(
                title = "Monthly overview",
                subtitle = "Your income and spending"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(
                    title = "Income",
                    amount = monthlyIncome,
                    color = DashboardIncome,
                    icon = {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = DashboardIncome
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                OverviewCard(
                    title = "Expenses",
                    amount = monthlyExpenses,
                    color = DashboardExpense,
                    icon = {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = DashboardExpense
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeading(
                title = "Savings goals",
                subtitle = "${savingsGoals.size} active goal(s)"
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = DashboardGreen
                        )

                        Spacer(Modifier.size(10.dp))

                        Text(
                            "Total saved",
                            color = DashboardMuted,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            dashboardINR(totalSaved),
                            color = DashboardGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { savingsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = DashboardGreen,
                        trackColor = Color(0xFFE5EEE9),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(savingsProgress * 100).toInt()}% of target",
                            color = DashboardMuted,
                            fontSize = 12.sp
                        )

                        Text(
                            "Target ${dashboardINR(totalSavingsTarget)}",
                            color = DashboardMuted,
                            fontSize = 12.sp
                        )
                    }

                    if (savingsGoals.isEmpty()) {
                        Text(
                            "Create a savings goal to track your progress here.",
                            color = DashboardMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            SectionHeading(
                title = "Budget overview",
                subtitle = "This month's budget usage"
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = DashboardGreen
                        )

                        Spacer(Modifier.size(10.dp))

                        Text(
                            "Total budget",
                            color = DashboardMuted
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            dashboardINR(totalBudget),
                            fontWeight = FontWeight.Bold,
                            color = DashboardText
                        )
                    }

                    val budgetProgress = if (totalBudget > 0.0) {
                        (budgetedSpending / totalBudget)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    LinearProgressIndicator(
                        progress = { budgetProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = when {
                            totalBudget <= 0.0 -> DashboardMuted
                            budgetedSpending >= totalBudget -> DashboardExpense
                            budgetedSpending >= totalBudget * 0.8 -> Color(0xFFE0A126)
                            else -> DashboardGreen
                        },
                        trackColor = Color(0xFFE5EEE9),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Spent ${dashboardINR(budgetedSpending)}",
                            color = DashboardMuted,
                            fontSize = 12.sp
                        )

                        Text(
                            "${(budgetProgress * 100).toInt()}%",
                            color = DashboardMuted,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = when {
                            budgets.isEmpty() ->
                                "No budgets set for this month."
                            remainingBudget < 0 ->
                                "${dashboardINR(-remainingBudget)} over budget"
                            else ->
                                "${dashboardINR(remainingBudget)} remaining"
                        },
                        color = when {
                            remainingBudget < 0 -> DashboardExpense
                            else -> DashboardGreen
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionHeading(
                    title = "Recent transactions",
                    subtitle = "Your latest activity"
                )

                TextButton(onClick = onAddTransaction) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = DashboardGreen
                    )
                    Text("Add", color = DashboardGreen)
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
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
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = DashboardMuted,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "No transactions yet",
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Add your first transaction to get started.",
                            color = DashboardMuted,
                            fontSize = 13.sp
                        )

                        TextButton(onClick = onAddTransaction) {
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
                RecentTransactionCard(transaction)
            }
        }
    }
}

@Composable
private fun CashFlowCard(
    income: Double,
    expenses: Double,
    net: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF16A34A),
                            Color(0xFF087443),
                            Color(0xFF064E3B)
                        )
                    )
                )
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.White
                )

                Spacer(Modifier.size(8.dp))

                Text(
                    "MONTHLY NET CASH FLOW",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                dashboardINR(net),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                if (net >= 0) "You're spending less than you earn."
                else "Your expenses are higher than your income.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniAmount(
                    label = "Income",
                    amount = income,
                    modifier = Modifier.weight(1f)
                )
                MiniAmount(
                    label = "Expenses",
                    amount = expenses,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MiniAmount(
    label: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )

        Text(
            dashboardINR(amount),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun OverviewCard(
    title: String,
    amount: Double,
    color: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(Modifier.size(8.dp))

                Text(
                    title,
                    color = DashboardMuted,
                    fontSize = 13.sp
                )
            }

            Text(
                dashboardINR(amount),
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            color = DashboardText,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            subtitle,
            color = DashboardMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RecentTransactionCard(
    transaction: TransactionEntity
) {
    val isIncome = transaction.type.equals(
        "INCOME",
        ignoreCase = true
    )

    val amountColor = if (isIncome) {
        DashboardIncome
    } else {
        DashboardExpense
    }

    val dateText = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    ).format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        amountColor.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Default.ArrowDownward
                    else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = amountColor
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title,
                    color = DashboardText,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "${transaction.category} • $dateText",
                    color = DashboardMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                (if (isIncome) "+" else "−") +
                        dashboardINR(transaction.amount),
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
