package com.sk.domain.shared.entity.compositeKey.tif;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefinitivNewsDataId implements Serializable {

    @Column(name = "data_reg_sn")
    private Integer dataRegistrationSn;

    @Column(name = "first_created")
    private LocalDateTime firstCreated;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefinitivNewsDataId that = (RefinitivNewsDataId) o;
        return Objects.equals(dataRegistrationSn, that.dataRegistrationSn) &&
               Objects.equals(firstCreated, that.firstCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataRegistrationSn, firstCreated);
    }
}