package ch.newturicum.droidiqa.dto

import kotlinx.serialization.Serializable

@Serializable
data class ZilAccount(
    val name: String,
    val address: String,
    val zilBalance: Double
) : Comparable<ZilAccount> {
    override fun compareTo(other: ZilAccount): Int {
        return name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ZilAccount) {
            return address == other.address
        }
        return false
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}