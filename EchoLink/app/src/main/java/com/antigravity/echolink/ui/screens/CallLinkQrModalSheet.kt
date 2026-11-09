package com.antigravity.echolink.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.antigravity.echolink.model.CallLinkSession
import com.antigravity.echolink.model.PeerNode
import com.antigravity.echolink.model.RecentPeer
import com.antigravity.echolink.ui.components.TacticalAvatarBadge
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Screen 3: "Call Link" QR & NFC Pairing Modal Bottom Sheet (`call_link_qr_modal`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLinkQrModalSheet(
    session: CallLinkSession,
    remainingSeconds: Int,
    onDismiss: () -> Unit,
    onShareViaHotspot: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copiedPin by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val freshnessFraction = (remainingSeconds / 300f).coerceIn(0.05f, 1.0f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EchoLinkColors.SurfaceContainerLow,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.OutlineVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Host Voice Room",
                        style = EchoLinkTypography.HeadlineSm,
                        color = EchoLinkColors.TextOnSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Scan from another phone to connect",
                        style = EchoLinkTypography.BodySm,
                        color = EchoLinkColors.TextOnSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.SurfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close modal",
                        tint = EchoLinkColors.TextOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(224.dp)
                    .shadow(30.dp, RoundedCornerShape(22.dp), spotColor = EchoLinkColors.CyberCyan)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                StylizedTacticalQrCode(qrContent = session.qrUri)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clean Copyable Room Passcode Pill
            Row(
                modifier = Modifier
                    .width(240.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerLowest)
                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("EchoLink URI", session.qrUri))
                        copiedPin = true
                        scope.launch {
                            delay(1800L)
                            copiedPin = false
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EchoLinkColors.EmeraldPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (copiedPin) "Link Copied" else "Room Code: ${session.pinCode}",
                        style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                        color = EchoLinkColors.TextOnSurface
                    )
                }

                Icon(
                    imageVector = if (copiedPin) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy Link",
                    tint = if (copiedPin) EchoLinkColors.EmeraldPulse else EchoLinkColors.TextOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Key Freshness Progress Bar (replaces countdown numbers)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.SurfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(freshnessFraction)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.CyberCyan)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my EchoLink Voice Room:\n${session.qrUri}"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share EchoLink Invite"))
                    } catch (_: Exception) {
                    }
                    onShareViaHotspot()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EchoLinkColors.CyberCyan,
                    contentColor = EchoLinkColors.ObsidianCanvas
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = EchoLinkColors.ObsidianCanvas,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Room Invite",
                    style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold),
                    color = EchoLinkColors.ObsidianCanvas
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StylizedTacticalQrCode(qrContent: String) {
    val qrMatrix = remember(qrContent) {
        try {
            QRCodeWriter().encode(qrContent, BarcodeFormat.QR_CODE, 33, 33)
        } catch (_: Exception) {
            null
        }
    }

    Canvas(modifier = Modifier.size(200.dp)) {
        val w = size.width
        val h = size.height

        if (qrMatrix != null) {
            val cols = qrMatrix.width
            val rows = qrMatrix.height
            val cellW = w / cols
            val cellH = h / rows

            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    if (x in (cols / 2 - 4)..(cols / 2 + 4) && y in (rows / 2 - 4)..(rows / 2 + 4)) {
                        continue
                    }
                    if (qrMatrix.get(x, y)) {
                        drawRoundRect(
                            color = Color.Black,
                            topLeft = Offset(x * cellW, y * cellH),
                            size = Size(cellW * 0.92f, cellH * 0.92f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }
                }
            }
        }

        val islandSize = w * 0.25f
        val islandLeft = (w - islandSize) / 2f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(islandLeft - 4.dp.toPx(), islandLeft - 4.dp.toPx()),
            size = Size(islandSize + 8.dp.toPx(), islandSize + 8.dp.toPx()),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawRoundRect(
            color = EchoLinkColors.SurfaceDim,
            topLeft = Offset(islandLeft, islandLeft),
            size = Size(islandSize, islandSize),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )

        val wavePath = Path().apply {
            moveTo(w * 0.43f, h * 0.54f)
            lineTo(w * 0.50f, h * 0.44f)
            lineTo(w * 0.57f, h * 0.54f)
        }
        drawPath(
            path = wavePath,
            color = EchoLinkColors.CyberCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = EchoLinkColors.EmeraldPulse,
            radius = 2.5.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.51f)
        )
    }
}

