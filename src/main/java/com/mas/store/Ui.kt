package com.mas.store

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset as GOffset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.toIntSize
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LocalAr = compositionLocalOf { false }
val LocalDark = compositionLocalOf { true }
val LocalTick = compositionLocalOf { 0 }

@Composable fun accent(): Color = if (LocalDark.current) Color(0xFFF2F2F3) else Color(0xFF2A2B2E)
@Composable fun onAccent(): Color = if (LocalDark.current) Color(0xFF1E1F22) else Color.White

@Composable
fun tr(en: String, ar: String): String = if (LocalAr.current) ar else en

fun sizeText(bytes: Long, ar: Boolean): String {
    if (bytes <= 0) return "-"
    val mb = bytes / 1048576.0
    return String.format(Locale.ROOT, "%.1f", mb) + if (ar) " م.ب" else " MB"
}

@Composable fun bgColor(): Color = if (LocalDark.current) Color(0xFF1E1F22) else Color(0xFFF5F7FA)
@Composable fun textColor(): Color = if (LocalDark.current) Color(0xFFF4F4F5) else Color(0xFF17181A)
@Composable fun subColor(): Color = if (LocalDark.current) Color.White.copy(alpha = 0.58f) else Color(0xFF17181A).copy(alpha = 0.55f)
@Composable fun hairline(): Color = if (LocalDark.current) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)

/** Press feedback: springy scale-down, no ripple. */
fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        if (pressed) 0.965f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this.graphicsLayer { scaleX = s; scaleY = s }.clickable(interactionSource = src, indication = null, onClick = onClick)
}

object RevealClock {
    @Volatile var t = System.currentTimeMillis()
}

/** Fade + rise entrance, staggered by index. Only plays right after a tab change or app start, never while scrolling. */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    val fresh = System.currentTimeMillis() - RevealClock.t < 1200
    val a = remember { Animatable(if (fresh) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (a.value < 1f) {
            delay((index.coerceAtMost(6) * 45).toLong())
            a.animateTo(1f, tween(380))
        }
    }
    Box(Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 28f }) { content() }
}

/** Kept for API compatibility. Recording the whole screen every frame was a main cause of lag, so it is a no-op now. */
fun Modifier.recordTo(layer: GraphicsLayer): Modifier = this

/** Real backdrop blur (Android 12+): redraws the recorded content behind this surface through a blur RenderEffect. */
@Composable
fun Modifier.liveGlass(source: GraphicsLayer, shape: Shape, blur: Float = 40f, tintAlpha: Float = 0.5f): Modifier {
    val dark = LocalDark.current
    val blurLayer = rememberGraphicsLayer()
    var pos by remember { mutableStateOf(GOffset.Zero) }
    val tint = if (dark) Color(0xFF2B2C30).copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha)
    val edge = if (dark) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.06f)))
        else Brush.verticalGradient(listOf(Color.White, Color(0xFFD5DAE1).copy(alpha = 0.8f)))
    val t2 = if (dark) Color(0xFF2B2C30).copy(alpha = tintAlpha.coerceAtLeast(0.86f)) else Color.White.copy(alpha = tintAlpha.coerceAtLeast(0.88f))
    return this.clip(shape)
        .drawBehind { drawRect(t2) }
        .border(BorderStroke(0.8.dp, edge), shape)
}

@Composable
fun Modifier.glass(radius: Int = 20, strong: Boolean = false): Modifier {
    val dark = LocalDark.current
    val shape = RoundedCornerShape(radius.dp)
    val fill = if (dark) Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (strong) 0.13f else 0.09f), Color.White.copy(alpha = 0.04f))
    ) else Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (strong) 0.95f else 0.80f), Color.White.copy(alpha = 0.55f))
    )
    val stroke = Brush.verticalGradient(
        if (dark) listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
        else listOf(Color.White, Color(0xFFDDE1E7).copy(alpha = 0.7f))
    )
    return this.clip(shape).background(fill).border(BorderStroke(0.8.dp, stroke), shape)
}

