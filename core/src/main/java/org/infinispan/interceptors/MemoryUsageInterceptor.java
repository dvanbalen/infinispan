package org.infinispan.interceptors;

import org.infinispan.util.concurrent.ConcurrentMapFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.Class;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesExpired;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntriesExpiredEvent;
import org.infinispan.notifications.cachelistener.CacheNotifier;
import org.infinispan.commands.write.ClearCommand;
import org.infinispan.commands.write.EvictCommand;
import org.infinispan.commands.write.ExpireCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.RemoveCommand;
import org.infinispan.context.InvocationContext;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.Start;
import org.infinispan.interceptors.base.JmxStatsCommandInterceptor;
import org.infinispan.interceptors.memory.MemoryUsageKeyEntry;
import org.infinispan.jmx.annotations.MBean;
import org.infinispan.jmx.annotations.ManagedAttribute;
import org.infinispan.jmx.annotations.ManagedOperation;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.jboss.libra.Libra;
import org.rhq.helpers.pluginAnnotations.agent.DisplayType;
import org.rhq.helpers.pluginAnnotations.agent.MeasurementType;
import org.rhq.helpers.pluginAnnotations.agent.Metric;
import org.rhq.helpers.pluginAnnotations.agent.Operation;

@Listener
@MBean(objectName = "MemoryUsage", description = "Measures memory usage for cache, either through JBoss Libra Java Agent or by simple object counts")
public class MemoryUsageInterceptor extends JmxStatsCommandInterceptor {

   protected ConcurrentMap<Class<?>, Long> usageMap;
   protected Map<Object, MemoryUsageKeyEntry> sizeMap;
   protected AtomicLong totalSize = new AtomicLong(0);

   private boolean useAgent = true;

   private static String METRIC_TYPE_OBJECT_SIZE = "Object Size";
   private static String METRIC_TYPE_OBJECT_COUNT = "Object Count";

   private static final Log log = LogFactory.getLog(MemoryUsageInterceptor.class);
   private static boolean debug = log.isDebugEnabled();
   private static boolean trace = log.isTraceEnabled();

   private CacheNotifier cacheNotifier = null;

   @Inject
   public void init(CacheNotifier cacheNotifier) {
      this.cacheNotifier = cacheNotifier;
   }

   @Start
   public void start() {
      cacheNotifier.addListener(this);
      int concurrencyLevel = cacheConfiguration.locking().concurrencyLevel();
      usageMap = ConcurrentMapFactory.makeConcurrentMap(32, concurrencyLevel);
      sizeMap = ConcurrentMapFactory.makeConcurrentMap(32, concurrencyLevel);
   }

   // Map.put(key,value) :: oldValue
   public Object visitPutKeyValueCommand(InvocationContext ctx, PutKeyValueCommand command) throws Throwable {

       Object retval = invokeNextInterceptor(ctx, command);
       if (trace)
          log.tracef("In visitPutKeyValueCommand with value of '%s' and of type '%s'.", command.getValue(), ((command.getValue()!=null)?command.getValue().getClass().getName():"null"));

       if (command.isSuccessful()) {
          handleAddOrUpdate(command.getKey(), command.getValue());
       }
       return retval;
   }

   @Override
   public Object visitPutMapCommand(InvocationContext ctx, PutMapCommand command) throws Throwable {
      Map<Object, Object> data = command.getMap();
      Object retval = invokeNextInterceptor(ctx, command);
      if(trace) log.tracef("In visitPutMapCommand with map: '%s'.", data);

      if (data != null && !data.isEmpty() && command.isSuccessful()) {
         for (Entry<Object, Object> entry : data.entrySet()) {
            if (trace) log.tracef("In visitPutMapCommand with value of '%s' and of type '%s'.", entry.getValue(), ((entry.getValue()!=null)?entry.getValue().getClass().getName():"null"));

             handleAddOrUpdate(entry.getKey(), entry.getValue());
          }
      }
      return retval;
   }

