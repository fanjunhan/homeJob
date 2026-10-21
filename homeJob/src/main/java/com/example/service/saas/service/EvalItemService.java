package com.example.service.saas.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评价服务项目目录：客户勾选的服务项目标签。
 * 依据国家医保局、财政部《长期护理保险服务项目目录（试行）》（医保发〔2024〕17 号）：
 * 生活照护类 20 项 + 医疗护理类 16 项，客户最多勾选 5 项。
 *
 * 说明：骨架为可运行内置清单；生产环境建议存字典表（含启用/停用、排序），
 * 目录调整时无需发版，本 Service 改为读字典并缓存。
 */
@Service
public class EvalItemService {

    public static final int MAX_SELECT = 5;

    private static final List<Group> GROUPS = List.of(
            new Group("生活照护类", 20, List.of(
                    "协助进食/水及指导(含管饲饮食)", "协助排泄", "人工取便", "失禁照护", "人工肛门/便袋清洁",
                    "洗发", "理发", "口腔清洁(包括义齿)", "指/趾甲清洁", "擦浴", "沐浴", "整理床单位(床铺)",
                    "协助更衣及指导", "体位管理", "生活功能训练及指导", "协助翻身叩背排痰", "压疮预防照护及指导",
                    "协助给药", "生命体征测量", "安全防护及指导")),
            new Group("医疗护理类", 16, List.of(
                    "吸痰", "氧气吸入", "常规换药", "雾化吸入", "灌肠", "置管(胃管)", "导尿(留置导尿)",
                    "快速血糖测定", "一般物理降温", "造口(造瘘)护理", "气管切开护理", "深静脉置管护理",
                    "认知功能训练", "运动功能训练", "吞咽功能训练", "言语功能训练"))
    );

    public List<Group> groups() {
        return GROUPS;
    }

    public record Group(String name, int count, List<String> items) {
    }
}
