package io.hops.hopsworks.persistence.entity.util;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertManagerConfigEntity;

public class EntityUtils {
    public static int hashCode(Integer id) {
        return (id != null ? id.hashCode() : 0);
    }

    public static boolean equals(AlertManagerConfigEntity entity, AlertManagerConfigEntity other) {
        return (entity.getId() != null && entity.getId().equals(other.getId()));
    }
}
