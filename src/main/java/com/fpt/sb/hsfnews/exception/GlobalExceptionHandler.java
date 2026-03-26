package com.fpt.sb.hsfnews.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi khi dung lượng file vượt quá giới hạn cấu hình (MaxUploadSizeExceededException)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Request size exceeded limit (10MB). Please reduce thumbnail or content image size.");
        return "redirect:/admin/articles/new";
    }

    /**
     * Xử lý các lỗi liên quan đến Multipart (Upload file) nói chung
     */
    @ExceptionHandler(MultipartException.class)
    public String handleMultipartException(MultipartException ex, RedirectAttributes redirectAttributes) {
        if (ex.getCause() instanceof MaxUploadSizeExceededException) {
            redirectAttributes.addFlashAttribute("error", "File size too large. Please upload a smaller file.");
            return "redirect:/admin/articles/new";
        }
        redirectAttributes.addFlashAttribute("error", "File upload error: " + ex.getMessage());
        return "redirect:/admin/articles/new";
    }

    /**
     * Xử lý lỗi Runtime (Lỗi logic trong quá trình chạy)
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    /**
     * Xử lý tất cả các loại ngoại lệ chưa được khai báo cụ thể (Generic Exception)
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("error", "An unexpected error occurred: " + ex.getMessage());
        return "error";
    }
}