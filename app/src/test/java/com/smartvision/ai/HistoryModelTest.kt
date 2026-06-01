package com.smartvision.ai

import com.smartvision.ai.data.local.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryModelTest {
    @Test
    fun historyEntityRoundTripKeepsFields() {
        val record = FakeDataFactory.historyRecord("Translation")
        val entity = record.toEntity()
        val roundTrip = entity.toDomain()
        assertEquals(record.type, roundTrip.type)
        assertEquals(record.title, roundTrip.title)
        assertEquals(record.details, roundTrip.details)
    }
}
