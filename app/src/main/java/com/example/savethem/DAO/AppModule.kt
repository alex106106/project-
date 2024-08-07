package com.example.savethem.DAO

import android.content.Context
import androidx.room.Room
import com.example.savethem.Data.UserDatabase
import com.example.savethem.Repository.Repository
import com.example.savethem.Repository.RepositoryImpl
import com.example.savethem.Repository.UserRepository
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
	fun provideContext(@ApplicationContext context: Context): Context {
		return context
	}

	@Provides
	@Singleton
	fun provideUserRepository(context: Context): UserRepository {
		return UserRepository(context)
	}
	@Provides
	fun provideUserDao(userDatabase: UserDatabase): UserDao {
		return userDatabase.userDao()
	}

	@Provides
	fun provideUserDatabase(@ApplicationContext appContext: Context): UserDatabase {
		return Room.databaseBuilder(
			appContext,
			UserDatabase::class.java,
			"user_database"
		).build()
	}

	@Provides
	fun provideRepository(userDao: UserDao): RepositoryImpl {
		return RepositoryImpl(userDao)
	}
//
//	@Provides
//	fun provideUserRepository(context: Context): UserRepository {
//		return UserRepository()
//	}
}
