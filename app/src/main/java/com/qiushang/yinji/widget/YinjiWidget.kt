package com.qiushang.yinji.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.qiushang.yinji.MainActivity
import com.qiushang.yinji.R
import com.qiushang.yinji.data.AppGraph
import com.qiushang.yinji.data.WidgetItem
import com.qiushang.yinji.data.WidgetSnapshot
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 桌面小卡片（v1.5「奶油薄荷 · 小芽生长」视觉，与豆包概念图 ui-concept-widget.png 一致）：
 * 奶白软糖卡 + 四色手账胶带 + 笑脸小芽 + 空心圆勾选框 + 「还有 N 项」。
 * 习惯 + 待办同卡，点一下圆圈即写库完成并从卡片上消失。
 *
 * 实现照搬 Android 官方 Glance 样例的经验（android/socialite、home-assistant/android）：
 *  - 数据在 provideGlance 里一次性从 Room 取快照，卡片是 stateless & passive；
 *  - 点击走 ActionCallback（suspend，可直接调 Room suspend DAO），不自己写 goAsync、
 *    不在 onAction 里 launch 后立刻返回（写入会丢）；
 *  - 写后必须显式 `YinjiWidget().update(context, glanceId)`，不能指望 Flow 自动重组；
 *  - App 内改数据统一由 YinjiRepository.refreshWidget() 调 updateAll 收口。
 *
 * 「勾掉即消失」靠查询/过滤本身消失（Repository.widgetSnapshot 只返回未完成项），
 * 不是内存 state 假装隐藏 —— 否则系统重建 provideGlance 时完成项会「复活」。
 *
 * Glance 1.1.1 没有 border / shadow / Canvas：奶油细描边用「外层描边色 Box + 内层
 * 奶白 Column（padding 2dp）」双层模拟；胶带、小芽、空心圈用 vector drawable + background。
 */
private val WIDGET_ITEM_ID = ActionParameters.Key<Long>("itemId")
private val WIDGET_ITEM_KIND = ActionParameters.Key<String>("kind")

class YinjiWidget : GlanceAppWidget() {

    /** 可缩放卡片：给几档尺寸让系统挑最接近的，官方称缩放更顺、性能更好。 */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 140.dp),
            DpSize(300.dp, 200.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AppGraph.repository(context).widgetSnapshot()
        provideContent {
            WidgetContent(snapshot)
        }
    }
}

/** 点一下写库完成；实现类必须 public + 无参构造（Glance 框架反射实例化）。 */
class ToggleWidgetItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val id = parameters.getOrDefault(WIDGET_ITEM_ID, -1L)
        val kind = parameters.getOrDefault(WIDGET_ITEM_KIND, "")
        if (id < 0) return
        val repo = AppGraph.repository(context)
        when (kind) {
            "habit" -> repo.setCompletion(id, Dates.todayStr(), true)
            "todo" -> repo.setTodoDone(id, true)
        }
        // 官方：action 执行完要新建实例并 update；Repository 里的 updateAll 是 App 内改动的兜底。
        YinjiWidget().update(context, glanceId)
    }
}

class YinjiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = YinjiWidget()

    /**
     * 每日翻篇：系统跨日/改时间时刷新卡片，让"今天"按 LocalDate.now() 重算。
     * APPWIDGET_UPDATE 等由 super 处理，这里只补日期类广播。
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in setOf(
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
            )
        ) {
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    runCatching { YinjiWidget().updateAll(context.applicationContext) }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

// ---- 奶油薄荷视觉令牌（与 ui/theme/Theme.kt 同款） ----
private val PageColor = Color(0xFFFBF8F1)
private val BorderColor = Color(0xFFEADFC6)
private val InkColor = Color(0xFF33564B)
private val MutedColor = Color(0xFF5E7C70)
private val Primary = Color(0xFF3A9A80)
private val PrimaryDark = Color(0xFF2A6F5B)
private val PrimarySoft = Color(0xFFE6F5EF)
private val MintLight = Color(0xFFB7E7D6)
private val Cream = Color(0xFFF7D98C)
private val Peach = Color(0xFFF6B8AE)

private fun titleStyle() = TextStyle(
    color = ColorProvider(InkColor),
    fontSize = 15.sp,
    fontWeight = FontWeight.Bold
)

private fun sectionStyle() = TextStyle(
    color = ColorProvider(PrimaryDark),
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold
)

private fun subStyle() = TextStyle(
    color = ColorProvider(MutedColor),
    fontSize = 11.sp
)

private fun rowStyle() = TextStyle(
    color = ColorProvider(InkColor),
    fontSize = 13.sp
)

/** 顶部四色手账胶带：薄荷 / 奶油黄 / 蜜桃粉 / 浅薄荷。 */
@Composable
private fun TapeStrip() {
    Row(modifier = GlanceModifier.fillMaxWidth().height(6.dp)) {
        Box(modifier = GlanceModifier.defaultWeight().height(6.dp).background(Primary)) {}
        Box(modifier = GlanceModifier.defaultWeight().height(6.dp).background(Cream)) {}
        Box(modifier = GlanceModifier.defaultWeight().height(6.dp).background(Peach)) {}
        Box(modifier = GlanceModifier.defaultWeight().height(6.dp).background(MintLight)) {}
    }
}

