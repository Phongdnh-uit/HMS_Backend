package com.hms.common.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface SimpleRepository<E,ID> extends JpaRepository<E,ID>, JpaSpecificationExecutor<E> {
}
