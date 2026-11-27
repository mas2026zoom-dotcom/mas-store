package com.mas.store

import android.content.Intent
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

@Composable fun accent(): Color = if (LocalDark.current) Color.White else Color(0xFF0A0A0A)
@Composable fun onAccent(): Color = if (LocalDark.current) Color.Black else Color.White

@Composable
fun tr(en: String, ar: String): String = if (LocalAr.current) ar else en

fun sizeText(bytes: Long, ar: Boolean): String {
    if (bytes <= 0) return "-"
    val mb = bytes / 1048576.0
    return String.format(Locale.ROOT, "%.1f", mb) + if (ar) " م.ب" else " MB"
}

@Composable fun bgColor(): Color = if (LocalDark.current) Color.Black else Color.White
@Composable fun textColor(): Color = if (LocalDark.current) Color.White else Color(0xFF0A0A0A)
@Composable fun subColor(): Color = if (LocalDark.current) Color.White.copy(alpha = 0.58f) else Color.Black.copy(alpha = 0.52f)
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

/** Fade + rise entrance, staggered by index. */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(8) * 55).toLong())
        a.animateTo(1f, tween(480))
    }
    Box(Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 36f }) { content() }
}

@Composable
fun Modifier.glass(radius: Int = 20, strong: Boolean = false): Modifier {
    val dark = LocalDark.current
    val shape = RoundedCornerShape(radius.dp)
    val fill = if (dark) Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (strong) 0.13f else 0.09f), Color.White.copy(alpha = 0.04f))
    ) else Brush.verticalGradient(
        listOf(Color.Black.copy(alpha = if (strong) 0.07f else 0.05f), Color.Black.copy(alpha = 0.02f))
    )
    val stroke = Brush.verticalGradient(
        if (dark) listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.05f))
        else listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.04f))
    )
    return this.clip(shape).background(fill).border(BorderStroke(0.8.dp, stroke), shape)
}

@Composable
fun StoreApp(tickState: MutableIntState, startTab: String?, openId: String?, getFlag: String?) {
    val ctx = LocalContext.current
    val ar = LocalConfiguration.current.locales[0].language == "ar"
    val dark = isSystemInDarkTheme()
    var apps by remember { mutableStateOf(CatalogRepo.bundled(ctx)) }
    var tab by remember {
        mutableIntStateOf(when (startTab) { "games" -> 1; "apps" -> 2; "search" -> 3; else -> 0 })
    }
    var selected by remember { mutableStateOf<AppItem?>(null) }
    var handled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) { CatalogRepo.cached(ctx) }
        if (cached != null && cached.isNotEmpty()) apps = cached
        val remote = withContext(Dispatchers.IO) { CatalogRepo.fetchRemote(ctx) }
        if (remote != null) apps = remote
    }
    LaunchedEffect(apps) {
        if (!handled && openId != null) {
            val a = apps.firstOrNull { it.id == openId }
            if (a != null) {
                handled = true
                selected = a
                if (getFlag == "1") Downloads.start(ctx, a)
            }
        }
    }
    BackHandler(enabled = selected != null) { selected = null }

    CompositionLocalProvider(LocalAr provides ar, LocalDark provides dark, LocalTick provides tickState.intValue) {
        Box(Modifier.fillMaxSize().background(bgColor())) {
            Ambient()
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 40 }) togetherWith fadeOut(tween(120))
                },
                label = "tabs"
            ) { t ->
                when (t) {
                    0 -> TodayScreen(apps, { selected = it })
                    1 -> ListScreen(tr("Games", "الألعاب"), apps.filter { it.category == "game" }, tr("Games coming soon", "الألعاب قريبًا")) { selected = it }
                    2 -> ListScreen(tr("Apps", "التطبيقات"), apps.filter { it.category != "game" }, tr("Apps coming soon", "التطبيقات قريبًا")) { selected = it }
                    else -> SearchScreen(apps) { selected = it }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter)) { Dock(tab) { tab = it } }
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn(tween(220)) + slideInVertically(tween(320)) { it / 8 },
                exit = fadeOut(tween(180)) + slideOutVertically(tween(240)) { it / 10 },
            ) {
                val a = selected
                if (a != null) DetailScreen(a) { selected = null } else Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun Ambient() {
    val dark = LocalDark.current
    Canvas(Modifier.fillMaxSize()) {
        val a = if (dark) 0.12f else 0.07f
        fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
            Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), Offset(size.width * x, size.height * y), size.width * r),
            radius = size.width * r, center = Offset(size.width * x, size.height * y)
        )
        blob(if (dark) Color.White else Color.Black, 0.05f, 0.0f, 0.8f)
        blob(if (dark) Color.White else Color.Black, 1.0f, 0.55f, 0.6f)
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
    Box(
        modifier.height(h.dp).width(w.dp).clip(shape).background(base).pressScale {
            when {
                dl is Dl.Prog -> {}
                upToDate -> Downloads.open(ctx, app.pkg)
                dl is Dl.Ready && java.io.File(dl.path).exists() -> Downloads.install(ctx, dl.path)
                else -> Downloads.start(ctx, app)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        if (dl is Dl.Prog) Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(accent()).align(Alignment.CenterStart))
        Text(
            label, color = if (dl is Dl.Prog && frac > 0.5f) onAccent() else accent(),
            fontWeight = FontWeight.Bold, fontSize = if (large) 15.sp else 13.sp, maxLines = 1
        )
    }
}

