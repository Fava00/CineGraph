package com.martonegyed.data.local.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.martonegyed.data.database.CineGraphDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(CineGraphDatabase.Companion.Schema, context, "cinegraph.db")
    }
}