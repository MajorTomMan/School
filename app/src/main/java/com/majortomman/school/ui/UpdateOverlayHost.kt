package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.majortomman.school.update.UpdateCoordinator
import com.majortomman.school.update.UpdateRuntimeBus
import com.majortomman.school.update.UpdateState

@Composable
fun UpdateOverlayHost(coordinatorProvider: () -> UpdateCoordinator) {
    val state by UpdateRuntimeBus.state.collectAsState()
    val visible by UpdateRuntimeBus.dialogVisible.collectAsState()
    if (!visible || state is UpdateState.Idle) return

    val coordinator = remember { coordinatorProvider() }
    SchoolUpdateDialog(
        state = state,
        isMandatory = coordinator::isMandatory,
        onDismiss = coordinator::dismissStatus,
        onLater = coordinator::remindLater,
        onIgnore = coordinator::ignoreVersion,
        onDownload = coordinator::download,
        onCancelDownload = coordinator::cancelDownload,
    )
}
