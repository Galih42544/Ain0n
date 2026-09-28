package com.example.engine

import org.json.JSONObject

object TemplateInterpolator {

    /**
     * Interpolates patterns such as:
     * - {{$json.propertyName}}
     * - {{$json}}
     * - {{input}}
     * into values from the upstream input JSON or raw text.
     */
    fun interpolate(template: String, inputJsonStr: String): String {
        if (template.isBlank()) {
            return inputJsonStr
        }

        var result = template
        val jsonObject = runCatching { JSONObject(inputJsonStr) }.getOrNull()

        // Replace whole {{$json}} or {{input}}
        result = result.replace("{{\${'$'}json}}", inputJsonStr)
        result = result.replace("{{input}}", inputJsonStr)

        if (jsonObject != null) {
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.opt(key)?.toString() ?: ""
                result = result.replace("{{\${'$'}json.$key}}", value)
                result = result.replace("{{$key}}", value)
            }
        } else {
            // If input is not JSON (e.g. plain text response), replace {{$json.output}} or {{$output}}
            result = result.replace("{{\${'$'}json.output}}", inputJsonStr)
            result = result.replace("{{output}}", inputJsonStr)
        }

        return result
    }

    /**
     * Extracts a property from a JSON string, or returns defaultValue if not found.
     */
    fun extractJsonField(jsonStr: String, fieldPath: String, defaultValue: String = ""): String {
        return runCatching {
            val obj = JSONObject(jsonStr)
            obj.opt(fieldPath)?.toString() ?: defaultValue
        }.getOrDefault(defaultValue)
    }
}
