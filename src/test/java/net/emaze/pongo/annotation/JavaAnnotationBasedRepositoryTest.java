package net.emaze.pongo.annotation;

import net.emaze.pongo.EntityRepository;
import net.emaze.pongo.Identifiable;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JavaAnnotationBasedRepositoryTest {

    public static class Entity extends Identifiable {
    }

    public interface Entities extends EntityRepository<Entity> {
        default void store(Entity entity) {
            save(entity);
        }
    }

    @Test
    @Ignore
    public void proxyRepositoryIsAbleToInvokeDefaultMethods() {
        final EntityRepository repository = mock(EntityRepository.class);
        final Entities proxy = AnnotatedRepository.create(repository, Entities.class);
        final Entity entity = new Entity();
        proxy.store(entity);
        verify(repository).save(entity);
    }
}