package com.jpos.adm.user;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jpos.adm.core.entity.BaseModel;
import com.jpos.adm.core.extension.ModelConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "users"
)
@JsonPropertyOrder(
        {
                ModelConstant.ID,
                "userName",
                ModelConstant.CREATED_BY,
                ModelConstant.CREATED_AT,
                ModelConstant.UPDATED_BY,
                ModelConstant.UPDATED_AT
        }
)
public class UserEntity extends BaseModel {
    @Column(nullable = false, length = 20)
    private String userName;
}
