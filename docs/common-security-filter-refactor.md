# common-security 过滤器拆分修改文档

## 1. 背景

当前 `common-security` 中的 `AuthTokenFilter` 同时承担了两类职责：

1. 从网关传入的 `x-user-*` 请求头恢复用户上下文
2. 解析 `Authorization: Bearer ...` 中的 JWT Token

因此 `AuthTokenFilter` 的构造函数强依赖 `TokenService`：

```java
public class AuthTokenFilter extends OncePerRequestFilter implements Ordered {

    private final TokenService tokenService;
}
```

业务服务如果只依赖 `common-security`，没有引入 `common-security-jwt`，容器中就不会有 `TokenService` Bean，启动时会出现：

```text
Parameter 0 of constructor in com.dev.lib.security.filter.AuthTokenFilter
required a bean of type 'com.dev.lib.security.service.TokenService' that could not be found.
```

这不是业务服务需要重复解析 Token，而是过滤器职责没有拆开。

## 2. 目标架构

认证流程调整为：

```text
客户端
  |
  v
APISIX
  - 清理外部伪造的 x-user-* 请求头
  - 完成认证或转发到 auth 服务
  - 注入可信的 x-user-* 请求头
  |
  v
业务服务
  - 不解析 Token
  - 从 x-user-* 请求头恢复用户上下文
  - 使用 @RequireRole / @RequirePermission 做权限校验
```

只有 auth 服务或认证网关需要解析 Token。

目标依赖关系：

```text
auth 服务 / 认证网关：
  common-security
  common-security-jwt

普通业务服务：
  common-security
```

## 3. 过滤器拆分

### 3.1 过滤器顺序

| 顺序 | 过滤器 | 所在模块 | 职责 |
| --- | --- | --- | --- |
| 10000 | `UserContextFilter` | `common-security` | 从可信 `x-user-*` 请求头恢复用户上下文 |
| 10001 | `JwtAuthFilter` | `common-security-jwt` | 没有用户上下文时才解析 Bearer Token |
| 10011 | `UserContextRelayFilter` | `common-security` | 把当前用户上下文重新写入请求头，供后续转发 |

### 3.2 顺序说明

```text
UserContextFilter  ->  从请求头恢复用户
JwtAuthFilter      ->  如果仍为匿名，才解析 Token
UserContextRelay    ->  把已认证用户写回可信请求头
```

`APISIX` 的职责是清理外部请求中的 `x-user-*`，避免客户端伪造用户身份。`AuthGlobalFilter` 不再由 Spring 服务实现。

## 4. 逐文件改动

### 4.1 新增 `SecurityFilterOrder`

文件：

```text
common-security/src/main/java/com/dev/lib/security/filter/SecurityFilterOrder.java
```

作用：统一过滤器顺序，避免各个模块和网关重复定义魔法数字。

```java
package com.dev.lib.security.filter;

public final class SecurityFilterOrder {

    public static final int USER_CONTEXT = 10000;

    public static final int JWT_AUTH = 10001;

    public static final int USER_CONTEXT_RELAY = 10011;

    private SecurityFilterOrder() {
    }
}
```

### 4.2 新增 `UserContextFilter`

文件：

```text
common-security/src/main/java/com/dev/lib/security/filter/UserContextFilter.java
```

职责：

- 使用 `SecurityContextHolder.withAnonymous` 绑定整个请求链的用户上下文
- 从 `UserContextHeaders.from(request)` 读取网关注入的用户信息
- 不再依赖 `TokenService`

```java
package com.dev.lib.security.filter;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.security.web.UserContextHeaders;
import com.dev.lib.util.StringUtils;
import com.dev.lib.web.model.ServerResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserContextFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = SecurityFilterOrder.USER_CONTEXT;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            SecurityContextHolder.withAnonymous(() -> {
                try {
                    setContextInfo(request);
                } catch (TokenException ex) {
                    ServerResponse.fail(ex.getCoder(), ex.getMsger()).to(response);
                    return;
                }

                try {
                    filterChain.doFilter(request, response);
                } catch (IOException | ServletException ex) {
                    throw new FilterChainException(ex);
                }
            });
        } catch (FilterChainException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof ServletException servletException) {
                throw servletException;
            }
            throw ex;
        }
    }

    private void setContextInfo(HttpServletRequest request) {
        UserContextHeaders.from(request).ifPresent(user -> {
            fillMissingClientInfo(request, user);
            SecurityContextHolder.set(user);
        });
    }

    private void fillMissingClientInfo(HttpServletRequest request, UserDetails userDetail) {
        if (StringUtils.isBlank(userDetail.getClientIp())) {
            userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
        }
        if (StringUtils.isBlank(userDetail.getClientType())) {
            userDetail.setClientType(ClientInfoExtractor.getClientType(request));
        }
        if (StringUtils.isBlank(userDetail.getDeviceId())) {
            userDetail.setDeviceId(request.getHeader("X-Device-Id"));
        }
    }

    private static class FilterChainException extends RuntimeException {

        private FilterChainException(Exception cause) {
            super(cause);
        }
    }
}
```

