package com.ptbox.osint.repository

import com.ptbox.osint.exception.NotFoundException
import com.ptbox.osint.model.Scan
import org.springframework.stereotype.Repository

@Repository
class ScanRepositoryImpl(
    private val repository: ScanPersistenceRepository
) : ScanRepository {
    override fun save(scan: Scan): Scan {
        return repository.save(scan)
    }

    override fun findById(scanId: Long): Scan {
        return repository.findById(scanId).orElseThrow {
            NotFoundException("scan not found by id: $scanId")
        }
    }

    override fun findAllByOrderByDisplayOrderAsc(): List<Scan> {
        return repository.findAllByOrderByDisplayOrderAsc()
    }

    override fun updateDisplayOrder(scanId: Long, newOrder: Int) {
        repository.updateDisplayOrder(scanId, newOrder)
    }
}
