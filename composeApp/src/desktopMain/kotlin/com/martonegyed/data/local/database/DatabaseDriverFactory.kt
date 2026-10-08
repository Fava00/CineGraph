package com.martonegyed.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = openDesktopDatabase(
        File(System.getProperty("user.home"), ".cinegraph/cinegraph.db")
    )
}

internal fun openDesktopDatabase(dbFile: File): SqlDriver {
    dbFile.parentFile?.mkdirs()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", java.util.Properties().apply {
        setProperty("foreign_keys", "true")
    })
    try {
        DesktopDatabaseInitializer.initialize(driver) {
            val backup = File(
                dbFile.parentFile,
                "${dbFile.name}.before-v${com.martonegyed.data.database.CineGraphDatabase.Schema.version}-${java.util.UUID.randomUUID()}.bak"
            )
            driver.execute(null, "VACUUM INTO ?", 1) { bindString(0, backup.absolutePath) }
        }
        return driver
    } catch (error: Throwable) {
        driver.close()
        throw error
    }
}
