package com.jpos.adm.core.configuration;

import com.jpos.adm.core.exception.NotAuthorizedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        HttpServletRequest request =
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))
                        .getRequest();

        String userId = request.getHeader("x-user-id");

//        if(StringUtils.isBlank(userId)) {
//            throw new NotAuthorizedException();
//        }
        return Optional.of("system");
    }

}
