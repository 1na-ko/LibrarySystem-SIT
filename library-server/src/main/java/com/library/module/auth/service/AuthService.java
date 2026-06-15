package com.library.module.auth.service;

import com.library.module.auth.dto.LoginDTO;
import com.library.module.auth.dto.RegisterDTO;
import com.library.module.auth.vo.LoginVO;

public interface AuthService {
    void register(RegisterDTO dto);
    LoginVO login(LoginDTO dto);
    LoginVO refresh(String refreshToken);
    void logout();
}
