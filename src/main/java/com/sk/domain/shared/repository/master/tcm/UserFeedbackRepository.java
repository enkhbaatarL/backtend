package com.sk.domain.shared.repository.master.tcm;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sk.domain.shared.entity.master.tcm.UserFeedback;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
}
