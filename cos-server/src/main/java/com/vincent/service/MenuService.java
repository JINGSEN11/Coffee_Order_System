package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.entity.Menu;
import com.vincent.vo.MenuVO;

import java.util.List;

public interface MenuService extends IService<Menu> {

    List<MenuVO> tree();

    void createMenu(Menu menu);

    void updateMenu(Menu menu);

    void deleteMenu(Long id);

    List<Long> getMenuIdsByRoleId(Long roleId);
}