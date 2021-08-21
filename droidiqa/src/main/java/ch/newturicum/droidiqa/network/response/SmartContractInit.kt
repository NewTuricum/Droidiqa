package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.database.dao.ZilTokenEntity
import ch.newturicum.droidiqa.dto.response.ZilSmartContractInit
import ch.newturicum.droidiqa.network.ZILLIQA.FIELD.SMARTCONTRACTINIT_ADDRESS
import ch.newturicum.droidiqa.network.ZILLIQA.FIELD.SMARTCONTRACTINIT_DECIMALS
import ch.newturicum.droidiqa.network.ZILLIQA.FIELD.SMARTCONTRACTINIT_INITSUPPLY
import ch.newturicum.droidiqa.network.ZILLIQA.FIELD.SMARTCONTRACTINIT_NAME
import ch.newturicum.droidiqa.network.ZILLIQA.FIELD.SMARTCONTRACTINIT_SYMBOL
import ch.newturicum.droidiqa.transitions.TransitionParameter
import ch.newturicum.droidiqa.util.DroidiqaUtils

internal class SmartContractInit : BaseDto<List<TransitionParameter>>() {

    private fun getName(): String? = getValueAsString(SMARTCONTRACTINIT_NAME)
    private fun getSymbol(): String? = getValueAsString(SMARTCONTRACTINIT_SYMBOL)
    private fun getInitialSupply(): Long? = getValueAsString(SMARTCONTRACTINIT_INITSUPPLY)?.toLong()
    private fun getDecimals(): Int? = getValueAsString(SMARTCONTRACTINIT_DECIMALS)?.toInt()
    private fun getAddress(): String? = getValueAsString(SMARTCONTRACTINIT_ADDRESS)
    private fun getAddressBech32(): String? {
        getAddress()?.let { address ->
            return DroidiqaUtils.toBech32Address(address)
        }
        return null
    }

    fun getValueAsString(key: String): String? {
        return result?.findLast { it.vname == key }?.value?.toString()
    }

    fun getValue(key: String): Any? {
        return result?.findLast { it.vname == key }?.value
    }

    fun asZilToken(): ZilTokenEntity {
        return ZilTokenEntity(
            contractAddress = getAddressBech32() ?: "",
            name = getName() ?: "",
            decimalPlaces = getDecimals() ?: 0,
            symbol = getSymbol() ?: "",
            supply = getInitialSupply() ?: 0,
            balances = emptyMap()
        )
    }

    fun asZilSmartContractInit(): ZilSmartContractInit {
        return ZilSmartContractInit(
            contractAddress = getAddressBech32() ?: "",
            name = getName() ?: "",
            decimalPlaces = getDecimals() ?: 0,
            symbol = getSymbol() ?: "",
            supply = getInitialSupply() ?: 0,
            balances = emptyMap()
        )
    }
}
