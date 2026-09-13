package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.EmployeeCreateDTO;
import com.vincent.entity.Employee;
import com.vincent.vo.EmployeeVO;
import com.vincent.vo.PageVO;
import java.util.List;

public interface EmployeeService extends IService<Employee> {

    List<EmployeeVO> listAll();

    PageVO<EmployeeVO> pageQuery(String keyword, Long roleId, Long shopId, Integer page, Integer pageSize);

    EmployeeVO getEmployeeDetail(Long id);

    void createEmployee(EmployeeCreateDTO dto);

    void updateEmployee(Long id, EmployeeCreateDTO dto);

    void deleteEmployee(Long id);

    void toggleStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);
}