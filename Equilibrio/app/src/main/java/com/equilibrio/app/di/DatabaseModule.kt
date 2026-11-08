package com.equilibrio.app.di

import android.content.Context
import com.equilibrio.core.data.db.AnalysisDao
import com.equilibrio.core.data.db.BaselineDao
import com.equilibrio.core.data.db.CalendarEventDao
import com.equilibrio.core.data.db.CheckInDao
import com.equilibrio.core.data.db.CrisisDao
import com.equilibrio.core.data.db.DatabaseFactory
import com.equilibrio.core.data.db.EquilibrioDatabase
import com.equilibrio.core.data.db.PredictionDao
import com.equilibrio.core.data.db.SensorWindowDao
import com.equilibrio.core.data.security.KeystoreSecretStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun keystoreSecretStore(@ApplicationContext context: Context): KeystoreSecretStore =
        KeystoreSecretStore(context)

    @Provides
    @Singleton
    fun databaseFactory(
        @ApplicationContext context: Context,
        secrets: KeystoreSecretStore
    ): DatabaseFactory = DatabaseFactory(context, secrets)

    @Provides
    @Singleton
    fun database(factory: DatabaseFactory): EquilibrioDatabase = factory.create()

    @Provides
    fun sensorWindowDao(db: EquilibrioDatabase): SensorWindowDao = db.sensorWindowDao()

    @Provides
    fun checkInDao(db: EquilibrioDatabase): CheckInDao = db.checkInDao()

    @Provides
    fun crisisDao(db: EquilibrioDatabase): CrisisDao = db.crisisDao()

    @Provides
    fun analysisDao(db: EquilibrioDatabase): AnalysisDao = db.analysisDao()

    @Provides
    fun predictionDao(db: EquilibrioDatabase): PredictionDao = db.predictionDao()

    @Provides
    fun baselineDao(db: EquilibrioDatabase): BaselineDao = db.baselineDao()

    @Provides
    fun calendarEventDao(db: EquilibrioDatabase): CalendarEventDao = db.calendarDao()
}
