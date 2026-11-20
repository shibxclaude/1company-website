package com.cn01.solution.service;

import com.cn01.solution.dto.SolutionViews.*;
import com.cn01.solution.entity.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SolutionAssembler {
    private SolutionAssembler() {}

    static final Comparator<SolutionVersion> VERSION_ORDER =
            Comparator.comparing((SolutionVersion v) -> v.getSort() == null ? 0 : v.getSort()).thenComparing(SolutionVersion::getId);
    static final Comparator<SolutionService> SERVICE_ORDER =
            Comparator.comparing((SolutionService s) -> s.getSort() == null ? 0 : s.getSort()).thenComparing(SolutionService::getId);

    static CategoryView category(SolutionCategory c) {
        return new CategoryView(c.getId(), c.getCode(), c.getName(), c.getSort(), c.getStatus());
    }

    static VersionView version(SolutionVersion v) {
        BillingType bt = BillingType.valueOf(v.getBillingType());
        return new VersionView(v.getId(), v.getName(), v.getPrice(), v.getOriginalPrice(), v.getBillingType(),
                bt.label(), bt.unit(), v.getDescription(), v.getStatus(), v.getSort());
    }

    static ServiceView service(SolutionService s) {
        return new ServiceView(s.getId(), s.getName(), s.getPrice(), s.getUnit(), s.getDescription(), s.getStatus(), s.getSort());
    }

    static ArrayList<VersionView> versions(List<SolutionVersion> list) {
        return list.stream().sorted(VERSION_ORDER).map(SolutionAssembler::version).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    static ArrayList<ServiceView> services(List<SolutionService> list) {
        return list.stream().sorted(SERVICE_ORDER).map(SolutionAssembler::service).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    static Detail detail(Solution s, SolutionCategory c, List<SolutionVersion> versions, List<SolutionService> services) {
        DeliveryMode dm = DeliveryMode.valueOf(s.getDeliveryMode());
        return new Detail(s.getId(), s.getSlug(), s.getName(), s.getSubtitle(), s.getSummary(), s.getMainImage(),
                s.getGallery() == null ? new ArrayList<>() : new ArrayList<>(s.getGallery()), s.getContent(),
                s.getCategoryId(), c == null ? null : c.getCode(), c == null ? null : c.getName(),
                s.getDeliveryMode(), dm.label(), s.getAfterSales(), s.getServicePeriod(),
                s.getRecommended(), s.getStatus(), s.getSort(), versions(versions), services(services), s.getUpdatedAt());
    }

    /** 列表卡片：展示最低价（"¥xx 起"） */
    static Card card(Solution s, SolutionCategory c, List<SolutionVersion> activeVersions) {
        SolutionVersion cheapest = activeVersions.stream().min(Comparator.comparing(SolutionVersion::getPrice)).orElse(null);
        BigDecimal min = cheapest == null ? null : cheapest.getPrice();
        BillingType bt = cheapest == null ? null : BillingType.valueOf(cheapest.getBillingType());
        return new Card(s.getId(), s.getSlug(), s.getName(), s.getSubtitle(), s.getSummary(), s.getMainImage(),
                s.getCategoryId(), c == null ? null : c.getCode(), c == null ? null : c.getName(),
                min, cheapest == null ? null : cheapest.getOriginalPrice(),
                bt == null ? null : bt.unit(), bt == null ? null : bt.label(),
                activeVersions.size() > 1, s.getRecommended(), s.getSort());
    }
}
