package com.library.kg.vo;

import com.library.core.vo.BookSimpleVO;
import com.library.kg.model.TracePath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文献溯源图 VO.
 * <p>
 * 对应 OpenAPI {@code TraceGraph} Schema，
 * 以起始图书和一组溯源路径构成文献演变关系图。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceGraphVO {

    /** 溯源起始图书 */
    private BookSimpleVO sourceBook;

    /** 溯源路径列表 */
    private List<TracePath> paths;
}
