package ch.newturicum.droidiqa.network.response

internal data class Error(
    val code: Int,
    val data: String?,
    var message: String = ""
)