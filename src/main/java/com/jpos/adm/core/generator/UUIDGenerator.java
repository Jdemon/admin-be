package com.jpos.adm.core.generator;

import com.github.f4b6a3.uuid.UuidCreator;
import com.jpos.adm.core.base62.Base62;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.spi.Configurable;

import java.io.Serializable;
import java.util.Map;

public class UUIDGenerator implements IdentifierGenerator, Configurable {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object o) throws HibernateException {
        return Base62.encodeUUID(UuidCreator.getTimeOrderedWithRandom());
    }

    @Override
    public void configure(Map map) {
    }
}
