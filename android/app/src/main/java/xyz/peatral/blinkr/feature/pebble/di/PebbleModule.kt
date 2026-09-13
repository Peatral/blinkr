package xyz.peatral.blinkr.feature.pebble.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import xyz.peatral.blinkr.core.domain.AppInitializer
import xyz.peatral.blinkr.feature.pebble.domain.PebbleInitializer

@Module
@InstallIn(SingletonComponent::class)
abstract class PebbleModule {

    @Binds
    @IntoSet
    abstract fun bindPebbleInitializer(
        initializer: PebbleInitializer
    ): AppInitializer
}