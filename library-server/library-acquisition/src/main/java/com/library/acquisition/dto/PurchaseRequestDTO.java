package com.library.acquisition.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 采购查重请求 DTO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
public class PurchaseRequestDTO {
    private String isbn;
    @NotBlank(message = "书名不能为空")
    private String title;
    private String author;
}
