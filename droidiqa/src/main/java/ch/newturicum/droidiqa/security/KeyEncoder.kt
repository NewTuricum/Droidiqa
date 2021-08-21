package ch.newturicum.droidiqa.security

interface KeyEncoder {

    fun encode(value: String): String?
    fun decode(value: String): String?
}