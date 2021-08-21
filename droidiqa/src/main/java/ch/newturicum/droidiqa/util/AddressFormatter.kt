package ch.newturicum.droidiqa.util

import ch.newturicum.droidiqa.Constants
import com.firestack.laksaj.utils.Bech32


fun String.fromBech32Address(): String {
    return if (startsWith(Constants.ZIL_HRP)) Bech32.fromBech32Address(this) else this
}

fun String.hexPrefixed(): String {
    return if (startsWith("0x")) this
    else "0x$this"
}