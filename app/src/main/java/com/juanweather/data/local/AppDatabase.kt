package com.juanweather.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.juanweather.data.models.User
import com.juanweather.data.models.UserLocation
import com.juanweather.data.models.AppSettings
import com.juanweather.data.models.EmergencyContact
import com.juanweather.data.models.SOSSettings

@Database(
    entities = [User::class, UserLocation::class, AppSettings::class, EmergencyContact::class, SOSSettings::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userLocationDao(): UserLocationDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosSettingsDao(): SOSSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from v1 → v2: add the role column with default "user"
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'user'")
            }
        }

        // Migration from v2 → v3: create the user_locations table
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        cityName TEXT NOT NULL,
                        addedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration from v3 → v4: create the app_settings table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL,
                        temperatureUnit TEXT NOT NULL DEFAULT 'C',
                        windSpeedUnit TEXT NOT NULL DEFAULT 'km/h',
                        pressureUnit TEXT NOT NULL DEFAULT 'mb',
                        visibilityUnit TEXT NOT NULL DEFAULT 'km',
                        notificationsEnabled INTEGER NOT NULL DEFAULT 1,
                        theme TEXT NOT NULL DEFAULT 'light',
                        language TEXT NOT NULL DEFAULT 'en'
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration from v4 → v5: create the emergency_contacts table (cache for Firestore)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS emergency_contacts (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        relationship TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migration from v5 → v6: create the sos_settings table
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sos_settings (
                        id TEXT PRIMARY KEY NOT NULL,
                        enableLocationSharing INTEGER NOT NULL DEFAULT 1,
                        messageTemplate TEXT NOT NULL DEFAULT 'I need help. This is an emergency SOS alert from JuanWeather.',
                        lastSentTime INTEGER NOT NULL DEFAULT 0,
                        twilioAccountSid TEXT NOT NULL DEFAULT '',
                        twilioAuthToken TEXT NOT NULL DEFAULT '',
                        twilioPhoneNumber TEXT NOT NULL DEFAULT ''
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "juanweather.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
