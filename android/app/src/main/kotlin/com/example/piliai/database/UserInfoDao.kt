package com.example.piliai.database

import androidx.room.*

@Dao
interface UserInfoDao {
    @Query("SELECT * FROM user_info WHERE key = :key")
    suspend fun getByKey(key: String): UserInfoEntity?
    
    @Upsert
    suspend fun upsert(entity: UserInfoEntity)
    
    @Delete
    suspend fun delete(entity: UserInfoEntity)
    
    @Query("SELECT * FROM user_info")
    suspend fun getAll(): List<UserInfoEntity>
}
