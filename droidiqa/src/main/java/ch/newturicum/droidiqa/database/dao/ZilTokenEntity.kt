package ch.newturicum.droidiqa.database.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.newturicum.droidiqa.dto.ZilToken
import ch.newturicum.droidiqa.util.DroidiqaUtils
import ch.newturicum.droidiqa.util.fromBech32Address
import ch.newturicum.droidiqa.util.hexPrefixed
import java.util.*

@Entity
internal data class ZilTokenEntity(
    @PrimaryKey var contractAddress: String,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "symbol") var symbol: String,
    @ColumnInfo(name = "decimalPlaces") var decimalPlaces: Int,
    @ColumnInfo(name = "supply") var supply: Long,
    @ColumnInfo(name = "balances") var balances: Map<String, String>
) : Comparable<ZilTokenEntity> {
    override fun compareTo(other: ZilTokenEntity): Int = symbol.compareTo(other.symbol)
    override fun equals(other: Any?): Boolean {
        return if (other is ZilTokenEntity) compareTo(other) == 0 else
            super.equals(other)
    }

    companion object {
        fun fromZilToken(token: ZilToken): ZilTokenEntity {
            token.apply {
                return ZilTokenEntity(
                    address, name, symbol, decimalPlaces, supply, balances
                )
            }
        }
    }

    fun toZilToken(): ZilToken {
        return ZilToken(
            name = name,
            symbol = symbol,
            address = contractAddress,
            decimalPlaces = decimalPlaces,
            supply = supply,
            balances = balances
        )
    }

    fun isValid(): Boolean = DroidiqaUtils.isValidAddress(contractAddress)
}

internal fun ZilTokenEntity.getBalanceForAccount(account: ZilAccountEntity): Double =
    getBalanceForAccount(account.address)

internal fun ZilTokenEntity.getBalanceForAccount(accountAddress: String): Double {
    balances[accountAddress.fromBech32Address().lowercase(Locale.ENGLISH).hexPrefixed()]?.let {
        if (it.isNotEmpty()) {
            return it.toDouble()
        }
    }
    return 0.0
}