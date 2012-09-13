/**
 * 
 */
package org.infinispan.commands.write;

import java.util.ArrayList;
import java.util.Collection;

import org.infinispan.commands.LocalCommand;
import org.infinispan.commands.Visitor;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.context.InvocationContext;
import org.infinispan.notifications.cachelistener.CacheNotifier;

/**
 * @author David van Balen
 * @since 5.1
 */
public class ExpireCommand extends RemoveCommand implements LocalCommand {

	   public ExpireCommand(Object key, CacheNotifier notifier) {
	      this.key = key;
	      this.notifier = notifier;
	   }

	   public void initialize(CacheNotifier notifier) {
	      this.notifier = notifier;
	   }

	   @Override
	   public Object acceptVisitor(InvocationContext ctx, Visitor visitor) throws Throwable {
	      return visitor.visitExpireCommand(ctx, this);
	   }

	   @Override
	   public Object perform(InvocationContext ctx) throws Throwable {
	      if (key == null) {
	         throw new NullPointerException("Key is null!!");
	      }
	      super.perform(ctx);
	      return null;
	   }

	   @Override
	   public void notify(InvocationContext ctx, Object value, boolean isPre) {
		   Collection<CacheEntry> entries = null;
		   CacheEntry ce = null;
		   CacheEntry ice = null;
		   
	      if (!isPre) {
	    	 ce = ctx.lookupEntry(key);
		 ice = (CacheEntry)ce;
		 entries = new ArrayList<CacheEntry>();
		 entries.add(ice);
		 notifier.notifyCacheEntriesExpired(entries, ctx);
	      }
	   }

	   @Override
	   public byte getCommandId() {
	      return -1; // these are not meant for replication!
	   }
	   
	   @Override
	   public String toString() {
	      return new StringBuilder()
	         .append("ExpireCommand{key=")
	         .append(key)
	         .append(", value=").append(value)
	         .append(", flags=").append(flags)
	         .append("}")
	         .toString();
	   }}
