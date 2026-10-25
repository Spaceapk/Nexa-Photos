package org.nexlibre.nexaphotos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Favorito persistido. La URI de contenido es la clave primaria, de modo que marcar dos veces
 * el mismo elemento no crea duplicados (el setter del repositorio es idempotente).
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val uri: String,
    val addedAtMillis: Long
)
