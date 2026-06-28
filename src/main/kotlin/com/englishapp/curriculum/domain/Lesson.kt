package com.englishapp.curriculum.domain

import com.fasterxml.jackson.databind.JsonNode
import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "lessons")
class Lesson(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "module_id", nullable = false, columnDefinition = "uuid")
    val moduleId: UUID,

    @Column(nullable = false)
    var title: String,

    @Column(name = "order_index", nullable = false)
    var orderIndex: Int,

    @Column(name = "estimated_minutes", nullable = false)
    var estimatedMinutes: Int = 15,

    @Type(JsonType::class)
    @Column(name = "content_jsonb", nullable = false, columnDefinition = "jsonb")
    var content: JsonNode,

    @Column(name = "is_published", nullable = false)
    var isPublished: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
