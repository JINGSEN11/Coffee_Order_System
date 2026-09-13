package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.PointsQueryDTO;
import com.vincent.entity.PointsRecord;
import com.vincent.vo.PageVO;
import com.vincent.vo.PointsRecordVO;

public interface PointsRecordService extends IService<PointsRecord> {

    PageVO<PointsRecordVO> pageQuery(PointsQueryDTO dto);

}