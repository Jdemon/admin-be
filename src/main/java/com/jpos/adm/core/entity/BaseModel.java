package com.jpos.adm.core.entity;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jpos.adm.core.annotation.NotEditable;
import com.jpos.adm.core.extension.ModelConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
@JsonPropertyOrder(
        {
                ModelConstant.ID
        }
)
public abstract class BaseModel extends Auditable {
    @Id
    @GeneratedValue(generator = "uuid-generator")
    @GenericGenerator(
            name = "uuid-generator",
            strategy = "com.jpos.adm.core.generator.UUIDGenerator"
    )
    @Column(length = 22, unique = true, nullable = false)
    @NotEditable
    protected String id;
}
