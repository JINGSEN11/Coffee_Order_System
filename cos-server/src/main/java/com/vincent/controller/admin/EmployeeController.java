package com.vincent.controller.admin;

import com.vincent.annotation.OpLog;
import com.vincent.annotation.RequirePerm;
import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.EmployeeCreateDTO;
import com.vincent.service.EmployeeService;
import com.vincent.vo.EmployeeVO;
import com.vincent.vo.PageVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminEmployeeController")
@RequestMapping("/admin/employee")
@OpLog(module = "员工")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/list")
    @RequirePerm("system:employee")
    public Result<List<EmployeeVO>> list() {
        return Result.success(employeeService.listAll());
    }

    @GetMapping("/page")
    @RequirePerm("system:employee")
    public Result<PageVO<EmployeeVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(employeeService.pageQuery(keyword, roleId, shopId, page, pageSize));
    }

    /**
     * 当前登录员工信息（只读自己，任何已登录员工都可调）
     */
    @GetMapping("/current")
    @RequirePerm
    public Result<EmployeeVO> current() {
        return Result.success(employeeService.getEmployeeDetail(BaseContext.getCurrentId()));
    }

    @GetMapping("/{id}")
    @RequirePerm("system:employee")
    public Result<EmployeeVO> detail(@PathVariable Long id) {
        return Result.success(employeeService.getEmployeeDetail(id));
    }

    @PostMapping
    @RequirePerm("system:employee:edit")
    public Result<Void> create(@RequestBody EmployeeCreateDTO dto) {
        employeeService.createEmployee(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePerm("system:employee:edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody EmployeeCreateDTO dto) {
        employeeService.updateEmployee(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePerm("system:employee:edit")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePerm("system:employee:edit")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        employeeService.toggleStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    @RequirePerm("system:employee:reset")
    @OpLog(action = "重置密码", args = false)
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        employeeService.resetPassword(id, newPassword);
        return Result.success();
    }

}