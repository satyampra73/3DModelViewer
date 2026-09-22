package com.app.a3dmodelviewer.ui.container

/**
 * Gestural interaction mode for a [ModelContainerView].
 *
 * - [NORMAL]: One-finger drag moves the container; two-finger pinch resizes the container. 3D model does NOT rotate/zoom.
 * - [INTERACTION]: One-finger drag rotates the 3D model; two-finger pinch zooms the 3D content. Container does NOT move/resize.
 */
enum class InteractionMode {
    NORMAL,
    INTERACTION
}
