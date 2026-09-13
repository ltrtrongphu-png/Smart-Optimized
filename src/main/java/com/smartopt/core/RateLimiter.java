package com.smartopt.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ đếm tần suất theo "cửa sổ trượt" (sliding window) đơn giản, dùng để phát hiện
 * và chặn các máy lag redstone (clock quá nhanh) hoặc bất kỳ hành động nào lặp lại
 * quá nhiều lần trong 1 khoảng thời gian ngắn tại cùng 1 vị trí/khoá.
 *
 * Thuần Java, không phụ thuộc Bukkit -> test độc lập được.
 */
public class RateLimiter {

    private static final class Counter {
        long windowStart;
        int count;
    }

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final long windowMillis;

    public RateLimiter(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Ghi nhận 1 sự kiện tại "key" vào thời điểm "nowMillis".
     * Trả về true nếu sự kiện này VƯỢT QUÁ giới hạn cho phép trong cửa sổ hiện tại
     * (nghĩa là nên bị chặn/huỷ), false nếu vẫn trong giới hạn cho phép.
     */
    public boolean isOverLimit(String key, int maxEventsPerWindow, long nowMillis) {
        Counter c = counters.computeIfAbsent(key, k -> {
            Counter nc = new Counter();
            nc.windowStart = nowMillis;
            nc.count = 0;
            return nc;
        });

        synchronized (c) {
            if (nowMillis - c.windowStart >= windowMillis) {
                // sang cửa sổ mới -> reset đếm
                c.windowStart = nowMillis;
                c.count = 0;
            }
            c.count++;
            return c.count > maxEventsPerWindow;
        }
    }

    /** Dọn các key đã lâu không hoạt động để tránh rò rỉ bộ nhớ trên server chạy lâu dài. */
    public void cleanupStale(long nowMillis, long staleAfterMillis) {
        counters.entrySet().removeIf(e -> (nowMillis - e.getValue().windowStart) > staleAfterMillis);
    }

    public int size() {
        return counters.size();
    }
}
