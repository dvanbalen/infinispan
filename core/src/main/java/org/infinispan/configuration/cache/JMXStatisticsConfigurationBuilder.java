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
public class JMXStatisticsConfigurationBuilder extends AbstractConfigurationChildBuilder<JMXStatisticsConfiguration> {

   private boolean enabled = false;
   private Boolean trackMemoryUsage = false;

   JMXStatisticsConfigurationBuilder(ConfigurationBuilder builder) {
      super(builder);
   }

   /**
    * Enable statistics gathering and reporting
    */
   public JMXStatisticsConfigurationBuilder enable() {
      this.enabled = true;
      return this;
   }

   /**
    * Disable statistics gathering and reporting
    */
   public JMXStatisticsConfigurationBuilder disable() {
      this.enabled = false;
      return this;
   }

   /**
    * Enable or disable statistics gathering and reporting
    */
   public JMXStatisticsConfigurationBuilder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
   }

   /**
    * Enable or disable tracking of memory usage statistics
    */
   public JMXStatisticsConfigurationBuilder trackMemoryUsage(boolean trackMemoryUsage) {
      this.trackMemoryUsage = trackMemoryUsage;
      return this;
   }

   /**
    * Enable or disable tracking of memory usage statistics
    */
   public boolean trackMemoryUsage() {
      return this.trackMemoryUsage;
   }

   @Override
   public void validate() {
   }

   @Override
   public JMXStatisticsConfiguration create() {
      return new JMXStatisticsConfiguration(enabled, trackMemoryUsage);
   }

   @Override
   public JMXStatisticsConfigurationBuilder read(JMXStatisticsConfiguration template) {
      this.enabled = template.enabled();
      this.trackMemoryUsage = template.trackMemoryUsage();

      return this;
   }

   @Override
   public String toString() {
      return "JMXStatisticsConfigurationBuilder{" +
            "enabled=" + enabled +
            "trackMemoryUsage=" + trackMemoryUsage +
            '}';
   }

}
