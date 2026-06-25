package com.library.acquisition.dto;

import lombok.Data;

/**
 * 采购查重请求 DTO.
 * <p>isbn / title 至少提供一个（ISBN 精确查重或标题模糊查重），
 * 由 Service 层校验，避免强制 title 致 ISBN-only 查重不可用.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
public class PurchaseRequestDTO {
    private String isbn;
    private String title;
    private String author;
}
