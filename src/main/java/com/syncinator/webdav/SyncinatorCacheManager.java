package com.syncinator.webdav;

import java.util.concurrent.TimeUnit;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.syncinator.webdav.server.SyncinatorDavResource;

public class SyncinatorCacheManager {
	
	private static final String RESOURCE_ALIAS = "resource";
	
	private static CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
			.withCache(RESOURCE_ALIAS, 
					CacheConfigurationBuilder
						.newCacheConfigurationBuilder(String.class, SyncinatorDavResource.class, ResourcePoolsBuilder.heap(50000))
						.withExpiry(Expirations.timeToLiveExpiration(Duration.of(15, TimeUnit.SECONDS))))
			.build();
	
	static {
		cacheManager.init();
	}
	
	public static Cache<String, SyncinatorDavResource> getResourceCache(){
		return cacheManager.getCache(RESOURCE_ALIAS, String.class, SyncinatorDavResource.class);
	}
}
