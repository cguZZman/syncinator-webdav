package com.syncinator.webdav;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.syncinator.webdav.server.SyncinatorDavResource;

public class SyncinatorCacheManager {
	
	private static String RESOURCE_ALIAS = "resource";
	private static String CONTENT_ALIAS = "content";
	private static String NOT_FOUND_ALIAS = "notFound";
	
	private static CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
			.withCache(RESOURCE_ALIAS, 
					CacheConfigurationBuilder
						.newCacheConfigurationBuilder(String.class, SyncinatorDavResource.class, ResourcePoolsBuilder.heap(1000))
						.withExpiry(Expirations.timeToLiveExpiration(Duration.of(5, TimeUnit.SECONDS))))
//			.withCache(CONTENT_ALIAS, 
//					CacheConfigurationBuilder
//						.newCacheConfigurationBuilder(String.class, ByteBuffer.class, 
//								ResourcePoolsBuilder.newResourcePoolsBuilder().disk(512, MemoryUnit.MB)
//						)
//						.withExpiry(Expirations.timeToLiveExpiration(Duration.of(3, TimeUnit.SECONDS))))
//			.withCache(NOT_FOUND_ALIAS, 
//					CacheConfigurationBuilder
//						.newCacheConfigurationBuilder(String.class, Object.class, ResourcePoolsBuilder.heap(1000))
//						.withExpiry(Expirations.timeToLiveExpiration(Duration.of(3, TimeUnit.SECONDS))))
			.build();
	
	static {
		cacheManager.init();
	}
	
	public static Cache<String, SyncinatorDavResource> getResourceCache(){
		return cacheManager.getCache(RESOURCE_ALIAS, String.class, SyncinatorDavResource.class);
	}
	
	public static Cache<String, ByteBuffer> getContentCache(){
		return cacheManager.getCache(CONTENT_ALIAS, String.class, ByteBuffer.class);
	}
	
	public static Cache<String, Object> getNotFoundCache(){
		return cacheManager.getCache(NOT_FOUND_ALIAS, String.class, Object.class);
	}
	
}
