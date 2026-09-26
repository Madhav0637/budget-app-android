package com.madhav0637.budgetapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madhav0637.budgetapp.ui.theme.Koku
import com.madhav0637.budgetapp.ui.theme.KokuType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Short messages at the bottom of the screen, such as "Deleted Zomato · Undo". Shared by the whole app, so a toast
 * started in a sheet is still visible after the sheet closes.
 */
@Stable
class ToastCenter(private val scope: CoroutineScope) {
    data class Toast(val id: Long, val message: String, val icon: ImageVector?, val actionTitle: String?)

    var current by mutableStateOf<Toast?>(null)
        private set

    private var action: (() -> Unit)? = null
    private var dismissal: Job? = null
    private var nextId = 0L

    fun show(message: String, icon: ImageVector? = null, actionTitle: String? = null, durationMillis: Long = 4_000, action: (() -> Unit)? = null) {
        dismissal?.cancel()
        this.action = action
        current = Toast(nextId++, message, icon, actionTitle)
        dismissal = scope.launch {
            delay(durationMillis)
            this@ToastCenter.action = null
            current = null
        }
    }

    fun performAction() {
        val action = action
        dismiss()
        action?.invoke()
    }

    fun dismiss() {
        dismissal?.cancel()
        action = null
        current = null
    }

    companion object {
        /** Undo stays up longer than other messages, the Android convention for undoable actions. */
        const val UNDO_MILLIS = 8_000L
    }
}

val LocalToasts = staticCompositionLocalOf<ToastCenter> { error("No ToastCenter provided") }

@Composable
fun rememberToastCenter(): ToastCenter {
    val scope = rememberCoroutineScope()
    return remember(scope) { ToastCenter(scope) }
}

/** The dark banner itself, the same in light and dark mode. */
@Composable
fun ToastView(toast: ToastCenter.Toast, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
            .background(Koku.colors.toast, shape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .heightIn(min = 54.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (toast.icon != null) {
            Icon(toast.icon, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            toast.message,
            style = KokuType.subheadline.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (toast.actionTitle != null) {
            Spacer(Modifier.width(12.dp))
            Text(
                toast.actionTitle,
                style = KokuType.subheadline.copy(fontWeight = FontWeight.Bold),
                color = Koku.colors.highlightFill,
                modifier = Modifier.pressable(onClick = onAction).padding(vertical = 4.dp),
            )
        }
    }
}

/** The current toast, sliding up from the bottom. */
@Composable
fun ToastSlot(modifier: Modifier = Modifier) {
    val toasts = LocalToasts.current
    AnimatedContent(
        targetState = toasts.current,
        transitionSpec = {
            (slideInVertically(spring(dampingRatio = 0.82f, stiffness = 500f)) { it } + fadeIn()) togetherWith
                (slideOutVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 2 } + fadeOut())
        },
        contentAlignment = Alignment.BottomStart,
        label = "toast",
        modifier = modifier,
    ) { toast ->
        if (toast != null) ToastView(toast, onAction = toasts::performAction)
    }
}

/** The bottom of Home and Activity: the current toast, and the add button beside it. */
@Composable
fun FloatingActions(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = PageMargin).padding(bottom = 12.dp),
    ) {
        Box(Modifier.weight(1f)) { ToastSlot() }
        AddButton(onClick = onAdd)
    }
}
