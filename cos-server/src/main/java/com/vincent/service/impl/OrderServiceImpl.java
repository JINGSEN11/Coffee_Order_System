package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.OrderCreateDTO;
import com.vincent.dto.OrderQueryDTO;
import com.vincent.entity.OrderItem;
import com.vincent.entity.Orders;
import com.vincent.mapper.OrderItemMapper;
import com.vincent.mapper.OrderMapper;
import com.vincent.service.OrderService;
import com.vincent.vo.OrderItemVO;
import com.vincent.vo.OrderVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public OrderVO detail(Long id) {
        // 使用继承自 ServiceImpl 的 getById
        Orders order = getById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, id)
        );

        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList()));

        return vo;
    }

    @Override
    public PageVO<OrderVO> pageQuery(OrderQueryDTO dto) {
        // 使用 MyBatis-Plus 分页
        Page<Orders> page = new Page<>(dto.getPage(), dto.getPageSize());

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<Orders>()
                .eq(dto.getStatus() != null, Orders::getStatus, dto.getStatus())
                .eq(dto.getType() != null, Orders::getType, dto.getType())
                .eq(dto.getShopId() != null, Orders::getShopId, dto.getShopId())
                .like(StringUtils.hasText(dto.getOrderNo()), Orders::getOrderNo, dto.getOrderNo())
                .ge(dto.getStartDate() != null, Orders::getCreatedAt, dto.getStartDate().atStartOfDay())
                .le(dto.getEndDate() != null, Orders::getCreatedAt, dto.getEndDate().plusDays(1).atStartOfDay())
                .orderByDesc(Orders::getCreatedAt);

        // 使用继承自 ServiceImpl 的 page 方法
        Page<Orders> result = page(page, wrapper);

        List<OrderVO> voList = result.getRecords().stream().map(order -> {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(OrderCreateDTO dto, Long userId) {
        Orders order = new Orders();
        BeanUtils.copyProperties(dto, order);
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(0);
        order.setAmount(dto.getItems() != null
                ? BigDecimal.valueOf(dto.getItems().size())
                : BigDecimal.ZERO);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        save(order); // 使用继承自 ServiceImpl 的 save

        if (dto.getItems() != null) {
            List<OrderItem> items = dto.getItems().stream().map(itemDTO -> {
                OrderItem item = new OrderItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setOrderId(order.getId());
                return item;
            }).collect(Collectors.toList());
            items.forEach(orderItemMapper::insert);
        }

        return detail(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, String reason) {
        Orders order = getById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() != 0 && order.getStatus() != 1) {
            throw new ServiceException("当前状态不允许取消");
        }
        order.setStatus(5);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        updateById(order); // 使用继承自 ServiceImpl 的 updateById
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Orders order = getById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        order.setStatus(status);
        if (status == 3) {
            order.setFinishTime(LocalDateTime.now());
        }
        updateById(order); // 使用继承自 ServiceImpl 的 updateById
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

}