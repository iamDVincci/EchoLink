package com.kinetix.pro.templates

import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.TemplateComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class TemplateEngineTest {

    private val engine = TemplateEngine()

    @Test
    fun `factory templates are populated and valid`() {
        assertTrue(TemplateEngine.FACTORY_TEMPLATES.size >= 5)
        val viral = engine.findTemplateById(null, "factory_viral_shorts")
        assertNotNull(viral)
        assertEquals(AspectRatio.RATIO_9_16, viral?.canvas?.ratio)
    }

    @Test
    fun `serialize and deserialize kxtemplate preserves all fields`() {
        val project = EditProject(
            id = "proj_1",
            name = "Neon Demo",
            sourceUri = "file:///test.mp4",
            canvas = CanvasConfig(ratio = AspectRatio.RATIO_21_9, fitMode = FitMode.FIT_SOLID_BG),
            filter = FilterConfig(filterId = "cyberpunk", filterName = "Cyberpunk", intensity = 0.92f),
            speed = 1.5f
        )

        val template = engine.createTemplateFromProject(
            project = project,
            name = "Cyberpunk 2.39:1",
            description = "Test template",
            includedComponents = TemplateComponent.entries
        )

        val json = engine.serializeTemplate(template)
        assertTrue(json.contains("Cyberpunk 2.39:1"))

        val restored = engine.deserializeTemplate(json)
        assertEquals(template.name, restored.name)
        assertEquals(AspectRatio.RATIO_21_9, restored.canvas.ratio)
        assertEquals("cyberpunk", restored.filter.filterId)
        assertEquals(1.5f, restored.speed, 0.001f)
    }

    @Test
    fun `saveCustomTemplate and loadCustomTemplates persist to disk`() {
        val tempDir = Files.createTempDirectory("kinetix_templates_test").toFile()
        try {
            val baseProject = EditProject(
                id = "proj_custom",
                name = "Sunset Grade",
                sourceUri = "file:///sunset.mp4",
                speed = 1.25f
            )
            val customTemplate = engine.createTemplateFromProject(
                project = baseProject,
                name = "My Sunset Custom",
                description = "Saved on disk",
                includedComponents = TemplateComponent.entries
            )

            val savedFile = engine.saveCustomTemplate(tempDir, customTemplate)
            assertTrue(savedFile.exists())
            assertTrue(savedFile.name.endsWith(".kxtemplate"))

            val loaded = engine.loadCustomTemplates(tempDir)
            assertEquals(1, loaded.size)
            assertEquals("My Sunset Custom", loaded.first().name)

            val found = engine.findTemplateById(tempDir, customTemplate.id)
            assertNotNull(found)
            assertEquals(1.25f, found!!.speed, 0.001f)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
