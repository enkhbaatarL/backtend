package com.sk.domain.shared.repository.master.tcm;

import static org.assertj.core.api.Assertions.assertThat;

import com.sk.domain.shared.entity.compositeKey.tcm.BulletinBoardCustomerCompanyMappingId;
import com.sk.domain.shared.entity.master.tcm.BulletinBoard;
import com.sk.domain.shared.entity.master.tcm.BulletinBoardCustomerCompanyMapping;
import com.sk.domain.shared.entity.master.tmd.CustomerCompany;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.orm.jpa.SharedEntityManagerCreator;

@DataJpaTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:bulletin-board;MODE=MySQL;"
                        + "DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        },
        excludeAutoConfiguration = JpaRepositoriesAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BulletinBoardRepositoryTest.JpaSliceSupport.class)
@DisplayName("BulletinBoardRepository 공지사항 접근 범위 테스트")
class BulletinBoardRepositoryTest {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String SYSTEM_USER = "SYSTEM";

    @Autowired
    private BulletinBoardRepository bulletinBoardRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void registerMySqlDateFormatFunction() {
        entityManager.getEntityManager()
                .createNativeQuery("""
                        CREATE ALIAS IF NOT EXISTS DATE_FORMAT
                        FOR 'com.sk.testsupport.H2MySqlFunctions.dateFormat'
                        """)
                .executeUpdate();
    }

    @Test
    @DisplayName("현재 게시 중인 전체 공지는 고객사 매핑 없이 조회한다")
    void should_findNotice_when_activeWholePost() {
        BulletinBoard notice = persistBulletinBoard(
                "NOTICE",
                true,
                today(-1),
                today(1),
                false);

        assertThat(bulletinBoardRepository.findAccessibleNoticeForService(
                notice.getBulletinNo(),
                999))
                .contains(notice);
    }

    @Test
    @DisplayName("현재 게시 중인 고객사 지정 공지는 매핑된 고객사에서 조회한다")
    void should_findNotice_when_activeAndMappedToCompany() {
        CustomerCompany company = persistCustomerCompany("COMPANY_A");
        BulletinBoard notice = persistBulletinBoard(
                "NOTICE",
                false,
                today(-1),
                today(1),
                false);
        persistMapping(notice, company);

        assertThat(bulletinBoardRepository.findAccessibleNoticeForService(
                notice.getBulletinNo(),
                company.getCustomerCompanyNo()))
                .contains(notice);
    }

    @Test
    @DisplayName("다른 고객사에만 매핑된 공지는 조회하지 않는다")
    void should_notFindNotice_when_mappedToAnotherCompany() {
        CustomerCompany mappedCompany = persistCustomerCompany("COMPANY_A");
        CustomerCompany requestingCompany = persistCustomerCompany("COMPANY_B");
        BulletinBoard notice = persistBulletinBoard(
                "NOTICE",
                false,
                today(-1),
                today(1),
                false);
        persistMapping(notice, mappedCompany);

        assertThat(bulletinBoardRepository.findAccessibleNoticeForService(
                notice.getBulletinNo(),
                requestingCompany.getCustomerCompanyNo()))
                .isEmpty();
    }

    @ParameterizedTest(name = "{0} 공지는 조회하지 않는다")
    @EnumSource(InaccessibleNoticeCase.class)
    @DisplayName("게시 범위 밖 공지는 조회하지 않는다")
    void should_notFindNotice_when_outsideAccessibleScope(InaccessibleNoticeCase testCase) {
        BulletinBoard notice = persistBulletinBoard(
                testCase.bulletinTypeCode,
                true,
                today(testCase.beginDayOffset),
                today(testCase.endDayOffset),
                testCase.deleted);

        assertThat(bulletinBoardRepository.findAccessibleNoticeForService(
                notice.getBulletinNo(),
                999))
                .isEmpty();
    }

