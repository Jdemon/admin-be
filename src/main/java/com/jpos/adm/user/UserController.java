package com.jpos.adm.user;

import com.jpos.adm.core.controller.AbsAdminController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UserController extends AbsAdminController<UserEntity> {
    public UserController(UserService userService) {
        super(userService);
    }
}
