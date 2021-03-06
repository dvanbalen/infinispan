/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

package org.infinispan.atomic;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.dummy.DummyInMemoryCacheStore;
import org.infinispan.test.MultipleCacheManagersTest;
import org.testng.annotations.Test;

import javax.transaction.TransactionManager;

import java.util.concurrent.Callable;

import static org.infinispan.test.TestingUtil.withTx;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test atomic map operations in a clustered environment where passivation
 * has been configured.
 *
 * @author Galder Zamarreño
 * @since 5.1
 */
@Test(groups = "functional", testName = "atomic.ClusteredAtomicMapPassivationTest")
public class ClusteredAtomicMapPassivationTest extends MultipleCacheManagersTest {

   @Override
   protected void createCacheManagers() throws Throwable {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(
            CacheMode.REPL_SYNC, true);
      builder.loaders().passivation(true)
                  .addCacheLoader().cacheLoader(new DummyInMemoryCacheStore());
      createClusteredCaches(2, "atomic", builder);
   }

   public void testEviction() throws Exception {
      final Cache<String, Object> cache1 = cache(0, "atomic");
      final Cache<String, Object> cache2 = cache(1, "atomic");
      TransactionManager tm1 = cache1.getAdvancedCache().getTransactionManager();

      withTx(tm1, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            // Add atomic map in first node
            AtomicMap<String, String> map = AtomicMapLookup.getAtomicMap(
                  cache1, "map");
            map.put("key1", "value1");
            map.put("key2", "value2");
            return null;
         }
      });

      // From second node, passivate the map
      cache2.evict("map");

      withTx(tm1, new Callable<Void>() {
         @Override
         public Void call() throws Exception {
            // Modify atomic map from first node
            AtomicMap<String, String> map = AtomicMapLookup.getAtomicMap(
                  cache1, "map");
            map.put("key1", "new_value1");
            assertTrue(map.containsKey("key2"));
            assertEquals("value2", map.get("key2"));
            return null;
         }
      });

      // Lookup entry from second node and verify it has all the data
      AtomicMap<String, String> map = AtomicMapLookup.getAtomicMap(cache2, "map");
      assertTrue(map.containsKey("key1"));
      assertTrue(map.containsKey("key2"));
      assertEquals("new_value1", map.get("key1"));
      assertEquals("value2", map.get("key2"));
   }

}
