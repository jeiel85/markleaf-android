# Markleaf Architecture

## 목표

Markleaf의 아키텍처는 단순하고 검증 가능해야 한다.  
MVP에서는 과도한 추상화보다 명확한 책임 분리를 우선한다.

## 기본 원칙

- UI는 데이터베이스에 직접 접근하지 않는다.
- ViewModel은 UI 상태를 관리한다.
- Repository는 데이터 소스를 조정한다.
- Room entity와 domain model을 분리한다.
- core utility는 순수 함수로 만들고 unit test를 작성한다.
- 앱은 MVP에서 네트워크 계층을 갖지 않는다.

## 권장 패키지 구조

```text
com.markleaf.notes
  app
  core
    markdown
    text
    time
    result
  data
    local
      db
      dao
      entity
      mapper
    repository
    export
  domain
    model
    repository
    usecase
  feature
    notes
    editor
    tags
    search
    trash
    settings
  ui
    component
    navigation
    theme
```

## Layer Responsibilities

### UI Layer

- Compose screen
- Reusable component
- Theme
- Navigation
- UI event 전달
- ViewModel state rendering

UI layer는 다음을 하지 않는다.

- DAO 직접 호출
- Entity 직접 사용
- 파일 export 직접 처리
- business rule 직접 구현

### ViewModel Layer

- UI state 보관
- UI event 처리
- Repository 또는 UseCase 호출
- Flow collect
- debounce 처리

### Domain Layer

- domain model
- repository interface
- use case
- business rule

### Data Layer

- Room database
- DAO
- entity
- mapper
- repository implementation
- Markdown export implementation

### Core Layer

- tag parser
- title extractor
- excerpt generator
- slug generator
- date/time utility
- result wrapper

## MVP에서 없는 레이어

MVP에서는 다음 레이어를 만들지 않는다.

- network layer
- remote data source
- API client
- analytics service
- auth service
- sync engine

## 데이터 흐름

```text
Compose Screen
  -> ViewModel
    -> UseCase or Repository
      -> Room DAO
        -> SQLite
```

## 테스트 우선 대상

- TagParser
- TitleExtractor
- ExcerptGenerator
- SlugGenerator
- Note sorting
- Repository behavior
- DAO query
- Room migration

## 위험 요소

### 과도한 아키텍처

초기부터 Clean Architecture를 너무 엄격하게 적용하면 개발 속도가 느려질 수 있다.  
MVP에서는 구조를 단순하게 유지한다.

### 네트워크 계층 유입

MVP에서는 네트워크 기능이 없어야 한다.  
`android.permission.INTERNET`이 생기면 설계 위반으로 본다.

### 의존성 증가

Markdown 렌더러, DI, export helper 등은 필요할 때만 추가한다.  
F-Droid 호환성을 해치지 않는지 확인한다.
