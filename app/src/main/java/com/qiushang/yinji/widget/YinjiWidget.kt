package com.qiushang.yinji.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.qiushang.yinji.MainActivity
import com.qiushang.yinji.data.AppGraph
import com.qiushang.yinji.data.WidgetSnapshot
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 桌面小卡片：习惯 + 待办同卡，点一下即写库完成并从卡片上消失。
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

private val BgColor = Color(0xFFF7F9F8)
private val InkColor = Color(0xFF1B2B2B)
private val MutedColor = Color(0xFF7A8A87)
private val AccentColor = Color(0xFF2E9C8E)

private fun titleStyle() = TextStyle(
    color = ColorProvider(InkColor),
    fontSize = 15.sp,
    fontWeight = FontWeight.Bold
)

private fun sectionStyle() = TextStyle(
    color = ColorProvider(AccentColor),
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold
)

private fun subStyle() = TextStyle(
    color = ColorProvider(MutedColor),
    fontSize = 11.sp
)

@androidx.compose.runtime.Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val openApp = actionStartActivity(MainActivity::class.java)
    val maxHabits = 3
    val maxTodos = 2
    val habits = snapshot.habits.take(maxHabits)
    val todos = snapshot.todos.take(maxTodos)
    val hidden = (snapshot.habits.size - habits.size) + (snapshot.todos.size - todos.size)
    val empty = habits.isEmpty() && todos.isEmpty()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(openApp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("印记 · 今天", style = titleStyle(), modifier = GlanceModifier.defaultWeight())
            Text(Dates.monthLabel(Dates.today()), style = subStyle())
        }
        Spacer(GlanceModifier.height(8.dp))

        if (empty) {
            Text("今天都完成啦 🎉", style = subStyle())
        } else {
            if (habits.isNotEmpty()) {
                Text("今日习惯", style = sectionStyle())
                Spacer(GlanceModifier.height(4.dp))
                habits.forEach { item ->
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<ToggleWidgetItemAction>(
                            actionParametersOf(
                                WIDGET_ITEM_ID to item.id,
                                WIDGET_ITEM_KIND to "habit"
                            )
                        ),
                        text = item.label,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
            if (todos.isNotEmpty()) {
                Spacer(GlanceModifier.height(6.dp))
                Text("待办", style = sectionStyle())
                Spacer(GlanceModifier.height(4.dp))
                todos.forEach { item ->
                    CheckBox(
                        checked = false,
                        onCheckedChange = actionRunCallback<ToggleWidgetItemAction>(
                            actionParametersOf(
                                WIDGET_ITEM_ID to item.id,
                                WIDGET_ITEM_KIND to "todo"
                            )
                        ),
                        text = item.label,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(6.dp))
        Text(
            if (hidden > 0) "还有 $hidden 项 · 点空白处打开 App" else "点空白处打开 App",
            style = subStyle()
        )
    }
}
