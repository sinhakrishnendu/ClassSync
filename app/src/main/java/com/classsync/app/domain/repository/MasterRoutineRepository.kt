package com.classsync.app.domain.repository

import com.classsync.app.domain.master.MasterRoutineData
import com.classsync.app.domain.master.MasterRoutineSummary
import kotlinx.coroutines.flow.Flow

interface MasterRoutineRepository {
    fun observeSummaries(): Flow<List<MasterRoutineSummary>>
    fun observeRoutine(id: String): Flow<MasterRoutineData?>
    suspend fun getRoutine(id: String): MasterRoutineData?
    suspend fun save(data: MasterRoutineData)
    suspend fun delete(id: String)
    suspend fun deleteAll()
}
