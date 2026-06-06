# TinyHr 컨벤션

호호HR(NestJS) 프로젝트의 핵심 모듈을 Spring Boot 로 발췌·이식하는 참고용 프로젝트.
운영 목적이 아니라 **구조·도메인 로직 학습/참고용**이며, 인메모리 DB(H2)로 단독 실행·테스트된다.

## 스택

- Java 21, Spring Boot 4.0.x (Gradle)
- 영속성: **쓰기 = Spring Data JPA**, **읽기 = MyBatis**
- DB: H2 (인메모리). JPA `ddl-auto`로 엔티티에서 스키마 생성
- 보안: Spring Security (현재는 전면 permit, iam 컨텍스트 이식 시 강화)
- 테스트: JUnit5 + Mockito + AssertJ (도메인·유즈케이스 **유닛테스트만**)
- Lombok: **`@Getter`만 사용. `@Setter` 절대 금지**

## 컨텍스트(이식 대상)와 순서

기반→의존 순으로 점진 이식한다.

1. **organization** (사원/부서/직위/직책) - 모든 컨텍스트의 기반
2. **iam** (인증 OTP·JWT / 인가 RBAC)
3. **file** (첨부/블롭/스토리지)
4. **notification** (인앱 알림/푸시)
5. **approval** (결재 허브)
6. **vacation** (휴가/휴직/잔고/부여)

## 패키지 구조

```
com.example.tinyhr
├── TinyHrApplication
├── shared/
│   ├── kernel/        # 도메인 예외 베이스(DomainException 및 상태별 하위), 공용 VO
│   └── config/        # SecurityConfig, GlobalExceptionHandler, MyBatis 설정 등
└── <context>/         # organization, iam, file, ...
    ├── domain/
    │   └── <aggregate>/   # Entity(JPA) · Repository(JPA 인터페이스) · VO · 도메인예외 · 도메인서비스
    ├── application/
    │   ├── <Aggregate>Service   # 유즈케이스(@Service, @Transactional)
    │   └── dto/                 # 요청 DTO(record)
    └── adapter/
        ├── web/        # @RestController
        └── mapper/     # MyBatis @Mapper 인터페이스 + 조회 뷰(record)
```

MyBatis 매퍼 XML: `src/main/resources/mapper/<context>/<Name>Mapper.xml`

> NestJS 의 `domain/application/adapter{web,persistence}` 를 그대로 가져오되, 아래 "Spring 차이"를 반영한다.

## NestJS → Spring 차이 (중요)

### 1. JPA 리포지토리는 구현체가 없다
- 쓰기 리포지토리는 **`domain/<agg>/XxxRepository extends JpaRepository<Xxx, String>`** 인터페이스 하나로 끝.
  Spring 이 구현을 생성하므로 **`adapter/persistence` 구현체를 두지 않는다.**
- 파생 쿼리(`existsByName`, `findByNameIgnoreCase` 등)·`@Query` 로 단건/단순 조회 해결.
- 도메인이 `JpaRepository`(Spring Data)에 의존하는 실용적 결합은 허용한다(참고 프로젝트).

### 2. 읽기(복잡 조회)는 MyBatis
- 조인·집계·뷰 응답은 **`adapter/mapper/Xxx QueryMapper`(@Mapper)** + `resources/mapper/.../*.xml`.
- 매퍼는 SQL 을 직접 실행하는 **출력 어댑터**라 `adapter` 에 둔다(application 은 영속 기술 무지).
- 컨트롤러(또는 얇은 read 서비스)가 매퍼를 주입해 뷰 record 를 그대로 반환.

### 3. 모듈/SPI 레지스트리 불필요
- NestJS 모듈·`OnModuleInit` self-register 레지스트리는 **Spring 단일 컨텍스트로 대체**.
- 다중 핸들러(예: 결재 후속처리)는 **`List<Handler>` 빈 주입** 후 kind 별 `Map` 구성으로 대체(레지스트리 클래스 불필요).
- OHS(Open Host Service)는 그냥 **`@Service` 빈** — 타 컨텍스트가 주입해 호출. 컨텍스트 공개 표면은 application 의 OHS 서비스로 표현한다.

### 4. 트랜잭션 = `@Transactional`
- NestJS 의 UnitOfWork 는 유즈케이스 메서드의 `@Transactional` 로 대체.

### 5. DTO 검증
- Zod → Jakarta Bean Validation(`@NotBlank`, `@Size` …) + `@Valid`.

## 도메인 모델 규칙 (Lombok getter-only)

