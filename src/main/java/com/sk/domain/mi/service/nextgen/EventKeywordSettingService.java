package com.sk.domain.mi.service.nextgen;

import com.sk.domain.mi.dto.request.nextgen.EventKeywordSettingReq;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingListRes;
import com.sk.domain.mi.dto.response.nextgen.EventKeywordSettingRes;
import com.sk.domain.shared.entity.master.tmd.User;

/**
 * Next Gen MI 이벤트 키워드 알림 설정
 */
public interface EventKeywordSettingService {

    EventKeywordSettingListRes getKeywordSettings(User user);

    EventKeywordSettingRes createKeywordSetting(EventKeywordSettingReq.Insert dto, User user);

    EventKeywordSettingRes updateNotificationUse(Long keywordSettingId, EventKeywordSettingReq.UpdateNotificationUse dto, User user);

    EventKeywordSettingRes deleteKeywordSetting(Long keywordSettingId, User user);

}
