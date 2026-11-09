package com.kinetix.pro.ui.templates

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.model.AdjustmentParams
import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.FilterPreset
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.Template
import com.kinetix.pro.core.model.TemplateComponent
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.VoiceConfig
import com.kinetix.pro.core.model.VoicePreset
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.SunsetChip
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.components.SunsetSlider
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.StatusSuccess
import com.kinetix.pro.core.ui.theme.TertiaryAmber
import com.kinetix.pro.templates.TemplateEngine
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onApplyTemplate: (templateId: String) -> Unit,
    onApplyTemplateWithVideo: (templateId: String, videoUri: String, title: String) -> Unit = { id, _, _ -> onApplyTemplate(id) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val templateEngine = remember { TemplateEngine() }
    val templatesDir = remember { File(context.filesDir, "templates") }

    var customTemplates by remember {
        mutableStateOf(
            if (isInspection) emptyList() else templateEngine.loadCustomTemplates(templatesDir)
        )
    }
    val factoryTemplates = remember { TemplateEngine.FACTORY_TEMPLATES }
    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var pendingTemplateForVideoPick by remember { mutableStateOf<Template?>(null) }

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3500L)
            bannerMessage = null
        }
    }

    val importTemplateLauncher = if (!isInspection) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    val jsonText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!jsonText.isNullOrBlank()) {
                        val imported = templateEngine.deserializeTemplate(jsonText)
                        templateEngine.saveCustomTemplate(templatesDir, imported)
                        customTemplates = templateEngine.loadCustomTemplates(templatesDir)
                        bannerMessage = "Imported template '${imported.name}'"
                    }
                }.onFailure {
                    bannerMessage = "Invalid .kxtemplate JSON file"
                }
            }
        }
    } else null

    val pickVideoForTemplateLauncher = if (!isInspection) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            val targetTemplate = pendingTemplateForVideoPick
            pendingTemplateForVideoPick = null
            if (uri != null && targetTemplate != null) {
                onApplyTemplateWithVideo(
                    targetTemplate.id,
                    uri.toString(),
                    "${targetTemplate.name} Edit"
                )
            }
        }
    } else null

    fun shareOrExportTemplate(template: Template) {
        runCatching {
            val serialized = templateEngine.serializeTemplate(template)
            val savedFile = templateEngine.saveCustomTemplate(templatesDir, template)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "${template.name}.kxtemplate")
                putExtra(Intent.EXTRA_SUBJECT, "${template.name}.kxtemplate")
                putExtra(Intent.EXTRA_TEXT, serialized)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export ${template.name} (.kxtemplate)"))
            bannerMessage = "Exported ${savedFile.name}"
        }.onFailure {
            bannerMessage = "Saved ${template.name}.kxtemplate to storage"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Editing Templates",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ContentHigh,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Save your edit once, apply the entire brand look to any video in one tap",
                fontSize = 13.sp,
                color = ContentMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        item {
            AnimatedVisibility(visible = bannerMessage != null) {
                ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = bannerMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ContentHigh
                        )
                    }
                }
            }
        }

        // Quick Batch Apply & Import/Create Hero Card
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth(), isHighlighted = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = PrimarySunset,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Batch Apply & .kxtemplate Engine",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                    }
                    Text(
                        text = "Select any template below to apply its canvas ratio, 3D LUT filter, color matrix adjustments, typography, voice effects, and background music to a video.",
                        fontSize = 12.sp,
                        color = ContentMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SunsetPrimaryButton(
                            text = "New Template",
                            onClick = { showCreateSheet = true },
                            icon = Icons.Default.Add,
                            modifier = Modifier.weight(1f)
                        )
                        SunsetOutlinedButton(
                            text = "Import .kxtemplate",
                            onClick = { importTemplateLauncher?.launch("*/*") },
                            icon = Icons.Default.FileUpload,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (customTemplates.isNotEmpty()) {
            item {
                Text(
                    text = "Your Custom & Imported Templates (${customTemplates.size})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimarySunset,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(customTemplates, key = { "custom_${it.id}" }) { template ->
                TemplateCardItem(
                    template = template,
                    isCustom = true,
                    onApplyStudio = { onApplyTemplate(template.id) },
                    onPickVideoAndApply = {
                        pendingTemplateForVideoPick = template
                        pickVideoForTemplateLauncher?.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    onShareTemplate = { shareOrExportTemplate(template) },
                    onDeleteTemplate = {
                        val safeId = template.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        File(templatesDir, "$safeId.kxtemplate").delete()
                        customTemplates = templateEngine.loadCustomTemplates(templatesDir)
                        bannerMessage = "Deleted '${template.name}'"
                    }
                )
            }
        }

        item {
            Text(
                text = "Featured Brand Templates",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ContentHigh,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Factory Template List
        items(factoryTemplates, key = { it.id }) { template ->
            TemplateCardItem(
                template = template,
                isCustom = false,
                onApplyStudio = { onApplyTemplate(template.id) },
                onPickVideoAndApply = {
                    pendingTemplateForVideoPick = template
                    pickVideoForTemplateLauncher?.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onShareTemplate = { shareOrExportTemplate(template) },
                onDeleteTemplate = null
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var newName by remember { mutableStateOf("Neon Creator 9:16") }
        var newDesc by remember { mutableStateOf("Custom LUT grade, 9:16 blur canvas & vocal preset") }
        var selectedRatio by remember { mutableStateOf(AspectRatio.RATIO_9_16) }
        var selectedFilter by remember { mutableStateOf(FilterPreset.ALL_PRESETS[1]) }
        var selectedVoice by remember { mutableStateOf(VoicePreset.PRESETS.first()) }
        var selectedSpeed by remember { mutableFloatStateOf(1.0f) }

        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create Reusable .kxtemplate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ContentHigh
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Template Name", color = ContentMedium) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimarySunset,
                        unfocusedBorderColor = ObsidianStroke,
                        focusedTextColor = ContentHigh,
                        unfocusedTextColor = ContentHigh
                    )
                )

                OutlinedTextField(
                    value = newDesc,
                    onValueChange = { newDesc = it },
                    label = { Text("Description", color = ContentMedium) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimarySunset,
                        unfocusedBorderColor = ObsidianStroke,
                        focusedTextColor = ContentHigh,
                        unfocusedTextColor = ContentHigh
                    )
                )

                Text("Canvas Aspect Ratio", fontSize = 12.sp, color = ContentMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AspectRatio.RATIO_9_16 to "9:16",
                        AspectRatio.RATIO_16_9 to "16:9",
                        AspectRatio.RATIO_1_1 to "1:1",
                        AspectRatio.RATIO_4_5 to "4:5"
                    ).forEach { (r, label) ->
                        SunsetChip(
                            text = label,
                            selected = selectedRatio == r,
                            onClick = { selectedRatio = r },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Color LUT Preset", fontSize = 12.sp, color = ContentMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterPreset.ALL_PRESETS.take(12)) { preset ->
                        SunsetChip(
                            text = preset.name,
                            selected = selectedFilter.id == preset.id,
                            onClick = { selectedFilter = preset }
                        )
                    }
                }

                Text("Voice Effect Preset", fontSize = 12.sp, color = ContentMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VoicePreset.PRESETS) { v ->
                        SunsetChip(
                            text = v.name,
                            selected = selectedVoice.id == v.id,
                            onClick = { selectedVoice = v }
                        )
                    }
                }

                SunsetSlider(
                    value = selectedSpeed,
                    onValueChange = { selectedSpeed = it },
                    valueRange = 0.5f..2.0f,
                    label = "Template Playback Speed",
                    valueFormatter = { String.format("%.2fx", it) }
                )

                SunsetPrimaryButton(
                    text = "Save .kxtemplate Recipe",
                    onClick = {
                        val created = Template(
                            id = "custom_${System.currentTimeMillis()}",
                            name = newName.ifBlank { "Custom Template" },
                            description = newDesc.ifBlank { "Custom Kinetix Pro template" },
                            includes = TemplateComponent.entries,
                            canvas = CanvasConfig(ratio = selectedRatio, fitMode = FitMode.FIT_BLUR_BG),
                            filter = FilterConfig(
                                filterId = selectedFilter.id,
                                filterName = selectedFilter.name,
                                intensity = selectedFilter.defaultIntensity
                            ),
                            adjustment = AdjustmentParams(
                                contrast = selectedFilter.contrastMultiplier,
                                saturation = selectedFilter.saturationMultiplier,
                                brightness = selectedFilter.brightnessOffset,
                                temperature = selectedFilter.temperatureOffset
                            ),
                            texts = listOf(TextOverlay(text = "{{title}}", positionNormY = 0.7f)),
                            voice = VoiceConfig(
                                presetId = selectedVoice.id,
                                pitchSemitones = selectedVoice.pitchSemitones,
                                formantRatio = selectedVoice.formantRatio
                            ),
                            speed = selectedSpeed
                        )
                        templateEngine.saveCustomTemplate(templatesDir, created)
                        customTemplates = templateEngine.loadCustomTemplates(templatesDir)
                        bannerMessage = "Saved '${created.name}' (.kxtemplate)"
                        showCreateSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun TemplateCardItem(
    template: Template,
    isCustom: Boolean,
    onApplyStudio: () -> Unit,
    onPickVideoAndApply: () -> Unit,
    onShareTemplate: () -> Unit,
    onDeleteTemplate: (() -> Unit)?
) {
    ObsidianCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    Text(
                        text = "${template.filter.filterName} • ${String.format("%.2fx", template.speed)}",
                        fontSize = 11.sp,
                        color = PrimarySunset,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33EA6113))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = template.canvas.ratio.label.split(" ").first(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimarySunset
                        )
                    }
                    IconButton(
                        onClick = onShareTemplate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export .kxtemplate",
                            tint = ContentMedium,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isCustom && onDeleteTemplate != null) {
                        IconButton(
                            onClick = onDeleteTemplate,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Template",
                                tint = PrimarySunset,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = template.description,
                fontSize = 12.sp,
                color = ContentMedium
            )

            // Included components pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                template.includes.take(4).forEach { comp ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSurface2)
                            .border(1.dp, ObsidianStroke, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = comp.label,
                            fontSize = 10.sp,
                            color = TertiaryAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SunsetPrimaryButton(
                    text = "Apply in Studio",
                    onClick = onApplyStudio,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PlayArrow
                )
                SunsetOutlinedButton(
                    text = "Pick Video",
                    onClick = onPickVideoAndApply,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.VideoLibrary
                )
            }
        }
    }
}
