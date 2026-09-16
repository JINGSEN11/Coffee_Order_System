package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.entity.Menu;
import com.vincent.service.MenuService;
import com.vincent.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminMenuController")
@RequestMapping("/admin/menu")
@OpLog(module = "菜单")
@RequiredArgsConstructor
@Slf4j
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @RequirePerm("system:menu")
    public Result<List<MenuVO>> tree() {
        return Result.success(menuService.tree());
    }

    @PostMapping
    @RequirePerm("system:menu:edit")
    public Result<Void> create(@RequestBody Menu menu) {
        menuService.createMenu(menu);
        return Result.success();
    }

    @PutMapping
    @RequirePerm("system:menu:edit")
    public Result<Void> update(@RequestBody Menu menu) {
        menuService.updateMenu(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:menu:edit")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

}