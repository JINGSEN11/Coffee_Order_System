package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.SysConfigCreateDTO;
import com.vincent.service.SysConfigService;
import com.vincent.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminConfigController")
@RequestMapping("/admin/config")
@RequiredArgsConstructor
@Slf4j
public class ConfigController {

    private final SysConfigService sysConfigService;

    @GetMapping("/list")
    @RequirePerm("system:config")
    public Result<List<SysConfigVO>> list() {
        log.info("\u7BA1\u7406\u7AEF\u67E5\u8BE2\u914D\u7F6E\u9879\u5217\u8868");
        return Result.success(sysConfigService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePerm("system:config")
    public Result<SysConfigVO> detail(@PathVariable Long id) {
        log.info("\u7BA1\u7406\u7AEF\u67E5\u8BE2\u914D\u7F6E\u9879\u8BE6\u60C5\uFF1Aid={}", id);
        return Result.success(sysConfigService.getConfigDetail(id));
    }

    @PostMapping
    @RequirePerm("system:config:save")
    public Result<Void> create(@RequestBody SysConfigCreateDTO dto) {
        log.info("\u7BA1\u7406\u7AEF\u65B0\u589E\u914D\u7F6E\u9879\uFF1A{}", dto);
        sysConfigService.createConfig(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("system:config:save")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysConfigCreateDTO dto) {
        log.info("\u7BA1\u7406\u7AEF\u4FEE\u6539\u914D\u7F6E\u9879\uFF1Aid={}, dto={}", id, dto);
        sysConfigService.updateConfig(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:config:save")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("\u7BA1\u7406\u7AEF\u5220\u9664\u914D\u7F6E\u9879\uFF1Aid={}", id);
        sysConfigService.deleteConfig(id);
        return Result.success();
    }

}