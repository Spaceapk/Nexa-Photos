package org.nexlibre.nexaphotos.ui.media.gallery

/** Estado inmutable de selección múltiple por URI. */
data class SelectionState(
    val selected: Set<String> = emptySet()
) {
    val isActive: Boolean get() = selected.isNotEmpty()
    val count: Int get() = selected.size

    fun contains(uri: String): Boolean = uri in selected

    fun toggle(uri: String): SelectionState =
        if (uri in selected) copy(selected = selected - uri) else copy(selected = selected + uri)

    fun add(uris: Collection<String>): SelectionState = copy(selected = selected + uris)

    fun clear(): SelectionState = SelectionState()

    /** Descarta selecciones de elementos que ya no están en la lista visible. */
    fun retainOnly(visibleUris: Collection<String>): SelectionState =
        copy(selected = selected.intersect(visibleUris.toSet()))
}
