package com.vincent.controller.user;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.AppCartAddDTO;
import com.vincent.dto.AppCartUpdateDTO;
import com.vincent.service.AppCartService;
import com.vincent.vo.AppCartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
@Slf4j
public class AppCartController {

    private final AppCartService appCartService;

    /** 购物车列表 */
    @GetMapping
    public Result<AppCartVO> list() {
        return Result.success(appCartService.buildCart(BaseContext.getCurrentId()));
    }

    /** 加入购物车（同 SKU + 同门店 + 同加料才合并数量） */
    @PostMapping
    public Result<AppCartVO> add(@RequestBody AppCartAddDTO dto) {
        return Result.success(appCartService.add(BaseContext.getCurrentId(), dto));
    }

    /** 修改购物车行（qty / checked 可同时传） */
    @PutMapping("/{id}")
    public Result<AppCartVO> update(@PathVariable Long id, @RequestBody AppCartUpdateDTO dto) {
        return Result.success(appCartService.update(BaseContext.getCurrentId(), id, dto));
    }

    /** 删除指定行；不传 id 表示清空当前用户购物车 */
    @DeleteMapping
    public Result<AppCartVO> delete(@RequestParam(value = "id", required = false) Long id) {
        return Result.success(appCartService.delete(BaseContext.getCurrentId(), id));
    }
}
