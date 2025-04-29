package com.sk.domain.shared.repository.master.tlm;

import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInquiryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInquiryUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsDataInquiryUserRepository extends JpaRepository<GoodsDataInquiryUser, GoodsDataInquiryUserId> {

}
