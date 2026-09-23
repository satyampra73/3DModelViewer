package com.app.a3dmodelviewer.glb

import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


object GlbMetadataParser {

    private const val GLB_MAGIC = 0x46546C67 // "glTF" in ASCII little-endian
    private const val CHUNK_TYPE_JSON = 0x4E4F534A // "JSON" in ASCII little-endian


    fun parse(inputStream: InputStream): List<GlbNodeMetadata> {
        val bytes = inputStream.readBytes()
        return parse(bytes)
    }

    fun parse(bytes: ByteArray): List<GlbNodeMetadata> {
        if (bytes.size < 20) return emptyList()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        val magic = buffer.int
        val version = buffer.int
        val length = buffer.int

        if (magic != GLB_MAGIC || version != 2 || length > bytes.size) {
            return emptyList()
        }

        val chunkLength = buffer.int
        val chunkType = buffer.int

        if (chunkType != CHUNK_TYPE_JSON || chunkLength <= 0 || chunkLength > bytes.size - 20) {
            return emptyList()
        }

        val jsonBytes = ByteArray(chunkLength)
        buffer.get(jsonBytes)
        val jsonString = String(jsonBytes, Charsets.UTF_8)

        return parseJson(jsonString)
    }


    fun parseJson(jsonString: String): List<GlbNodeMetadata> {
        val result = mutableListOf<GlbNodeMetadata>()
        val root = JSONObject(jsonString)
        val nodesArray = root.optJSONArray("nodes") ?: return emptyList()

        for (i in 0 until nodesArray.length()) {
            val nodeObj = nodesArray.optJSONObject(i) ?: continue
            val extras = nodeObj.optJSONObject("extras")
            if (extras != null && extras.has("prop")) {
                val propValue = extras.optString("prop", "").trim()
                if (propValue.isNotEmpty()) {
                    val name = nodeObj.optString("name", "Node_$i")
                    val translation = nodeObj.optJSONArray("translation")?.let { tArray ->
                        if (tArray.length() >= 3) {
                            floatArrayOf(
                                tArray.optDouble(0, 0.0).toFloat(),
                                tArray.optDouble(1, 0.0).toFloat(),
                                tArray.optDouble(2, 0.0).toFloat()
                            )
                        } else null
                    }

                    result.add(
                        GlbNodeMetadata(
                            nodeIndex = i,
                            nodeName = name,
                            labelText = propValue,
                            localTranslation = translation
                        )
                    )
                }
            }
        }
        return result
    }
}
