package com.arborecence.databehavior.di

import android.content.Context
import com.arborecence.databehavior.dao.BehaviorDao
import com.arborecence.databehavior.db.BehaviorDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BehaviorModule {
    @Provides @Singleton
    fun provideBehaviorDatabase(@ApplicationContext context: Context): BehaviorDatabase =
        BehaviorDatabase.build(context)

    @Provides
    fun provideBehaviorDao(db: BehaviorDatabase): BehaviorDao = db.dao()
}
