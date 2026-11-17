package br.com.curralzap.backend.shared.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EntityScan(basePackageClasses = TestPersistenceEntity.class)
@EnableJpaRepositories(basePackageClasses = TestPersistenceEntityRepository.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PersistenceConventionsTest {

    @Autowired
    private TestPersistenceEntityRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void uuidBaseEntityDefinesIdColumnAndGenerationStrategy() throws NoSuchFieldException {
        assertThat(AbstractUuidJpaEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();

        Field id = AbstractUuidJpaEntity.class.getDeclaredField("id");

        assertThat(id.getType()).isEqualTo(UUID.class);
        assertThat(id.isAnnotationPresent(Id.class)).isTrue();
        assertThat(id.getAnnotation(GeneratedValue.class).strategy()).isEqualTo(GenerationType.UUID);
        assertThat(id.getAnnotation(Column.class).name()).isEqualTo(PersistenceConventions.ID);
        assertThat(id.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(id.getAnnotation(Column.class).updatable()).isFalse();
    }

    @Test
    void auditableBaseEntityDefinesAuditColumnsAsMappedSuperclass() throws NoSuchFieldException {
        assertThat(AbstractAuditableJpaEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();
        assertThat(AbstractAuditableJpaEntity.class.getAnnotation(EntityListeners.class).value())
                .containsExactly(PersistenceAuditListener.class);

        Field createdAt = AbstractAuditableJpaEntity.class.getDeclaredField("createdAt");
        Field updatedAt = AbstractAuditableJpaEntity.class.getDeclaredField("updatedAt");

        assertThat(createdAt.getAnnotation(Column.class).name()).isEqualTo(PersistenceConventions.CREATED_AT);
        assertThat(createdAt.getAnnotation(Column.class).nullable()).isFalse();
        assertThat(createdAt.getAnnotation(Column.class).updatable()).isFalse();

        assertThat(updatedAt.getAnnotation(Column.class).name()).isEqualTo(PersistenceConventions.UPDATED_AT);
        assertThat(updatedAt.getAnnotation(Column.class).nullable()).isFalse();
    }

    @Test
    void farmScopedBaseEntityDefinesExplicitFarmIdColumn() throws NoSuchFieldException {
        assertThat(AbstractFarmScopedAuditableJpaEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();

        Field farmId = AbstractFarmScopedAuditableJpaEntity.class.getDeclaredField("farmId");
        assertThat(farmId.getType()).isEqualTo(UUID.class);
        assertThat(farmId.getAnnotation(Column.class).name()).isEqualTo(PersistenceConventions.FARM_ID);
        assertThat(farmId.getAnnotation(Column.class).nullable()).isFalse();
    }

    @Test
    void softDeleteBaseEntityIsSelectiveAndUsesDeletedAt() throws NoSuchFieldException {
        assertThat(AbstractSoftDeletableFarmScopedAuditableJpaEntity.class.isAnnotationPresent(MappedSuperclass.class)).isTrue();

        Field deletedAt = AbstractSoftDeletableFarmScopedAuditableJpaEntity.class.getDeclaredField("deletedAt");
        assertThat(deletedAt.getType()).isEqualTo(OffsetDateTime.class);
        assertThat(deletedAt.getAnnotation(Column.class).name()).isEqualTo(PersistenceConventions.DELETED_AT);
    }

    @Test
    void auditListenerSetsCreatedAndUpdatedTimestampsInUtc() {
        TestFarmEntity entity = new TestFarmEntity();
        PersistenceAuditListener listener = new PersistenceAuditListener();

        listener.onPrePersist(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(entity.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void auditListenerRefreshesUpdatedAtWithoutChangingCreatedAt() {
        TestFarmEntity entity = new TestFarmEntity();
        PersistenceAuditListener listener = new PersistenceAuditListener();

        listener.onPrePersist(entity);
        OffsetDateTime createdAt = entity.getCreatedAt();
        OffsetDateTime firstUpdatedAt = entity.getUpdatedAt();

        listener.onPreUpdate(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    void persistenceHelpersExposeSelectiveSoftDeleteConventions() {
        assertThat(PersistenceConventions.newUuid()).isNotNull();

        TestSoftDeleteEntity entity = new TestSoftDeleteEntity();
        assertThat(entity.isDeleted()).isFalse();

        entity.markDeletedForTest(PersistenceConventions.nowUtc());

        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void persistedEntityReceivesUuidAndAuditTimestamps() {
        TestPersistenceEntity entity = new TestPersistenceEntity(UUID.randomUUID(), "Lote 01");

        TestPersistenceEntity persisted = repository.saveAndFlush(entity);

        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull();
        assertThat(persisted.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(persisted.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);

        OffsetDateTime createdAt = persisted.getCreatedAt();
        OffsetDateTime firstUpdatedAt = persisted.getUpdatedAt();

        persisted.rename("Lote 02");
        TestPersistenceEntity updated = repository.saveAndFlush(persisted);

        assertThat(updated.getId()).isEqualTo(persisted.getId());
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(firstUpdatedAt);
    }

    @Test
    void realEntityMappingUsesExplicitSnakeCaseTableAndColumnNames() {
        List<String> columnNames = jdbcTemplate.queryForList(
                """
                        select column_name
                        from information_schema.columns
                        where table_name = 'TEST_PERSISTENCE_ENTITY'
                        order by ordinal_position
                        """,
                String.class
        );

        assertThat(columnNames)
                .containsExactlyInAnyOrder("ID", "CREATED_AT", "UPDATED_AT", "FARM_ID", "DELETED_AT", "DISPLAY_NAME");
    }

    private static final class TestFarmEntity extends AbstractFarmScopedAuditableJpaEntity {
    }

    private static final class TestSoftDeleteEntity extends AbstractSoftDeletableFarmScopedAuditableJpaEntity {

        void markDeletedForTest(OffsetDateTime deletedAt) {
            markDeleted(deletedAt);
        }
    }
}
