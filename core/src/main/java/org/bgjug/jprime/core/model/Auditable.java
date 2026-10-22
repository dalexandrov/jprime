package org.bgjug.jprime.core.model;

import java.time.LocalDateTime;

public interface Auditable {

    String getCreatedBy();

    void setCreatedBy(String createdBy);

    LocalDateTime getCreatedDate();

    void setCreatedDate(LocalDateTime createdDate);

    String getLastModifiedBy();

    void setLastModifiedBy(String lastModifiedBy);

    LocalDateTime getLastModifiedDate();

    void setLastModifiedDate(LocalDateTime lastModifiedDate);
}
