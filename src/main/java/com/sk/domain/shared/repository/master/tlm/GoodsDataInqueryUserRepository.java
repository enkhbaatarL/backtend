package com.sk.domain.shared.repository.master.tlm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInqueryUserId;
import com.sk.domain.shared.entity.master.tlm.GoodsDataInqueryUser;

public interface GoodsDataInqueryUserRepository extends JpaRepository<GoodsDataInqueryUser, GoodsDataInqueryUserId> {

}
