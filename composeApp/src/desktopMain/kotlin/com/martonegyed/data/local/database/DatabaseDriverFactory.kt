package com.martonegyed.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.martonegyed.data.database.CineGraphDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(System.getProperty("user.home"), ".cinegraph/cinegraph.db")
        dbFile.parentFile?.mkdirs()

        val isNewDatabase = !dbFile.exists()

        val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        if (isNewDatabase) {
            CineGraphDatabase.Schema.create(driver)
        }
        return driver
    }
}