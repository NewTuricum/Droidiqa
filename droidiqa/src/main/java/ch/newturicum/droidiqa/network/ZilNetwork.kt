package ch.newturicum.droidiqa.network

import ch.newturicum.droidiqa.Constants

enum class ZilNetwork {
    MAIN,
    TEST
}

fun ZilNetwork.getRoot(): String {
    return when (this) {
        ZilNetwork.TEST -> ZILLIQA.API_ROOT_TEST
        ZilNetwork.MAIN -> ZILLIQA.API_ROOT_MAIN
    }
}

fun ZilNetwork.getChainId(): Int {
    return when (this) {
        ZilNetwork.TEST -> Constants.ZIL_CHAINID_TEST
        ZilNetwork.MAIN -> Constants.ZIL_CHAINID_MAIN
    }
}