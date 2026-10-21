package com.example.service.saas.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 评价服务项目目录单元测试。
 *
 * <p>目录来源为国家医保局、财政部《长期护理保险服务项目目录（试行）》（医保发〔2024〕17 号），
 * 条目固定为生活照护类 20 项 + 医疗护理类 16 项。这两项一旦被改动就会导致
 * 历史评价数据无法对齐，因此用测试锁住。
 */
class EvalItemServiceTest {

    private final EvalItemService service = new EvalItemService();

    @Test
    @DisplayName("分组为生活照护类 20 项 + 医疗护理类 16 项，合计 36 项")
    void groupsShouldMatchOfficialCatalog() {
        List<EvalItemService.Group> groups = service.groups();
        assertEquals(2, groups.size());
        assertEquals("生活照护类", groups.get(0).name());
        assertEquals(20, groups.get(0).count());
        assertEquals("医疗护理类", groups.get(1).name());
        assertEquals(16, groups.get(1).count());
        assertEquals(36, groups.stream().mapToInt(g -> g.items().size()).sum());
    }

    @Test
    @DisplayName("声明数量与实际条目数必须一致")
    void declaredCountShouldMatchActualItems() {
        service.groups().forEach(g ->
                assertEquals(g.count(), g.items().size(), g.name() + " 声明数量与实际不符"));
    }

    @Test
    @DisplayName("36 项条目互不重复")
    void itemsShouldBeUnique() {
        List<String> all = service.groups().stream().flatMap(g -> g.items().stream()).toList();
        assertEquals(36, all.size());
        assertEquals(36, all.stream().distinct().count(), "目录中存在重复条目");
    }

    @Test
    @DisplayName("条目不得为空白")
    void itemsShouldNotBeBlank() {
        service.groups().forEach(g -> g.items().forEach(item -> {
            assertTrue(!item.isBlank(), "存在空白条目：" + g.name());
            assertTrue(item.length() <= 30, "条目名过长：" + item);
        }));
    }

    @Test
    @DisplayName("客户最多可勾选 5 项")
    void maxSelectShouldBeFive() {
        assertEquals(5, EvalItemService.MAX_SELECT);
    }

    @Test
    @DisplayName("抽样校验官方目录中的典型条目确实存在")
    void shouldContainRepresentativeItems() {
        List<String> all = service.groups().stream().flatMap(g -> g.items().stream()).toList();
        for (String expected : List.of(
                "协助进食/水及指导(含管饲饮食)", "协助排泄", "洗发", "擦浴", "生命体征测量",
                "吸痰", "氧气吸入", "常规换药", "快速血糖测定", "吞咽功能训练")) {
            assertTrue(all.contains(expected), "缺少官方目录条目：" + expected);
        }
    }
}