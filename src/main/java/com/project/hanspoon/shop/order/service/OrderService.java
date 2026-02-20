package com.project.hanspoon.shop.order.service;

import com.project.hanspoon.common.user.entity.User;
import com.project.hanspoon.shop.cart.entity.Cart;
import com.project.hanspoon.shop.cart.entity.CartItem;
import com.project.hanspoon.shop.cart.repository.CartItemRepository;
import com.project.hanspoon.shop.cart.repository.CartRepository;
import com.project.hanspoon.shop.constant.OrderStatus;
import com.project.hanspoon.shop.order.dto.OrderCreateMyRequestDto;
import com.project.hanspoon.shop.order.dto.OrderItemResponseDto;
import com.project.hanspoon.shop.order.dto.OrderListItemDto;
import com.project.hanspoon.shop.order.dto.OrderResponseDto;
import com.project.hanspoon.shop.order.entity.Order;
import com.project.hanspoon.shop.order.entity.OrderItem;
import com.project.hanspoon.shop.order.repository.OrderItemRepository;
import com.project.hanspoon.shop.order.repository.OrderRepository;
import com.project.hanspoon.shop.product.entity.Product;
import com.project.hanspoon.shop.product.entity.ProductImage;
import com.project.hanspoon.shop.product.repository.ProductImageRepository;
import com.project.hanspoon.shop.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager em;

    // =========================================================
    // ✅ 1) 주문 생성: 내 장바구니 -> 주문
    // =========================================================
    @Transactional
    public OrderResponseDto createOrderFromMyCart(Long userId, OrderCreateMyRequestDto req) {

        Cart cart = cartRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "장바구니가 없습니다. userId=" + userId));

        Long cartId = cart.getId();

        List<CartItem> cartItems = cartItemRepository.findByCart_IdOrderByIdDesc(cartId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "장바구니가 비어있습니다.");
        }

        User userRef = em.getReference(User.class, userId);

        Order order = Order.builder()
                .user(userRef)
                .cartId(cartId)
                .status(OrderStatus.CREATED)
                .receiverName(req.getReceiverName())
                .receiverPhone(req.getReceiverPhone())
                .address1(req.getAddress1())
                .address2(req.getAddress2())
                .totalPrice(0)
                .build();

        int total = 0;

        for (CartItem ci : cartItems) {
            Long productId = ci.getProduct().getId();
            int qty = ci.getQuantity();

            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "상품이 없습니다. id=" + productId));

            if (qty <= 0) throw new ResponseStatusException(BAD_REQUEST, "수량이 올바르지 않습니다.");
            if (product.getStock() < qty) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "재고 부족: " + product.getName() + " (재고 " + product.getStock() + ", 요청 " + qty + ")");
            }

            product.setStock(product.getStock() - qty);

            String thumb = productImageRepository.findFirstByProduct_IdAndRepYnTrue(productId)
                    .map(ProductImage::getImgUrl)
                    .orElse(null);

            OrderItem oi = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .orderPrice(product.getPrice())
                    .quantity(qty)
                    .thumbnailUrl(thumb)
                    .build();

            order.addItem(oi);
            total += (product.getPrice() * qty);
        }

        order.setTotalPrice(total);

        Order saved = orderRepository.save(order);

        // 주문 성공 시 장바구니 비우기
        cartItemRepository.deleteAll(cartItems);

        return toResponse(saved);
    }

    // =========================================================
    // ✅ 2) 내 주문 상세/목록
    // =========================================================
    public OrderResponseDto getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithItemsByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));
        return toResponse(order);
    }

    public Page<OrderListItemDto> getMyOrders(Long userId, int page, int size) {
        return getMyOrdersWithFilters(userId, null, null, null, page, size);
    }

    public Page<OrderListItemDto> getMyOrdersWithFilters(Long userId, LocalDateTime startDate, LocalDateTime endDate, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Repository 메서드 호출
        Page<Order> ordersPage = orderRepository.findWithFilters(userId, startDate, endDate, status, pageable);

        List<Long> orderIds = ordersPage.getContent().stream().map(Order::getId).toList();

        Map<Long, Integer> countMap = new HashMap<>();
        Map<Long, OrderItem> firstItemMap = new HashMap<>();

        if (!orderIds.isEmpty()) {
            List<OrderItem> items = orderItemRepository.findByOrder_IdInOrderByOrder_IdAscIdAsc(orderIds);
            for (OrderItem oi : items) {
                Long oid = oi.getOrder().getId();
                countMap.merge(oid, 1, Integer::sum);
                firstItemMap.putIfAbsent(oid, oi);
            }
        }

        List<OrderListItemDto> dtoList = ordersPage.getContent().stream()
                .map(o -> {
                    OrderItem first = firstItemMap.get(o.getId());
                    return OrderListItemDto.builder()
                            .orderId(o.getId())
                            .status(o.getStatus())
                            .totalPrice(o.getTotalPrice())
                            .createdAt(o.getCreatedAt())
                            .itemCount(countMap.getOrDefault(o.getId(), 0))
                            .firstItemName(first != null ? first.getProductName() : null)
                            .firstItemThumbnailUrl(first != null ? first.getThumbnailUrl() : null)
                            .build();
                })
                .toList();

        return new PageImpl<>(dtoList, pageable, ordersPage.getTotalElements());
    }

    // =========================================================
    // ✅ 3) 내 주문 취소/환불
    // =========================================================
    @Transactional
    public OrderResponseDto cancelMyOrder(Long userId, Long orderId, String reason) {
        Order order = orderRepository.findWithItemsByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(BAD_REQUEST, "이미 취소/환불된 주문입니다.");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(BAD_REQUEST, "배송 시작 이후에는 취소할 수 없습니다.");
        }

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(BAD_REQUEST, "취소 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        // 재고 원복
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,
                            "주문 상품이 삭제되었거나 존재하지 않습니다. productId=" + item.getProductId()));
            product.setStock(product.getStock() + item.getQuantity());
        }

        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.CANCELED);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new ResponseStatusException(BAD_REQUEST, "환불 사유(reason)는 필수입니다.");
            }
            order.setStatus(OrderStatus.REFUNDED);
            order.setRefundedAt(LocalDateTime.now());
            order.setRefundReason(reason.trim());
        }

        return toResponse(order);
    }

    // =========================================================
    // ✅ 4) 내 주문 결제
    // =========================================================
    @Transactional
    public OrderResponseDto payMyOrder(Long userId, Long orderId, String payMethod) {
        Order order = orderRepository.findWithItemsByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(BAD_REQUEST, "취소/환불된 주문은 결제할 수 없습니다.");
        }

        if (order.getStatus() == OrderStatus.PAID) {
            return toResponse(order); // idempotent
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(BAD_REQUEST, "결제 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        // payMethod 저장하려면 Order에 컬럼 추가해서 저장

        return toResponse(order);
    }

    // =========================================================
    // ✅ 5) (추가) 배송 시작 / 배송 완료
    // =========================================================

    /**
     * ✅ 내 주문 출고 처리 (PAID -> SHIPPED)
     * - 보통은 관리자 기능이지만, 지금 프론트 버튼 유지하려고 만들어둠
     */
    @Transactional
    public OrderResponseDto shipMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithItemsByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(BAD_REQUEST, "취소/환불된 주문은 배송 처리할 수 없습니다.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(BAD_REQUEST, "출고 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        return toResponse(order);
    }

    /**
     * ✅ 내 주문 배송완료 처리 (SHIPPED -> DELIVERED)
     */
    @Transactional
    public OrderResponseDto deliverMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findWithItemsByIdAndUserIdForUpdate(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(BAD_REQUEST, "배송완료 처리 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        return toResponse(order);
    }

    // =========================================================
    // ✅ 6) (기존 유지) 관리자/테스트용 ship/deliver
    // - 컨트롤러에서 /api/orders/{id}/ship 유지하려면 필요
    // =========================================================

    @Transactional
    public OrderResponseDto ship(Long orderId) {
        // user 체크 없이, 주문만 잠금 조회 (레포 없으면 일반 findById + @Lock 메서드 추가해도 됨)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new ResponseStatusException(BAD_REQUEST, "취소/환불된 주문은 배송 처리할 수 없습니다.");
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(BAD_REQUEST, "출고 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        return toResponse(order);
    }

    @Transactional
    public OrderResponseDto deliver(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "주문이 없습니다. orderId=" + orderId));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new ResponseStatusException(BAD_REQUEST, "배송완료 처리 가능한 상태가 아닙니다. status=" + order.getStatus());
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        return toResponse(order);
    }

    // =========================================================
    // 공통: Response 변환
    // =========================================================
    private OrderResponseDto toResponse(Order order) {
        List<OrderItemResponseDto> items = order.getItems().stream()
                .map(i -> OrderItemResponseDto.builder()
                        .orderItemId(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .orderPrice(i.getOrderPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .thumbnailUrl(i.getThumbnailUrl())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .cartId(order.getCartId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .refundedAt(order.getRefundedAt())
                .refundReason(order.getRefundReason())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .address1(order.getAddress1())
                .address2(order.getAddress2())
                .items(items)
                .build();
    }
}
