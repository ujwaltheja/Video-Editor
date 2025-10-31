package uc.ucworks.videosnap.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uc.ucworks.videosnap.data.local.VideoEditorDatabase
import uc.ucworks.videosnap.data.local.dao.ProjectDao
import uc.ucworks.videosnap.data.repository.ProjectRepository
import uc.ucworks.videosnap.data.repository.ProjectRepositoryImpl
import uc.ucworks.videosnap.domain.engine.*
import uc.ucworks.videosnap.domain.export.ExportEngine
import uc.ucworks.videosnap.domain.export.ExportEngineImpl
import uc.ucworks.videosnap.domain.rendering.RenderingEngine
import uc.ucworks.videosnap.domain.rendering.RenderingEngineImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideVideoEditorDatabase(
        @ApplicationContext context: Context
    ): VideoEditorDatabase {
        return Room.databaseBuilder(
            context,
            VideoEditorDatabase::class.java,
            "video_editor_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: VideoEditorDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        @ApplicationContext context: Context
    ): ProjectRepository {
        return ProjectRepositoryImpl(projectDao, context)
    }

    @Provides
    @Singleton
    fun provideRenderingEngine(
        @ApplicationContext context: Context
    ): RenderingEngine {
        return RenderingEngineImpl(context)
    }

    @Provides
    @Singleton
    fun provideEffectsEngine(
        @ApplicationContext context: Context
    ): EffectsEngine {
        return EffectsEngineImpl(context)
    }

    @Provides
    @Singleton
    fun provideTransitionEngine(): TransitionEngine {
        return TransitionEngineImpl()
    }

    @Provides
    @Singleton
    fun provideAudioEngine(
        @ApplicationContext context: Context
    ): AudioEngine {
        return AudioEngineImpl(context)
    }

    @Provides
    @Singleton
    fun provideKeyframeEngine(): KeyframeEngine {
        return KeyframeEngineImpl()
    }

    @Provides
    @Singleton
    fun provideExportEngine(
        @ApplicationContext context: Context,
        renderingEngine: RenderingEngine,
        effectsEngine: EffectsEngine,
        audioEngine: AudioEngine
    ): ExportEngine {
        return ExportEngineImpl(context, renderingEngine, effectsEngine, audioEngine)
    }
}
