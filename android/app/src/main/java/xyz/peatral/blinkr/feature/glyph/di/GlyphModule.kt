package xyz.peatral.blinkr.feature.glyph.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import xyz.peatral.blinkr.core.domain.AppInitializer
import xyz.peatral.blinkr.feature.glyph.domain.GlyphInitializer

@Module
@InstallIn(SingletonComponent::class)
abstract class GlyphModule {

    @Binds
    @IntoSet
    abstract fun bindGlyphInitializer(
        initializer: GlyphInitializer
    ): AppInitializer
}