@Composable
fun StoreApp(tickState: MutableIntState, startTab: String?, openId: String?, getFlag: String?, demo: String? = null) {
    val ctx = LocalContext.current
    val ar = LocalConfiguration.current.locales[0].language == "ar"
    val prefs = remember { ctx.getSharedPreferences("mas", android.content.Context.MODE_PRIVATE) }
    var theme by remember { mutableIntStateOf(prefs.getInt("theme", 0)) }
    val sysDark = isSystemInDarkTheme()
    val dark = when (theme) { 1 -> false; 2 -> true; else -> sysDark }
    var onboarded by remember {
        mutableStateOf(
            prefs.getBoolean("onboarded", false) ||
                (androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled() && ctx.packageManager.canRequestPackageInstalls())
        )
    }
    androidx.compose.runtime.SideEffect {
        (ctx as? android.app.Activity)?.window?.let { w ->
            val c = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    var apps by remember { mutableStateOf(CatalogRepo.bundled(ctx)) }
    var tab by remember {
        mutableIntStateOf(when (startTab) { "games" -> 1; "apps" -> 2; "search" -> 3; "settings" -> 4; else -> 0 })
    }
    var selected by remember { mutableStateOf<AppItem?>(null) }
    var viewer by remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    var handled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { CatalogRepo.cached(ctx) }
        if (cached != null && cached.isNotEmpty()) apps = cached
        val remote = withContext(Dispatchers.IO) { CatalogRepo.fetchRemote(ctx) }
        if (remote != null) apps = remote
        if (demo == "update" || demo == "force") {
            Meta.store = StoreInfo(
                "1.2.0", 999, "", 0, "", demo == "force", 0,
                L("A new look, faster downloads and a Settings page.", "شكل جديد، تحميل أسرع، وصفحة إعدادات.")
            )
        }
    }
    val scope = rememberCoroutineScope()
    val ownVc = remember(tickState.intValue) {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode } catch (e: Exception) { 0L }
    }
    val su = Meta.store
    val needUpdate = su != null && su.versionCode > ownVc
    val forced = needUpdate && su != null && (su.force || ownVc < su.minVersionCode)
    var later by remember { mutableStateOf(false) }
    BackHandler(enabled = forced) { }
    LaunchedEffect(apps) {
        if (!handled && openId != null) {
            val a = apps.firstOrNull { it.id == openId }
            if (a != null) {
                handled = true
                selected = a
                if (demo == "viewer" && a.screenshots.isNotEmpty()) viewer = a.screenshots to 0
                if (getFlag == "1") Downloads.start(ctx, a)
            }
        }
    }
    BackHandler(enabled = selected != null && !forced) { selected = null }

    CompositionLocalProvider(LocalAr provides ar, LocalDark provides dark, LocalTick provides tickState.intValue) {
        val srcLayer = rememberGraphicsLayer()
        Box(Modifier.fillMaxSize().background(bgColor())) {
          Box(Modifier.fillMaxSize().recordTo(srcLayer).background(bgColor())) {
            Ambient()
            val visited = remember { mutableSetOf(0) }
            visited.add(tab)
            for (t in 0..4) if (t in visited) key(t) {
                val active = tab == t
                val al by androidx.compose.animation.core.animateFloatAsState(
                    if (active) 1f else 0f,
                    if (active) tween(160) else androidx.compose.animation.core.snap(), label = "tabalpha")
                Box(
                    Modifier.fillMaxSize()
                        .graphicsLayer { alpha = al }
                        .layout { m, c ->
                            val p = m.measure(c)
                            layout(p.width, p.height) { if (active) p.place(0, 0) else p.place(100000, 0) }
                        }
                ) {
                    when (t) {
                        0 -> TodayScreen(apps, { selected = it }, { tab = 4; RevealClock.t = System.currentTimeMillis() })
                        1 -> ListScreen(tr("Games", "الألعاب"), apps.filter { it.category == "game" }, tr("Games coming soon", "الألعاب قريبًا")) { selected = it }
                        2 -> ListScreen(tr("Apps", "التطبيقات"), apps.filter { it.category != "game" }, tr("Apps coming soon", "التطبيقات قريبًا")) { selected = it }
                        3 -> SearchScreen(apps) { selected = it }
                        else -> SettingsScreen(apps, su, needUpdate, theme, { theme = it; prefs.edit().putInt("theme", it).apply() }, {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) { CatalogRepo.fetchRemote(ctx) }
                                if (r != null) apps = r
                            }
                        }) { selected = it }
                    }
                }
            }
          }
            Box(Modifier.align(Alignment.BottomCenter)) { Dock(tab, srcLayer) { tab = it; RevealClock.t = System.currentTimeMillis() } }
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 8 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 10 },
            ) {
                val a = selected
                if (a != null) DetailScreen(a, onBack = { selected = null }, onShot = { l, i -> viewer = l to i }) else Box(Modifier.fillMaxSize())
            }
            val vw = viewer
            if (vw != null) ImageViewer(vw.first, vw.second) { viewer = null }
            if (demo == "onboard" || (!onboarded && !(needUpdate && forced))) Onboarding { prefs.edit().putBoolean("onboarded", true).apply(); onboarded = true; if (demo == "onboard") later = true }
            if (needUpdate && su != null && (forced || !later) && (onboarded || forced) && demo != "onboard") UpdateSheet(su, forced) { later = true }
        }
    }
}

