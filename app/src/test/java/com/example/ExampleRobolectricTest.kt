package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.TemplateInterpolator
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("NodeFlow", appName)
    }

    @Test
    fun testTemplateInterpolator() {
        val json = "{\"topic\": \"AI Agents\", \"count\": 42}"
        val template = "Investigating: {{\${'$'}json.topic}} with {{\${'$'}json.count}} nodes"
        val result = TemplateInterpolator.interpolate(template, json)
        assertEquals("Investigating: AI Agents with 42 nodes", result)
    }

    @Test
    fun testJsonFieldExtraction() {
        val json = "{\"sentiment\": \"positive\", \"urgency\": \"high\"}"
        val sentiment = TemplateInterpolator.extractJsonField(json, "sentiment")
        assertEquals("positive", sentiment)
    }
}
