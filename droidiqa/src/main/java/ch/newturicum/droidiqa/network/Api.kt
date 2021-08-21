package ch.newturicum.droidiqa.network

internal object ZILLIQA {
    const val API_ID = "1"
    const val JSON_RPC = "2.0"
    const val API_ROOT_MAIN = "https://api.zilliqa.com/"
    const val API_ROOT_TEST = "https://dev-api.zilliqa.com/"

    object METHOD {
        const val BLOCKCHAIN_INFO = "GetBlockchainInfo"
        const val SMARTCONTRACT_CODE = "GetSmartContractCode"
        const val SMARTCONTRACT_STATE = "GetSmartContractState"
        const val SMARTCONTRACT_INIT = "GetSmartContractInit"
        const val CREATE_TRANSACTION = "CreateTransaction"
        const val ACCOUNT_BALANCE = "GetBalance"
        const val GET_TRANSACTION = "GetTransaction"
        const val MIN_GAS_PRICE = "GetMinimumGasPrice"
        const val NETWORK_ID = "GetNetworkId"
    }

    object FIELD {
        const val RESULT = "result"
        const val CODE = "code"
        const val SMARTCONTRACTINIT_NAME = "name"
        const val SMARTCONTRACTINIT_SYMBOL = "symbol"
        const val SMARTCONTRACTINIT_INITSUPPLY = "init_supply"
        const val SMARTCONTRACTINIT_DECIMALS = "decimals"
        const val SMARTCONTRACTINIT_ADDRESS = "_this_address"
    }
}