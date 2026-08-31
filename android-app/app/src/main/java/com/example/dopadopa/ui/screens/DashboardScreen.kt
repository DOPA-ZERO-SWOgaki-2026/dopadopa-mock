package com.example.dopadopa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dopadopa.data.Formatters
import com.example.dopadopa.data.ScreenOffEvent
import com.example.dopadopa.state.AppState
import com.example.dopadopa.state.UsageSlice
import com.example.dopadopa.tracker.ScreenOffTracker
import com.example.dopadopa.ui.components.DonutChart
import com.example.dopadopa.ui.components.ProgressRing
import com.example.dopadopa.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.JAPAN)
private val dateFormatter = SimpleDateFormat("M/d(E)", Locale.JAPAN)

/** index.html の #appContainer 相当のメイン画面。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(appState: AppState) {
    val uiState by appState.uiState.collectAsState()
    val trackerState by ScreenOffTracker.state.collectAsState()

    var isGoalSheetVisible by remember { mutableStateOf(false) }
    var isShowingMoreEvents by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val collapsedEventCount = 3
    val maxExpandedEvents = 8

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            DashboardHeader(
                username = uiState.username,
                onGoalClick = { isGoalSheetVisible = true },
                onLogout = { appState.logout() },
            )
            HeroCard(uiState = uiState)
            UsageBreakdownCard(slices = uiState.usageBreakdown)
            StatsGrid(uiState = uiState, events = trackerState.events)
            ActivityCard(
                events = trackerState.events,
                isScreenOff = trackerState.isScreenOff,
                isShowingMoreEvents = isShowingMoreEvents,
                onToggleShowMore = { isShowingMoreEvents = !isShowingMoreEvents },
                collapsedEventCount = collapsedEventCount,
                maxExpandedEvents = maxExpandedEvents,
            )
            RewardsCard()
        }
    }

    if (isGoalSheetVisible) {
        fun closeSheet() {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) isGoalSheetVisible = false
            }
        }

        GoalSheetSheet(
            currentGoalMinutes = uiState.goalMinutes,
            sheetState = sheetState,
            onDismiss = { closeSheet() },
            onSave = { minutes ->
                appState.updateGoalMinutes(minutes)
                closeSheet()
            },
        )
    }
}

@Composable
private fun DashboardHeader(username: String?, onGoalClick: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Theme.ringGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column {
                Text("DopaDopa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Theme.ink)
                Text(
                    text = username?.let { "$it さんの記録" } ?: "スマホを見ない時間が、今日のご褒美に",
                    fontSize = 12.sp,
                    color = Theme.subtleInk,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "目標時間設定",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.primary,
                modifier = Modifier.clickable(onClick = onGoalClick),
            )
            Text(
                text = "ログアウト",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.subtleInk,
                modifier = Modifier.clickable(onClick = onLogout),
            )
        }
    }
}

@Composable
private fun CardContainer(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.cardBackground, RoundedCornerShape(Theme.cardCorner))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun HeroCard(uiState: AppState.UiState) {
    CardContainer(modifier = Modifier.padding(4.dp)) {
        Text("今日の達成度", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Theme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "スマホフリー時間で\n点数が貯まる",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "1分スマホを見ない（画面ロックしている）ごとにポイントが増える。\n" +
                "今日の休憩時間を、気持ちよく自分に還元しよう。",
            fontSize = 12.sp,
            color = Theme.subtleInk,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ProgressRing(
                ratio = uiState.goalRatio,
                centerLabel = "累計",
                centerValue = Formatters.points(uiState.totalPoints),
                modifier = Modifier.size(150.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("スクリーンタイム", Formatters.duration(uiState.totalScreenOnSeconds))
                MiniStat("デジタルデトックスタイム", Formatters.duration(uiState.digitalDetoxSeconds))
                MiniStat("目標達成率", "${Math.round(uiState.goalRatio * 100)}%")
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Theme.subtleInk)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
    }
}

@Composable
private fun UsageBreakdownCard(slices: List<UsageSlice>) {
    val total = slices.sumOf { it.seconds }
    CardContainer {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("使用時間の内訳", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Theme.ink)
            Text("推定を含む", fontSize = 10.sp, color = Theme.subtleInk)
        }
        Spacer(Modifier.height(12.dp))

        if (total <= 0.0) {
            Text(
                text = "記録がまだありません",
                fontSize = 13.sp,
                color = Theme.subtleInk,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                DonutChart(slices = slices, modifier = Modifier.size(130.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    slices.forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(slice.color, CircleShape))
                            Text(slice.label, fontSize = 12.sp, color = Theme.subtleInk, modifier = Modifier.width(96.dp))
                            Text(
                                Formatters.duration(slice.seconds),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Theme.ink,
                            )
                            Text(
                                " (${Math.round(slice.seconds / total * 100)}%)",
                                fontSize = 10.sp,
                                color = Theme.subtleInk,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "※ アプリ単位の内訳（Instagram○分、など）には、通知アクセスや利用状況アクセスなど" +
                "OS側の特別な権限を許可してもらう必要があります。",
            fontSize = 10.sp,
            color = Theme.subtleInk,
        )
    }
}

@Composable
private fun StatsGrid(uiState: AppState.UiState, events: List<ScreenOffEvent>) {
    val longestEvent = events.maxByOrNull { it.durationSeconds }
    val bestRecordText = Formatters.hoursAndMinutes(longestEvent?.durationSeconds ?: uiState.digitalDetoxSeconds)
    val bestRecordTrend = longestEvent?.let { dateFormatter.format(java.util.Date(it.offAtMillis)) } ?: "記録なし"
    val dailyProgressText = if (uiState.isGoalAchieved) {
        "目標達成!"
    } else {
        val remainingMinutes = Math.ceil(uiState.remainingSeconds / 60.0).toInt()
        "あと ${remainingMinutes}分"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "今週の獲得ポイント",
                value = Formatters.points(uiState.weeklyPoints),
                trend = "今週",
                trendColor = Theme.accent,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = "最高記録",
                value = bestRecordText,
                trend = bestRecordTrend,
                trendColor = Theme.subtleInk,
                modifier = Modifier.weight(1f),
            )
        }
        StatCard(
            title = "今日の目標",
            value = "${uiState.goalMinutes}分",
            trend = dailyProgressText,
            trendColor = if (uiState.isGoalAchieved) Theme.accent else Theme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, trend: String, trendColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Theme.cardBackground, RoundedCornerShape(Theme.cardCorner))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontSize = 12.sp, color = Theme.subtleInk)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
        Text(trend, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = trendColor)
    }
}

/**
 * 各記録（画面ロック区間）に何ポイント割り当てるかを、時系列順の累積秒数から算出する。
 * 区間ごとに秒数を個別に切り捨てるのではなく、累積の切り捨て差分を使うことで、
 * 一覧に表示される +○P をすべて合計すると、必ずリング中央の累計ポイントと一致するようにしている。
 */
