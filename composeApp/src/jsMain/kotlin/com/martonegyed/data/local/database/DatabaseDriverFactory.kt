package com.martonegyed.data.local.database

import app.cash.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        //TODO
        throw UnsupportedOperationException("JS database initialization requires async setup.")
    }
}