package java.util.concurrent;

public enum TimeUnit {
    NANOSECONDS {
        public long toNanos(long d)   { return d; }
        public long toMicros(long d)  { return d/(C1/C0); }
        public long toMillis(long d)  { return d/(C2/C0); }
        public long toSeconds(long d) { return d/(C3/C0); }
        public long toMinutes(long d) { return d/(C4/C0); }
        public long toHours(long d)   { return d/(C5/C0); }
        public long toDays(long d)    { return d/(C6/C0); }
        public long convert(long d, TimeUnit u) { return u.toNanos(d); }
        int excessNanos(long d, long m) { return (int)(d - (m*C2)); }
    },

    MICROSECONDS {
        public long toNanos(long d)   { return x(d, C1/C0, MAX/(C1/C0)); }
        public long toMicros(long d)  { return d; }
        public long toMillis(long d)  { return d/(C2/C1); }
        public long toSeconds(long d) { return d/(C3/C1); }
        public long toMinutes(long d) { return d/(C4/C1); }
        public long toHours(long d)   { return d/(C5/C1); }
        public long toDays(long d)    { return d/(C6/C1); }
        public long convert(long d, TimeUnit u) { return u.toMicros(d); }
        int excessNanos(long d, long m) { return (int)((d*C1) - (m*C2)); }
    },

    MILLISECONDS {
        public long toNanos(long d)   { return x(d, C2/C0, MAX/(C2/C0)); }
        public long toMicros(long d)  { return x(d, C2/C1, MAX/(C2/C1)); }
        public long toMillis(long d)  { return d; }
        public long toSeconds(long d) { return d/(C3/C2); }
        public long toMinutes(long d) { return d/(C4/C2); }
        public long toHours(long d)   { return d/(C5/C2); }
        public long toDays(long d)    { return d/(C6/C2); }
        public long convert(long d, TimeUnit u) { return u.toMillis(d); }
        int excessNanos(long d, long m) { return 0; }
    },

    SECONDS {
        public long toNanos(long d)   { return x(d, C3/C0, MAX/(C3/C0)); }
        public long toMicros(long d)  { return x(d, C3/C1, MAX/(C3/C1)); }
        public long toMillis(long d)  { return x(d, C3/C2, MAX/(C3/C2)); }
        public long toSeconds(long d) { return d; }
        public long toMinutes(long d) { return d/(C4/C3); }
        public long toHours(long d)   { return d/(C5/C3); }
        public long toDays(long d)    { return d/(C6/C3); }
        public long convert(long d, TimeUnit u) { return u.toSeconds(d); }
        int excessNanos(long d, long m) { return 0; }
    },

    MINUTES {
        public long toNanos(long d)   { return x(d, C4/C0, MAX/(C4/C0)); }
        public long toMicros(long d)  { return x(d, C4/C1, MAX/(C4/C1)); }
        public long toMillis(long d)  { return x(d, C4/C2, MAX/(C4/C2)); }
        public long toSeconds(long d) { return x(d, C4/C3, MAX/(C4/C3)); }
        public long toMinutes(long d) { return d; }
        public long toHours(long d)   { return d/(C5/C4); }
        public long toDays(long d)    { return d/(C6/C4); }
        public long convert(long d, TimeUnit u) { return u.toMinutes(d); }
        int excessNanos(long d, long m) { return 0; }
    },

    HOURS {
        public long toNanos(long d)   { return x(d, C5/C0, MAX/(C5/C0)); }
        public long toMicros(long d)  { return x(d, C5/C1, MAX/(C5/C1)); }
        public long toMillis(long d)  { return x(d, C5/C2, MAX/(C5/C2)); }
        public long toSeconds(long d) { return x(d, C5/C3, MAX/(C5/C3)); }
        public long toMinutes(long d) { return x(d, C5/C4, MAX/(C5/C4)); }
        public long toHours(long d)   { return d; }
        public long toDays(long d)    { return d/(C6/C5); }
        public long convert(long d, TimeUnit u) { return u.toHours(d); }
        int excessNanos(long d, long m) { return 0; }
    },

    DAYS {
        public long toNanos(long d)   { return x(d, C6/C0, MAX/(C6/C0)); }
        public long toMicros(long d)  { return x(d, C6/C1, MAX/(C6/C1)); }
        public long toMillis(long d)  { return x(d, C6/C2, MAX/(C6/C2)); }
        public long toSeconds(long d) { return x(d, C6/C3, MAX/(C6/C3)); }
        public long toMinutes(long d) { return x(d, C6/C4, MAX/(C6/C4)); }
        public long toHours(long d)   { return x(d, C6/C5, MAX/(C6/C5)); }
        public long toDays(long d)    { return d; }
        public long convert(long d, TimeUnit u) { return u.toDays(d); }
        int excessNanos(long d, long m) { return 0; }
    };

    // 时间常量，以纳秒为单位
    // 1纳秒
    static final long C0 = 1L;
    // 1微秒
    static final long C1 = C0 * 1000L;
    // 1毫秒
    static final long C2 = C1 * 1000L;
    // 1秒
    static final long C3 = C2 * 1000L;
    // 1分钟
    static final long C4 = C3 * 60L;
    // 1小时
    static final long C5 = C4 * 60L;
    // 1天
    static final long C6 = C5 * 24L;
    static final long MAX = Long.MAX_VALUE;

    /**
     * 时间单位转化方法
     * 如DAYS中toHours,则入参为 d = 1，m = C6/C5=24，over = MAX/m；返回值为24
     * d需要乘以m，over字段为MAX/m，保证了 d*m不超过Long的最大值
     */
    static long x(long d, long m, long over) {
        if (d >  over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }

    /**
     * 时间换算方法
     * 如 DAYS.convert(48, TimeUnit.HOURS) 即为48小时转化为天为单位 返回2
     */
    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        throw new AbstractMethodError();
    }

    public long toNanos(long duration) {
        throw new AbstractMethodError();
    }
    public long toMicros(long duration) {
        throw new AbstractMethodError();
    }
    public long toMillis(long duration) {
        throw new AbstractMethodError();
    }
    public long toSeconds(long duration) {
        throw new AbstractMethodError();
    }
    public long toMinutes(long duration) {
        throw new AbstractMethodError();
    }
    public long toHours(long duration) {
        throw new AbstractMethodError();
    }
    public long toDays(long duration) {
        throw new AbstractMethodError();
    }

    /**
     * 将毫秒以下 转化为纳秒
     * 毫秒以上单位 返回0
     */
    abstract int excessNanos(long d, long m);

    /**
     * wait
     */
    public void timedWait(Object obj, long timeout)
            throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            obj.wait(ms, ns);
        }
    }

    /**
     * join
     */
    public void timedJoin(Thread thread, long timeout)
            throws InterruptedException {
        if (timeout > 0) {
            long ms = toMillis(timeout);
            int ns = excessNanos(timeout, ms);
            thread.join(ms, ns);
        }
    }

    /**
     * sleep
     */
    public void sleep(long timeout) throws InterruptedException {
        if (timeout > 0) {
            // 以毫秒划分，往上部分转化为毫秒
            long ms = toMillis(timeout);
            // 毫秒以下转化为纳秒
            int ns = excessNanos(timeout, ms);
            Thread.sleep(ms, ns);
        }
    }
}