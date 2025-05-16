package com.sk.domain.shared.entity.master.tcm;

import com.sk.common.converter.BooleanToYNAttributeConverter;
import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardUserMappingId;
import com.sk.domain.shared.entity.master.tmd.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Getter
@Entity
@Table(name = "tcm_bbmnotiusrl")
@Comment("TCM_게시글알림사용자내역")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulletinBoardNotificationUser extends BaseEntity {

    @EmbeddedId
    private BulletinBoardUserMappingId id;

    @MapsId("bulletinNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bbm_no", nullable = false, referencedColumnName = "bbm_no")
    @Comment("게시판")
    private BulletinBoard bulletinBoard;

    @MapsId("userNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usr_no", referencedColumnName = "usr_no")
    private User user;

    @Column(name = "usr_inqr_yn", length = 1, columnDefinition = "char(1)")
    @Comment("사용자조회여부")
    @ColumnDefault("'N'")
    @Convert(converter = BooleanToYNAttributeConverter.class)
    private Boolean isRead;

    @Builder
    public BulletinBoardNotificationUser(LocalDateTime createDt, LocalDateTime modifyDt, String createUserId,
            String modifyUserId, Boolean isDeleted, BulletinBoardUserMappingId id,
            BulletinBoard bulletinBoard, User user, Boolean isRead) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.bulletinBoard = bulletinBoard;
        this.user = user;
        this.isRead = isRead;
    }

    public static BulletinBoardNotificationUser of(BulletinBoard bulletinBoard, User user, Boolean isRead) {
        return BulletinBoardNotificationUser.builder()
                .id(BulletinBoardUserMappingId.of(bulletinBoard.getBulletinNo(), user.getUserNo()))
                .bulletinBoard(bulletinBoard)
                .user(user)
                .isRead(isRead)
                .build();
    }
}
