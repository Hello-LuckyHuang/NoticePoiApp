package com.helloluckyhuang.lbspoiapp.ui

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
    onNavigateToHomePage: () -> Unit
) {
    val context = LocalContext.current

    // 定位权限
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var hasPostPermission by remember { mutableStateOf(initHasPostPermission) }

    val postLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPostPermission = granted
    }

    Column {
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
        Column (
            modifier = Modifier.padding(16.dp),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
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
                Column {
                    Text(modifier = Modifier.padding(10.dp), fontSize = 4.0.em, text = "关于软件")
                    TextField(
                        value = aboutText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color.Gray,
                            unfocusedTextColor = Color.Gray,
                            disabledTextColor = Color.Gray
                        )
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
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