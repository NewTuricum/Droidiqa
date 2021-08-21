package ch.newturicum.droidiqa.database

import androidx.room.TypeConverter
import ch.newturicum.droidiqa.database.dao.AccountContainer
import ch.newturicum.droidiqa.database.dao.TokenContainer
import ch.newturicum.droidiqa.dto.ZilTransaction
import ch.newturicum.droidiqa.network.ZilNetwork
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

internal class Converters {

    private val gson = Gson()

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
        return if (value == null) null else ZilNetwork.valueOf(value)
    }

    @TypeConverter
    fun toStoredZilNetwork(value: ZilNetwork?): String? {
        return value?.name
    }

    @TypeConverter
    fun fromStoredStringMap(value: String?): Map<String, String> {
        value?.let {
            val type: Type = object : TypeToken<Map<String, String>>() {}.type
            return gson.fromJson(value, type)
        }
        return emptyMap()
    }

    @TypeConverter
    fun toStoredStringMap(value: Map<String, String>?): String? {
        value?.let {
            return gson.toJson(it)
        }
        return null
    }

    @TypeConverter
    fun fromStoredZilAccountList(value: String?): AccountContainer {
        value?.let {
            val type: Type = object : TypeToken<AccountContainer>() {}.type
            return gson.fromJson(value, type)
        }
        return AccountContainer(mutableListOf())
    }

    @TypeConverter
    fun toStoredZilAccountList(value: AccountContainer?): String? {
        value?.let {
            return gson.toJson(it)
        }
        return null
    }

    @TypeConverter
    fun fromStoredZilTokenList(value: String?): TokenContainer {
        value?.let {
            val type: Type = object : TypeToken<TokenContainer>() {}.type
            return gson.fromJson(value, type)
        }
        return TokenContainer(mutableListOf())
    }

    @TypeConverter
    fun toStoredZilTokenList(value: TokenContainer?): String? {
        value?.let {
            return gson.toJson(it)
        }
        return null
    }

    @TypeConverter
    fun fromStoredTransaction(value: String?): ZilTransaction? {
        val type: Type = object : TypeToken<ZilTransaction>() {}.type
        return if (value == null) null else gson.fromJson(value, type)
    }

    @TypeConverter
    fun toStoredTransaction(value: ZilTransaction?): String? {
        return if (value == null) null else gson.toJson(value)
    }

    @TypeConverter
    fun fromStoredTransactionList(value: String?): List<ZilTransaction> {
        val type: Type = object : TypeToken<List<ZilTransaction>>() {}.type
        return if (value == null) emptyList() else gson.fromJson(value, type)
    }

    @TypeConverter
    fun toStoredTransactionList(value: List<ZilTransaction>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
}