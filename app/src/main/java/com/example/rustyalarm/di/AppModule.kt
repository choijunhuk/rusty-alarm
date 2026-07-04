package com.example.rustyalarm.di

import android.app.Application
import android.content.Context
import com.example.rustyalarm.alarm.AlarmDao
import com.example.rustyalarm.alarm.AlarmDatabase
import com.example.rustyalarm.alarm.AlarmEventDao
import com.example.rustyalarm.alarm.AlarmRepository
import com.example.rustyalarm.alarm.AlarmScheduler
import com.example.rustyalarm.auth.AuthRepository
import com.example.rustyalarm.pet.PetDao
import com.example.rustyalarm.prefs.ThemePreferences
import com.example.rustyalarm.prefs.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlarmDatabase(@ApplicationContext context: Context): AlarmDatabase =
        AlarmDatabase.getDatabase(context)

    @Provides
    fun provideAlarmDao(db: AlarmDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideAlarmEventDao(db: AlarmDatabase): AlarmEventDao = db.alarmEventDao()

    @Provides
    fun providePetDao(db: AlarmDatabase): PetDao = db.petDao()

    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): AlarmScheduler =
        AlarmScheduler(context)

    @Provides
    @Singleton
    fun provideAlarmRepository(
        dao: AlarmDao,
        eventDao: AlarmEventDao,
        scheduler: AlarmScheduler,
        @ApplicationContext context: Context,
    ): AlarmRepository = AlarmRepository(
        dao = dao,
        eventDao = eventDao,
        scheduler = scheduler,
        appContext = context,
    )

    @Provides
    @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository =
        AuthRepository(context)

    @Provides
    @Singleton
    fun provideThemePreferences(@ApplicationContext context: Context): ThemePreferences =
        ThemePreferences(context)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)
}
