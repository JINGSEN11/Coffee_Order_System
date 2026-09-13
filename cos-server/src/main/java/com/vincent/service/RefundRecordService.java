package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.RefundQueryDTO;
import com.vincent.entity.RefundRecord;
import com.vincent.vo.PageVO;
import com.vincent.vo.RefundRecordVO;

public interface RefundRecordService extends IService<RefundRecord> {

    PageVO<RefundRecordVO> pageQuery(RefundQueryDTO dto);

    RefundRecordVO getDetail(Long id);

}