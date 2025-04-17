package com.sk.domain.shared.entity.master.tlm;

import java.time.LocalDateTime;

import com.sk.domain.shared.enums.Role;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;

import com.sk.domain.shared.entity.base.BaseEntity;
import com.sk.domain.shared.entity.compositeKey.tam.AdminPermissionId;
import com.sk.domain.shared.entity.compositeKey.tlm.GoodsDataInqueryUserId;
import com.sk.domain.shared.enums.UserRoleCd;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Comment("TLM_상품데이터조회사용자내역")
@Table(name = "tlm_gdsdatainqrusrl")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoodsDataInqueryUser extends BaseEntity {

    @EmbeddedId
    private GoodsDataInqueryUserId id;

}
