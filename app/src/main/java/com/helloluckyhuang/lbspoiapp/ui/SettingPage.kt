package com.helloluckyhuang.lbspoiapp.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.helloluckyhuang.lbspoiapp.PoiApp
import com.helloluckyhuang.lbspoiapp.data.DataStoreManager
import com.helloluckyhuang.lbspoiapp.ui.floatframe.closeFloat
import com.helloluckyhuang.lbspoiapp.util.hasBackgroundLocation
import com.helloluckyhuang.lbspoiapp.util.hasLocationPermission
import com.helloluckyhuang.lbspoiapp.util.hasPostPermission
import com.helloluckyhuang.lbspoiapp.util.openBackgroundLocationPermission
import com.helloluckyhuang.lbspoiapp.util.openFloatFramePermission
import com.lzf.easyfloat.permission.PermissionUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
    onNavigateToHomePage: () -> Unit,
    onNavigateToAboutPage: () -> Unit
) {
    val context = LocalContext.current

    // 定位权限
    var hasLocationPermission by remember {
        mutableStateOf(
            hasLocationPermission(context)
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // 通知权限
    val initHasPostPermission = remember {
        hasPostPermission(context)
    }
    var hasPostPermission by remember { mutableStateOf(initHasPostPermission) }
    val postLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostPermission = granted
    }

    // 提示弹窗
    var showBackgroundLocationNotice by remember { mutableStateOf(false) }
    var showFloatFrameNotice by remember { mutableStateOf(false) }

    // 浮窗开关
    val scope = rememberCoroutineScope()
    val switchState by DataStoreManager.getSwitchFlow(context)
        .collectAsState(initial = false)

    Column (
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text("设置")
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            navigationIcon = {
                IconButton(
                    onClick = {
                        onNavigateToHomePage()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            },
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        )

        val scrollState = rememberScrollState()
        Column (
            modifier = Modifier.padding(16.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingCard(
                title = "定位权限申请",
                text = "申请定位权限以获取您的位置信息",
                content = {
                    Button(
                        enabled = !hasLocationPermission,
                        onClick = {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text(if (hasLocationPermission) "拥有" else "申请")
                    }
                }
            )
            SettingCard(
                title = "通知权限申请",
                text = "申请通知权限在您接近预设位置时提醒",
                content = {
                    Button(
                        enabled = !hasPostPermission,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    ) {
                        Text(if (hasPostPermission) "拥有" else "申请")
                    }
                }
            )
            SettingCard(
                title = "后台定位权限申请",
                text = "申请后台定位权限",
                content = {
                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            showBackgroundLocationNotice = true
                        }
                    ) {
                        Text("设置")
                    }
                }
            )
            SettingCard(
                title = "显示在其他应用上方",
                text = "申请显示在其他应用上方",
                content = {
                    Button(
                        onClick = {
                            showFloatFrameNotice = true
                        }
                    ) {
                        Text("设置")
                    }
                }
            )
            SettingCard(
                title = "浮窗开关",
                text = "是否在后台时显示下一个提醒点浮窗",
                content = {
                    Switch(
                        enabled = PermissionUtils.checkPermission(context),
                        checked = if (PermissionUtils.checkPermission(context)) switchState else false,
                        onCheckedChange = { switchState ->
                            scope.launch {
                                DataStoreManager.setSwitch(context, switchState)
                            }
                            if (!switchState) {
                                closeFloat()
                            }
                        }
                    )
                }
            )
            SettingCard(
                title = "停止定位",
                text = "点击立即停止软件定位跟踪",
                content = {
                    Button(
                        onClick = {
                            PoiApp.locationRepo.stop()
                        }
                    ) {
                        Text("停止")
                    }
                }
            )
            SettingCard(
                title = "前往关于",
                text = "点击前往关于页面",
                content = {
                    Button(
                        onClick = {
                            onNavigateToAboutPage()
                        }
                    ) {
                        Text("前往")
                    }
                }
            )
        }
        Spacer(Modifier.weight(1f))
    }

    if (showBackgroundLocationNotice) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationNotice = false },
            title = { Text("后台定位声明") },
            text = { Text(backGroundLocationNotice) },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundLocationNotice = false
                    openBackgroundLocationPermission()
                }) {
                    Text("设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundLocationNotice = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showFloatFrameNotice) {
        AlertDialog(
            onDismissRequest = { showFloatFrameNotice = false },
            title = { Text("浮窗声明") },
            text = { Text(floatFrameNotice) },
            confirmButton = {
                TextButton(onClick = {
                    showFloatFrameNotice = false
                    openFloatFramePermission()
                }) {
                    Text("设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFloatFrameNotice = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun SettingCard(
    modifier: Modifier = Modifier,
    title: String,
    text: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(9.dp)
            )
            .clip(RoundedCornerShape(9.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row {
            Column (
                modifier = Modifier.align(Alignment.CenterVertically).padding(10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 4.0.em,
                )
                Text(
                    text = text,
                    color = Color.Gray,
                    fontSize = 3.0.em
                )
            }
            Spacer(Modifier.weight(1f))
            Box (
                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 10.dp)
            ) {
                content()
            }
        }
    }
}