   @Override
   public Object visitEvictCommand(InvocationContext ctx, EvictCommand command) throws Throwable {
      Object retval = invokeNextInterceptor(ctx, command);
      if(trace) log.tracef("In visitEvictCommand with key: '%s'.", command.getKey());

      if (command.isSuccessful()) {
         handleRemove(command.getKey());
      } else {
         if(trace) log.tracef("Expire command wasn't successful.");
      }
      return retval;
   }
    
   @Override
   public Object visitExpireCommand(InvocationContext ctx, ExpireCommand command) throws Throwable {
      Object retval = invokeNextInterceptor(ctx, command);
      if(trace) log.tracef("In visitExpireCommand with key: '%s'.", command.getKey());
    	
      if(command.isSuccessful()) {
         handleRemove(command.getKey());
      }
      return retval;
   }

   @Override
   public Object visitRemoveCommand(InvocationContext ctx, RemoveCommand command) throws Throwable {
      Object retval = invokeNextInterceptor(ctx, command);
      if(trace) log.tracef("In visitRemoveCommand with key: '%s'.", command.getKey());

      if (command.isSuccessful() && retval != null) {
         handleRemove(command.getKey());
      }
      return retval;
   }

   @Override
   public Object visitClearCommand(InvocationContext ctx, ClearCommand command) throws Throwable {
      Object retval = invokeNextInterceptor(ctx, command);
      if(trace) log.tracef("In visitClearCommand.");

      if (command.isSuccessful()) {
         reset();
      }
      return retval;
   }

   @CacheEntriesEvicted
   public void handleEvictions(CacheEntriesEvictedEvent<?, ?> event) {
      if(trace) log.tracef("In handleEvictions.");
      if(!event.isPre()) {
         if(trace) log.tracef("There are '%d' entries to be evicted", event.getEntries().size());
            for(Entry<?, ?> e : event.getEntries().entrySet()) {
               if(trace) log.tracef("Handling eviction of entry with key '%s'", e.getKey());
               handleRemove(e.getKey());
            }
      }
   }
    
   @CacheEntriesExpired
   public void handleExpirations(CacheEntriesExpiredEvent<?, ?> event) {
      if(trace) log.tracef("In handleExpirations.");
      if(!event.isPre()) {
         if(trace) log.tracef("There are '%d' entries being expired.", event.getEntries().size());
         for(Entry<?, ?> e : event.getEntries().entrySet()) {
            if(trace) log.tracef("Handling expiration of entry with key '%s'", e.getKey());
            handleRemove(e.getKey());
         }
      }
   }

   @ManagedAttribute(description = "string representation of memory usage, or object count, per object type")
   @Metric(displayName = "Memory use, or object count, per object type", measurementType = MeasurementType.TRENDSUP, displayType = DisplayType.SUMMARY)
   public String getMemoryUsage() {
      return usageMap.toString();
   }

   @ManagedAttribute(description = "total memory used, or object count, across all object types")
   @Metric(displayName = "Memory use, or object count, for all object types", measurementType = MeasurementType.TRENDSUP, displayType = DisplayType.SUMMARY)
   public long getTotalMemory() {
      return totalSize.get();
   }

   @ManagedAttribute(description = "whether object sizes or object counts are being tracked.")
   @Metric(displayName = "Type of metric tracking being used.")
   public String getTrackingType() {
      return useAgent ? METRIC_TYPE_OBJECT_SIZE : METRIC_TYPE_OBJECT_COUNT;
   }

   @Override
   @ManagedOperation(description = "Resets statistics gathered by this component")
   @Operation(displayName = "Reset Statistics (Statistics)")
   public void resetStatistics() {
      this.reset();
   }

   @ManagedOperation(description = "Toggles between gathering actual memory size and storing only a simple object count.")
   @Operation(displayName = "Toggle between object size and object count measurement types.")
   public void toggleMeasurementType() {
      useAgent = !useAgent;
      reset();
   }

   public void reset() {
      usageMap.clear();
      sizeMap.clear();
      totalSize.set(0);
   }

