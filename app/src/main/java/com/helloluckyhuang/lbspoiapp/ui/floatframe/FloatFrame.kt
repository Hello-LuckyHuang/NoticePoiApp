package com.helloluckyhuang.lbspoiapp.ui.floatframe

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.helloluckyhuang.lbspoiapp.ui.component.SlidingDigitText
import com.helloluckyhuang.lbspoiapp.ui.map.HeightLightIcon
import com.helloluckyhuang.lbspoiapp.ui.viewmodel.PoiViewModel
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern

fun createFloat(context: Context, poiViewModel: PoiViewModel) {
    val owner = FloatWindowComposeLifecycleOwner().also {
        it.onCreate()
    }

    val composeView = FloatComposeView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        )

        setContent {
            val poiList by poiViewModel.uiPoiListState.collectAsState()
            Card (
                modifier = Modifier
                    .width(300.dp)
                    .padding(horizontal = 30.dp, vertical = 12.dp)
                    .border(
                        width = 1.dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(25.dp)
                    )
                    .clip(RoundedCornerShape(25.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                val item = poiList.firstOrNull()
                Row(modifier = Modifier.fillMaxWidth().padding(start = 16.dp)) {
                    val distance = item?.distance
                    val color = if (distance != null && distance < item.arriveDistance) Color(0, 128, 0) else Color.Gray
                    HeightLightIcon(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        normalColor = Color.Gray,
                        blinkColor1 = Color.Green,
                        blinkColor2 = Color.Blue,
                        isBlinking = distance != null && distance < item.arriveDistance
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 10.dp, vertical = 12.dp),
                        text = item?.label?:"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        textDecoration = if (item==null) TextDecoration.None else if (item.isArrived) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row (
                        modifier = Modifier.padding(end = 20.dp).align(Alignment.CenterVertically),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        SlidingDigitText(
                            text = if (item==null) "没有数据" else if (distance == null) "定位中" else if (distance < 1000) "%.2f".format(distance) else "%.2f".format(distance/1000),
                            color = color,
                            fontWeight = FontWeight.Light,
                            fontSize = 6.em
                        )
                        SlidingDigitText(
                            text = if (item==null) "" else if (distance == null) "" else if (distance < 1000) " m" else " km",
                            color = color,
                            fontWeight = FontWeight.Light,
                            fontSize = 3.em
                        )
                    }
                }
            }
        }
    }

    composeView.setOnAttachedToWindowListener {
        owner!!.attachToDecorView(composeView.parent as View) //此处是寻找ParentFrameLayout，EasyFloat库未提供ParentFrameLayout的访问API
    }

    EasyFloat.with(context)
        .setLayout(composeView)
        .setTag("composeFloat")
        .setShowPattern(ShowPattern.ALL_TIME)
        .setSidePattern(SidePattern.RESULT_HORIZONTAL)
        .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 200)
        .registerCallback {
            createResult { isCreated, msg, _ ->
                Log.d("EasyFloat", "create result: $isCreated, msg: $msg")
            }
        }
        .show()
    EasyFloat.hide("composeFloat")
}

fun closeFloat() {
    EasyFloat.dismiss("composeFloat")
}

fun hideFloat() {
    EasyFloat.hide("composeFloat")
}

fun showFloat() {
    EasyFloat.show("composeFloat")
}