### 4.3 新增 `JwtAuthFilter`

文件：

```text
common-security-jwt/src/main/java/com/dev/lib/security/jwt/JwtAuthFilter.java
```

职责：

- 仅在当前上下文仍然匿名时解析 Token
- 请求头用户优先，避免网关注入用户后重复解析 Token
- 只有 auth 服务或认证网关引入 `common-security-jwt`

```java
package com.dev.lib.security.jwt;

import com.dev.lib.security.TokenException;
import com.dev.lib.security.filter.SecurityFilterOrder;
import com.dev.lib.security.service.TokenService;
import com.dev.lib.security.util.ClientInfoExtractor;
import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.util.StringUtils;
import com.dev.lib.web.model.ServerResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = SecurityFilterOrder.JWT_AUTH;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!SecurityContextHolder.isAnonymous()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (StringUtils.isBlank(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UserDetails userDetail = tokenService.parseToken(token);
            if (userDetail != null) {
                setContextUser(request, userDetail);
            }
        } catch (TokenException ex) {
            ServerResponse.fail(ex.getCoder(), ex.getMsger()).to(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        return (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX))
               ? bearerToken.substring(BEARER_PREFIX.length())
               : null;
    }

    private void setContextUser(HttpServletRequest request, UserDetails userDetail) {
        userDetail.setClientIp(ClientInfoExtractor.getClientIp(request));
        userDetail.setClientType(ClientInfoExtractor.getClientType(request));
        userDetail.setDeviceId(request.getHeader("X-Device-Id"));
        SecurityContextHolder.set(userDetail);
    }
}
```

注意：这里用 `!SecurityContextHolder.isAnonymous()`，而不是 `isLogin()`。这样即使网关传入的用户是 `validated=false`，也不会继续解析 Bearer Token，保持“请求头用户优先”的语义。

### 4.4 新增 `UserContextRelayFilter`

文件：

```text
common-security/src/main/java/com/dev/lib/security/filter/UserContextRelayFilter.java
```

职责：把 `SecurityContextHolder` 中的可信用户重新写入请求头，供后续 Servlet 转发使用。

```java
package com.dev.lib.security.filter;

import com.dev.lib.security.util.SecurityContextHolder;
import com.dev.lib.security.util.UserDetails;
import com.dev.lib.security.web.HeaderRewriteRequestWrapper;
import com.dev.lib.security.web.UserContextHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserContextRelayFilter extends OncePerRequestFilter implements Ordered {

    public static final int ORDER = SecurityFilterOrder.USER_CONTEXT_RELAY;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!SecurityContextHolder.isLogin()) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails user = SecurityContextHolder.get();
        HeaderRewriteRequestWrapper wrappedRequest = new HeaderRewriteRequestWrapper(
                request,
                UserContextHeaders.names(),
                UserContextHeaders.toHeaders(user)
        );

        filterChain.doFilter(wrappedRequest, response);
    }
}
```

`UserContextRelayFilter` 只能改写当前 Servlet 请求头。如果下游调用使用 `RestClient`、`WebClient` 或 Dubbo，需要分别在客户端拦截器或 Dubbo Filter 中传播用户上下文。

### 4.5 移动 `HeaderRewriteRequestWrapper`

从：

```text
common-cloud-gateway/src/main/java/com/dev/lib/cloud/HeaderRewriteRequestWrapper.java
```

移动到：

```text
common-security/src/main/java/com/dev/lib/security/web/HeaderRewriteRequestWrapper.java
```

包名从：

```java
package com.dev.lib.cloud;
```

改为：

```java
package com.dev.lib.security.web;
```

实现不变。

### 4.6 删除旧过滤器

删除：

```text
common-security/src/main/java/com/dev/lib/security/filter/AuthTokenFilter.java
common-security/src/test/java/com/dev/lib/security/filter/AuthTokenFilterTest.java
```

### 4.7 测试调整

新增：

```text
common-security/src/test/java/com/dev/lib/security/filter/UserContextFilterTest.java
common-security/src/test/java/com/dev/lib/security/filter/UserContextRelayFilterTest.java
common-security-jwt/src/test/java/com/dev/lib/security/jwt/JwtAuthFilterTest.java
```

测试重点：

1. 网关注入的 `x-user-*` 能恢复用户上下文
2. 没有请求头时保持匿名上下文
3. 非法请求头返回失败响应
4. Bearer Token 能解析并写入上下文
5. 请求头用户优先，Token 不被重复解析
6. 无效 Token 返回失败响应
7. `UserContextRelayFilter` 在登录用户下写入可信请求头

### 4.8 `common-cloud-gateway` 处理

该模块计划删除。删除前如需保持编译，补丁已经做了以下兼容：

