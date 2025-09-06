# 🎯 Eureka Server

MSA E-Commerce Platform의 서비스 디스커버리 서버입니다.

## 🚀 빠른 시작

### 개별 실행
```bash
# 1. 프로젝트 폴더로 이동
cd eureka-server

# 2. 실행
gradlew.bat bootRun

# 3. 확인
http://localhost:8761
```

### Docker 실행
```bash
# 빌드
gradlew.bat bootJar
docker build -t eureka-server .

# 실행
docker run -p 8761:8761 eureka-server
```

## 📋 설정

- **포트**: 8761
- **프로필**: 기본값 사용
- **의존성**: 없음 (독립 실행 가능)

## 🔍 기능

- 서비스 등록 및 발견
- 헬스체크 모니터링
- 서비스 인스턴스 관리
- 웹 대시보드 제공

## 📊 모니터링

- **대시보드**: http://localhost:8761
- **헬스체크**: http://localhost:8761/actuator/health
- **메트릭**: http://localhost:8761/actuator/metrics