@Composable
fun Ambient() {
    val dark = LocalDark.current
    Canvas(Modifier.fillMaxSize()) {
        val a = if (dark) 0.16f else 0.65f
        fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
            Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), Offset(size.width * x, size.height * y), size.width * r),
            radius = size.width * r, center = Offset(size.width * x, size.height * y)
        )
        blob(Color.White, 0.05f, 0.0f, 0.85f)
        blob(if (dark) Color(0xFF9AA0A8) else Color(0xFFCBD2DB), 1.0f, 0.5f, 0.7f)
        blob(if (dark) Color.White else Color(0xFFE3E7EE), 0.0f, 1.0f, 0.6f)
    }
}

@Composable
fun AppIcon(app: AppItem, size: Int, radius: Int = (size * 0.225f).toInt()) {
    val shape = RoundedCornerShape(radius.dp)
    Box(
        Modifier.size(size.dp).clip(shape).background(Color.Gray.copy(alpha = 0.25f))
            .border(BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f)), shape)
    ) {
        AsyncImage(model = app.icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun Logo(size: Int) {
    androidx.compose.foundation.Image(painterResource(R.drawable.mas_logo), "MAS STORE", Modifier.size(size.dp))
}

@Composable
fun GetButton(app: AppItem, modifier: Modifier = Modifier, large: Boolean = false) {
    val ctx = LocalContext.current
    val tick = LocalTick.current
    val installed = remember(tick, app.pkg) { Downloads.installedVersion(ctx, app.pkg) }
    val dl = Downloads.states[app.id]
    val upToDate = installed != null && installed >= app.versionCode
    var label: String
    var target = 0f
    if (dl is Dl.Prog) {
        target = if (dl.total > 0) (dl.done.toFloat() / dl.total).coerceIn(0f, 1f) else 0f
        label = (target * 100).toInt().toString() + "%"
    } else if (upToDate) label = tr("OPEN", "فتح")
    else if (dl is Dl.Ready) label = tr("INSTALL", "تثبيت")
    else if (dl is Dl.Err) label = tr("RETRY", "إعادة")
    else if (installed != null) label = tr("UPDATE", "تحديث")
    else label = tr("GET", "تحميل")
    val frac by animateFloatAsState(target, tween(250), label = "dl")
    val dark = LocalDark.current
    val shape = RoundedCornerShape(50)
    val base = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.07f)
    val h = if (large) 40 else 30
    val w = if (large) 128 else 76
    if (dl is Dl.Prog) {
        Box(modifier.height(h.dp).width(w.dp), contentAlignment = Alignment.Center) {
            ProgressRing(frac, if (large) 40 else 32)
        }
        return
    }
    Box(
        modifier.height(h.dp).width(w.dp).clip(shape).background(base).pressScale {
            when {
                upToDate -> Downloads.open(ctx, app.pkg)
                dl is Dl.Ready && java.io.File(dl.path).exists() -> Downloads.install(ctx, dl.path)
                else -> Downloads.start(ctx, app)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, color = accent(),
            fontWeight = FontWeight.Bold, fontSize = if (large) 15.sp else 13.sp, maxLines = 1
        )
    }
}

@Composable
fun ProgressRing(frac: Float, size: Int) {
    val dark = LocalDark.current
    val ring = accent()
    val track = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
    val fill = if (dark) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)))
        else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.6f)))
    val stroke = if (dark) Color.White.copy(alpha = 0.22f) else Color(0xFFDDE1E7)
    Box(
        Modifier.size(size.dp).clip(androidx.compose.foundation.shape.CircleShape).background(fill)
            .border(BorderStroke(0.8.dp, stroke), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding((size / 6).dp)) {
            val sw = 2.6.dp.toPx()
            drawArc(track, 0f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
            drawArc(ring, -90f, 360f * frac.coerceIn(0.02f, 1f), false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Text((frac * 100).toInt().toString(), color = textColor(), fontSize = (size / 4).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Dock(tab: Int, src: GraphicsLayer, onTab: (Int) -> Unit) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.Today to tr("Today", "اليوم"),
        Icons.Filled.SportsEsports to tr("Games", "الألعاب"),
        Icons.Filled.Apps to tr("Apps", "التطبيقات"),
        Icons.Filled.Search to tr("Search", "بحث"),
        Icons.Filled.Settings to tr("Settings", "الإعدادات"),
    )
    val dark = LocalDark.current
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(62.dp)
            .liveGlass(src, shape, blur = 45f, tintAlpha = 0.42f)
            .padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = tab == i
            val c by animateColorAsState(if (on) accent() else subColor(), tween(200), label = "dockc")
            val pill by animateColorAsState(if (on) accent().copy(alpha = 0.14f) else Color.Transparent, tween(200), label = "dockp")
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(25.dp)).background(pill).pressScale { onTab(i) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = label, tint = c, modifier = Modifier.size(22.dp))
                Text(label, color = c, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun Header(title: String, kicker: String? = null, onLogo: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Text(kicker, color = subColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            Text(title, color = textColor(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }
        if (onLogo != null) Box(Modifier.pressScale(onLogo)) { Logo(40) } else Logo(40)
    }
}

@Composable
fun SectionTitle(t: String, sub: String? = null) {
    Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 10.dp)) {
        Text(t, color = textColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        if (sub != null) Text(sub, color = subColor(), fontSize = 12.5.sp)
    }
}

@Composable
fun TodayScreen(apps: List<AppItem>, onOpen: (AppItem) -> Unit, onSettings: () -> Unit = {}) {
    val ctx = LocalContext.current
    val tick = LocalTick.current
    val ar = LocalAr.current
    val locale = if (ar) Locale("ar", "EG") else Locale.getDefault()
    val date = remember(ar) { SimpleDateFormat("EEEE d MMMM", locale).format(Date()) }
    val featured = apps.filter { it.featured }.ifEmpty { apps.take(1) }
    val others = apps.filter { a -> featured.none { it.id == a.id } }
    val updates = remember(tick, apps) {
        apps.filter { val v = Downloads.installedVersion(ctx, it.pkg); v != null && v < it.versionCode }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Header(tr("Today", "اليوم"), date, onSettings) }
        if (apps.isEmpty()) item { EmptyState(tr("Loading the store...", "جاري تحميل المتجر...")) }
        items(featured.size) { i ->
            Reveal(i) { Hero(featured[i]) { onOpen(featured[i]) } }
            Spacer(Modifier.height(14.dp))
        }
        if (updates.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(end = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { SectionTitle(tr("Updates available", "تحديثات متاحة")) }
                    SmallButton(tr("Update all", "تحديث الكل")) { Downloads.updateAll(ctx, updates) }
                }
            }
            items(updates.size) { i -> Reveal(i) { AppRow(updates[i]) { onOpen(updates[i]) } } }
        }
        if (others.isNotEmpty()) {
            item { SectionTitle(tr("Picks for you", "مختارات لك"), tr("Hand-picked by MAS", "اختيارات MAS")) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(others.size) { i -> PickCard(others[i]) { onOpen(others[i]) } }
                }
            }
        }
        if (apps.isNotEmpty()) {
            item { SectionTitle(tr("New & updated", "الجديد والمحدّث"), tr("Latest from MAS", "آخر أعمال MAS")) }
            items(apps.size) { i -> Reveal(i) { AppRow(apps[i]) { onOpen(apps[i]) } } }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Logo(28)
                Spacer(Modifier.height(6.dp))
                Text("MAS STORE", color = subColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
    }
}

@Composable
fun Hero(app: AppItem, onClick: () -> Unit) {
    val ar = LocalAr.current
        val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(330.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF38393D), Color(0xFF1A1B1E), Color(0xFF121315))))
            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)), shape)
            .pressScale(onClick)
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(app.color).copy(alpha = 0.60f), Color(app.color).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(540f, 420f), radius = 900f
                )
            )
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.55f)))))
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp)) {
            Text(tr("FEATURED", "مميز"), color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(4.dp))
            Text(app.name.get(ar), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, letterSpacing = (-0.4).sp)
            Text(app.subtitle.get(ar), color = Color.White.copy(alpha = 0.78f), fontSize = 14.sp, maxLines = 2)
        }
        Box(Modifier.align(Alignment.Center).padding(top = 38.dp)) { AppIcon(app, 112) }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.30f))
                .border(BorderStroke(0.6.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app, 40)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(app.name.get(ar), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sizeText(app.size, ar) + "  •  v" + app.version, color = Color.White.copy(alpha = 0.7f), fontSize = 11.5.sp, maxLines = 1)
            }
            androidx.compose.runtime.CompositionLocalProvider(LocalDark provides true) { GetButton(app) }
        }
    }
}

