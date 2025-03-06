package com.ptbox.osint.model

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(name = "scans")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class Scan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val domain: String,

    @Column(nullable = false)
    val tool: String,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "end_time")
    var endTime: LocalDateTime? = null,

    @Column(nullable = false)
    var status: ScanStatus = ScanStatus.PENDING,

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    var results: Map<String, Any>? = null,

    @Column(name = "display_order")
    var displayOrder: Int = 0,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
