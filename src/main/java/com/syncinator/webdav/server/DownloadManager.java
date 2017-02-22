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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syncinator.webdav.SyncinatorWebdavApplication;

public class DownloadManager {
	private static Logger log = LoggerFactory.getLogger(DownloadManager.class);
	private static final String BASE_DIR = SyncinatorWebdavApplication.APP_BASE_DIR +  File.separator + "downloads";
	private static Map<String, Item> itemMap = new HashMap<String, Item>();
	
	static {
		File baseDir = new File(BASE_DIR);
		baseDir.mkdirs();
		for (File file : baseDir.listFiles()){
			file.delete();
		}
	}
	
	private static List<Long[]> getRequestRangers(String ranges, long totalSize){
		List<Long[]> list = new ArrayList<>();
		if (ranges == null){
			list.add(new Long[]{0L,totalSize-1});
		} else {
			for (String range : ranges.split("\\=")[1].split(",")){
				int index = range.indexOf('-');
				Long start = null;
				Long end = null;
				if (index > 0) {
					start = Long.valueOf(range.substring(0, index));
				}
				if (index < range.length() - 1) {
					end = Long.valueOf(range.substring(index+1));
				}
				if (start == null) {
					start = totalSize - end;
					end = totalSize - 1;
				}
				if (end == null) {
					end = totalSize - 1;
				}
				list.add(new Long[]{start, end});
			}
		}
		return list;
	}
	
	private static ItemPart createPart(String id, Long[] range) {
		ItemPart part = new ItemPart();
		part.setStart(range[0]);
		part.setPosition(range[0]);
		part.setEnd(range[1]);
		part.setFinalEnd(range[1]);
		part.setOwner(Thread.currentThread());
		part.setStatus(ItemPart.STATUS_CREATED);
		part.setFile(new File(BASE_DIR, id +"."+range[0]+"-"+range[1]));
		return part;
	}

