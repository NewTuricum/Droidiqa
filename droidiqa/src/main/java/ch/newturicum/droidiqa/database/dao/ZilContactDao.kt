package ch.newturicum.droidiqa.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ch.newturicum.droidiqa.util.DroidiqaUtils
import kotlinx.coroutines.runBlocking

@Dao
internal interface ZilContactDao {
    @Query("SELECT * FROM zilcontactentity ORDER BY name ASC ")
    fun getAll(): LiveData<List<ZilContactEntity>>

    @Query("SELECT * FROM zilcontactentity WHERE name LIKE :username LIMIT 1")
    fun findByName(username: String): ZilContactEntity?

    @Query("SELECT * FROM zilcontactentity WHERE address LIKE :useraddress LIMIT 1")
    fun findByAddress(useraddress: String): ZilContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg zilContact: ZilContactEntity)

    @Delete
    suspend fun delete(zilContact: ZilContactEntity)

    @Update
    suspend fun update(zilContact: ZilContactEntity)
}
/*
fun ZilContactDao.newOrUpdateExisting(name: String, addressBech32: String): ZilContact {
    require(name.isNotEmpty() && ZilUtils.isValidBech32Address(addressBech32))
    return runBlocking {
        val existingEntry = findByAddress(addressBech32.trim())
        if (existingEntry == null) {
            ZilContact(address = addressBech32.trim(), name = name.trim()).apply {
                insertAll(this)
                return@runBlocking this
            }
        } else {
            ZilContact(address = existingEntry.address, name = name.trim()).apply {
                update(this)
                return@runBlocking this
            }
        }
    }
}*/

internal fun ZilContactDao.safeInsert(contact: ZilContactEntity) {
    require(contact.name.isNotEmpty() && DroidiqaUtils.isValidBech32Address(contact.address))
    runBlocking {
        val existingEntry = findByAddress(contact.address)
        if (existingEntry == null) {
            insertAll(contact)
        }
    }
}