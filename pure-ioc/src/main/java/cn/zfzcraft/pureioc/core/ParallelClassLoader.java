package cn.zfzcraft.pureioc.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ParallelClassLoader {

	// CPU核心数
	private static final int CPU = Runtime.getRuntime().availableProcessors();

	// 自定义线程池
	private static ThreadPoolExecutor ThreadPool = new ThreadPoolExecutor(0, CPU, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(256), new ThreadPoolExecutor.CallerRunsPolicy());

	public static List<Class<?>> load(List<String> classNameList) {
		List<Class<?>> applicationClasses = new ArrayList<>();
		// 1. 按照线程数分片
		List<List<String>> splitList = splitList(classNameList, 200);
		// 2. 每一片提交异步任务，批量加载，返回当前片的List<Class<?>>
		List<CompletableFuture<List<Class<?>>>> futures = new ArrayList<>();
		for (List<String> batch : splitList) {
			CompletableFuture<List<Class<?>>> future = CompletableFuture.supplyAsync(() -> {
				// 每一个分片 自己的集合
				List<Class<?>> partClassList = new ArrayList<>();
				for (String className : batch) {
					try {
						// 核心：加载类
						Class<?> clazz = Class.forName(className, false, ResourceLoader.getClassLoader());
						// 放进当前分片集合
						partClassList.add(clazz);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				// 返回当前分片所有Class
				return partClassList;
			}, ThreadPool);
			futures.add(future);
		}

		// 3. 等待所有任务执行完
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[splitList.size()])).join();

		// 4. 最终总集合，收集所有Class
		for (CompletableFuture<List<Class<?>>> future : futures) {
			try {
				// 把每一片的Class 全部合并到总List
				applicationClasses.addAll(future.get());
			} catch (Exception e) {
				throw new RuntimeException("Load Class Failed", e);
			}
		}
		ThreadPool.shutdown();
		ThreadPool = null;
		return applicationClasses;
	}

	/**
	 * 列表均匀分片
	 */
	private static List<List<String>> splitList(List<String> source, int part) {
		List<List<String>> result = new ArrayList<>();
		int total = source.size();
		int step = (total + part - 1) / part;

		for (int i = 0; i < part; i++) {
			int start = i * step;
			int end = Math.min(start + step, total);
			if (start >= end)
				break;
			result.add(source.subList(start, end));
		}
		return result;
	}
}
