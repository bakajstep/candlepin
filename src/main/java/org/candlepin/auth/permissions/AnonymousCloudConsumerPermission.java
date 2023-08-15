package org.candlepin.auth.permissions;

import java.util.Objects;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import org.candlepin.auth.Access;
import org.candlepin.auth.SubResource;
import org.candlepin.model.AnonymousCloudConsumer;
import org.candlepin.model.AnonymousCloudConsumer_;
import org.candlepin.model.Owner;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

// TODO: Java Docs
public class AnonymousCloudConsumerPermission extends TypedPermission<AnonymousCloudConsumer> {

    private AnonymousCloudConsumer consumer;

    public AnonymousCloudConsumerPermission(AnonymousCloudConsumer consumer) {
        this.consumer = Objects.requireNonNull(consumer);
    }

    @Override
    public Criterion getCriteriaRestrictions(Class entityClass) {
        if (AnonymousCloudConsumer.class.equals(entityClass)) {
            return Restrictions.idEq(consumer.getId());
        }

        return null;
    }

    @Override
    public <T> Predicate getQueryRestriction(Class<T> entityClass,
        CriteriaBuilder builder, From<?, T> path) {
        if (AnonymousCloudConsumer.class.equals(entityClass)) {
            return builder.equal(((From<?, AnonymousCloudConsumer>) path).get(AnonymousCloudConsumer_.id), this.getAnonymousCloudConsumer().getId());
        }

        return null;
    }

    @Override
    public Owner getOwner() {
        return null;
    }

    @Override
    public Class<AnonymousCloudConsumer> getTargetType() {
        return AnonymousCloudConsumer.class;
    }

    @Override
    public boolean canAccessTarget(AnonymousCloudConsumer target,
        SubResource subResource, Access action) {
            if (target == null) {
                return false;
            }

            return this.consumer.getUuid().equals(target.getUuid());
    }
    
    public AnonymousCloudConsumer getAnonymousCloudConsumer() {
        return this.consumer;
    }
}