/**
 * Scan Call Link & Manual PIN/IP Entry Bottom Sheet Modal with Live CameraX QR Decoder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanCallLinkSheet(
    onDismiss: () -> Unit,
    onJoinLink: (String, String) -> Unit
) {
    val context = LocalContext.current
    var peerIp by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var scannedOnce by remember { mutableStateOf(false) }
    val hasCameraPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EchoLinkColors.SurfaceContainerLow,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.OutlineVariant)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = EchoLinkColors.EmeraldPulse
                    )
                    Text(
                        text = "Scan or Enter Call Link",
                        style = EchoLinkTypography.HeadlineSm,
                        color = EchoLinkColors.TextOnSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = EchoLinkColors.TextOnSurfaceVariant
                    )
                }
            }

            // Live CameraX QR Viewfinder (or Permission Notice if Camera not granted)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(EchoLinkColors.SurfaceContainerLowest)
                    .border(1.dp, EchoLinkColors.EmeraldPulse.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    LiveCameraQrScanner(
                        onQrDecoded = { decodedText ->
                            if (!scannedOnce && decodedText.isNotBlank()) {
                                scannedOnce = true
                                val extractedPin = if (decodedText.contains("pin=")) {
                                    decodedText.substringAfter("pin=").substringBefore("&")
                                } else {
                                    pinCode
                                }
                                onJoinLink(decodedText, extractedPin)
                            }
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = EchoLinkColors.AmberWarning,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CAMERA PERMISSION REQUIRED FOR QR SCAN",
                            style = EchoLinkTypography.TelemetrySm,
                            color = EchoLinkColors.AmberWarning
                        )
                        Text(
                            text = "Grant Camera on HUD tab or enter peer IP below",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = peerIp,
                onValueChange = { peerIp = it },
                label = { Text("Peer P2P / LAN IP or echolink:// URI", style = EchoLinkTypography.TelemetrySm) },
                placeholder = { Text("e.g. 192.168.1.25 or 192.168.49.1", style = EchoLinkTypography.TelemetrySm) },
                textStyle = EchoLinkTypography.TelemetryMd.copy(color = EchoLinkColors.TextPrimary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EchoLinkColors.CyberCyan,
                    unfocusedBorderColor = EchoLinkColors.ArmorStroke
                )
            )

            OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 6) pinCode = it },
                label = { Text("4-Digit Room Security PIN (Optional for LAN)", style = EchoLinkTypography.TelemetrySm) },
                placeholder = { Text("e.g. 4829", style = EchoLinkTypography.TelemetrySm) },
                textStyle = EchoLinkTypography.TelemetryMd.copy(color = EchoLinkColors.PrimaryLight),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EchoLinkColors.CyberCyan,
                    unfocusedBorderColor = EchoLinkColors.ArmorStroke
                )
            )

            Button(
                onClick = { onJoinLink(peerIp, pinCode) },
                enabled = peerIp.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EchoLinkColors.EmeraldPulse,
                    contentColor = EchoLinkColors.ObsidianCanvas
                )
            ) {
                Text(
                    text = "Negotiate P2P Link & Join Voice Channel",
                    style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold),
                    color = EchoLinkColors.ObsidianCanvas
                )
            }
        }
    }
}

@Composable
private fun LiveCameraQrScanner(
    onQrDecoded: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val qrReader = remember { MultiFormatReader() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                analysisExecutor.shutdown()
            } catch (_: Exception) {
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            try {
                val controller = LifecycleCameraController(ctx).apply {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                    setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
                        try {
                            val plane = imageProxy.planes.firstOrNull()
                            if (plane != null) {
                                val buffer = plane.buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                val width = imageProxy.width
                                val height = imageProxy.height
                                val source = PlanarYUVLuminanceSource(
                                    bytes,
                                    width,
                                    height,
                                    0,
                                    0,
                                    width,
                                    height,
                                    false
                                )
                                val bitmap = BinaryBitmap(HybridBinarizer(source))
                                val result = qrReader.decodeWithState(bitmap)
                                val text = result.text
                                if (!text.isNullOrBlank()) {
                                    previewView.post { onQrDecoded(text) }
                                }
                            }
                        } catch (_: Exception) {
                        } finally {
                            qrReader.reset()
                            imageProxy.close()
                        }
                    }
                    bindToLifecycle(lifecycleOwner)
                }
                previewView.controller = controller
            } catch (_: Exception) {
            }
            previewView
        }
    )
}

/**
 * Multi-Peer Room Manager Bottom Sheet: Allows the Host to add nearby or saved people to the active room,
 * show the room QR code, or scan/dial another peer into the room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPeersToRoomSheet(
    activePeers: List<PeerNode>,
    discoveredPeers: List<PeerNode>,
    recentPeers: List<RecentPeer>,
    onAddDiscoveredPeer: (PeerNode) -> Unit,
    onAddRecentPeer: (RecentPeer) -> Unit,
    onRenamePeer: (String, String) -> Unit,
    onRemoveRecentPeer: (String) -> Unit,
    onShowHostQr: () -> Unit,
    onOpenScanOrIp: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableNearby = discoveredPeers.filter { candidate ->
        activePeers.none { it.ipAddress == candidate.ipAddress }
    }
    val availableRecent = recentPeers.filter { rec ->
        activePeers.none { it.ipAddress == rec.ipAddress } &&
            availableNearby.none { it.ipAddress == rec.ipAddress }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EchoLinkColors.SurfaceContainerLow,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.OutlineVariant)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = EchoLinkColors.CyberCyan
                    )
                    Column {
                        Text(
                            text = "Add People to Room",
                            style = EchoLinkTypography.HeadlineSm,
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "${activePeers.size} connected • Up to 6 peers in room",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = EchoLinkColors.TextOnSurfaceVariant
                    )
                }
            }

            // Quick Invite / Scan Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EchoLinkColors.SurfaceContainer)
                        .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onShowHostQr()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = EchoLinkColors.CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Room QR",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Let peer scan",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(EchoLinkColors.SurfaceContainer)
                        .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(16.dp))
                        .clickable {
                            onDismiss()
                            onOpenScanOrIp()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = EchoLinkColors.EmeraldPulse,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "Scan / IP",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Add by QR/IP",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
            }

            // Nearby Discovered Devices Not Yet in Room
            if (availableNearby.isNotEmpty()) {
                Text(
                    text = "Nearby Devices",
                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold),
                    color = EchoLinkColors.CyberCyan
                )
                availableNearby.take(4).forEach { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EchoLinkColors.SurfaceContainer)
                            .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            TacticalAvatarBadge(
                                name = peer.name,
                                accentColor = Color(peer.transport.colorHex),
                                size = 34.dp
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = peer.name,
                                        style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                                        color = EchoLinkColors.TextOnSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = EchoLinkColors.TextOnSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRenamePeer(peer.ipAddress, peer.name) }
                                    )
                                }
                                Text(
                                    text = peer.transport.fullName,
                                    style = EchoLinkTypography.BodySm,
                                    color = EchoLinkColors.TextOnSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EchoLinkColors.EmeraldPulse)
                                .clickable { onAddDiscoveredPeer(peer) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = EchoLinkColors.ObsidianCanvas,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Add",
                                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Bold),
                                color = EchoLinkColors.ObsidianCanvas
                            )
                        }
                    }
                }
            }

            // Recent / Saved Devices Not Yet in Room
            if (availableRecent.isNotEmpty()) {
                Text(
                    text = "Recent Connected Devices",
                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold),
                    color = EchoLinkColors.TextOnSurfaceVariant
                )
                availableRecent.take(4).forEach { recent ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(EchoLinkColors.SurfaceContainer)
                            .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            TacticalAvatarBadge(
                                name = recent.name,
                                accentColor = Color(recent.transport.colorHex),
                                size = 34.dp
                            )
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recent.name,
                                        style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                                        color = EchoLinkColors.TextOnSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = EchoLinkColors.TextOnSurfaceVariant,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onRenamePeer(recent.ipAddress, recent.name) }
                                    )
                                }
                                Text(
                                    text = recent.transport.fullName,
                                    style = EchoLinkTypography.BodySm,
                                    color = EchoLinkColors.TextOnSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EchoLinkColors.CyberCyan)
                                    .clickable { onAddRecentPeer(recent) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = EchoLinkColors.ObsidianCanvas,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Add",
                                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Bold),
                                    color = EchoLinkColors.ObsidianCanvas
                                )
                            }

                            IconButton(
                                onClick = { onRemoveRecentPeer(recent.ipAddress) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete from history",
                                    tint = EchoLinkColors.ErrorLight,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (availableNearby.isEmpty() && availableRecent.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EchoLinkColors.SurfaceContainerLowest)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Use Room QR or Scan/IP above to add more devices to this voice room.",
                        style = EchoLinkTypography.BodySm,
                        color = EchoLinkColors.TextOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Rename Connected Device Modal Dialog.
 */