@Composable
fun PickCard(app: AppItem, onClick: () -> Unit) {
    val ar = LocalAr.current
    Column(
        Modifier.width(146.dp).glass(20).pressScale(onClick).padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        AppIcon(app, 66)
        Spacer(Modifier.height(10.dp))
        Text(app.name.get(ar), color = textColor(), fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(app.subtitle.get(ar), color = subColor(), fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.height(32.dp))
        Spacer(Modifier.height(8.dp))
        GetButton(app)
    }
}

@Composable
fun AppRow(app: AppItem, onClick: () -> Unit) {
    val ar = LocalAr.current
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 4.dp).fillMaxWidth().glass(18).pressScale(onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app, 54)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(app.name.get(ar), color = textColor(), fontWeight = FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.subtitle.get(ar), color = subColor(), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sizeText(app.size, ar) + "  •  v" + app.version, color = subColor().copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
        }
        GetButton(app)
    }
}

@Composable
fun EmptyState(text: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).glass(22).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Logo(46)
        Spacer(Modifier.height(10.dp))
        Text(text, color = subColor(), fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    val dark = LocalDark.current
    val bg by animateColorAsState(
        if (on) accent() else if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f), tween(180), label = "chip"
    )
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape).background(bg).border(BorderStroke(0.6.dp, hairline()), shape).pressScale(onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (on) onAccent() else textColor(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ListScreen(title: String, list: List<AppItem>, emptyText: String, onOpen: (AppItem) -> Unit) {
    val ctx = LocalContext.current
    val tick = LocalTick.current
    var chip by remember { mutableIntStateOf(0) }
    val shown = remember(chip, list, tick) {
        when (chip) {
            1 -> list.filter { val v = Downloads.installedVersion(ctx, it.pkg); v != null && v < it.versionCode }
            2 -> list.filter { Downloads.installedVersion(ctx, it.pkg) != null }
            else -> list
        }
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Header(title) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                item { Chip(tr("All", "الكل"), chip == 0) { chip = 0 } }
                item { Chip(tr("Updates", "تحديثات"), chip == 1) { chip = 1 } }
                item { Chip(tr("Installed", "مثبّتة"), chip == 2) { chip = 2 } }
            }
        }
        if (list.isEmpty()) item { EmptyState(emptyText) }
        else if (shown.isEmpty()) item { EmptyState(tr("Nothing here yet", "مفيش حاجة هنا لسه")) }
        items(shown.size) { i -> Reveal(i) { AppRow(shown[i]) { onOpen(shown[i]) } } }
    }
}

