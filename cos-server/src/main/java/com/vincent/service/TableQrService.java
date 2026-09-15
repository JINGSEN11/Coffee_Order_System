package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.TableQrCreateDTO;
import com.vincent.entity.TableQr;
import com.vincent.vo.PageVO;
import com.vincent.vo.TableQrVO;

import java.util.List;

public interface TableQrService extends IService<TableQr> {

    List<TableQrVO> listAll();

    PageVO<TableQrVO> pageQuery(String keyword, Integer page, Integer pageSize);

    TableQrVO getDetail(Long id);

    void createTable(TableQrCreateDTO dto);

    void updateTable(Long id, TableQrCreateDTO dto);

    void deleteTable(Long id);
}
