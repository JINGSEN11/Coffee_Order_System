package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.EmployeeCreateDTO;
import com.vincent.service.EmployeeService;
import com.vincent.vo.EmployeeVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminEmployeeController")
@RequestMapping("/admin/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/page")
    public Result<PageVO<EmployeeVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(employeeService.pageQuery(keyword, roleId, shopId, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<EmployeeVO> detail(@PathVariable Long id) {
        return Result.success(employeeService.getEmployeeDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody EmployeeCreateDTO dto) {
        employeeService.createEmployee(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody EmployeeCreateDTO dto) {
        employeeService.updateEmployee(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        employeeService.toggleStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        employeeService.resetPassword(id, newPassword);
        return Result.success();
    }

}