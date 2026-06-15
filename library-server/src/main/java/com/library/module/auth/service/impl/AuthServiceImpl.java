package com.library.module.auth.service.impl;

import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.service.AuthService;
import com.library.module.auth.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Override
    public void register(RegisterDTO dto) {
        // TODO: 实现用户注册逻辑
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // TODO: 实现登录逻辑
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        // TODO: 实现Token刷新
        throw new UnsupportedOperationException("待实现");
    }

    @Override
    public void logout() {
        // TODO: 实现登出逻辑
    }
}
