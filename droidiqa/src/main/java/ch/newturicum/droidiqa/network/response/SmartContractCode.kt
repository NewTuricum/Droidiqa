package ch.newturicum.droidiqa.network.response

import ch.newturicum.droidiqa.dto.response.ZilSmartContractCode

internal data class SmartContractCodeResult(
    val code: String
)

internal class SmartContractCode : BaseDto<SmartContractCodeResult>()


internal fun SmartContractCode.getCode(): String? = if (isError()) null else result?.code

internal fun SmartContractCode.asZilSmartContractCode(): ZilSmartContractCode {
    return ZilSmartContractCode(
        code = result?.code ?: ""
    )
}