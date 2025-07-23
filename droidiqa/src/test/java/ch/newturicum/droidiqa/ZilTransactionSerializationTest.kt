package ch.newturicum.droidiqa

import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.dto.ZilTransactionStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class ZilTransactionSerializationTest {

    private val jsonSerializer = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    @Test
    fun `serializes ZilTransaction to expected JSON`() {
        val tx = ZilTransaction(
            hash = "0x123",
            status = ZilTransactionStatus.COMPLETED,
            timestamp = 1620000000L,
            amount = 100000,
            contract = "contract1",
            receiver = "zil1testreceiver"
        )
        val expectedJson =
            """{"id":"0x123","status":"COMPLETED","timestamp":1620000000,"amount":100000,"contract":"contract1","receiver":"zil1testreceiver"}"""
        val actualJson = jsonSerializer.encodeToString(tx)
        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun `deserializes ZilTransaction from JSON`() {
        val json =
            """{"id":"0xabc","status":"PENDING","timestamp":123456789,"amount":54321,"contract":null,"receiver":"zil1abc"}"""
        val expected = ZilTransaction(
            hash = "0xabc",
            status = ZilTransactionStatus.PENDING,
            timestamp = 123456789L,
            amount = 54321,
            contract = null,
            receiver = "zil1abc"
        )
        val actual = jsonSerializer.decodeFromString<ZilTransaction>(json)
        assertEquals(expected, actual)
    }
}