@Composable
fun SearchScreen(apps: List<AppItem>, onOpen: (AppItem) -> Unit) {
    val ar = LocalAr.current
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableIntStateOf(0) }
    val res = apps.filter {
        (cat == 0 || (cat == 1 && it.category != "game") || (cat == 2 && it.category == "game")) &&
            (q.isBlank() || it.name.en.contains(q, true) || it.name.ar.contains(q, true) ||
                it.subtitle.en.contains(q, true) || it.subtitle.ar.contains(q, true) || it.description.get(ar).contains(q, true))
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Header(tr("Search", "بحث")) }
        item {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 4.dp).fillMaxWidth().height(48.dp).glass(16, strong = true).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = subColor(), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text(tr("Apps and games", "تطبيقات وألعاب"), color = subColor(), fontSize = 16.sp)
                    BasicTextField(
                        value = q, onValueChange = { q = it }, singleLine = true,
                        textStyle = TextStyle(color = textColor(), fontSize = 16.sp), cursorBrush = SolidColor(accent()),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (q.isNotEmpty()) Icon(Icons.Filled.Close, null, tint = subColor(), modifier = Modifier.size(18.dp).pressScale { q = "" })
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                item { Chip(tr("All", "الكل"), cat == 0) { cat = 0 } }
                item { Chip(tr("Apps", "تطبيقات"), cat == 1) { cat = 1 } }
                item { Chip(tr("Games", "ألعاب"), cat == 2) { cat = 2 } }
            }
        }
        if (q.isBlank()) item { SectionTitle(tr("Suggested", "مقترحات")) }
        if (res.isEmpty()) item { EmptyState(tr("No results", "مفيش نتائج")) }
        items(res.size) { i -> Reveal(i) { AppRow(res[i]) { onOpen(res[i]) } } }
    }
}

