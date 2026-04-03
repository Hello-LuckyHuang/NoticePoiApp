package com.helloluckyhuang.lbspoiapp.ui.floatframe

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.compositionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class FloatComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private val content = mutableStateOf<(@Composable () -> Unit)?>(null)
    private var onAttachedToWindowListener: (() -> Unit)? = null

    @Suppress("RedundantVisibilityModifier")
    protected override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    @Composable
    override fun Content() {
        content.value?.invoke()
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    fun setOnAttachedToWindowListener(listener: () -> Unit) {
        onAttachedToWindowListener = listener
    }

    override fun onAttachedToWindow() {
        onAttachedToWindowListener?.invoke()
        super.onAttachedToWindow()
    }

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * [createComposition] is called, whichever comes first.
     */
    fun setContent(content: @Composable () -> Unit) {
        shouldCreateCompositionOnAttachedToWindow = true
        this.content.value = content
        if (isAttachedToWindow) {
            createComposition()
        }
        val coroutineContext = AndroidUiDispatcher.CurrentThread
        val runRecomposeScope = CoroutineScope(coroutineContext)
        val reComposer = Recomposer(coroutineContext)
        this.compositionContext = reComposer
        runRecomposeScope.launch {
            reComposer.runRecomposeAndApplyChanges()
        }
    }
}