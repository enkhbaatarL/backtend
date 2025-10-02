package com.sk.domain.shared.entity.master.tif;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tif.RefinitivNewsDataId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Comment("TIF_Refinitiv News Data_수신")
@Table(name = "tif_refinewsdatar")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefinitivNewsData extends BaseEntity {

    @EmbeddedId
    private RefinitivNewsDataId id;

    @Column(name = "story_id", length = 100, unique = true)
    @Comment("NEWSID")
    private String storyId;

    @Column(name = "version_created")
    @Comment("NEWS최종수정일시")
    private LocalDateTime versionCreated;

    @Column(name = "pub_status", length = 20)
    @Comment("NEWS발간상태")
    private String pubStatus;

    @Column(name = "title", length = 1000)
    @Comment("NEWS제목")
    private String title;

    @Lob
    @Column(name = "story", columnDefinition = "MEDIUMTEXT")
    @Comment("NEWS본문")
    private String story;

    @Column(name = "expires")
    @Comment("NEWS만료일시")
    private LocalDateTime expires;

    @Builder
    public RefinitivNewsData(LocalDateTime createDt, LocalDateTime modifyDt,
                           String createUserId, String modifyUserId, Boolean isDeleted,
                           RefinitivNewsDataId id, String storyId, LocalDateTime versionCreated,
                           String pubStatus, String title, String story, LocalDateTime expires) {
        super(createDt, modifyDt, createUserId, modifyUserId, isDeleted);
        this.id = id;
        this.storyId = storyId;
        this.versionCreated = versionCreated;
        this.pubStatus = pubStatus;
        this.title = title;
        this.story = story;
        this.expires = expires;
    }

    public static RefinitivNewsData of(Integer dataRegistrationSn, LocalDateTime firstCreated,
                                     String storyId, LocalDateTime versionCreated, String pubStatus,
                                     String title, String story, LocalDateTime expires) {
        return RefinitivNewsData.builder()
                .id(new RefinitivNewsDataId(dataRegistrationSn, firstCreated))
                .storyId(storyId)
                .versionCreated(versionCreated)
                .pubStatus(pubStatus)
                .title(title)
                .story(story)
                .expires(expires)
                .build();
    }
}
