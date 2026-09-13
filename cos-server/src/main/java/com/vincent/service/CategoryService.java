package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.CategoryCreateDTO;
import com.vincent.entity.Category;
import com.vincent.vo.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<CategoryVO> listAll();

    void createCategory(CategoryCreateDTO dto);

    void updateCategory(Long id, CategoryCreateDTO dto);

    void deleteCategory(Long id);
}