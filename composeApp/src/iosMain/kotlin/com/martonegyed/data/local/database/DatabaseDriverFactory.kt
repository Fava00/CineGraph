package com.martonegyed.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.martonegyed.data.database.CineGraphDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(CineGraphDatabase.Companion.Schema, "cinegraph.db")
    }
}