@Composable
fun RenamePeerModalDialog(
    ipAddress: String,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirmRename: (String, String) -> Unit
) {
    var nameInput by remember(ipAddress, currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EchoLinkColors.SurfaceContainerHigh,
        titleContentColor = EchoLinkColors.TextOnSurface,
        textContentColor = EchoLinkColors.TextOnSurfaceVariant,
        title = {
            Text(
                text = "Rename Device",
                style = EchoLinkTypography.HeadlineSm
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Set a custom display name for this peer ($ipAddress):",
                    style = EchoLinkTypography.BodySm,
                    color = EchoLinkColors.TextOnSurfaceVariant
                )
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 28) nameInput = it },
                    label = { Text("Device Name", style = EchoLinkTypography.BodySm) },
                    singleLine = true,
                    textStyle = EchoLinkTypography.BodyMd.copy(color = EchoLinkColors.TextPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EchoLinkColors.CyberCyan,
                        unfocusedBorderColor = EchoLinkColors.ArmorStroke
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRename(ipAddress, nameInput) },
                enabled = nameInput.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EchoLinkColors.EmeraldPulse,
                    contentColor = EchoLinkColors.ObsidianCanvas
                )
            ) {
                Text(
                    text = "Save Name",
                    style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.Bold),
                    color = EchoLinkColors.ObsidianCanvas
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = EchoLinkTypography.BodyMd,
                    color = EchoLinkColors.TextOnSurfaceVariant
                )
            }
        }
    )
}