	public static void download(String id, long size, String url, String requestedRange, OutputContext context, HttpServletResponse response) throws IOException, DavException {
		Item item = null;
		synchronized (itemMap) {
			item = itemMap.get(id);
			if (item == null) {
				item = new Item();
				item.setSize(size);
				item.parts = new TreeSet<ItemPart>();
				itemMap.put(id, item);
			}
		}
		List<Long[]> ranges = getRequestRangers(requestedRange, size);
		Long[] range = ranges.get(0); //Only one range supported from the request for now. Will add multipart/byteranges if needed.
		long totalNeededSize = range[1] - range[0] + 1;
		if (requestedRange != null) {
			response.setStatus(206);
			context.setProperty("Content-Range", "bytes " + range[0]+"-"+range[1]+"/"+size);
			log.info("Partial content requested from client: " + requestedRange);
		}
		context.setContentLength(totalNeededSize);
		context.setProperty("Accept-Ranges", "bytes");
		
		OutputStream ros = context.getOutputStream();
		boolean abort = false;
		long lowerByte = range[0];
		long upperByte = range[1];
		
		while (lowerByte <= upperByte && !abort){
			log.info("+ Looking for [" + lowerByte + " -> " + upperByte + "]");
			ItemPart workWith = null;
			synchronized (item.parts) {
				ItemPart stopPart = null;
				Iterator<ItemPart> iterator = item.parts.iterator();
				while (iterator.hasNext()) {
					ItemPart part = iterator.next();
					if (part.status == ItemPart.STATUS_INVALID) {
						continue;
					}
					//log.info("    Testing [" + part.start + " -> " + part.finalEnd + " | " + part.position + "]");
					if (lowerByte == part.start){
						workWith = part;
						break;
					} else if (part.start < lowerByte) {
						if (lowerByte <= part.finalEnd) {
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
					log.info("    Found! [" + workWith.start + " -> " + workWith.finalEnd + " | " + workWith.position + "] (" + workWith.owner.getName() + ")");
				}
				if (workWith == null && stopPart == null && iterator.hasNext()) {
					ItemPart part = iterator.next();
					log.info("  Testing stop [" + part.start + " -> " + part.finalEnd + " | " + part.position + "]");
					if (lowerByte < part.start && part.start < upperByte && (workWith == null || workWith.position < part.position)) {
						stopPart = part;
						log.info("\tFound!");
					}
				}
				if (workWith == null) {
					workWith = createPart(id, new Long[]{lowerByte, stopPart!=null?stopPart.start:upperByte});
					item.parts.add(workWith);
					log.info("  New part created [" + workWith.start + " -> " + workWith.finalEnd + "]");
				}
			}
			
			if (workWith.owner == Thread.currentThread() && workWith.status == ItemPart.STATUS_CREATED) {
				FileOutputStream fos = null;
				InputStream is = null;
				int length=0;
				try {
					workWith.status = ItemPart.STATUS_STARTED;
					fos = new FileOutputStream(workWith.file);
					HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
					if (lowerByte > 0 || upperByte < size-1) {
						String rangeHeader = "bytes=" + lowerByte + "-" + (upperByte==size-1?"":upperByte);
						connection.setRequestProperty("range", rangeHeader);
						log.info("  Downloading partial file from cloud: " + rangeHeader);
					} else {
						log.info("  Downloading complete file from cloud");
					}
					if (connection.getContentLengthLong() != 0){
						is = connection.getInputStream();
						boolean abortedByClient = false;
						byte[] buffer = new byte[4096];
					    long downloaded = 0;
					    long target = workWith.end;
					    while ((length = is.read(buffer)) > 0 && workWith.position < target + 1) {
					    	if (downloaded == 0) {
					    		log.info("  Remote streaming started ["+id+"]...");
					    	}
					    	downloaded += length;
					    	fos.write(buffer, 0, length);
					    	fos.flush();
					    	workWith.position += length;
					    	if (!abortedByClient) {
					    		try {
					    			ros.write(buffer, 0, length);
					    		} catch (ClientAbortException e) {
					    			target = Math.min(workWith.end, workWith.position+5242880);
					    			log.error("  Aborted by the client. Target position is " + target);
					    			abortedByClient = true;
					    		}
					    	}
					    }
					    log.info("  Remote streaming finished ["+id+"]. " + downloaded + " bytes downloaded.");
					} else {
						log.error("  Contente length is 0! ["+id+"]");
						abort = true;
						//throw new DavException(HttpServletResponse.SC_BAD_GATEWAY);
					}
				} catch (ClientAbortException e){
					log.error("  Aborted by the client when writing "+length+" bytes. From "+ (workWith.position - length) +" to final position " + workWith.position);
					abort = true;
					//throw new DavException(HttpServletResponse.SC_OK);
//				} catch (DavException e){
//					throw e;
				} catch (Exception e){
					log.error("  Unexpected error at position " + workWith.getPosition()+ ": " + e.getMessage(), e);
					abort = true;
					//throw new DavException(HttpServletResponse.SC_BAD_GATEWAY);
				} finally {
					workWith.finalEnd = workWith.position - 1;
					if (workWith.start > workWith.finalEnd) {
						workWith.status = ItemPart.STATUS_INVALID;
					} else {
						workWith.status = ItemPart.STATUS_FINISHED;
						lowerByte = workWith.position;
						synchronized (workWith.file) {
							if (workWith.end != workWith.finalEnd) {
								File nFile = new File(BASE_DIR, id +"."+workWith.start+"-"+workWith.finalEnd);
								if (workWith.file.renameTo(nFile)){
									workWith.file = nFile;
								}
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
				while (lowerByte <= workWith.finalEnd && lowerByte <= upperByte && !abort) {
					long startPosition = lowerByte - workWith.start;
					long neededSize = upperByte - lowerByte + 1;
					long maxExpectedSize =  workWith.finalEnd - workWith.start + 1;
					long readSize = Math.min(neededSize, maxExpectedSize);
					log.info("  Reading "+readSize+" cached bytes. ["+startPosition+" -> "+(startPosition+readSize-1)+"]. Available bytes: " + workWith.file.length());
					while (workWith.file.length() <= startPosition && workWith.status == ItemPart.STATUS_STARTED){
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
							ByteBuffer buffer = ByteBuffer.allocate(4096);
							aFile = new RandomAccessFile(workWith.file, "r");
							FileChannel fc = aFile.getChannel();
							fc.position(startPosition);
							while ((length = fc.read(buffer)) > 0) {
								if (lowerByte+length-1 > upperByte) {
									length = (int) (upperByte-lowerByte+1);
								}
								ros.write(buffer.array(), 0, length);
								lowerByte += length;
								buffer.clear();
							}
						} catch (ClientAbortException e){
							log.error("  Aborted by the client when writing "+length+" bytes. From "+ lowerByte+ " to " + (lowerByte+length-1));
							abort = true;
							//throw new DavException(HttpServletResponse.SC_OK);
						} catch (Exception e){
							log.error("  Unexpected error at position " + lowerByte+ ": " + e.getMessage(), e);
							abort = true;
							//throw new DavException(HttpServletResponse.SC_BAD_GATEWAY);
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
	
	private static class Item {
		private boolean completed;
		private Date expirationTime;
		private TreeSet<ItemPart> parts;
		private long size;
		
		public Date getExpirationTime() { return expirationTime; }
		public void setExpirationTime(Date expirationTime) { this.expirationTime = expirationTime; }
		public TreeSet<ItemPart> getParts() { return parts; }
		public void setParts(TreeSet<ItemPart> parts) { this.parts = parts; }
		public boolean isCompleted() { return completed; }
		public void setCompleted(boolean completed) { this.completed = completed; }
		public long getSize() { return size; }
		public void setSize(long size) { this.size = size; }
	}
	
	private static class ItemPart implements Comparable{
		public static final byte STATUS_FINISHED = 0;
		public static final byte STATUS_STARTED = 1;
		public static final byte STATUS_CREATED = 2;
		public static final byte STATUS_INVALID = 3;
		private byte status;
		private Long start;
		private Long end;
		private Long finalEnd;
		private Long position;
		private File file;
		private Thread owner;
		private long startTime;
		
		public ItemPart(){}
		
		public byte getStatus() { return status; }
		public void setStatus(byte status) { this.status = status; }
		public Long getStart() { return start; }
		public void setStart(Long start) { this.start = start; }
		public Long getEnd() {return end; }
		public void setEnd(Long end) { this.end = end; }
		public Long getPosition() { return position; }
		public void setPosition(Long position) { this.position = position; }
		public File getFile() { return file; }
		public void setFile(File file) { this.file = file; }
		public Thread getOwner() { return owner; }
		public void setOwner(Thread owner) { this.owner = owner; }
		public Long getFinalEnd() { return finalEnd; }
		public void setFinalEnd(Long finalEnd) { this.finalEnd = finalEnd; }
		public long getStartTime() { return startTime; }
		public void setStartTime(long startTime) { this.startTime = startTime; }
		@Override
		public int compareTo(Object o) {
			return start.compareTo(((ItemPart) o).getStart());
		}
		
	}
}