    @Test
    @DisplayName("게시 시작일과 종료일이 오늘인 공지는 조회한다")
    void should_findNotice_when_validPeriodBoundaryIsToday() {
        BulletinBoard notice = persistBulletinBoard(
                "NOTICE",
                true,
                today(0),
                today(0),
                false);

        assertThat(bulletinBoardRepository.findAccessibleNoticeForService(
                notice.getBulletinNo(),
                999))
                .contains(notice);
    }

    private BulletinBoard persistBulletinBoard(
            String bulletinTypeCode,
            boolean wholePost,
            String validBeginDate,
            String validEndDate,
            boolean deleted) {
        LocalDateTime now = LocalDateTime.now();
        BulletinBoard bulletinBoard = BulletinBoard.builder()
                .createDt(now)
                .modifyDt(now)
                .createUserId(SYSTEM_USER)
                .modifyUserId(SYSTEM_USER)
                .isDeleted(deleted)
                .bulletinTypeCd(bulletinTypeCode)
                .isTopFixed(false)
                .isPopupUse(false)
                .bulletinCategorySeparateCd("CATEGORY1")
                .isWholePost(wholePost)
                .bulletinValidBeginDt(validBeginDate)
                .bulletinValidEndDt(validEndDate)
                .isValidPerioUnrestrict(false)
                .bulletinTitleNm("공지 제목")
                .bulletinCntt("공지 내용")
                .build();

        return entityManager.persistAndFlush(bulletinBoard);
    }

    private CustomerCompany persistCustomerCompany(String companyCode) {
        LocalDateTime now = LocalDateTime.now();
        CustomerCompany company = CustomerCompany.builder()
                .createDt(now)
                .modifyDt(now)
                .createUserId(SYSTEM_USER)
                .modifyUserId(SYSTEM_USER)
                .isDeleted(false)
                .customerCompanyNm(companyCode)
                .customerCompanyCd(companyCode)
                .useBeginDt(today(-1))
                .useEndDt(today(1))
                .isUsed(true)
                .isMipManagementCompany(false)
                .isQualitative(false)
                .isMari(true)
                .isFuture(false)
                .isNewsLetter(false)
                .build();

        return entityManager.persistAndFlush(company);
    }

    private void persistMapping(BulletinBoard notice, CustomerCompany company) {
        LocalDateTime now = LocalDateTime.now();
        BulletinBoardCustomerCompanyMapping mapping = BulletinBoardCustomerCompanyMapping.builder()
                .createDt(now)
                .modifyDt(now)
                .createUserId(SYSTEM_USER)
                .modifyUserId(SYSTEM_USER)
                .isDeleted(false)
                .id(BulletinBoardCustomerCompanyMappingId.of(
                        notice.getBulletinNo(),
                        company.getCustomerCompanyNo()))
                .bulletinBoard(notice)
                .customerCompany(company)
                .build();

        entityManager.persistAndFlush(mapping);
    }

    private String today(int dayOffset) {
        return LocalDate.now().plusDays(dayOffset).format(YYYYMMDD);
    }

    private enum InaccessibleNoticeCase {
        FUTURE("NOTICE", 1, 2, false),
        EXPIRED("NOTICE", -2, -1, false),
        DELETED("NOTICE", -1, 1, true),
        OTHER_TYPE("FAQ", -1, 1, false);

        private final String bulletinTypeCode;
        private final int beginDayOffset;
        private final int endDayOffset;
        private final boolean deleted;

        InaccessibleNoticeCase(
                String bulletinTypeCode,
                int beginDayOffset,
                int endDayOffset,
                boolean deleted) {
            this.bulletinTypeCode = bulletinTypeCode;
            this.beginDayOffset = beginDayOffset;
            this.endDayOffset = endDayOffset;
            this.deleted = deleted;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JpaSliceSupport {

        @Bean
        BulletinBoardRepository bulletinBoardRepository(EntityManagerFactory entityManagerFactory) {
            return new JpaRepositoryFactory(
                    SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory))
                    .getRepository(BulletinBoardRepository.class);
        }
    }
}
