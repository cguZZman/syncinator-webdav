package com.syncinator.webdav.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.SyncinatorWebdavApplication;

public class DownloadManager {
	private static Logger log = LoggerFactory.getLogger(DownloadManager.class);
	private static final String BASE_DIR = SyncinatorWebdavApplication.APP_BASE_DIR +  File.separator + "download_cache";
	private static Map<String, DownloadItem> itemMap = new HashMap<String, DownloadItem>();
	
	static {
		File baseDir = new File(BASE_DIR);
		baseDir.mkdirs();
		for (File file : baseDir.listFiles()){
			file.delete();
		}
	}
	
	private static List<long[]> getRangeList(String ranges, long maxLastByte) throws DavException{
		List<long[]> list = new ArrayList<>();
		try {
			if (ranges == null){
				list.add(new long[]{0L,maxLastByte});
			} else {
				for (String range : ranges.split("\\=")[1].split(",")){
					int index = range.indexOf('-');
					long firstByte = index > 0 ? Long.parseLong(range.substring(0, index)) : 0;
					long lastByte = index < range.length() - 1 ? Math.min(maxLastByte, Long.parseLong(range.substring(index+1))) : maxLastByte;
					if (index == 0) {
						firstByte = maxLastByte - lastByte + 1;
						lastByte = maxLastByte;
					}
					if (firstByte > maxLastByte) {
						throw new DavException(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
					}
					list.add(new long[]{firstByte, lastByte});
				}
			}
		} catch (DavException e) {
			throw e;
		} catch (Exception e) {
			throw new DavException(HttpServletResponse.SC_BAD_REQUEST, e);
		}
		return list;
	}
	private static DownloadItem getDownloadItem(String id, long realSize) {
		DownloadItem downloadItem = null;
		synchronized (itemMap) {
			downloadItem = itemMap.get(id);
			if (downloadItem == null) {
				downloadItem = new DownloadItem();
				downloadItem.realSize = realSize;
				downloadItem.parts = new TreeSet<DownloadItemPart>();
				itemMap.put(id, downloadItem);
			}
		}
		return downloadItem;
	}
	private static DownloadItemPart createPart(String id, Long[] range) {
		DownloadItemPart part = new DownloadItemPart();
		part.firstByte = range[0];
		part.position = range[0];
		part.lastByte = range[1];
		part.owner = Thread.currentThread();
		part.status = DownloadItemPart.STATUS_CREATED;
		part.file = new File(BASE_DIR, id +"."+range[0]+"-"+range[1]);
		return part;
	}
	
	

	public static void download(SyncinatorDavResource resource, String url) throws IOException, DavException {
		String id = resource.getLocator().getResourcePath().replace('/', '=');
		HttpServletResponse response = resource.response;
		String clientRangeHeader = resource.request.getHeader("range");
		boolean clientRequestedPartialContent = clientRangeHeader != null;
		boolean reponseInitialized = false;
		DownloadItem downloadItem = getDownloadItem(id, resource.size);
		List<long[]> rangeList = getRangeList(clientRangeHeader, downloadItem.realSize - 1);
		long[] requestedRange = rangeList.get(0); //Only one range supported from the request for now. Will add multipart/byteranges if needed.
		if (clientRequestedPartialContent) {
			log.info("* Client has requested partial content: " + clientRangeHeader);
		}
		OutputStream clientOutputStream = response.getOutputStream();
		boolean abort = false;
		long lowerByte = requestedRange[0];
		while (lowerByte <= requestedRange[1] && !abort){
			log.info("+ Looking for [" + lowerByte + " -> " + requestedRange[1] + "]");
			DownloadItemPart workWith = null;
			synchronized (downloadItem.parts) {
				DownloadItemPart stopPart = null;
				Iterator<DownloadItemPart> iterator = downloadItem.parts.iterator();
				while (iterator.hasNext()) {
					DownloadItemPart part = iterator.next();
					if (part.status == DownloadItemPart.STATUS_INVALID) {
						continue;
					}
					log.info("    Testing [" + part.firstByte + " -> " + part.lastByte + " | " + part.position + "]");
					if (lowerByte == part.firstByte){
						workWith = part;
						break;
					} else if (part.firstByte < lowerByte) {
						if (lowerByte <= part.lastByte) {
							if (lowerByte < part.position) {
								workWith = part;
							}
							break;
						}
					} else {
						stopPart = part;
						break;
					}
				}
				if (workWith != null) {
					log.info("    Found! [" + workWith.firstByte + " -> " + workWith.lastByte + " | " + workWith.position + "] (" + workWith.owner.getName() + ")");
				}
				if (workWith == null && stopPart == null && iterator.hasNext()) {
					DownloadItemPart part = iterator.next();
					log.info("  Testing stop [" + part.firstByte + " -> " + part.lastByte + " | " + part.position + "]");
					if (lowerByte < part.firstByte && part.firstByte < requestedRange[1] && (workWith == null || workWith.position < part.position)) {
						stopPart = part;
						log.info("\tFound!");
					}
				}
				if (workWith == null) {
					workWith = createPart(id, new Long[]{lowerByte, stopPart != null ? stopPart.firstByte - 1 : requestedRange[1]});
					downloadItem.parts.add(workWith);
					log.info("  New part created [" + workWith.firstByte + " -> " + workWith.lastByte + "]");
				}
			}
			
			if (workWith.owner == Thread.currentThread() && workWith.status == DownloadItemPart.STATUS_CREATED) {
				FileOutputStream fos = null;
				InputStream is = null;
				int length=0;
				try {
					workWith.status = DownloadItemPart.STATUS_STARTED;
					fos = new FileOutputStream(workWith.file);
					HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
					boolean partialContent = lowerByte > 0 || requestedRange[1] < downloadItem.realSize - 1; 
					if (partialContent) {
						String rangeHeader = "bytes=" + lowerByte + "-" + (workWith.lastByte==downloadItem.realSize - 1?"":workWith.lastByte);
						connection.setRequestProperty("range", rangeHeader);
						log.info("  Downloading partial file from drive cloud: " + rangeHeader);
					} else {
						log.info("  Downloading complete file from drive cloud");
					}
					int responseCode = connection.getResponseCode();
					if (responseCode < 400 && connection.getContentLengthLong() > 0){
						if (!reponseInitialized) {
							reponseInitialized = true;
							downloadItem.realSize = connection.getContentLengthLong();
							if (partialContent) {
								downloadItem.realSize = Long.parseLong(connection.getHeaderField("Content-Range").split("/")[1]);
							}
							requestedRange[1] = Math.min(requestedRange[1], downloadItem.realSize - 1);
							workWith.lastByte = Math.min(workWith.lastByte, downloadItem.realSize - 1);
							if (clientRequestedPartialContent) {
								response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
								response.setHeader("Content-Range", "bytes " + requestedRange[0]+"-"+requestedRange[1]+"/"+downloadItem.realSize);
							} else {
								response.setStatus(HttpServletResponse.SC_OK);
							}
							response.setHeader("Accept-Ranges", "bytes");
							response.addHeader(DavConstants.HEADER_CONTENT_LENGTH, Long.toString(requestedRange[1] - requestedRange[0] + 1));
						}
						
						is = connection.getInputStream();
						boolean abortedByClient = false;
						byte[] buffer = new byte[4096];
					    long downloaded = 0;
					    while ((length = is.read(buffer)) > 0 && workWith.position < workWith.lastByte + 1) {
					    	if (downloaded == 0) {
					    		log.info("  Remote streaming started ["+id+"]...");
					    	}
					    	downloaded += length;
					    	fos.write(buffer, 0, length);
					    	workWith.position += length;
					    	if (!abortedByClient) {
//					    		try {
					    			clientOutputStream.write(buffer, 0, length);
					    			clientOutputStream.flush();
//					    		} catch (ClientAbortException e) {
//					    			workWith.lastByte = Math.min(workWith.lastByte, workWith.position+5242880);
//					    			log.error("  Aborted by the client. Target position is " + workWith.lastByte);
//					    			abortedByClient = true;
//					    		}
					    	}
					    }
					    log.info("  Remote streaming finished ["+id+"]. " + downloaded + " bytes downloaded.");
					} else {
						log.error("  An error happened! ["+id+"]");
						for (Entry<String,List<String>> e : connection.getHeaderFields().entrySet()) {
							log.error("   << " + e.getKey() + ": " + e.getValue());
						}
						if (!reponseInitialized) {
							throw new DavException(responseCode);
						}
						abort = true;
					}
				} catch (ClientAbortException e){
					log.error("  Aborted by the client when writing "+length+" bytes. From "+ (workWith.position - length) +" to final position " + workWith.position);
					if (!reponseInitialized) {
						throw new DavException(HttpServletResponse.SC_OK);
					}
					abort = true;
				} catch (DavException e){
					throw e;
				} catch (Exception e){
					log.error("  Unexpected error at position " + workWith.position + ": " + e.getMessage(), e);
					if (!reponseInitialized) {
						throw new DavException(HttpServletResponse.SC_BAD_GATEWAY);
					}
					abort = true;
				} finally {
					workWith.lastByte = workWith.position - 1;
					if (workWith.firstByte > workWith.lastByte) {
						workWith.status = DownloadItemPart.STATUS_INVALID;
					} else {
						workWith.status = DownloadItemPart.STATUS_FINISHED;
						lowerByte = workWith.position;
						synchronized (workWith.file) {
							File nFile = new File(BASE_DIR, id +"."+workWith.firstByte+"-"+workWith.lastByte);
							if (!nFile.equals(workWith.file) && workWith.file.renameTo(nFile)){
								workWith.file = nFile;
							}
						}
					}
					if (fos != null) {
						fos.close();
					}
					if (is != null) {
						is.close();
					}
				}
			} else {
				while (lowerByte <= workWith.lastByte && lowerByte <= requestedRange[1] && !abort) {
					long startPosition = lowerByte - workWith.firstByte;
					long neededSize = Math.min(requestedRange[1],workWith.lastByte) - lowerByte + 1;
					log.info("  Reading "+neededSize+" bytes from file. ["+startPosition+" -> "+(startPosition+neededSize-1)+"]. Available bytes: " + workWith.file.length());
					while (workWith.file.length() <= startPosition && workWith.status == DownloadItemPart.STATUS_STARTED){
						log.info("    Waiting for file data...");
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (workWith.file.length() > startPosition){
						RandomAccessFile aFile = null;
						int length = 0;
						try {
							if (!reponseInitialized) {
								reponseInitialized = true;
								if (clientRequestedPartialContent) {
									response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
									response.setHeader("Content-Range", "bytes " + requestedRange[0]+"-"+requestedRange[1]+"/"+downloadItem.realSize);
								} else {
									response.setStatus(HttpServletResponse.SC_OK);
								}
								response.setHeader("Accept-Ranges", "bytes");
								response.addHeader(DavConstants.HEADER_CONTENT_LENGTH, Long.toString(requestedRange[1] - requestedRange[0] + 1));
							}
							
							ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(neededSize, 4096));
							aFile = new RandomAccessFile(workWith.file, "r");
							FileChannel fc = aFile.getChannel();
							fc.position(startPosition);
							while ((length = fc.read(buffer)) > 0) {
								if (lowerByte+length-1 > requestedRange[1]) {
									length = (int) (requestedRange[1]-lowerByte+1);
									if (length == 0) {
										break;
									}
								}
								clientOutputStream.write(buffer.array(), 0, length);
								lowerByte += length;
								buffer.clear();
							}
						} catch (ClientAbortException e){
							log.error("  Aborted by the client when writing "+length+" bytes. From "+ lowerByte+ " to " + (lowerByte+length-1));
							if (!reponseInitialized) {
								throw new DavException(HttpServletResponse.SC_OK);
							}
							abort = true;
						} catch (Exception e){
							log.error("  Unexpected error at position " + lowerByte+ ": " + e.getMessage(), e);
							if (!reponseInitialized) {
								throw new DavException(HttpServletResponse.SC_BAD_GATEWAY);
							}
							abort = true;
						} finally {
							if (aFile != null) {
								aFile.close();
							}
						}
					}
				}
				log.info("  Done reading from cached file.");
			}
		}
	}
	
	private static class DownloadItem {
//		private boolean completed;
//		private Date expirationTime;
		private long realSize;
		private TreeSet<DownloadItemPart> parts;
	}
	
	private static class DownloadItemPart implements Comparable<DownloadItemPart>{
		public static final byte STATUS_FINISHED = 0;
		public static final byte STATUS_STARTED = 1;
		public static final byte STATUS_CREATED = 2;
		public static final byte STATUS_INVALID = 3;
		private byte status;
		private Long firstByte;
		private Long lastByte;
		private Long position;
		private File file;
		private Thread owner;
		
		@Override
		public int compareTo(DownloadItemPart o) {
			return this.firstByte.compareTo(o.firstByte);
		}
		
	}
}
