package com.vincent.controller.admin;

import com.vincent.annotation.RequirePerm;
import com.vincent.common.Result;
import com.vincent.dto.PointsQueryDTO;
import com.vincent.service.PointsRecordService;
import com.vincent.vo.PageVO;
import com.vincent.vo.PointsRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminPointsController")
@RequestMapping("/admin/points")
@RequiredArgsConstructor
@Slf4j
public class PointsController {

    private final PointsRecordService pointsRecordService;

    /**
     * 积分流水分页查询
     */
    @GetMapping("/page")
    @RequirePerm("marketing:points")
    public Result<PageVO<PointsRecordVO>> page(PointsQueryDTO dto) {
        log.info("管理端分页查询积分流水：{}", dto);
        PageVO<PointsRecordVO> pageVO = pointsRecordService.pageQuery(dto);
        return Result.success(pageVO);
    }

}