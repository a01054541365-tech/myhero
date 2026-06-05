# JJK Mod — 서버 기동 전 필수 설정

## 필수 server.properties 설정

```properties
gamemode=adventure       # survival 아님
spawn-protection=0       # 반드시 0
pvp=true
difficulty=normal
```

## 권장 JVM 플래그

```
-Xmx4G -Xms4G -XX:+UseG1GC
```

## 모드 의존성

- Fabric Loader 0.17.2+
- Fabric API 0.116.6+1.21.1
- GeckoLib Fabric 4.8.3 (4.8.4 Forge 전용 — 사용 금지)
- Player Animation Lib Fabric 2.0.4+1.21.1
