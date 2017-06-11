package net.emaze.pongo.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.emaze.pongo.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.postgresql.ds.PGSimpleDataSource
import java.util.*

object Context {

    val port = System.getenv("PONGO_POSTGRES_PORT")?.toInt() ?: 5432
    val dataSource = PGSimpleDataSource().apply {
        user = "postgres"
        url = "jdbc:postgresql://localhost:$port/pongo"
    }
    val mapper = ObjectMapper().registerModule(KotlinModule())!!

    fun <T : Identifiable> repository(cls: Class<T>) = PostgresJsonRepository(cls, dataSource, mapper).apply {
        createTable().createIndex().deleteAll()
    }
}

class ITPostgresJsonRepository {

    data class SomeEntity(var x: Int, var y: Int) : Identifiable()

    val repository = Context.repository(SomeEntity::class.java)

    @Test
    fun itCanInsertNewEntity() {
        repository.save(SomeEntity(1, 2))
        val got = repository.findAll()
        assertEquals(1, got.size)
        assertEquals(listOf(SomeEntity(1, 2)), got)
    }

    @Test
    fun newEntityShouldHaveMetadata() {
        val entity = repository.save(SomeEntity(1, 2))
        val (got) = repository.findAll()
        assertNotNull(got.metadata)
        assertEquals(entity.metadata, got.metadata)
    }

    @Test(expected = OptimisticLockException::class)
    fun itCannotUpdateAnOldVersionOfEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        repository.save(entity.copy(x = 3).attach(entity))
        repository.save(entity.copy(x = 4).attach(entity))
    }

    @Test
    fun itCanUpdateAnExistingEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        entity.x = 3
        repository.save(entity)
        val got = repository.findAll()
        assertEquals(1, got.size)
        assertEquals(listOf(SomeEntity(3, 2)), got)
    }


    @Test
    fun itCanDeleteAnExistingEntity() {
        val entity = repository.save(SomeEntity(1, 2))
        repository.delete(entity)
        val got = repository.findAll()
        assertEquals(0, got.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun itCannotDeleteATransientEntity() {
        repository.delete(SomeEntity(1, 2))
    }


    @Test(expected = IllegalStateException::class)
    fun itCannotDeleteANotExistingEntity() {
        val entity = SomeEntity(1, 2).attach(Identifiable.Metadata(identity = 1, version = 4))
        repository.delete(entity)
    }

    @Test
    fun itCanFindAllByCriteria() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.findAll("where (data->>'x')::int < ?", 3)
        assertEquals(listOf(SomeEntity(1, 2), SomeEntity(2, 5)), got)
    }

    @Test
    fun itCanFindAllByExample() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.findAllLike(mapOf("x" to 2))
        assertEquals(listOf(SomeEntity(2, 5)), got)
    }

    @Test
    fun itCanFindFirstByCriteria() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.findFirst("where (data->>'x')::int < ?", 3)
        assertEquals(Optional.of(SomeEntity(1, 2)), got)
    }

    @Test
    fun itCanFindFirstByExample() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(1, 5))
        repository.save(SomeEntity(3, 3))
        val got = repository.findFirstLike(mapOf("x" to 1))
        assertEquals(Optional.of(SomeEntity(1, 2)), got)
    }

    @Test
    fun itCanFindNothing() {
        repository.save(SomeEntity(1, 2))
        val got = repository.findFirstLike(mapOf("x" to 10))
        assertEquals(Optional.empty<SomeEntity>(), got)
    }

    @Test
    fun itCanMapFirstByExample() {
        repository.save(SomeEntity(1, 2))
        repository.save(SomeEntity(2, 5))
        repository.mapFirstLike(mapOf("x" to 1)) { entity -> SomeEntity(entity.x, 0).attach(entity) }
        val got = repository.findFirstLike(mapOf("x" to 1))
        assertEquals(Optional.of(SomeEntity(1, 0)), got)
    }
}