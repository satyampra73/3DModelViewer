package com.app.a3dmodelviewer.glb


data class GlbNodeMetadata(
    val nodeIndex: Int,
    val nodeName: String,
    val labelText: String,
    val localTranslation: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GlbNodeMetadata
        if (nodeIndex != other.nodeIndex) return false
        if (nodeName != other.nodeName) return false
        if (labelText != other.labelText) return false
        if (localTranslation != null) {
            if (other.localTranslation == null) return false
            if (!localTranslation.contentEquals(other.localTranslation)) return false
        } else if (other.localTranslation != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = nodeIndex
        result = 31 * result + nodeName.hashCode()
        result = 31 * result + labelText.hashCode()
        result = 31 * result + (localTranslation?.contentHashCode() ?: 0)
        return result
    }
}
