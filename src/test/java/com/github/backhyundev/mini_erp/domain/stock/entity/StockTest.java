package com.github.backhyundev.mini_erp.domain.stock.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Stock 도메인 단위 테스트")
class StockTest {

    @Nested
    @DisplayName("입고 (increase) 테스트")
    class IncreaseTest {

        @Test
        @DisplayName("입고 요청 시 물리 재고 수량이 정상적으로 증가한다.")
        void increase_success() {
            // given
            Stock stock = new Stock(1L, 2L, 0L);

            // when
            stock.increase(5L);

            // then
            assertThat(stock.getQuantity()).isEqualTo(5L);
            assertThat(stock.getAllocatedQuantity()).isEqualTo(0L);
            assertThat(stock.getAvailableQuantity()).isEqualTo(5L);
        }

        @Test
        @DisplayName("0 이하의 수량으로 입고를 시도하면 예외가 발생한다.")
        void increase_invalidAmount_throwsException() {
            // given
            Stock stock = new Stock(1L, 2L, 0L);

            // when & then
            assertThatThrownBy(() -> stock.increase(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("증가시킬 수량은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("주문 선점 (reserve) 테스트")
    class ReserveTest {

        @Test
        @DisplayName("가용 재고 범위 내에서 선점 요청 시 선점 수량이 증가한다.")
        void reserve_success() {
            // given
            Stock stock = new Stock(1L, 2L, 10L); // 가용 재고 10개

            // when
            stock.reserve(7L);

            // then
            assertThat(stock.getQuantity()).isEqualTo(10L);
            assertThat(stock.getAllocatedQuantity()).isEqualTo(7L);
            assertThat(stock.getAvailableQuantity()).isEqualTo(3L); // 10 - 7 = 3
        }

        @Test
        @DisplayName("가용 재고보다 많은 수량을 선점하려 하면 예외(초과 판매 방지)가 발생한다.")
        void reserve_exceedAvailable_throwsException() {
            // given
            Stock stock = new Stock(1L, 2L, 10L);// 가용 재고 8개 (10 - 2)

            stock.reserve(2L);

            // when & then
            assertThatThrownBy(() -> stock.reserve(9L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("가용 재고가 부족합니다.");
        }
    }

    @Nested
    @DisplayName("선점 해제 / 주문 취소 (release) 테스트")
    class ReleaseTest {

        @Test
        @DisplayName("선점된 수량 내에서 해제 요청 시 선점 수량이 정상 차감된다.")
        void release_success() {
            // given
            Stock stock = new Stock(1L, 2L, 10L);
            stock.reserve(7L);

            // when
            stock.release(3L);

            // then
            assertThat(stock.getQuantity()).isEqualTo(10L);
            assertThat(stock.getAllocatedQuantity()).isEqualTo(4L);
            assertThat(stock.getAvailableQuantity()).isEqualTo(6L);
        }

        @Test
        @DisplayName("선점된 수량보다 많은 수량을 해제하려 하면 예외가 발생한다.")
        void release_exceedAllocated_throwsException() {
            // given
            Stock stock = new Stock(1L, 2L, 10L);
            stock.reserve(5L);

            // when & then
            assertThatThrownBy(() -> stock.release(6L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해제할 선점 재고 수량이 현재 선점된 수량보다 큽니다.");
        }
    }

    @Nested
    @DisplayName("실제 출고 (decrease) 테스트")
    class DecreaseTest {

        @Test
        @DisplayName("선점된 수량 범위 내에서 출고 시 물리 재고와 선점 수량이 함께 줄어든다.")
        void decrease_success() {
            // given
            Stock stock = new Stock(1L, 2L, 10L); // 물리 10, 선점 7
            stock.reserve(7L);

            // when
            stock.decrease(5L); // 5개 실물 출고

            // then
            assertThat(stock.getQuantity()).isEqualTo(5L);           // 10 - 5 = 5
            assertThat(stock.getAllocatedQuantity()).isEqualTo(2L);  // 7 - 5 = 2
            assertThat(stock.getAvailableQuantity()).isEqualTo(3L);  // 5 - 2 = 3 (동일 유지)
        }

        @Test
        @DisplayName("선점된 수량을 초과하여 출고를 시도하면 예외가 발생한다.")
        void decrease_exceedAllocated_throwsException() {
            // given
            Stock stock = new Stock(1L, 2L, 10L); // 선점 3개
            stock.reserve(3L);

            // when & then
            assertThatThrownBy(() -> stock.decrease(4L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("선점된 재고 수량을 초과하여 출고할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("실물 재고 강제 조정 (adjust) 테스트")
    class AdjustTest {

        @Test
        @DisplayName("현재 선점 수량 이상의 값으로 조정 시 물리 재고가 정상 변경된다.")
        void adjust_success() {
            // given
            Stock stock = new Stock(1L, 2L, 10L); // 물리 10, 선점 7
            stock.reserve(7L);

            // when
            stock.adjust(8L); // 실물 파손 등으로 8개로 재조정 (8 >= 7)

            // then
            assertThat(stock.getQuantity()).isEqualTo(8L);
            assertThat(stock.getAllocatedQuantity()).isEqualTo(7L);
            assertThat(stock.getAvailableQuantity()).isEqualTo(1L); // 8 - 7 = 1
        }

        @Test
        @DisplayName("선점 수량보다 적은 수치로 강제 조정 시 예외(가용 재고 음수 방지)가 발생한다.")
        void adjust_belowAllocated_throwsException() {
            // given
            Stock stock = new Stock(1L, 2L, 9L); // 물리 9, 선점 7 (가용 2)
            stock.reserve(7L);

            // when & then (3개 파손되어 6개로 조정 시도 -> 선점 7개보다 적으므로 차단)
            assertThatThrownBy(() -> stock.adjust(6L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("현재 선점된 주문 수량")
                    .hasMessageContaining("보다 적은 수량으로 재고를 조정할 수 없습니다.");
        }
    }
}