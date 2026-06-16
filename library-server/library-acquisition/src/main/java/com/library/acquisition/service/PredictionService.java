package com.library.acquisition.service;

import com.library.acquisition.vo.PurchasePredictionVO;

import java.util.List;

public interface PredictionService {
    List<PurchasePredictionVO> predict(Long subjectId, int months);
}
