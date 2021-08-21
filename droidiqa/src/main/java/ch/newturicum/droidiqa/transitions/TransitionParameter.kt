package ch.newturicum.droidiqa.transitions

import ch.newturicum.droidiqa.util.DroidiqaUtils

/**
 * Created by dzorn on 31.05.21.
 */

data class TransitionParameter(
    val vname: String,
    var type: String? = null,
    var value: Any
) {

    init {
        if (value is Int || value is Long) {
            type = "Uint128"
            value = value.toString()
        } else if (value is Float || value is Double) {
            type = "Number"
        } else if (value is String) {
            (value as String).let {
                if (DroidiqaUtils.isValidAddress(it)) {
                    type = "ByStr20"
                    if (DroidiqaUtils.isBech32(it)) {
                        value = DroidiqaUtils.fromBech32Address(it) ?: value
                    }
                } else {
                    type = "String"
                }
            }
        } else {
            type = ""
        }
    }
}