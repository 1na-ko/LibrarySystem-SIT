package com.library.android.network.exception;

/**
 * 业务冲突异常 — HTTP 409（如重复借阅、重复预约、ISBN 已存在）.
 *
 * <p>UI 层应展示后端 message 给用户（如"该书已被你借阅"）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BizConflictException extends ApiException {
    public BizConflictException(String serverMessage) {
        super(409, serverMessage);
    }
}
