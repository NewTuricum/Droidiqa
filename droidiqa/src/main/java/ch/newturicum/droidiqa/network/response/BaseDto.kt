package ch.newturicum.droidiqa.network.response

internal abstract class BaseDto<T> {
    var id: String = ""
    var jsonrpc: String = ""
    var result: T? = null
    var error: Error? = null
}

internal fun BaseDto<*>.isError(): Boolean = result == null || error != null
