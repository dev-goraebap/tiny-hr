package com.example.tinyhr.shared.kernel;

/**
 * 성공 응답 공통 봉투(envelope). 모든 성공 응답 바디는 {@code { "data": ... }} 형태로 내려간다.
 *
 * <p>에러는 이 봉투를 쓰지 않는다. HTTP 상태코드와 {@code { code, message }} 로 내려가며,
 * {@code GlobalExceptionHandler} 가 담당한다.
 *
 * @param data 실제 페이로드(ViewModel, 목록, 식별자 등)
 */
public record ApiResponse<T>(T data) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
