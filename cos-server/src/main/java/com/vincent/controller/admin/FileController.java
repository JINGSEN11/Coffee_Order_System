package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.service.AliOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传。商品、分类、Banner 等多处编辑页共用，
 * 没有单一权限码可挂，故只要求是已登录的管理员。
 */
@Slf4j
@RestController("adminFileController")
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequirePerm
public class FileController {

    private final AliOssService aliOssService;

    /**
     * 上传图片（用于商品/分类主图等）
     *
     * @param file 上传的文件
     * @param type 业务类型：product / category / other，默认 other
     * @return 图片的完整 OSS URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "type", defaultValue = "other") String type) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        Long operator = BaseContext.getCurrentId();
        String dir = switch (type) {
            case "product" -> "admin/product";
            case "category" -> "admin/category";
            default -> "admin/other";
        };
        String url = aliOssService.upload(file, dir);
        log.info("员工 {} 上传文件成功：{}", operator, url);
        return Result.success(url);
    }
}