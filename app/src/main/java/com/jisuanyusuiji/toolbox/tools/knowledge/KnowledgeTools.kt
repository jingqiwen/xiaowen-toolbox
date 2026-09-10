package com.jisuanyusuiji.toolbox.tools.knowledge

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jisuanyusuiji.toolbox.data.JsonStore
import com.jisuanyusuiji.toolbox.tools.extra.decodeImage
import com.jisuanyusuiji.toolbox.ui.components.ChoiceChips
import org.json.JSONObject
import java.io.File

private val assetLogoCache = mutableMapOf<String, Bitmap?>()

private fun loadAssetLogo(context: android.content.Context, name: String): Bitmap? =
    assetLogoCache.getOrPut(name) {
        try {
            context.assets.open("car_logos/$name").use { android.graphics.BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }

private fun loadSimpleIconMap(context: android.content.Context): Map<String, String> = try {
    context.assets.open("simple_icon_slugs.json").use { input ->
        val obj = JSONObject(input.readBytes().toString(Charsets.UTF_8))
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            val res = obj.optJSONObject(key)?.optString("res") ?: ""
            if (res.isNotBlank()) map[key] = res
        }
        map
    }
} catch (_: Exception) {
    emptyMap()
}

private fun drawableId(context: android.content.Context, name: String?): Int {
    if (name.isNullOrBlank()) return 0
    return try {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    } catch (_: Exception) {
        0
    }
}

// ============================================================
// 车标图鉴：文字徽标 + 用户导入真实车标图片（仅存本机）
// ============================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarLogoTool() {
    val context = LocalContext.current
    val store = remember { JsonStore(context, "car_logos") }
    var refresh by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("全部") }
    var editing by remember { mutableStateOf<CarLogo?>(null) }
    var pending by remember { mutableStateOf<CarLogo?>(null) }

    fun logoFile(car: CarLogo): File? {
        val name = store.getString(car.name) ?: return null
        val file = File(File(context.filesDir, "car_logos"), name)
        return if (file.exists()) file else null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val car = pending
        pending = null
        if (uri != null && car != null) {
            val bmp = decodeImage(context, uri, 512)
            if (bmp != null) {
                val dir = File(context.filesDir, "car_logos").apply { mkdirs() }
                val file = File(dir, "${car.badge}_${car.name.hashCode()}.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                store.putString(car.name, file.name)
                refresh++
            }
        }
    }

    val countries = remember(refresh) {
        listOf("全部") + CarLogoData.items.map { it.country }.distinct()
    }
    val items = remember(query, country, refresh) {
        CarLogoData.items.filter {
            (country == "全部" || it.country == country) &&
                (query.isBlank() || it.name.contains(query, true) || it.badge.contains(query, true))
        }
    }
    val assetFiles = remember(refresh) {
        try {
            context.assets.list("car_logos")?.mapNotNull { name ->
                val idx = name.substringBefore('_').toIntOrNull()
                if (idx != null) idx to name else null
            }?.toMap() ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }
    val simpleIcons = remember { loadSimpleIconMap(context) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "🚗 车标图鉴 · ${items.size} / ${CarLogoData.items.size} 个",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索车标名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        ChoiceChips(
            options = countries,
            selected = country,
            onSelect = { country = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp)
        ) {
            gridItems(items, key = { it.name }) { car ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = car }
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val file = logoFile(car)
                        val assetBitmap = assetFiles[CarLogoData.items.indexOf(car)]?.let { loadAssetLogo(context, it) }
                        val iconId = drawableId(context, simpleIcons[car.name])
                        if (file != null) {
                            Image(
                                bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath).asImageBitmap(),
                                contentDescription = car.name,
                                modifier = Modifier.size(52.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else if (iconId != 0) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .background(Color(0xFF37474F), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(iconId),
                                    contentDescription = car.name,
                                    colorFilter = ColorFilter.tint(Color.White),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        } else if (assetBitmap != null) {
                            Image(
                                bitmap = assetBitmap.asImageBitmap(),
                                contentDescription = car.name,
                                modifier = Modifier.size(52.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    car.badge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                        Spacer(Modifier.size(6.dp))
                        Text(car.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            car.country,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    "已自动加载 Wikimedia Commons 上可公开使用的车标；未收录或未下载到的可点击车标自行导入。图片版权与来源见项目根目录 CAR_LOGO_CREDITS.json。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    editing?.let { car ->
        val file = logoFile(car)
        val assetBitmap = assetFiles[CarLogoData.items.indexOf(car)]?.let { loadAssetLogo(context, it) }
        val iconId = drawableId(context, simpleIcons[car.name])
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(car.name) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    if (file != null) {
                        Image(
                            bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath).asImageBitmap(),
                            contentDescription = car.name,
                            modifier = Modifier.size(180.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else if (iconId != 0) {
                        Box(
                            Modifier
                                .size(140.dp)
                                .background(Color(0xFF37474F), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(iconId),
                                contentDescription = car.name,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(90.dp)
                            )
                        }
                        Text(
                            "车标来源：Simple Icons（CC0）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (assetBitmap != null) {
                        Image(
                            bitmap = assetBitmap.asImageBitmap(),
                            contentDescription = car.name,
                            modifier = Modifier.size(180.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            "公开版权车标（Wikimedia Commons）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Box(
                            Modifier
                                .size(120.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(car.badge, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("${car.country} · ${car.kind}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            pending = car
                            picker.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (file == null) "📷 导入自定义图片" else "🔄 更换图片")
                    }
                    if (file != null) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = {
                            file.delete()
                            store.remove(car.name)
                            refresh++
                            editing = null
                        }) { Text("删除已导入的图片", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("关闭") } }
        )
    }
}

// ============================================================
// 电脑快捷键查询（Windows + macOS，常用 + 进阶）
// ============================================================
@Composable
fun ShortcutTool() {
    var query by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("全部") }
    var level by remember { mutableStateOf("全部") }

    val filtered = remember(query, platform, level) {
        ShortcutData.items.filter { item ->
            (platform == "全部" || item.platform == platform) &&
                (level == "全部" || item.level == level) &&
                (query.isBlank() ||
                    item.keys.contains(query, true) ||
                    item.action.contains(query, true) ||
                    item.category.contains(query, true) ||
                    item.platform.contains(query, true))
        }
    }
    val grouped = filtered.groupBy { it.category }

    Column(Modifier.fillMaxSize()) {
        Text(
            "⌨️ 电脑快捷键 · ${filtered.size} 条",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索快捷键 / 功能") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        ChoiceChips(
            options = listOf("全部", "Windows", "macOS"),
            selected = platform,
            onSelect = { platform = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        ChoiceChips(
            options = listOf("全部", "常用", "进阶"),
            selected = level,
            onSelect = { level = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.size(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            grouped.forEach { (category, list) ->
                item {
                    Text(
                        category,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(list, key = { it.keys + it.action + it.platform }) { item ->
                    ShortcutCard(item)
                }
            }
            if (filtered.isEmpty()) {
                item { Text("没有找到相关快捷键", modifier = Modifier.padding(16.dp)) }
            }
            item { Spacer(Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun ShortcutCard(item: ShortcutItem) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    item.keys,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.action, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${item.platform} · ${item.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
