package com.helloluckyhuang.lbspoiapp.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.data.DataStoreManager
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.service.LocationService
import com.helloluckyhuang.lbspoiapp.ui.floatframe.createFloat
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiViewModel
import com.helloluckyhuang.lbspoiapp.util.hasBackgroundLocation
import com.lzf.easyfloat.permission.PermissionUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(
    viewModel: PoiProjectViewModel,
    poiViewModel: PoiViewModel,
    onNavigateToMapPage: (uid: Int) -> Unit,
    onNavigateToSettingPage: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val cardList by viewModel.projects.collectAsState()

    // 新建按钮弹窗
    var showDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    // 长按后操作菜单对应的卡片
    var selectedCard by remember { mutableStateOf<PoiProjectData?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // 重命名弹窗
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    // 删除确认弹窗
    var showDeleteDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // 浮窗开关设置
    val floatFrameEnabled by DataStoreManager.getSwitchFlow(context)
        .collectAsState(initial = false)

    // 计划的兴趣点项目列表
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "你将去的地方",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 1.dp, top = 24.dp, bottom = 12.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light
        )
        // 添加项目按钮 和 设置按钮
        Row {
            Button(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = {
                    showDialog = true
                    inputName = ""
                }
            ) {
                Text("添加将去的地方")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                onClick = {
                    onNavigateToSettingPage()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.Gray
                )
            }
        }


        Spacer(modifier = Modifier.height(12.dp))

        if (cardList.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = cardList,
                    key = { it.uid }
                ) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clip(RoundedCornerShape(9.dp))
                            .animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<androidx.compose.ui.unit.IntOffset>(
                                stiffness = Spring.StiffnessVeryLow,
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                visibilityThreshold = IntOffset.VisibilityThreshold
                            ))
                            .combinedClickable(
                                onClick = {
                                    // 点击卡片时执行的操作
                                    onNavigateToMapPage(item.uid)
                                    if (hasBackgroundLocation(context)) {
                                        ContextCompat.startForegroundService(
                                            context,
                                            Intent(context, LocationService::class.java)
                                        )
                                    } else {
                                        PoiApp.locationRepo.startForegroundOnly()
                                    }

                                    PoiApp.trackingEnabled = true

                                    // 如果有后台定位权限，创建距离小窗
                                    if (floatFrameEnabled && PermissionUtils.checkPermission(context) && hasBackgroundLocation(context)) {
                                        createFloat(PoiApp.instance, poiViewModel)
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedCard = item
                                    showCreateDialog = true
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(start = 16.dp)) {
                            RandomProjectIcon(modifier = Modifier.align(Alignment.CenterVertically), name = item.title)
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "创建时间: ${formatTime(item.createTime)}",
                                    color = Color.Gray,
                                    fontSize = 3.0.em
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    color = Color.Gray,
                    fontSize = 6.0.em,
                    text = "请添加将去的地方\ud83d\ude1b"
                )
                Text(
                    color = Color.Gray,
                    fontSize = 5.5.em,
                    text = "即将抵达时，我会提醒您！"
                )
            }
        }
    }

    // 弹窗
    if (showCreateDialog && selectedCard != null) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("卡片操作") },
            text = {
                Text("请选择对“${selectedCard!!.title}”执行的操作")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameInput = selectedCard!!.title
                        showCreateDialog = false
                        showRenameDialog = true
                    }
                ) {
                    Text("重命名")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showCreateDialog = false
                        }
                    ) {
                        Text("取消")
                    }
                    TextButton(
                        onClick = {
                            showCreateDialog = false
                            showDeleteDialog = true
                        }
                    ) {
                        Text("删除")
                    }
                }
            }
        )
    }
    // ---------- 重命名弹窗 ----------
    if (showRenameDialog && selectedCard != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名卡片") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("新的卡片名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameInput.trim()
                        if (newName.isNotEmpty()) {
                            viewModel.updateProject(selectedCard!!.copy(title = newName))
                            showRenameDialog = false
                            selectedCard = null
                        }
                    },
                    enabled = renameInput.trim().isNotEmpty()
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // ---------- 删除确认弹窗 ----------
    if (showDeleteDialog && selectedCard != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除卡片") },
            text = {
                Text("确定要删除“${selectedCard!!.title}”吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetId = selectedCard!!.uid
                        viewModel.deleteProject(selectedCard!!)
                        showDeleteDialog = false
                        selectedCard = null
                    }
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    // 新建弹窗
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false   // 点外部区域或返回键时关闭
            },
            title = {
                Text("新建卡片")
            },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("请输入卡片名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val text = inputName.trim()
                        if (text.isNotEmpty()) {
                            viewModel.addProject(text)
                        }
                        showDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false   // 取消，不创建
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun RandomProjectIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 40,
    cornerRadius: Int = 10,
) {
    val gradientColors = remember(name) {
        generateStableGradient(name)
    }

    val displayChar = name
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(gradientColors)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayChar,
            color = Color.White,
            fontSize = (size * 0.4f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun generateStableGradient(seedText: String): List<Color> {
    val seed = seedText.hashCode().absoluteValue
    val random = Random(75_1050+seed)

    // 1. 主色相（0~360）
    val baseHue = random.nextFloat() * 360f

    // 2. 控制偏移范围（越小越柔和）
    val hueOffset = 10f + random.nextFloat() * 15f // 10° ~ 25°

    // 3. 两个颜色：同一色相附近
    val hue1 = baseHue
    val hue2 = (baseHue + hueOffset) % 360f

    // 4. 适当变化饱和度 & 明度（增加层次）
    val sat1 = 0.65f + random.nextFloat() * 0.2f
    val sat2 = 0.65f + random.nextFloat() * 0.2f

    val light1 = 0.55f + random.nextFloat() * 0.1f
    val light2 = 0.65f + random.nextFloat() * 0.1f

    return listOf(
        hslToColor(hue1, sat1, light1),
        hslToColor(hue2, sat2, light2)
    )
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1 - abs(2 * l - 1)) * s
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = l - c / 2

    val (r, g, b) = when {
        h < 60 -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m)
}

private fun formatTime(time: Long): String {
    val sdf = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    return sdf.format(Date(time))
}