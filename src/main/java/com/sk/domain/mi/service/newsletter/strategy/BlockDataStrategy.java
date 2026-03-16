package com.sk.domain.mi.service.newsletter.strategy;

import com.sk.domain.mi.dto.internal.ResolvedBlockData;
import com.sk.domain.mi.dto.internal.TemplateTree;
import com.sk.domain.shared.entity.master.tmd.NewsletterPublishHistory;

public interface BlockDataStrategy {
    String getType();
    ResolvedBlockData resolve(TemplateTree.BlockNode block, NewsletterPublishHistory pubHist);
}
