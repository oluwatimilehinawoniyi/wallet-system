package com.wallet.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.exception.BadRequestException;

import java.lang.reflect.Type;
import java.time.Duration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object cacheResponse(ProceedingJoinPoint joinPoint,
                                Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BadRequestException(
                    "No active request context found for idempotent operation");
        }

        String keyValue =
                attributes.getRequest().getHeader(idempotent.keyHeader());
        if (keyValue == null || keyValue.isBlank()) {
            throw new BadRequestException(
                    idempotent.keyHeader() + " header is required");
        }

        String cacheKey = "idempotency:" + keyValue;
        String cachedResponse =
                stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            log.info("Idempotency cache hit for key {}", keyValue);
            Type returnType =
                    ((MethodSignature) joinPoint.getSignature()).getMethod()
                            .getGenericReturnType();
            return objectMapper.readValue(cachedResponse,
                    objectMapper.constructType(returnType));
        }

        // The first successful execution is cached so retries do not repeat writes or republish side effects.
        Object result = joinPoint.proceed();
        stringRedisTemplate.opsForValue()
                .set(cacheKey, objectMapper.writeValueAsString(result),
                        TTL);
        return result;
    }
}
