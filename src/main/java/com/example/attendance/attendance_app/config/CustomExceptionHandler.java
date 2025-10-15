package com.example.attendance.attendance_app.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * アプリケーション全体で発生する例外を一元的に処理するためのグローバルな例外ハンドラです。
 * {@code @ControllerAdvice} を使用し、標準化されたレスポンスを返却します。
 */
@ControllerAdvice
public class CustomExceptionHandler {

    /**
     * IllegalStateException がスローされた場合に処理します.
     * 
     * 【機能】
     * HTTPステータスコード 400 Bad Request と、例外メッセージをレスポンスボディとして返却します。
     * 
     * 【注意事項】
     * 外部APIとの連携エラーや、業務ロジック上の不正な状態遷移時に利用されます。
     * 
     * @param ex 捕捉された IllegalStateException オブジェクト
     * @return ステータスコード 400 とエラーメッセージを含む ResponseEntity<String>
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException ex) {
        // Return a 400 Bad Request status with the exception message
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}