- 엔티티 내부 상태는 **`private` 기본**. `@Getter`만, **`@Setter` 금지**.
- 상태 변경은 **도메인 메서드로만**(`rename()`, `archive()` …). 외부에서 필드 직접 수정 불가.
- 생성은 **정적 팩토리**(`create`/`provision`/`issue`)로. 불변식·정규화(trim 등)를 팩토리·메서드에서 강제. `@Builder`는 불변식 우회 위험이 있어 애그리거트엔 지양.
- **생성자 규칙(정론)**: 외부 생성자는 차단한다. JPA 용 **`protected` 빈 생성자 하나만**(`@NoArgsConstructor(access = PROTECTED)`) 두고, 정적 팩토리는 `new X()`(빈 생성자)로 인스턴스를 만든 뒤 **필드를 직접 주입**한다. **파라미터 생성자를 만들지 않는다**(엔티티 간 일관성).
  ```java
  public static Position create(String name, int order, String criteria) {
      Position p = new Position();   // 빈 생성자
      p.id = UUID.randomUUID().toString();
      p.name = name.trim();
      ...
      return p;
  }
  ```
- 식별자(ID)는 도메인이 소유: 팩토리에서 `UUID.randomUUID().toString()` 생성(NestJS `crypto.randomUUID()` 대응). 타입은 `String`.
- 불변 VO·조회 뷰·요청 DTO 는 Java 21 **`record`** 사용.
- 소프트 삭제는 `archivedAt`(nullable) + `isActive` 플래그로 표현(전역 필터 미적용 — 조회에서 명시 처리).
- SQL 예약어 충돌 피함: 예) `order` → 필드 `displayOrder`(컬럼 `display_order`).

## 예외/에러 (ErrorCode enum + 단일 BusinessException)

에러마다 예외 클래스를 만들지 않는다(Java 보일러플레이트 폭발 방지).

- `shared/kernel/ErrorCode`(인터페이스: `code()`·`status()`·`message()`) + `shared/kernel/BusinessException`(ErrorCode 보유) 하나.
- **컨텍스트별 ErrorCode enum** 카탈로그를 둔다: `<context>/domain/<Context>ErrorCode`(코드·HTTP상태·메시지). 예: `OrganizationErrorCode.DEPARTMENT_DEPTH_EXCEEDED`.
- 도메인/유즈케이스는 `throw new BusinessException(OrganizationErrorCode.XXX)` 로만 던진다.
- `shared/config/GlobalExceptionHandler`(@RestControllerAdvice)가 `BusinessException` 하나를 받아 `code.status()` 로 매핑, `{ code, message }` 응답.
- 테스트는 `isInstanceOf(BusinessException.class)` + `getErrorCode()` 가 기대 enum 인지 단언.

## 테스트 (유닛만)

- 위치: 대상과 같은 패키지 `src/test/java/...`.
- **도메인 엔티티/VO 테스트**: 순수 JUnit5, Spring 미기동.
- **유즈케이스(서비스) 테스트**: 리포지토리(JPA 인터페이스)·협력 포트는 mock. AssertJ 단언.
- **메서드명은 영문**(식별자), **설명은 `@DisplayName`** 으로 분리한다. 메서드명에 한글 금지.
- **`@DisplayName` 은 비즈니스 관점**으로 쓴다. 에러 코드·기술 용어(enum 이름 등) 노출 금지.
  - (X) `"POSITION_NAME_DUPLICATED 이면 예외"`  (O) `"이미 같은 이름의 직위가 있으면 등록할 수 없다"`
- **given/when/then**: 모킹은 **BDDMockito** 로 통일한다 — `given(...).willReturn(...)`(setup), 검증은 `then(...).should()`. 이렇게 해야 `// given` 구역과 코드(`given`)가 일치하고, `when` 키워드 혼동이 없다.
  - `// given` = 상태/스텁 셋업, `// when` = 대상 동작 호출, `// then` = 결과 단언. 예외 케이스는 호출+단언이 융합되므로 `// when & then` 허용.
- 비즈니스 예외 검증은 `isInstanceOf(BusinessException.class)` + `getErrorCode()` 가 기대 enum 인지 단언(공용 헬퍼 `assertBusiness`).
- 통합/웹 테스트는 만들지 않음(스캐폴드의 `contextLoads` 스모크만 유지).

## 커밋

- Conventional Commits. 한 컨텍스트/슬라이스 단위로 작게.
- 예: `feat(organization): 직위(position) 슬라이스 이식`