@Composable
fun Dock(tab: Int, onTab: (Int) -> Unit) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.Today to tr("Today", "اليوم"),
        Icons.Filled.SportsEsports to tr("Games", "الألعاب"),
        Icons.Filled.Apps to tr("Apps", "التطبيقات"),
        Icons.Filled.Search to tr("Search", "بحث"),
    )
    val dark = LocalDark.current
    val shape = RoundedCornerShape(30.dp)
    Row(
        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp, vertical = 10.dp).fillMaxWidth().height(62.dp)
            .clip(shape)
            .background(if (dark) Color(0xFF111111).copy(alpha = 0.92f) else Color(0xFFF4F4F4).copy(alpha = 0.94f))
            .border(BorderStroke(0.8.dp, if (dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f)), shape)
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
fun Header(title: String, kicker: String? = null) {
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (kicker != null) Text(kicker, color = subColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            Text(title, color = textColor(), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
        }
        Logo(40)
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
fun TodayScreen(apps: List<AppItem>, onOpen: (AppItem) -> Unit) {
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
        item { Header(tr("Today", "اليوم"), date) }
        if (apps.isEmpty()) item { EmptyState(tr("Loading the store...", "جاري تحميل المتجر...")) }
        items(featured.size) { i ->
            Reveal(i) { Hero(featured[i]) { onOpen(featured[i]) } }
            Spacer(Modifier.height(14.dp))
        }
        if (updates.isNotEmpty()) {
            item { SectionTitle(tr("Updates available", "تحديثات متاحة")) }
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
            .background(Brush.verticalGradient(listOf(Color(0xFF2B2B2F), Color(0xFF121214), Color.Black)))
            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)), shape)
            .pressScale(onClick)
    ) {
        AsyncImage(
            model = app.icon, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().scale(1.7f).blur(46.dp).graphicsLayer { alpha = 0.55f }
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
            GetButton(app)
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
fun DetailScreen(app: AppItem, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
        val scroll = rememberScrollState()
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(bgColor())) {
        Box(Modifier.fillMaxWidth().height(380.dp).background(Brush.verticalGradient(listOf(accent().copy(alpha = 0.12f), Color.Transparent))))
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
        // top bar: always-visible back and share, title fades in on scroll
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).glass(20, strong = true).pressScale { onBack() }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = textColor(), modifier = Modifier.size(21.dp))
            }
            Text(
                app.name.get(ar), color = textColor(), fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp).graphicsLayer { alpha = ((scroll.value - 120) / 120f).coerceIn(0f, 1f) }
            )
            Box(
                Modifier.size(40.dp).glass(20, strong = true).pressScale {
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
