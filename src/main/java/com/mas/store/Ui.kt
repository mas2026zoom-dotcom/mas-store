package com.mas.store

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val LocalAr = compositionLocalOf { false }
val LocalDark = compositionLocalOf { true }
val LocalTick = compositionLocalOf { 0 }

private val Accent = Color(0xFF4F8BFF)
private val Accent2 = Color(0xFF9B5CFF)

@Composable
fun tr(en: String, ar: String): String = if (LocalAr.current) ar else en

fun sizeText(bytes: Long, ar: Boolean): String {
    if (bytes <= 0) return "-"
    val mb = bytes / 1048576.0
    return String.format(Locale.ROOT, "%.1f", mb) + if (ar) " م.ب" else " MB"
}

@Composable
fun StoreApp(tickState: MutableIntState, startTab: String?, openId: String?, getFlag: String?) {
    val ctx = LocalContext.current
    val ar = LocalConfiguration.current.locales[0].language == "ar"
    val dark = isSystemInDarkTheme()
    var apps by remember { mutableStateOf(CatalogRepo.bundled(ctx)) }
    var tab by remember {
        mutableIntStateOf(
            when (startTab) { "games" -> 1; "apps" -> 2; "search" -> 3; else -> 0 }
        )
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
        val bg = if (dark) Color(0xFF07070C) else Color(0xFFF1F2F8)
        Box(Modifier.fillMaxSize().background(bg)) {
            Blobs()
            Box(Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> TodayScreen(apps) { selected = it }
                    1 -> ListScreen(tr("Games", "الألعاب"), apps.filter { it.category == "game" }) { selected = it }
                    2 -> ListScreen(tr("Apps", "التطبيقات"), apps.filter { it.category != "game" }) { selected = it }
                    else -> SearchScreen(apps) { selected = it }
                }
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
                TabBar(tab) { tab = it }
            }
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn() + slideInVertically { it / 6 },
                exit = fadeOut() + slideOutVertically { it / 6 },
            ) {
                val a = selected
                if (a != null) DetailScreen(a) { selected = null }
                else Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun Blobs() {
    val dark = LocalDark.current
    Canvas(Modifier.fillMaxSize()) {
        val a = if (dark) 0.35f else 0.28f
        drawCircle(
            Brush.radialGradient(listOf(Accent.copy(alpha = a), Color.Transparent), Offset(size.width * 0.1f, size.height * 0.08f), size.width * 0.8f),
            radius = size.width * 0.8f, center = Offset(size.width * 0.1f, size.height * 0.08f)
        )
        drawCircle(
            Brush.radialGradient(listOf(Accent2.copy(alpha = a), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.45f), size.width * 0.8f),
            radius = size.width * 0.8f, center = Offset(size.width * 0.95f, size.height * 0.45f)
        )
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFFFF6FA8).copy(alpha = a * 0.7f), Color.Transparent), Offset(size.width * 0.2f, size.height * 0.95f), size.width * 0.7f),
            radius = size.width * 0.7f, center = Offset(size.width * 0.2f, size.height * 0.95f)
        )
    }
}

@Composable
fun Modifier.glass(shape: RoundedCornerShape = RoundedCornerShape(24.dp)): Modifier {
    val dark = LocalDark.current
    val fill = if (dark) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f)))
    else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.55f)))
    val stroke = if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.95f)
    return this.clip(shape).background(fill).border(BorderStroke(1.dp, stroke), shape)
}

@Composable
fun textColor(): Color = if (LocalDark.current) Color.White else Color(0xFF111118)

@Composable
fun subColor(): Color = if (LocalDark.current) Color.White.copy(alpha = 0.62f) else Color(0xFF111118).copy(alpha = 0.55f)