   private long getMemoryUsage(Object obj) {
      long size = 0L;

      if (useAgent) {
         try {
            size = Libra.getDeepObjectSize(obj);
         } catch (Throwable e) {
            toggleMeasurementType();
            log.unableToInvokeLibraAgent(e.toString());
            if (debug) e.printStackTrace();
         }
      }

      if (!useAgent) {
         size = 1L;
      }

      if (trace)
         log.tracef("Calculated size for object '%s' is '%d'", obj, size);

      return size;
   }

   private void handleAddOrUpdate(Object key, Object value) {
      long size = 0L;
      long oldSize = 0L;
      Long classSize = null;
      MemoryUsageKeyEntry keyEntry = null;
      Class<?> objType = value.getClass();

      // fetch weight of value, based on configured metric type
      size = getMemoryUsage(value);
      // Get current size of stored objects of same type
      classSize = usageMap.get(objType);

      if (trace) log.tracef("Handling add or update for value with key '%s', size '%d' and type '%s'.", key, size, objType);

      // Now that usageMap entry should have been initialized, print trace info.
      if (trace) log.tracef("Total size before object add or update is '%d' and size for type '%s' is now '%d'.", totalSize.get(), objType, usageMap.get(objType));

      // If value is being updated for existing key, previous value should be subtracted
      if (sizeMap.containsKey(key)) {
         keyEntry = sizeMap.get(key);
         oldSize = keyEntry.getSize().get();
         // Update stored size value for key
         keyEntry.getSize().set(size);
         // Subtract key's previous stored size value from running totals
         if(classSize != null) {
            usageMap.put(objType, classSize - oldSize);
         }
         totalSize.getAndAdd(0 - oldSize);
         if (trace) log.tracef("Updating memory usage for key '%s'. Subtracting '%d' from size.", key, oldSize);
      } else { // store size value for new cache entry
         if (trace) log.tracef("Tracking new cache entry with key '%s', type '%s' and size '%d'", key, objType, size);
         keyEntry = new MemoryUsageKeyEntry(key, size);
         sizeMap.put(key, keyEntry);
      }

      // Add new size to running totals
      usageMap.put(objType, (classSize!=null)?(classSize + size):size);
      totalSize.getAndAdd(size);

      if (trace) log.tracef("Total size after object add or update is '%d' and size for type '%s' is now '%d'.", totalSize.get(), objType, usageMap.get(objType));
   }

   private void handleRemove(Object key) {
      MemoryUsageKeyEntry keyEntry = null;
      Class<?> objType;
      Long classSize = null;
      long size = 0L;

      if (trace) log.tracef("In handleRemove for key '%s'", key);

      if (sizeMap.containsKey(key)) {
         if (trace) log.tracef("Before remove, sizeMap has '%d' entries.", sizeMap.size());

         // stop tracking removed entry
         keyEntry = sizeMap.get(key);
         objType = keyEntry.getKey().getClass();
         size = keyEntry.getSize().get();
         sizeMap.remove(key);

         if (trace) log.tracef("After remove, sizeMap has '%d' entries.", sizeMap.size());

         if (trace) log.tracef("Handling remove for object with key '%s', type '%s' and size '%d'", key, objType, size);
         if (trace) log.tracef("Total size before object remove is '%d' and size for type '%s' is now '%d'.", totalSize.get(), objType, usageMap.get(objType));
      } else {
         if (debug) log.debugf("Was asked to process removal of entry with key '%s', which isn't being tracked. Doing nothing.", key);
         return;
      }

      if (usageMap.containsKey(objType)) {
          // subtract size of removed entry from running totals
    	  classSize = usageMap.get(objType);
    	  if(classSize != null && classSize > 0L) {
             usageMap.put(objType, classSize - size);
    	  }
          totalSize.getAndAdd(0 - size);
      } else {
          if (debug) log.debugf("Was asked to process removal of entry with key '%s' and of type '%s', but that type isn't being tracked. Total for type will not be decremented.", key, objType);
          return;
      }

      if (trace)
         log.tracef("Total size after object remove is '%d' and size for type '%s' is now '%d'.", totalSize.get(), objType, usageMap.get(objType));
   }
}
