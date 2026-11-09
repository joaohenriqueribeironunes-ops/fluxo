package com.lambda.dataacademic.di

import android.content.Context
import com.lambda.dataacademic.dao.AcademicDao
import com.lambda.dataacademic.db.AcademicDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AcademicModule {
    @Provides @Singleton
    fun provideAcademicDatabase(@ApplicationContext context: Context): AcademicDatabase =
        AcademicDatabase.build(context)

    @Provides
    fun provideAcademicDao(db: AcademicDatabase): AcademicDao = db.dao()
}
