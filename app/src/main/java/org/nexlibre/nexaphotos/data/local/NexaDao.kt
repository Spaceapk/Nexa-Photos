package org.nexlibre.nexaphotos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FavoriteDao {

    @Query("SELECT uri FROM favorites ORDER BY addedAtMillis DESC")
    abstract fun observeFavoriteUris(): Flow<List<String>>

    @Query("SELECT uri FROM favorites")
    abstract suspend fun favoriteUris(): List<String>

    @Query("SELECT COUNT(*) FROM favorites")
    abstract suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    abstract suspend fun delete(uri: String)

    /** Setter idempotente: el estado final coincide siempre con [favorite]. */
    @Transaction
    open suspend fun setFavorite(uri: String, favorite: Boolean, nowMillis: Long) {
        if (favorite) {
            insert(FavoriteEntity(uri = uri, addedAtMillis = nowMillis))
        } else {
            delete(uri)
        }
    }
}
