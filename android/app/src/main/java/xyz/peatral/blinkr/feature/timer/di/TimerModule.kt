package xyz.peatral.blinkr.feature.timer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import xyz.peatral.blinkr.core.domain.AppInitializer
import xyz.peatral.blinkr.feature.timer.domain.TimerInitializer

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    @IntoSet
    abstract fun bindTimerInitializer(
        initializer: TimerInitializer
    ): AppInitializer
}