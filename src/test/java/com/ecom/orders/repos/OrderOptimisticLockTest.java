package com.ecom.orders.repos;

import com.ecom.orders.models.Order;
import com.ecom.orders.models.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class OrderOptimisticLockTest {
    @Autowired
    OrderRepo orderRepo;

    @Test
    void staleVersionCannotOverwriteANewerChange() {
        Order order = new Order();
        order.setUserId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(1000);
        Order saved = orderRepo.saveAndFlush(order);
        Long id = saved.getId();
        Long staleVersion = saved.getVersion();

        Order first = orderRepo.findById(id).orElseThrow();
        first.setStatus(OrderStatus.PAID);
        orderRepo.saveAndFlush(first);

        // A second request that loaded the order before the first one committed
        Order stale = new Order();
        stale.setId(id);
        stale.setUserId(1L);
        stale.setTotalAmount(1000);
        stale.setStatus(OrderStatus.CANCELLED);
        stale.setVersion(staleVersion);
        assertThatThrownBy(() -> orderRepo.saveAndFlush(stale))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
