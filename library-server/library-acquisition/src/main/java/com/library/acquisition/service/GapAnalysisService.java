package com.library.acquisition.service;

import com.library.acquisition.vo.GapAnalysisResultVO;

public interface GapAnalysisService {
    GapAnalysisResultVO analyze(Long subjectId);
}
