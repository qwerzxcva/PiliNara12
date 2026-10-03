package com.example.pilinara.database

import androidx.room.*

@Dao
interface LoginAccountDao {
    @Query("SELECT * FROM login_account WHERE mid = :mid")
    suspend fun getByMid(mid: String): LoginAccountEntity?
    
    @Query("SELECT * FROM login_account")
    suspend fun getAll(): List<LoginAccountEntity>
    
    @Upsert
    suspend fun upsert(entity: LoginAccountEntity)
    
    @Delete
    suspend fun delete(entity: LoginAccountEntity)
    
    @Query("DELETE FROM login_account")
    suspend fun deleteAll()
}
