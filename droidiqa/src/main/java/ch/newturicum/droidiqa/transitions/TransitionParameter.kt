package ch.newturicum.droidiqa.transitions

import ch.newturicum.droidiqa.util.DroidiqaUtils
import kotlinx.serialization.Serializable

@Serializable
data class TransitionParameter(
    val vname: String,
    val type: String,
    val value: String
) {
    companion object {
        fun from(vname: String, rawValue: Any?): TransitionParameter {
            return when (rawValue) {
                is Int, is Long -> TransitionParameter(
                    vname,
                    "Uint128",
                    rawValue.toString()
                )

                is Float, is Double -> TransitionParameter(
                    vname,
                    "Number",
                    rawValue.toString()
                )

                is String -> {
                    val type = if (DroidiqaUtils.isValidAddress(rawValue)) {
                        "ByStr20"
                    } else {
                        "String"
                    }
                    val finalValue = if (type == "ByStr20" && DroidiqaUtils.isBech32(rawValue)) {
                        DroidiqaUtils.fromBech32Address(rawValue) ?: rawValue
                    } else {
                        rawValue
                    }
                    TransitionParameter(
                        vname,
                        type,
                        finalValue
                    )
                }

                else -> TransitionParameter(
                    vname,
                    "",
                    rawValue?.toString() ?: ""
                )
            }
        }
    }
}