/** 分节标签：彩色小圆点 + 文字（对应 App 内 SectionLabel）。 */
@Composable
private fun SectionDot(text: String, dot: Color) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = GlanceModifier.size(7.dp).cornerRadius(4.dp).background(dot)) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(text, style = sectionStyle())
    }
}

/** 一条习惯/待办：空心圆（点一下即完成）+ 文字。 */
@Composable
private fun ItemRow(item: WidgetItem, kind: String) {
    val toggle: Action = actionRunCallback<ToggleWidgetItemAction>(
        actionParametersOf(
            WIDGET_ITEM_ID to item.id,
            WIDGET_ITEM_KIND to kind
        )
    )
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_circle),
            contentDescription = "完成 ${item.label}",
            modifier = GlanceModifier.size(20.dp).clickable(toggle)
        )
        Spacer(GlanceModifier.width(9.dp))
        Text(item.label, style = rowStyle(), modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val openApp = actionStartActivity(MainActivity::class.java)
    val maxHabits = 3
    val maxTodos = 2
    val habits = snapshot.habits.take(maxHabits)
    val todos = snapshot.todos.take(maxTodos)
    val empty = habits.isEmpty() && todos.isEmpty()
    val today = Dates.today()

    // 外层描边色 Box + 内层奶白 Column，模拟 2dp 奶油细描边与 28dp 软糖圆角。
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(BorderColor)
            .clickable(openApp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp)
                .cornerRadius(26.dp)
                .background(PageColor)
        ) {
            TapeStrip()
            Spacer(GlanceModifier.height(6.dp))

            // 标题行：印记 · 今天 / 日期 / 笑脸小芽
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("印记 · 今天", style = titleStyle())
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        "${today.monthValue} 月 ${today.dayOfMonth} 日 周${Dates.weekdayLabel(today)}",
                        style = subStyle()
                    )
                }
                Image(
                    provider = ImageProvider(R.drawable.widget_cat),
                    contentDescription = "小猫同学",
                    modifier = GlanceModifier.size(34.dp)
                )
            }
            Spacer(GlanceModifier.height(10.dp))

            if (empty) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.widget_cat),
                        contentDescription = "小猫同学",
                        modifier = GlanceModifier.size(28.dp)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text("今天都完成啦，好棒！", style = rowStyle())
                }
            } else {
                if (habits.isNotEmpty()) {
                    SectionDot("今日习惯", Peach)
                    Spacer(GlanceModifier.height(3.dp))
                    habits.forEach { ItemRow(it, "habit") }
                }
                if (todos.isNotEmpty()) {
                    Spacer(GlanceModifier.height(6.dp))
                    SectionDot("待办", Cream)
                    Spacer(GlanceModifier.height(3.dp))
                    todos.forEach { ItemRow(it, "todo") }
                }
            }
            // 卡片按概念图干净收尾：点空白打开 App 由根 Box 的 clickable(openApp)
            // 承担；「还有 N 项」在原版是末尾独立一行，但 Glance 的 fillMaxSize
            // Column 末尾子项会被 RemoteViews 裁掉，故不再渲染这行装饰文字。
        }
    }
}
