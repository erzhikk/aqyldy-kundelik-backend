package kz.aqyldykundelik.classes.repo

import kz.aqyldykundelik.classes.domain.ClassEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface ClassRepository : JpaRepository<ClassEntity, UUID> {
    fun findByCode(code: String): ClassEntity?
    fun findByLangType(langType: String, pageable: Pageable): Page<ClassEntity>
    fun findByClassTeacherId(classTeacherId: UUID, pageable: Pageable): Page<ClassEntity>

    @Query(
        value = """
            select *
            from school_class
            order by
                cast(nullif(regexp_replace(code, E'\\D', '', 'g'), '') as int),
                regexp_replace(code, E'\\d', '', 'g'),
                code
        """,
        countQuery = "select count(*) from school_class",
        nativeQuery = true
    )
    fun findAllOrderByGradeAndLetter(pageable: Pageable): Page<ClassEntity>

    @Query(
        value = """
            select *
            from school_class
            where lang_type = :langType
            order by
                cast(nullif(regexp_replace(code, E'\\D', '', 'g'), '') as int),
                regexp_replace(code, E'\\d', '', 'g'),
                code
        """,
        countQuery = "select count(*) from school_class where lang_type = :langType",
        nativeQuery = true
    )
    fun findByLangTypeOrderByGradeAndLetter(langType: String, pageable: Pageable): Page<ClassEntity>

    @Query(
        value = """
            select *
            from school_class
            where class_teacher_id = :classTeacherId
            order by
                cast(nullif(regexp_replace(code, E'\\D', '', 'g'), '') as int),
                regexp_replace(code, E'\\d', '', 'g'),
                code
        """,
        countQuery = "select count(*) from school_class where class_teacher_id = :classTeacherId",
        nativeQuery = true
    )
    fun findByClassTeacherIdOrderByGradeAndLetter(classTeacherId: UUID, pageable: Pageable): Page<ClassEntity>

    @Query(
        value = """
            select *
            from school_class
            order by
                cast(nullif(regexp_replace(code, E'\\D', '', 'g'), '') as int),
                regexp_replace(code, E'\\d', '', 'g'),
                code
        """,
        nativeQuery = true
    )
    fun findAllOrderByGradeAndLetter(): List<ClassEntity>
}