- `AuthGlobalFilter` 改用 `SecurityFilterOrder.USER_CONTEXT`
- 网关的 `UserContextRelayFilter` 改用 `SecurityFilterOrder.USER_CONTEXT_RELAY`
- 删除网关内的 `HeaderRewriteRequestWrapper`，改为引用 `common-security`

正式上线前建议：

```xml
<!-- common-lib/pom.xml -->
<!-- 移除 -->
<module>common-cloud-gateway</module>
```

并删除 `common-cloud-gateway` 目录。

注意：在网关模块未删除前，`com.dev.lib.cloud.UserContextRelayFilter` 和
`com.dev.lib.security.filter.UserContextRelayFilter` 可能同时被组件扫描，造成重复注册。
因此应尽快完成模块移除。

## 5. 依赖调整

### auth 服务 / 认证网关

需要：

```xml
<dependency>
    <groupId>io.github.ilovejavac</groupId>
    <artifactId>common-security</artifactId>
</dependency>

<dependency>
    <groupId>io.github.ilovejavac</groupId>
    <artifactId>common-security-jwt</artifactId>
</dependency>
```

同时需要：

- `app.security.jwt.secret`
- `AuthenticateService` Bean

因为 `JwtTokenService.parseToken()` 会调用 `AuthenticateService.loadUserById()`。

### 普通业务服务

只需要：

```xml
<dependency>
    <groupId>io.github.ilovejavac</groupId>
    <artifactId>common-security</artifactId>
</dependency>
```

不再需要：

```text
common-security-jwt
app.security.jwt.secret
TokenService Bean
```

## 6. 权限注册说明

`PermissionRegistrationConfig` 上存在：

```java
@ConditionalOnBean(AuthenticateService.class)
```

因此业务服务没有 `AuthenticateService` 时，不会自动注册接口权限。

这不影响：

- `@RequireRole`
- `@RequirePermission`
- `@Anonymous`

这些注解的校验依赖 `SecurityContextHolder` 中的用户角色和权限。

如果业务服务也需要向 auth 服务注册接口权限，需要额外提供 `AuthenticateService` Bean，通常通过 Dubbo 远程调用 auth 服务。

## 7. 安全边界

必须保证业务服务只接受可信来源的 `x-user-*` 请求头。

APISIX 需要：

1. 从外部请求中移除客户端传入的 `x-user-*` 头
2. 在认证完成后注入服务端生成的用户头
3. 禁止客户端绕过 APISIX 直接访问业务服务

否则客户端可以伪造：

```text
x-user-id
x-user-role
x-user-permission
x-user-validated
```

直接冒充其他用户。

如果业务服务网络不能完全隔离，建议使用内部签名头或 mTLS，而不是只信任普通请求头。

## 8. 应用补丁

补丁位置：

```text
/Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
```

应用前检查：

```bash
cd /Users/lucheng/project/dev/common-lib
git apply --check /Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
```

应用：

```bash
git apply /Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
```

回滚：

```bash
git apply -R /Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
```

如果只想查看改动：

```bash
git apply --stat /Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
git apply --numstat /Users/lucheng/project/ware4u/smart-plat/server/common-lib-security-filter-refactor.patch
```

## 9. 验证步骤

### 9.1 编译

如果 `common-cloud-gateway` 尚未删除：

```bash
cd /Users/lucheng/project/dev/common-lib
mvn -pl common-security,common-security-jwt,common-cloud-gateway -am test
```

如果已经删除：

```bash
cd /Users/lucheng/project/dev/common-lib
mvn -pl common-security,common-security-jwt -am test
```

### 9.2 验证 auth 服务

启动 auth 服务，确认：

- 没有 `No qualifying bean of type 'TokenService'`
- Bearer Token 可以正常解析
- `app.security.jwt.secret` 已配置
- `AuthenticateService` 可用

### 9.3 验证普通业务服务

只引入 `common-security`，启动服务，确认：

- 没有 `TokenService` 缺失错误
- 请求携带网关注入的 `x-user-*` 头时，权限注解正常生效
- 未携带用户头时，非匿名接口返回未认证

### 9.4 验证 APISIX

确认：

- 外部传入的 `x-user-*` 被清除
- 认证成功后注入可信用户头
- 直接访问业务服务不可绕过 APISIX

## 10. 风险与注意事项

1. `JwtAuthFilter` 依赖 `UserContextFilter` 已绑定 `ScopedValue` 上下文。两者分别位于 `common-security` 和 `common-security-jwt`，后者依赖前者，正常组件扫描时都会注册。
2. `common-cloud-gateway` 删除前，避免同时启用两个 `UserContextRelayFilter`。
3. `UserContextRelayFilter` 只处理当前 Servlet 请求，不处理 `RestClient`、`WebClient`、Dubbo 的出站请求。
4. 业务服务必须处于可信网络，或使用内部签名头，不能直接暴露 `x-user-*` 信任逻辑。
5. 如果后续需要“业务服务也注册接口权限”，需要补齐远程 `AuthenticateService`，否则 `PermissionRegistrationConfig` 会被条件跳过。
