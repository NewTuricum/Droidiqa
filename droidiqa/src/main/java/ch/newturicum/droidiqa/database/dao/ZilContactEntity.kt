package ch.newturicum.droidiqa.database.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.newturicum.droidiqa.dto.ZilContact
import java.io.Serializable

@Entity
internal data class ZilContactEntity(
    @PrimaryKey val address: String,
    @ColumnInfo(name = "name") val name: String
) : Serializable, Comparable<ZilContactEntity> {

    companion object {
        fun fromZilContact(contact: ZilContact): ZilContactEntity {
            return ZilContactEntity(contact.address, contact.name)
        }
    }

    fun toZilContact(): ZilContact {
        return ZilContact(address, name)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ZilContactEntity) {
            address.compareTo(other.address) == 0
        } else false
    }

    override fun compareTo(other: ZilContactEntity): Int {
        return name.compareTo(other.name)
    }
}
