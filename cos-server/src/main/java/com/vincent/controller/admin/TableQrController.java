package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.TableQrCreateDTO;
import com.vincent.service.TableQrService;
import com.vincent.vo.PageVO;
import com.vincent.vo.TableQrVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminTableQrController")
@RequestMapping("/admin/table")
@OpLog(module = "桌码")
@RequiredArgsConstructor
@Slf4j
public class TableQrController {

    private final TableQrService tableQrService;

    @GetMapping("/list")
    @RequirePerm("shop:table")
    public Result<PageVO<TableQrVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(tableQrService.pageQuery(keyword, page, pageSize));
    }

    @GetMapping("/all")
    @RequirePerm("shop:table")
    public Result<List<TableQrVO>> all() {
        return Result.success(tableQrService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePerm("shop:table")
    public Result<TableQrVO> detail(@PathVariable Long id) {
        return Result.success(tableQrService.getDetail(id));
    }

    @PostMapping
    @RequirePerm("shop:table:edit")
    public Result<Void> create(@RequestBody TableQrCreateDTO dto) {
        tableQrService.createTable(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("shop:table:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody TableQrCreateDTO dto) {
        tableQrService.updateTable(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("shop:table:edit")
    public Result<Void> delete(@PathVariable Long id) {
        tableQrService.deleteTable(id);
        return Result.success();
    }
}
