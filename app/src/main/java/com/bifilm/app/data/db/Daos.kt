package com.bifilm.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    /** 全量: 按胶卷分组, 同胶卷内按张编号升序. */
    @Query("SELECT * FROM projects ORDER BY filmStockId DESC, frameIndexInRoll ASC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun findById(id: String): ProjectEntity?

    /**
     * 同一胶卷里的最大张编号, 用作"下一张"计算.
     */
    @Query("SELECT MAX(frameIndexInRoll) FROM projects WHERE filmStockId = :filmStockId")
    suspend fun maxFrameIndex(filmStockId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)
}

@Dao
interface LayerDao {

    @Query("SELECT * FROM layers WHERE projectId = :projectId ORDER BY `order` ASC")
    fun observeForProject(projectId: String): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE projectId = :projectId ORDER BY `order` ASC")
    suspend fun listForProject(projectId: String): List<LayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(layer: LayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(layers: List<LayerEntity>)

    @Update
    suspend fun update(layer: LayerEntity)

    @Query("DELETE FROM layers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM layers WHERE projectId = :projectId")
    suspend fun clearForProject(projectId: String)
}
