package com.smartvision.ai

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartvision.ai.data.db.AppDatabase
import com.smartvision.ai.data.db.ScanHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db:  AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndRetrieve() = runBlocking {
        val item = ScanHistoryEntity(
            id              = "test-1",
            moduleType      = "OCR",
            summary         = "Hello world",
            timestampMillis = System.currentTimeMillis()
        )
        db.scanHistoryDao().insert(item)
        val items = db.scanHistoryDao().getAllHistory().first()
        Assert.assertEquals(1, items.size)
        Assert.assertEquals("test-1", items[0].id)
    }

    @Test
    fun insertMultipleAndCount() = runBlocking {
        repeat(5) { i ->
            db.scanHistoryDao().insert(
                ScanHistoryEntity(id="item-$i", moduleType="OCR", summary="Summary $i", timestampMillis=System.currentTimeMillis())
            )
        }
        val count = db.scanHistoryDao().count()
        Assert.assertEquals(5, count)
    }

    @Test
    fun clearAllRemovesEverything() = runBlocking {
        db.scanHistoryDao().insert(
            ScanHistoryEntity(id="x", moduleType="OCR", summary="Test", timestampMillis=1000L)
        )
        db.scanHistoryDao().clearAll()
        val count = db.scanHistoryDao().count()
        Assert.assertEquals(0, count)
    }

    @Test
    fun filterByType() = runBlocking {
        db.scanHistoryDao().insert(ScanHistoryEntity(id="a", moduleType="OCR",       summary="OCR result",       timestampMillis=1000L))
        db.scanHistoryDao().insert(ScanHistoryEntity(id="b", moduleType="translator",summary="Translation",      timestampMillis=2000L))
        db.scanHistoryDao().insert(ScanHistoryEntity(id="c", moduleType="OCR",       summary="Another OCR scan", timestampMillis=3000L))

        val ocrItems = db.scanHistoryDao().getHistoryByType("OCR").first()
        Assert.assertEquals(2, ocrItems.size)
        Assert.assertTrue(ocrItems.all { it.moduleType == "OCR" })
    }

    @Test
    fun searchReturnsMatching() = runBlocking {
        db.scanHistoryDao().insert(ScanHistoryEntity(id="1", moduleType="OCR", summary="Hello world", timestampMillis=1000L))
        db.scanHistoryDao().insert(ScanHistoryEntity(id="2", moduleType="OCR", summary="Namaste", timestampMillis=2000L))

        val results = db.scanHistoryDao().search("Hello").first()
        Assert.assertEquals(1, results.size)
        Assert.assertTrue(results[0].summary.contains("Hello"))
    }
}
