package com.example.service.saas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单状态机单元测试。
 *
 * <p>状态机是整个排班闭环的守门人——一旦某条非法流转漏出，
 * 就会出现「未打卡直接评价」这类业务事故，因此对流转表做穷举式覆盖。
 */
class ScheduleStatusTest {

    @Test
    @DisplayName("正向主流程：排班 → 通知 → 接单 → 服务中 → 完成 → 签名 → 评价")
    void mainFlowShouldPass() {
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.SCHEDULED, ScheduleStatus.NOTIFIED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.NOTIFIED, ScheduleStatus.ACCEPTED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.ACCEPTED, ScheduleStatus.IN_SERVICE));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.IN_SERVICE, ScheduleStatus.FINISHED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.FINISHED, ScheduleStatus.SIGNED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.SIGNED, ScheduleStatus.EVALUATED));
    }

    @Test
    @DisplayName("取消分支：排班/通知/接单三态均可取消")
    void cancelBranchShouldPass() {
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.SCHEDULED, ScheduleStatus.CANCELLED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.NOTIFIED, ScheduleStatus.CANCELLED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.ACCEPTED, ScheduleStatus.CANCELLED));
    }

    @Test
    @DisplayName("改派后回到已排班，可重新通知")
    void reassignShouldReturnToScheduled() {
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.SCHEDULED, ScheduleStatus.REASSIGNED));
        assertDoesNotThrow(() -> ScheduleStatus.check(ScheduleStatus.REASSIGNED, ScheduleStatus.SCHEDULED));
    }

    @Test
    @DisplayName("终态不可再流转：已评价与已取消是死状态")
    void terminalStatesShouldRejectEverything() {
        for (ScheduleStatus to : ScheduleStatus.values()) {
            assertThrows(IllegalStateException.class,
                    () -> ScheduleStatus.check(ScheduleStatus.EVALUATED, to),
                    "EVALUATED → " + to + " 应被拒绝");
            assertThrows(IllegalStateException.class,
                    () -> ScheduleStatus.check(ScheduleStatus.CANCELLED, to),
                    "CANCELLED → " + to + " 应被拒绝");
        }
    }

    @Test
    @DisplayName("服务开始后不可取消——已上门就不能按取消处理")
    void inServiceShouldNotBeCancellable() {
        assertThrows(IllegalStateException.class,
                () -> ScheduleStatus.check(ScheduleStatus.IN_SERVICE, ScheduleStatus.CANCELLED));
    }

    @Test
    @DisplayName("跳过前置环节的非法流转全部被拒绝")
    void skippingStepsShouldBeRejected() {
        // 未打卡直接完成
        assertThrows(IllegalStateException.class,
                () -> ScheduleStatus.check(ScheduleStatus.ACCEPTED, ScheduleStatus.FINISHED));
        // 未签名直接评价
        assertThrows(IllegalStateException.class,
                () -> ScheduleStatus.check(ScheduleStatus.FINISHED, ScheduleStatus.EVALUATED));
        // 未接单直接服务
        assertThrows(IllegalStateException.class,
                () -> ScheduleStatus.check(ScheduleStatus.NOTIFIED, ScheduleStatus.IN_SERVICE));
        // 自流转也不允许
        assertThrows(IllegalStateException.class,
                () -> ScheduleStatus.check(ScheduleStatus.ACCEPTED, ScheduleStatus.ACCEPTED));
    }

    @Test
    @DisplayName("未完工状态集合用于员工当日负载统计")
    void activeStatusesShouldExcludeTerminalAndDoneStates() {
        var active = ScheduleStatus.activeStatuses();
        assertEquals(4, active.size());
        assertTrue(active.contains(ScheduleStatus.SCHEDULED));
        assertTrue(active.contains(ScheduleStatus.IN_SERVICE));
        assertTrue(active.stream().noneMatch(s -> s == ScheduleStatus.FINISHED
                || s == ScheduleStatus.CANCELLED || s == ScheduleStatus.EVALUATED));
    }
}