@Composable
fun AppIcon(app: AppItem, size: Int, radius: Int = size / 4) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape(radius.dp)).background(Color(app.color).copy(alpha = 0.35f))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(radius.dp))
    ) {
        AsyncImage(model = app.icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun Logo(size: Int) {
    androidx.compose.foundation.Image(
        painter = painterResource(R.drawable.mas_logo),
        contentDescription = "MAS STORE",
        modifier = Modifier.size(size.dp)
    )
}

@Composable
fun GetButton(app: AppItem, modifier: Modifier = Modifier, height: Int = 32, wide: Boolean = false) {
    val ctx = LocalContext.current
    val ar = LocalAr.current
    val tick = LocalTick.current
    val installed = remember(tick, app.pkg) { Downloads.installedVersion(ctx, app.pkg) }
    val dl = Downloads.states[app.id]
    val upToDate = installed != null && installed >= app.versionCode
    var label: String
    var frac = 0f
    if (dl is Dl.Prog) {
        frac = if (dl.total > 0) (dl.done.toFloat() / dl.total).coerceIn(0f, 1f) else 0f
        label = (frac * 100).toInt().toString() + "%"
    } else if (upToDate) {
        label = tr("OPEN", "فتح")
    } else if (dl is Dl.Ready) {
        label = tr("INSTALL", "تثبيت")
    } else if (dl is Dl.Err) {
        label = tr("RETRY", "إعادة")
    } else if (installed != null) {
        label = tr("UPDATE", "تحديث")
    } else {
        label = tr("GET", "تحميل")
    }
    val shape = RoundedCornerShape(50)
    val base = if (LocalDark.current) Color.White.copy(alpha = 0.16f) else Color(0xFF111118).copy(alpha = 0.08f)
    Box(
        modifier
            .height(height.dp)
            .then(if (wide) Modifier.width(110.dp) else Modifier.width(84.dp))
            .clip(shape)
            .background(base)
            .clickable {
                when {
                    dl is Dl.Prog -> {}
                    upToDate -> Downloads.open(ctx, app.pkg)
                    dl is Dl.Ready && java.io.File(dl.path).exists() -> Downloads.install(ctx, dl.path)
                    else -> Downloads.start(ctx, app)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (dl is Dl.Prog) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Accent).align(Alignment.CenterStart))
        }
        Text(
            label,
            color = if (dl is Dl.Prog) Color.White else Accent,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun TabBar(tab: Int, onTab: (Int) -> Unit) {
    val items: List<Pair<ImageVector, String>> = listOf(
        Icons.Filled.Today to tr("Today", "اليوم"),
        Icons.Filled.SportsEsports to tr("Games", "الألعاب"),
        Icons.Filled.Apps to tr("Apps", "التطبيقات"),
        Icons.Filled.Search to tr("Search", "بحث"),
    )
    val dark = LocalDark.current
    Row(
        Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(if (dark) Color(0xFF1A1A24).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f))
            .border(BorderStroke(1.dp, if (dark) Color.White.copy(alpha = 0.2f) else Color.White), RoundedCornerShape(34.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = tab == i
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(28.dp))
                    .background(if (on) Accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onTab(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = label, tint = if (on) Accent else subColor(), modifier = Modifier.size(24.dp))
                Text(label, color = if (on) Accent else subColor(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun Header(title: String, showDate: Boolean) {
    val ar = LocalAr.current
    val locale = if (ar) Locale("ar", "EG") else Locale.getDefault()
    val date = remember(ar) { SimpleDateFormat("EEEE d MMMM", locale).format(Date()) }
    Row(
        Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (showDate) Text(date, color = subColor(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = textColor(), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
        Logo(44)
    }
}

@Composable
fun TodayScreen(apps: List<AppItem>, onOpen: (AppItem) -> Unit) {
    val featured = apps.filter { it.featured }.ifEmpty { apps.take(1) }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)) {
        item { Header(tr("Today", "اليوم"), true) }
        items(featured) { a ->
            FeaturedCard(a) { onOpen(a) }
            Spacer(Modifier.height(18.dp))
        }
        if (apps.isNotEmpty()) {
            item {
                Text(
                    tr("All from MAS", "كل أعمال MAS"), color = textColor(),
                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                )
            }
            items(apps) { a -> AppRow(a) { onOpen(a) } }
        } else {
            item { EmptyState(tr("Loading the store...", "جاري تحميل المتجر...")) }
        }
    }
}

@Composable
fun FeaturedCard(app: AppItem, onClick: () -> Unit) {
    val ar = LocalAr.current
    val c = Color(app.color)
    Box(
        Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(420.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f), Accent2.copy(alpha = 0.8f))))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(30.dp))
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(tr("FEATURED", "مميز"), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(app.name.get(ar), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
        }
        Box(Modifier.align(Alignment.Center).padding(top = 20.dp)) { AppIcon(app, 150, 36) }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(22.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app, 48)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(app.name.get(ar), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.subtitle.get(ar), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            GetButton(app)
        }
    }
}

@Composable
fun AppRow(app: AppItem, onClick: () -> Unit) {
    val ar = LocalAr.current
    Row(
        Modifier.padding(horizontal = 18.dp, vertical = 5.dp).fillMaxWidth()
            .glass(RoundedCornerShape(22.dp)).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app, 62)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(app.name.get(ar), color = textColor(), fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.subtitle.get(ar), color = subColor(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        GetButton(app)
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(28.dp).glass().padding(28.dp), contentAlignment = Alignment.Center) {
        Text(text, color = subColor(), fontSize = 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ListScreen(title: String, list: List<AppItem>, onOpen: (AppItem) -> Unit) {
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)) {
        item { Header(title, false) }
        if (list.isEmpty()) item { EmptyState(tr("Coming soon", "قريبًا")) }
        items(list) { a -> AppRow(a) { onOpen(a) } }
    }
}

@Composable
fun SearchScreen(apps: List<AppItem>, onOpen: (AppItem) -> Unit) {
    val ar = LocalAr.current
    var q by remember { mutableStateOf("") }
    val res = if (q.isBlank()) apps else apps.filter {
        it.name.en.contains(q, true) || it.name.ar.contains(q, true) ||
            it.subtitle.en.contains(q, true) || it.subtitle.ar.contains(q, true)
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 130.dp)) {
        item { Header(tr("Search", "بحث"), false) }
        item {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 6.dp).fillMaxWidth().glass(RoundedCornerShape(18.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = subColor(), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (q.isEmpty()) Text(tr("Apps and games", "تطبيقات وألعاب"), color = subColor(), fontSize = 16.sp)
                    BasicTextField(
                        value = q, onValueChange = { q = it }, singleLine = true,
                        textStyle = TextStyle(color = textColor(), fontSize = 16.sp),
                        cursorBrush = SolidColor(Accent), modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (res.isEmpty()) item { EmptyState(tr("No results", "مفيش نتائج")) }
        items(res) { a -> AppRow(a) { onOpen(a) } }
    }
}

@Composable
fun DetailScreen(app: AppItem, onBack: () -> Unit) {
    val ar = LocalAr.current
    val dark = LocalDark.current
    val bg = if (dark) Color(0xFF07070C) else Color(0xFFF1F2F8)
    Box(Modifier.fillMaxSize().background(bg)) {
        Blobs()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(64.dp))
            Row(Modifier.padding(horizontal = 22.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                AppIcon(app, 112, 26)
                Column(Modifier.padding(horizontal = 16.dp).weight(1f)) {
                    Text(app.name.get(ar), color = textColor(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    Text(app.subtitle.get(ar), color = subColor(), fontSize = 14.sp, maxLines = 2)
                    Spacer(Modifier.height(12.dp))
                    GetButton(app, height = 36, wide = true)
                }
            }
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth().glass().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Stat(tr("VERSION", "الإصدار"), app.version)
                Stat(tr("SIZE", "الحجم"), sizeText(app.size, ar))
                Stat(tr("CATEGORY", "القسم"), if (app.category == "game") tr("Game", "لعبة") else tr("App", "تطبيق"))
            }
            if (app.screenshots.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(app.screenshots) { u ->
                        Box(
                            Modifier.width(190.dp).height(410.dp).clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(22.dp))
                        ) {
                            AsyncImage(model = u, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
            val wn = app.whatsNew.get(ar)
            if (wn.isNotBlank()) {
                SectionTitle(tr("What's New", "الجديد"))
                Text("v" + app.version + "  -  " + wn, color = subColor(), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 22.dp))
            }
            SectionTitle(tr("About", "نبذة"))
            Text(app.description.get(ar), color = textColor().copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(horizontal = 22.dp))
            val dl = Downloads.states[app.id]
            if (dl is Dl.Err) {
                Spacer(Modifier.height(14.dp))
                Text(dl.msg, color = Color(0xFFFF6B6B), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp))
            }
        }
        Box(
            Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(16.dp).size(42.dp)
                .glass(RoundedCornerShape(21.dp)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = textColor(), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun SectionTitle(t: String) {
    Text(t, color = textColor(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp))
}

@Composable
fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = subColor(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(value, color = textColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