@Composable
fun DetailScreen(app: AppItem, onBack: () -> Unit, onShot: (List<String>, Int) -> Unit = { _, _ -> }) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
        val scroll = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    val dLayer = rememberGraphicsLayer()
    Box(Modifier.fillMaxSize().background(bgColor())) {
      Box(Modifier.fillMaxSize().recordTo(dLayer).background(bgColor())) {
        Ambient()
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 40.dp)) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(70.dp))
            Reveal(0) {
                Row(Modifier.padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
                    AppIcon(app, 112)
                    Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
                        Text(app.name.get(ar), color = textColor(), fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, letterSpacing = (-0.3).sp)
                        Text(app.subtitle.get(ar), color = subColor(), fontSize = 13.5.sp, maxLines = 2)
                        Text("MAS", color = accent(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.height(10.dp))
                        GetButton(app, large = true)
                    }
                }
            }
            Reveal(1) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp).fillMaxWidth().glass(20).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
                ) {
                    Stat(tr("VERSION", "الإصدار"), app.version)
                    Box(Modifier.width(0.8.dp).height(28.dp).background(hairline()))
                    Stat(tr("SIZE", "الحجم"), sizeText(app.size, ar))
                    Box(Modifier.width(0.8.dp).height(28.dp).background(hairline()))
                    Stat(tr("CATEGORY", "القسم"), if (app.category == "game") tr("Game", "لعبة") else tr("App", "تطبيق"))
                }
            }
            if (app.screenshots.isNotEmpty()) {
                Reveal(2) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(app.screenshots) { u ->
                            val sh = RoundedCornerShape(18.dp)
                            Box(
                                Modifier.width(176.dp).height(376.dp).clip(sh).background(Color.White.copy(alpha = 0.06f))
                                    .border(BorderStroke(0.8.dp, hairline()), sh)
                                    .pressScale { onShot(app.screenshots, app.screenshots.indexOf(u).coerceAtLeast(0)) }
                            ) {
                                AsyncImage(model = u, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
            val wn = app.whatsNew.get(ar)
            if (wn.isNotBlank()) {
                SectionTitle(tr("What's New", "الجديد"), tr("Version ", "الإصدار ") + app.version)
                Text(wn, color = textColor().copy(alpha = 0.85f), fontSize = 14.5.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 22.dp))
            }
            SectionTitle(tr("About", "نبذة"))
            Text(
                app.description.get(ar), color = textColor().copy(alpha = 0.85f), fontSize = 14.5.sp, lineHeight = 22.sp,
                maxLines = if (expanded) 40 else 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 22.dp)
            )
            Text(
                if (expanded) tr("less", "أقل") else tr("more", "المزيد"), color = accent(), fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp).pressScale { expanded = !expanded }
            )
            SectionTitle(tr("Information", "معلومات"))
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(20).padding(horizontal = 16.dp)) {
                InfoRow(tr("Developer", "المطوّر"), "MAS")
                InfoRow(tr("Category", "القسم"), if (app.category == "game") tr("Game", "لعبة") else tr("App", "تطبيق"))
                InfoRow(tr("Size", "الحجم"), sizeText(app.size, ar))
                InfoRow(tr("Version", "الإصدار"), app.version)
                InfoRow(tr("Package", "الحزمة"), app.pkg, last = true)
            }
            val dl = Downloads.states[app.id]
            if (dl is Dl.Err) {
                Text(dl.msg, color = textColor(), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
            }
        }
      }
        val topPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            Modifier.fillMaxWidth().height(topPad + 62.dp)
                .graphicsLayer { alpha = (scroll.value / 160f).coerceIn(0f, 1f) }
                .liveGlass(dLayer, androidx.compose.ui.graphics.RectangleShape, blur = 45f, tintAlpha = 0.45f)
        )
        // top bar: always-visible back and share, title fades in on scroll
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).liveGlass(dLayer, RoundedCornerShape(20.dp), blur = 30f, tintAlpha = 0.45f).pressScale { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor(), modifier = Modifier.size(21.dp))
            }
            Text(
                app.name.get(ar), color = textColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).graphicsLayer { alpha = ((scroll.value - 120) / 120f).coerceIn(0f, 1f) }
            )
            Box(
                Modifier.size(40.dp).liveGlass(dLayer, RoundedCornerShape(20.dp), blur = 30f, tintAlpha = 0.45f).pressScale {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "text/plain"
                    i.putExtra(Intent.EXTRA_TEXT, app.name.get(ar) + "\n" + app.apk)
                    val ch = Intent.createChooser(i, null)
                    ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(ch)
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Share, null, tint = textColor(), modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
fun InfoRow(k: String, v: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(k, color = subColor(), fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(v, color = textColor(), fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(0.6.dp).background(hairline()))
}

@Composable
fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = subColor(), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = textColor(), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun SmallButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier.clip(shape).background(accent().copy(alpha = 0.14f)).border(BorderStroke(0.6.dp, hairline()), shape)
            .pressScale(onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = accent(), fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1)
    }
}

fun storeAsApp(ctx: android.content.Context, s: StoreInfo) = AppItem(
    id = "mas-store", pkg = ctx.packageName, name = L("MAS STORE", "MAS STORE"),
    subtitle = L("Store update", "تحديث المتجر"), category = "app", icon = "", color = 0xFF2A2B2EL,
    screenshots = emptyList(), apk = s.apk, version = s.version, versionCode = s.versionCode, size = s.size,
    sha256 = s.sha256, description = L("", ""), whatsNew = s.notes, featured = false,
)

@Composable
fun SettingRow(title: String, value: String, last: Boolean = false, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.pressScale(onClick) else Modifier
    Column(m.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = textColor(), fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, color = subColor(), fontSize = 14.sp, maxLines = 1)
        }
        if (!last) Box(Modifier.fillMaxWidth().height(0.6.dp).background(hairline()))
    }
}

