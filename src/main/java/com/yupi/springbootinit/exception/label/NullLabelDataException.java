package com.yupi.springbootinit.exception.label;

import com.yupi.springbootinit.common.ErrorCode;

/**
 * @author: tangchongjie
 * @creattime: 2023--05--05 16:11
 * @description label数据为空的异常
 */
public class NullLabelDataException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    public NullLabelDataException(int code, String message) {
        super(message);
        this.code = code;
    }

    public NullLabelDataException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public NullLabelDataException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
