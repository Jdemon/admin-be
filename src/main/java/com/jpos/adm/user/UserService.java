package com.jpos.adm.user;

import com.jpos.adm.core.service.AbsAdminService;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbsAdminService<UserEntity> {
    public UserService(UserRepository repository) {
        super(repository);
    }
}
