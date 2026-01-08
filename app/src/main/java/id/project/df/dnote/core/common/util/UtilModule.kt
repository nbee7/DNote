package id.project.df.dnote.core.common.util

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton fun provideIdGenerator(): IdGenerator = UuidGenerator()
    @Provides @Singleton fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}