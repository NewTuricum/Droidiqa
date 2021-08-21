package ch.newturicum.droidiqa.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import ch.newturicum.droidiqa.network.ZilNetwork

@Dao
internal interface ZilWalletDao {
    @Query("SELECT * FROM zilwalletentity")
    fun getAll(): LiveData<ZilWalletEntity>

    @Query("SELECT * FROM zilwalletentity WHERE network LIKE :network LIMIT 1")
    fun getFor(network: ZilNetwork): LiveData<ZilWalletEntity>

    @Query("SELECT * FROM zilwalletentity WHERE network LIKE :network LIMIT 1")
    suspend fun getDataFor(network: ZilNetwork): ZilWalletEntity?

    @Query("SELECT activeAccountAddress FROM zilwalletentity WHERE network LIKE :network LIMIT 1")
    fun getActiveAccount(network: ZilNetwork): LiveData<String>

    @RewriteQueriesToDropUnusedColumns
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT accounts FROM zilwalletentity WHERE network LIKE :network LIMIT 1")
    fun getAccounts(network: ZilNetwork): LiveData<AccountContainer>

    @Query("SELECT tokens FROM zilwalletentity WHERE network LIKE :network LIMIT 1")
    fun getTokens(network: ZilNetwork): LiveData<TokenContainer>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(vararg zilWallet: ZilWalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg zilWallet: ZilWalletEntity)

    @Delete
    suspend fun delete(zilWallet: ZilWalletEntity)

    @Update
    suspend fun update(zilWallet: ZilWalletEntity)

}
