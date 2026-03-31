package com.helloluckyhuang.lbspoiapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.helloluckyhuang.lbspoiapp.data.local.PoiProjectData
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiProjectViewModel
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePage(viewModel: PoiProjectViewModel, onNavigateToMapPage: (uid: Int) -> Unit) {
    val cardList by viewModel.projects.collectAsState()

    // 新建按钮弹窗
    var showDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    // 长按后操作菜单对应的卡片
    var selectedCard by remember { mutableStateOf<PoiProjectData?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }

    // 重命名弹窗
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }

    // 删除确认弹窗
    var showDeleteDialog by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // 计划的兴趣点项目列表
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("你将去的地方")
        // 添加项目按钮
        Button(
            onClick = {
                showDialog = true
                inputName = ""
            }
        ) {
            Text("添加 Card")
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        .combinedClickable(
                            onClick = {
                                // 点击卡片时执行的操作
                                onNavigateToMapPage(item.uid)
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedCard = item
                                showActionDialog = true
                            }
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 弹窗
    if (showActionDialog && selectedCard != null) {
        AlertDialog(
            onDismissRequest = { showActionDialog = false },
            title = { Text("卡片操作") },
            text = {
                Text("请选择对“${selectedCard!!.title}”执行的操作")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameInput = selectedCard!!.title
                        showActionDialog = false
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
                            showActionDialog = false
                            showDeleteDialog = true
                        }
                    ) {
                        Text("删除")
                    }

                    TextButton(
                        onClick = {
                            showActionDialog = false
                        }
                    ) {
                        Text("取消")
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
