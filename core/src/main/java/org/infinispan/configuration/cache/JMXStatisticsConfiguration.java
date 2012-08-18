/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
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
package org.infinispan.configuration.cache;

/**
 * Determines whether statistics are gather and reported.
 * 
 * @author pmuir
 *
 */
public class JMXStatisticsConfiguration {

   private final boolean enabled;
   private final Boolean trackMemoryUsage;

   /**
    * Enable or disable statistics gathering and reporting
    * 
    * @param enabled
    * @param trackMemoryUsage
    */
   JMXStatisticsConfiguration(boolean enabled, boolean trackMemoryUsage) {
      this.enabled = enabled;
      this.trackMemoryUsage = trackMemoryUsage;
   }
   
   public boolean enabled() {
      return enabled;
   }

   public boolean trackMemoryUsage() {
      return trackMemoryUsage;
   }

   @Override
   public String toString() {
      return "JMXStatisticsConfiguration{" +
            "enabled=" + enabled +
            "trackMemoryUsage=" + trackMemoryUsage +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      JMXStatisticsConfiguration that = (JMXStatisticsConfiguration) o;

      if (enabled != that.enabled) return false;
      if (trackMemoryUsage != that.trackMemoryUsage) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return (enabled ? 1 : 0);
   }

}
