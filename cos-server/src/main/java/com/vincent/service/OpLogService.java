package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.OpLogQueryDTO;
import com.vincent.entity.OpLog;
import com.vincent.vo.OpLogVO;
import com.vincent.vo.PageVO;

public interface OpLogService extends IService<OpLog> {

    PageVO<OpLogVO> pageQuery(OpLogQueryDTO dto);
}