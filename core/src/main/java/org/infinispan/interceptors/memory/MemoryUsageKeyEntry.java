package org.infinispan.interceptors.memory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Used by MemoryUsageInterceptor to keep track of space occupied by each value in cache
 *
 * @author David van Balen
 * @since 5.1
 */
public class MemoryUsageKeyEntry {
    
    private Object key;
    private AtomicLong size = new AtomicLong(0);
    
    public MemoryUsageKeyEntry(Object key, Long size) {
        this.key = key;
        this.size.set(size);
    }
    
    public Object getKey() {
        return key;
    }
    public void setKey(Object key) {
        this.key = key;
    }
    public AtomicLong getSize() {
        return size;
    }
    public void setSize(AtomicLong size) {
        this.size = size;
    }
    public void setSize(Long size) {
        this.size.set(size);
    }

}