@Composable
fun UpdateSheet(store: StoreInfo, forced: Boolean, onLater: () -> Unit) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    val app = remember(store) { storeAsApp(ctx, store) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!forced) onLater() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(26.dp).fillMaxWidth().sheetSurface()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(76)
            Spacer(Modifier.height(14.dp))
            Text(tr("Update available", "تحديث جديد متاح"), color = textColor(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text("MAS STORE  v" + store.version, color = subColor(), fontSize = 13.sp)
            val notes = store.notes.get(ar)
            if (notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(notes, color = textColor(), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            if (forced) {
                Spacer(Modifier.height(10.dp))
                Text(tr("This update is required to continue.", "التحديث ده إجباري علشان تكمل."), color = subColor(), fontSize = 12.5.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            GetButton(app, large = true)
            if (!forced) {
                Spacer(Modifier.height(10.dp))
                Box(Modifier.pressScale(onLater).padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text(tr("Later", "لاحقًا"), color = subColor(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(apps: List<AppItem>, store: StoreInfo?, needUpdate: Boolean, theme: Int, onTheme: (Int) -> Unit, onCheck: () -> Unit, onOpen: (AppItem) -> Unit) {
    val ctx = LocalContext.current
    val tick = LocalTick.current
    val pi = remember { ctx.packageManager.getPackageInfo(ctx.packageName, 0) }
    val updates = remember(tick, apps) {
        apps.filter { val v = Downloads.installedVersion(ctx, it.pkg); v != null && v < it.versionCode }
    }
    val installedN = remember(tick, apps) { apps.count { Downloads.installedVersion(ctx, it.pkg) != null } }
    val notifOn = remember(tick) { androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val canInstall = remember(tick) { ctx.packageManager.canRequestPackageInstalls() }
    val on = tr("On", "شغّال")
    val off = tr("Off", "مقفول")
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        item { Header(tr("Settings", "الإعدادات"), "MAS STORE") }
        item {
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(26).padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Logo(64)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text("MAS STORE", color = textColor(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tr("Version ", "الإصدار ") + pi.versionName + " (" + androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pi) + ")", color = subColor(), fontSize = 13.sp)
                        Text(
                            if (needUpdate && store != null) tr("Update available: v", "تحديث متاح: v") + store.version
                            else tr("You have the latest version", "معاك آخر إصدار"),
                            color = if (needUpdate) textColor() else subColor(), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (needUpdate && store != null) GetButton(storeAsApp(ctx, store), large = true)
                else SmallButton(tr("Check for updates", "تحقق من التحديثات"), onCheck)
            }
        }
        item { SectionTitle(tr("At a glance", "لمحة"), tr("Your store in numbers", "متجرك في أرقام")) }
        item {
            Row(
                Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(22).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Stat(tr("APPS", "تطبيقات"), apps.count { it.category != "game" }.toString())
                Stat(tr("GAMES", "ألعاب"), apps.count { it.category == "game" }.toString())
                Stat(tr("INSTALLED", "مثبّتة"), installedN.toString())
                Stat(tr("UPDATES", "تحديثات"), updates.size.toString())
            }
        }
        item { SectionTitle(tr("App updates", "تحديثات التطبيقات")) }
        item {
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(22).padding(16.dp)) {
                if (updates.isEmpty()) {
                    Text(tr("Everything is up to date.", "كل التطبيقات والألعاب محدّثة."), color = subColor(), fontSize = 14.sp)
                } else {
                    Text(
                        tr("${updates.size} updates are ready.", "فيه ${updates.size} تحديثات جاهزة."),
                        color = textColor(), fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    SmallButton(tr("Update apps & games", "تحديث التطبيقات والألعاب")) { Downloads.updateAll(ctx, updates) }
                }
            }
        }
        items(updates.size) { i -> AppRow(updates[i]) { onOpen(updates[i]) } }
        item { SectionTitle(tr("Appearance", "المظهر")) }
        item {
            Row(
                Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(22).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip(tr("System", "حسب الجهاز"), theme == 0) { onTheme(0) }
                Chip(tr("Light", "فاتح"), theme == 1) { onTheme(1) }
                Chip(tr("Dark", "داكن"), theme == 2) { onTheme(2) }
            }
        }
        item { SectionTitle(tr("Device", "الجهاز")) }
        item {
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(22).padding(horizontal = 16.dp)) {
                SettingRow(tr("Notifications", "الإشعارات"), if (notifOn) on else off) {
                    val i = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { ctx.startActivity(i) } catch (e: Exception) { }
                }
                SettingRow(tr("Install apps", "تثبيت التطبيقات"), if (canInstall) tr("Allowed", "مسموح") else tr("Tap to allow", "اضغط للسماح"), true) {
                    val i = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { ctx.startActivity(i) } catch (e: Exception) { }
                }
            }
        }
        item { SectionTitle(tr("About", "عن المتجر")) }
        item {
            Column(Modifier.padding(horizontal = 18.dp).fillMaxWidth().glass(22).padding(16.dp)) {
                Text(
                    tr(
                        "A private store for the apps and games made by MAS. New releases and updates reach you instantly.",
                        "متجر خاص بالتطبيقات والألعاب اللي بيعملها MAS. الجديد والتحديثات بتوصلك لحظيًا."
                    ),
                    color = subColor(), fontSize = 13.5.sp
                )
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MAS STORE", color = subColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
    }
}


@Composable
fun PermRow(title: String, sub: String, ok: Boolean, onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, color = textColor(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = subColor(), fontSize = 12.5.sp)
        }
        if (ok) Text("\u2713", color = accent(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        else SmallButton(tr("Allow", "سماح"), onAllow)
    }
}

@Composable
fun Onboarding(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val tick = LocalTick.current
    var notif by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val installOk = remember(tick) { ctx.packageManager.canRequestPackageInstalls() }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { notif = androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth().sheetSurface().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(76)
            Spacer(Modifier.height(12.dp))
            Text(tr("Welcome to MAS STORE", "أهلًا في MAS STORE"), color = textColor(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(tr("Two quick permissions so everything works.", "محتاجين إذنين سريعين علشان كل حاجة تشتغل."), color = subColor(), fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            PermRow(tr("Notifications", "الإشعارات"), tr("Hear about new apps and updates instantly", "اعرف بالجديد والتحديثات فورًا"), notif) {
                if (Build.VERSION.SDK_INT >= 33) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else {
                    val i = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { ctx.startActivity(i) } catch (e: Exception) { }
                }
            }
            Box(Modifier.fillMaxWidth().height(0.6.dp).background(hairline()))
            PermRow(tr("Install apps", "تثبيت التطبيقات"), tr("Let MAS STORE install what you download", "اسمح للمتجر يثبّت اللي بتحمّله"), installOk) {
                val i = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, android.net.Uri.parse("package:" + ctx.packageName))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(i) } catch (e: Exception) { }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(50)).background(accent()).pressScale(onDone),
                contentAlignment = Alignment.Center
            ) {
                Text(tr("Continue", "متابعة"), color = onAccent(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


@Composable
fun Modifier.sheetSurface(): Modifier {
    val dark = LocalDark.current
    val s = RoundedCornerShape(30.dp)
    return this.clip(s).background(if (dark) Color(0xFF2A2B2E) else Color.White).border(BorderStroke(0.8.dp, hairline()), s)
}


@Composable
fun ImageViewer(urls: List<String>, start: Int, onClose: () -> Unit) {
    BackHandler(true) { onClose() }
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = start.coerceIn(0, (urls.size - 1).coerceAtLeast(0))) { urls.size }
    Box(
        Modifier.fillMaxSize().background(Color(0xFF0B0B0C))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
    ) {
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize()) { p ->
            AsyncImage(
                model = urls[p], contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 56.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.White.copy(alpha = 0.16f))
                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)), androidx.compose.foundation.shape.CircleShape)
                    .pressScale(onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Close, contentDescription = "close", tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.weight(1f))
            Text(
                String.format(Locale.ROOT, "\u200E%d \u200E/ \u200E%d", pager.currentPage + 1, urls.size),
                color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold
            )
        }
    }
}
