package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.EmployeeCreateDTO;
import com.vincent.entity.Employee;
import com.vincent.entity.Role;
import com.vincent.entity.Shop;
import com.vincent.mapper.EmployeeMapper;
import com.vincent.mapper.RoleMapper;
import com.vincent.mapper.ShopMapper;
import com.vincent.service.EmployeeService;
import com.vincent.vo.EmployeeVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final RoleMapper roleMapper;
    private final ShopMapper shopMapper;

    @Override
    public PageVO<EmployeeVO> pageQuery(String keyword, Long roleId, Long shopId, Integer pageNum, Integer pageSize) {
        Page<Employee> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<Employee>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Employee::getUsername, keyword)
                        .or()
                        .like(Employee::getRealName, keyword)
                )
                .eq(roleId != null, Employee::getRoleId, roleId)
                .eq(shopId != null, Employee::getShopId, shopId)
                .orderByDesc(Employee::getCreatedAt);

        Page<Employee> result = page(page, wrapper);

        List<EmployeeVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public EmployeeVO getEmployeeDetail(Long id) {
        Employee employee = getById(id);
        if (employee == null) throw new ServiceException("员工不存在");
        return convertToVO(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createEmployee(EmployeeCreateDTO dto) {
        // 检查用户名是否重复
        Long count = employeeMapper.selectCount(
                new LambdaQueryWrapper<Employee>().eq(Employee::getUsername, dto.getUsername())
        );
        if (count > 0) throw new ServiceException("用户名已存在");

        Employee employee = new Employee();
        BeanUtils.copyProperties(dto, employee);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        save(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmployee(Long id, EmployeeCreateDTO dto) {
        Employee employee = getById(id);
        if (employee == null) throw new ServiceException("员工不存在");

        // 检查用户名是否被其他员工占用
        if (StringUtils.hasText(dto.getUsername()) && !dto.getUsername().equals(employee.getUsername())) {
            Long count = employeeMapper.selectCount(
                    new LambdaQueryWrapper<Employee>()
                            .eq(Employee::getUsername, dto.getUsername())
                            .ne(Employee::getId, id)
            );
            if (count > 0) throw new ServiceException("用户名已存在");
        }

        BeanUtils.copyProperties(dto, employee);
        employee.setId(id);
        employee.setUpdatedAt(LocalDateTime.now());
        updateById(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEmployee(Long id) {
        if (getById(id) == null) throw new ServiceException("员工不存在");
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        Employee employee = getById(id);
        if (employee == null) throw new ServiceException("员工不存在");
        employee.setStatus(status);
        updateById(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        Employee employee = getById(id);
        if (employee == null) throw new ServiceException("员工不存在");
        employee.setPassword(newPassword);
        updateById(employee);
    }

    private EmployeeVO convertToVO(Employee employee) {
        EmployeeVO vo = new EmployeeVO();
        BeanUtils.copyProperties(employee, vo);

        // 填充角色名
        Role role = roleMapper.selectById(employee.getRoleId());
        if (role != null) vo.setRoleName(role.getName());

        // 填充门店名
        Shop shop = shopMapper.selectById(employee.getShopId());
        if (shop != null) vo.setShopName(shop.getName());

        return vo;
    }

}