private fun eventPoints(events: List<ScreenOffEvent>): Map<String, Int> {
    val chronological = events.sortedBy { it.offAtMillis }
    var cumulativeSeconds = 0.0
    var previousPoints = 0
    val mapping = mutableMapOf<String, Int>()
    for (event in chronological) {
        cumulativeSeconds += event.durationSeconds
        val currentPoints = AppState.pointsFor(cumulativeSeconds)
        mapping[event.id] = currentPoints - previousPoints
        previousPoints = currentPoints
    }
    return mapping
}

@Composable
private fun ActivityCard(
    events: List<ScreenOffEvent>,
    isScreenOff: Boolean,
    isShowingMoreEvents: Boolean,
    onToggleShowMore: () -> Unit,
    collapsedEventCount: Int,
    maxExpandedEvents: Int,
) {
    val pointsByEvent = remember(events) { eventPoints(events) }
    val visibleCount = if (isShowingMoreEvents) maxExpandedEvents else collapsedEventCount
    val visibleEvents = events.take(visibleCount)

    CardContainer {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("今日の記録", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Theme.ink)
            Box(
                modifier = Modifier
                    .background(
                        (if (isScreenOff) Theme.primary else Theme.accent).copy(alpha = 0.15f),
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = if (isScreenOff) "画面オフ中" else "継続中",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isScreenOff) Theme.primary else Theme.accent,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (events.isEmpty()) {
            Text(
                text = "まだ記録がありません。\n画面をロックして解除すると、ここに記録されます。",
                fontSize = 13.sp,
                color = Theme.subtleInk,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        } else {
            visibleEvents.forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${timeFormatter.format(java.util.Date(event.offAtMillis))}〜" +
                            timeFormatter.format(java.util.Date(event.onAtMillis)),
                        fontSize = 12.sp,
                        color = Theme.subtleInk,
                        modifier = Modifier.width(96.dp),
                    )
                    Text(
                        text = "画面オフ ${Formatters.duration(event.durationSeconds)}",
                        fontSize = 13.sp,
                        color = Theme.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "+${pointsByEvent[event.id] ?: 0}P",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.accent,
                    )
                }
            }

            if (events.size > collapsedEventCount) {
                Text(
                    text = if (isShowingMoreEvents) "閉じる" else "もっと見る",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable(onClick = onToggleShowMore),
                )
            }
        }
    }
}

@Composable
private fun RewardsCard() {
    CardContainer {
        Text("獲得できる特典", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Theme.ink)
        Spacer(Modifier.height(4.dp))
        RewardRow("☕", "カフェ無料ドリンク", "1,200P")
        RewardRow("🎧", "シャープペン", "2,500P")
        RewardRow("📚", "図書券", "4,800P")
    }
}

@Composable
private fun RewardRow(emoji: String, title: String, points: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 22.sp)
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Theme.ink)
            Text(points, fontSize = 12.sp, color = Theme.subtleInk)
        }
    }
}
