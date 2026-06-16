package com.library.acquisition.service;

import com.library.acquisition.vo.DuplicateCheckResultVO;

public interface DuplicateCheckService {
    DuplicateCheckResultVO checkDuplicate(String isbn, String title, String author);
}
