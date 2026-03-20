package com.wallet.common.aop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.response.ApiResponse;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        idempotencyAspect = new IdempotencyAspect(stringRedisTemplate, new ObjectMapper().findAndRegisterModules());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", "abc-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsCachedResponseOnCacheHit() throws Throwable {
        ApiResponse<String> cached = ApiResponse.success("cached", "value");
        when(valueOperations.get("idempotency:abc-123")).thenReturn(new ObjectMapper().findAndRegisterModules().writeValueAsString(cached));
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(testMethod("sampleMethod"));

        Object result = idempotencyAspect.cacheResponse(joinPoint, annotation());

        assertEquals(cached.message(), ((ApiResponse<?>) result).message());
        assertEquals(cached.data(), ((ApiResponse<?>) result).data());
    }

    @Test
    void proceedsAndCachesOnCacheMiss() throws Throwable {
        ApiResponse<String> live = ApiResponse.success("live", "value");
        when(valueOperations.get("idempotency:abc-123")).thenReturn(null);
        when(joinPoint.proceed()).thenReturn(live);

        Object result = idempotencyAspect.cacheResponse(joinPoint, annotation());

        assertEquals(live, result);
        verify(valueOperations).set(org.mockito.ArgumentMatchers.eq("idempotency:abc-123"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    private Idempotent annotation() throws NoSuchMethodException {
        return testMethod("annotatedMethod").getAnnotation(Idempotent.class);
    }

    private Method testMethod(String name) throws NoSuchMethodException {
        return TestTarget.class.getDeclaredMethod(name);
    }

    static class TestTarget {
        @Idempotent
        void annotatedMethod() {
        }

        ApiResponse<String> sampleMethod() {
            return ApiResponse.success("x", "y");
        }
    }
}
