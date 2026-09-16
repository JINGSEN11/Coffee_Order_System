package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.RoleCreateDTO;
import com.vincent.service.RoleService;
import com.vincent.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminRoleController")
@RequestMapping("/admin/role")
@OpLog(module = "角色")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final RoleService roleService;

    @GetMapping("/list")
    @RequirePerm("system:role")
    public Result<List<RoleVO>> list() {
        return Result.success(roleService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePerm("system:role")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.success(roleService.getRoleDetail(id));
    }

    @PostMapping
    @RequirePerm("system:role:edit")
    public Result<Void> create(@RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("system:role:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody RoleCreateDTO dto) {
        roleService.updateRole(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:role:edit")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PutMapping("/{id}/assign-menus")
    @RequirePerm("system:role:edit")
    @OpLog(action = "分配角色权限")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }

}