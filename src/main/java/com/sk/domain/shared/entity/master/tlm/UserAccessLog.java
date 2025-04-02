package com.sk.domain.shared.entity.master.tlm;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import com.sk.domain.shared.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tlm_usraccsl")
@Comment("TLM_사용자접속내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccessLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usr_accs_sn", nullable = false, columnDefinition = "int unsigned")
    @Comment("사용자접속일련번호")
    private Integer userAccessSequence;

    @Column(name = "usr_logn_id", nullable = false, length = 50)
    @Comment("사용자로그인ID")
    private String userLoginId;

    @Builder
    public UserAccessLog(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId, String modifyUserId,
            Boolean isDeleted, Integer userAccessSequence, String userLoginId) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.userAccessSequence = userAccessSequence;
        this.userLoginId = userLoginId;
    }

    public static UserAccessLog of(String userLoginId) {
        return UserAccessLog.builder()
            .userLoginId(userLoginId)
            .build();
    }
}