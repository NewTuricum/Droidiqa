package ch.newturicum.droidiqa.database

import androidx.room.TypeConverter
import ch.newturicum.droidiqa.database.dao.AccountContainer
import ch.newturicum.droidiqa.database.dao.TokenContainer
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.network.ZilNetwork
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStoredList(value: String?): List<String> {
        return value?.split('|') ?: listOf()
    }

    @TypeConverter
    fun toStoredList(value: List<String>?): String? {
        return value?.joinToString("|")
    }

    @TypeConverter
    fun fromStoredZilNetwork(value: String?): ZilNetwork? {
        return value?.let { ZilNetwork.valueOf(it) }
    }

    @TypeConverter
    fun toStoredZilNetwork(value: ZilNetwork?): String? {
        return value?.name
    }

    @TypeConverter
    fun fromStoredStringMap(value: String?): Map<String, String> {
        return if (value != null) {
            try {
                json.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    value
                )
            } catch (e: Exception) {
                emptyMap()
            }
        } else emptyMap()
    }

    @TypeConverter
    fun toStoredStringMap(value: Map<String, String>?): String? {
        return value?.let {
            json.encodeToString(
                MapSerializer(
                    String.serializer(),
                    String.serializer()
                ), it
            )
        }
    }

    @TypeConverter
    fun fromStoredZilAccountList(value: String?): AccountContainer {
        return if (value != null) {
            try {
                json.decodeFromString(AccountContainer.serializer(), value)
            } catch (e: Exception) {
                AccountContainer(mutableListOf())
            }
        } else AccountContainer(mutableListOf())
    }

    @TypeConverter
    fun toStoredZilAccountList(value: AccountContainer?): String? {
        return value?.let { json.encodeToString(AccountContainer.serializer(), it) }
    }

    @TypeConverter
    fun fromStoredZilTokenList(value: String?): TokenContainer {
        return if (value != null) {
            try {
                json.decodeFromString(TokenContainer.serializer(), value)
            } catch (e: Exception) {
                TokenContainer(mutableListOf())
            }
        } else TokenContainer(mutableListOf())
    }

    @TypeConverter
    fun toStoredZilTokenList(value: TokenContainer?): String? {
        return value?.let { json.encodeToString(TokenContainer.serializer(), it) }
    }

    @TypeConverter
    fun fromStoredTransaction(value: String?): ZilTransaction? {
        return value?.let {
            try {
                json.decodeFromString(ZilTransaction.serializer(), it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun toStoredTransaction(value: ZilTransaction?): String? {
        return value?.let { json.encodeToString(ZilTransaction.serializer(), it) }
    }

    @TypeConverter
    fun fromStoredTransactionList(value: String?): List<ZilTransaction> {
        return if (value != null) {
            try {
                json.decodeFromString(ListSerializer(ZilTransaction.serializer()), value)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    @TypeConverter
    fun toStoredTransactionList(value: List<ZilTransaction>?): String? {
        return value?.let { json.encodeToString(ListSerializer(ZilTransaction.serializer()), it) }
    }
}