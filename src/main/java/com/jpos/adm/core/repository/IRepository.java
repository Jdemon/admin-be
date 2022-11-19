package com.jpos.adm.core.repository;

import com.jpos.adm.core.entity.BaseModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IRepository<T extends BaseModel> extends JpaRepository<T,String>, JpaSpecificationExecutor<T> {
}
