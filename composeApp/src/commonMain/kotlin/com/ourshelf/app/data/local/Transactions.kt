package com.ourshelf.app.data.local

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

/**
 * Ortak koddaki `withTransaction` karşılığı: bloktaki DAO çağrıları tek bir yazma transaction'ında çalışır.
 */
suspend fun <R> OurshelfDatabase.transaction(block: suspend () -> R): R =
    useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
