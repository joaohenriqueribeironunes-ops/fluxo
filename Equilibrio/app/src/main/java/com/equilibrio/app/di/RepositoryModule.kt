package com.equilibrio.app.di

import com.equilibrio.core.data.gemini.GeminiClientRouter
import com.equilibrio.core.data.report.PdfReportExporter
import com.equilibrio.core.data.repository.AnalysisRepositoryImpl
import com.equilibrio.core.data.repository.BaselineRepositoryImpl
import com.equilibrio.core.data.repository.CalendarRepositoryImpl
import com.equilibrio.core.data.repository.CheckInRepositoryImpl
import com.equilibrio.core.data.repository.CrisisRepositoryImpl
import com.equilibrio.core.data.repository.PredictionRepositoryImpl
import com.equilibrio.core.data.repository.SensorRepositoryImpl
import com.equilibrio.core.data.settings.SettingsRepositoryImpl
import com.equilibrio.core.data.repository.TimelineRepositoryImpl
import com.equilibrio.core.domain.repository.AnalysisRepository
import com.equilibrio.core.domain.repository.BaselineRepository
import com.equilibrio.core.domain.repository.CalendarRepository
import com.equilibrio.core.domain.repository.CheckInRepository
import com.equilibrio.core.domain.repository.CrisisRepository
import com.equilibrio.core.domain.repository.GeminiClient
import com.equilibrio.core.domain.repository.PredictionRepository
import com.equilibrio.core.domain.repository.ReportExporter
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.repository.TimelineRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun sensorRepository(impl: SensorRepositoryImpl): SensorRepository

    @Binds
    @Singleton
    abstract fun checkInRepository(impl: CheckInRepositoryImpl): CheckInRepository

    @Binds
    @Singleton
    abstract fun crisisRepository(impl: CrisisRepositoryImpl): CrisisRepository

    @Binds
    @Singleton
    abstract fun analysisRepository(impl: AnalysisRepositoryImpl): AnalysisRepository

    @Binds
    @Singleton
    abstract fun predictionRepository(impl: PredictionRepositoryImpl): PredictionRepository

    @Binds
    @Singleton
    abstract fun baselineRepository(impl: BaselineRepositoryImpl): BaselineRepository

    @Binds
    @Singleton
    abstract fun timelineRepository(impl: TimelineRepositoryImpl): TimelineRepository

    @Binds
    @Singleton
    abstract fun calendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    @Singleton
    abstract fun settingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun reportExporter(impl: PdfReportExporter): ReportExporter

    @Binds
    @Singleton
    abstract fun geminiClient(impl: GeminiClientRouter): GeminiClient
}
