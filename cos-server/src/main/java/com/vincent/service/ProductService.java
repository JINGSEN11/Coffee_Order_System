package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.ProductCreateDTO;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.entity.Product;
import com.vincent.vo.PageVO;
import com.vincent.vo.ProductVO;

import java.util.List;

public interface ProductService extends IService<Product> {

    List<ProductVO> listAll();

    PageVO<ProductVO> pageQuery(String name, Long categoryId, Integer status, String lowStock, Integer page, Integer pageSize);

    ProductVO getProductDetail(Long id);

    Long createProduct(ProductCreateDTO dto);

    void updateProduct(Long id, ProductCreateDTO dto);

    void deleteProduct(Long id);

    void setStatus(Long id, Integer status);

    void batchCreateSku(Long productId, List<SkuCreateDTO> skuList);

    void batchUpdateSku(Long productId, List<SkuCreateDTO> skuList);
}