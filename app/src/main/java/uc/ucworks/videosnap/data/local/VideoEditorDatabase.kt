package uc.ucworks.videosnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import uc.ucworks.videosnap.data.local.dao.ProjectDao
import uc.ucworks.videosnap.data.local.entity.ProjectEntity
import uc.ucworks.videosnap.data.local.converters.Converters

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VideoEditorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
