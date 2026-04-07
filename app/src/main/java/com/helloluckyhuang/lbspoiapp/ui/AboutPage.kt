package com.helloluckyhuang.lbspoiapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateToSettingPage: () -> Unit
) {
    val context = LocalContext.current
    Column (
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text("关于")
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.primary,
            ),
            navigationIcon = {
                IconButton(
                    onClick = {
                        onNavigateToSettingPage()
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
            AboutText("关于软件 LBSPOIApp")
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
            AboutText("作者和项目地址")
            AboutCard(
                title = "项目地址",
                text1 = "LBSPOIApp",
                onClick = {
                    openUrl(context, "https://github.com/Hello-LuckyHuang/NoticePoiApp")
                }
            )

            AboutCard(
                title = "HelloLuckyHuang",
                text1 = "bilibili 和谐的WM旅游",
                onClick = {
                    openUrl(context, "https://space.bilibili.com/3461573907581528")
                }
            )
            AboutText("依赖 (点击跳转到项目地址)")
            AboutCard(
                title = "AndroidX (Core / Lifecycle / Compose / Navigation / DataStore)",
                text1 = "Copyright (C) The Android Open Source Project",
                text2 = "Apache License 2.0",
                onClick = {
                    openUrl(context, "https://developer.android.com/jetpack")
                }
            )

            AboutCard(
                title = "Room",
                text1 = "Copyright (C) The Android Open Source Project",
                text2 = "Apache License 2.0",
                onClick = {
                    openUrl(context, "https://developer.android.com/training/data-storage/room")
                }
            )

            AboutCard(
                title = "EasyFloat",
                text1 = "Copyright (C) Princekin",
                text2 = "Apache License 2.0",
                onClick = {
                    openUrl(context, "https://github.com/princekin-f/EasyFloat")
                }
            )

            AboutCard(
                title = "JUnit",
                text1 = "Copyright (C) JUnit Team",
                text2 = "Eclipse Public License 1.0",
                onClick = {
                    openUrl(context, "https://junit.org/")
                }
            )

            AboutCard(
                title = "AndroidX Test (Espresso / JUnit)",
                text1 = "Copyright (C) The Android Open Source Project",
                text2 = "Apache License 2.0",
                onClick = {
                    openUrl(context, "https://developer.android.com/testing")
                }
            )

            AboutCard(
                title = "高德地图 SDK (AMap)",
                text1 = "Copyright (C) 高德软件有限公司",
                text2 = "高德开放平台服务协议",
                onClick = {
                    openUrl(context, "https://lbs.amap.com/")
                }
            )

            AboutCard(
                title = "高德地图隐私政策",
                text1 = "由高德软件有限公司提供地图与定位服务",
                text2 = "涉及位置信息、设备信息等数据处理",
                onClick = {
                    openUrl(context, "https://lbs.amap.com/pages/privacy/")
                }
            )
            AboutText("声明")
            AboutCard(
                title = "版权所有",
                text1 = "Copyright (C) HelloLuckyHuang",
                onClick = {
                }
            )

            AboutCard(
                title = "开源",
                text1 = "GPL-3.0",
                onClick = {
                }
            )
        }
    }
}

@Composable
fun AboutText(
    text: String
) {
    Text(
        modifier = Modifier.padding(vertical = 5.dp),
        color = Color.Gray,
        text = text
    )
}

@Composable
fun AboutCard(
    modifier: Modifier = Modifier,
    title: String,
    text1: String,
    text2: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(9.dp)
            )
            .clip(RoundedCornerShape(9.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
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
                    text = text1,
                    color = Color.Gray,
                    fontSize = 3.0.em
                )
                if (text2.isNotEmpty()) {
                    Text(
                        text = text2,
                        color = Color.Gray,
                        fontSize = 3.0.em
                    )
                }
            }
        }
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}