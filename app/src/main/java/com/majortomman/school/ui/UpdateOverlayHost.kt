package com.majortomman.school.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.majortomman.school.update.UpdateCoordinator
import com.majortomman.school.update.UpdateState

@Composable
fun UpdateOverlayHost(coordinator: UpdateCoordinator) {
    val state by coordinator.state.collectAsState()
    val visible by coordinator.dialogVisible.collectAsState()
    if (!visible || state is UpdateState.Idle) return

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
