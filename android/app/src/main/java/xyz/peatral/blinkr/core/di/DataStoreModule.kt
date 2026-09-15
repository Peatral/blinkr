package xyz.peatral.blinkr.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.peatral.blinkr.core.data.AppState
import xyz.peatral.blinkr.core.data.AppStateSerializer
import javax.inject.Qualifier
import javax.inject.Singleton


private val Context.appStateDataStore: DataStore<AppState> by dataStore(
    fileName = "app_state.json",
    serializer = AppStateSerializer
)
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideStateDataStore(@ApplicationContext context: Context): DataStore<AppState> {
        return context.appStateDataStore
    }

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.settingsDataStore
    }
}
