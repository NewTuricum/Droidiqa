package ch.newturicum.droidiqa.dto

import ch.newturicum.droidiqa.database.dao.ZilContactEntity

data class ZilContact(
    val address: String,
    val name: String
) : Comparable<ZilContact> {

    override fun equals(other: Any?): Boolean {
        return if (other is ZilContactEntity) {
            address.compareTo(other.address) == 0
        } else false
    }

    override fun compareTo(other: ZilContact): Int {
        return name.compareTo(other.name)
    }
}