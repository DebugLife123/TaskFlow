package com.taskflow.backend.entity;

public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    // 无参构造
    public Result() {}

    // 快捷成功静态方法
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    // --- 手动生成的 Getter 和 Setter (Jackson 必须要有它们才能工作) ---
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}