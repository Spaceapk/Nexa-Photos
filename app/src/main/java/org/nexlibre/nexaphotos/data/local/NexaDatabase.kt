package org.nexlibre.nexaphotos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base Room local. Solo almacena metadatos propios (favoritos): los originales nunca se copian
 * ni se modifican. `exportSchema = false` porque todavía no hay migraciones publicadas.
 */
@Database(entities = [FavoriteEntity::class], version = 1, exportSchema = false)
abstract class NexaDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        private const val DATABASE_NAME = "nexa-photos.db"

        fun build(context: Context): NexaDatabase =
            Room.databaseBuilder(context.applicationContext, NexaDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
