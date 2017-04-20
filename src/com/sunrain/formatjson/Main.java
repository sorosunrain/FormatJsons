package com.sunrain.formatjson;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import javax.imageio.ImageIO;

import com.github.kevinsawicki.http.HttpRequest;

public class Main {

	private final boolean DEBUG = true;
	private String outputPath;
	private String currentOperateFileName;

	public static void main(String[] args) {
		String jPath = "";
		String oPath = "";

		if (args != null) {

			jPath = args.length > 0 ? args[0] : jPath;
			oPath = args.length > 1 ? args[1] : oPath;

			if (oPath.isEmpty()) {
				oPath = jPath + "/../output";
			}

			new Main(jPath, oPath);

		}
	}

	public Main(String jsonPath, String outputPath) {
		this.outputPath = outputPath;

		try {
			File logFile = new File(String.format("%s/../output/log_%d.txt", outputPath, System.currentTimeMillis()));
			logFile.getParentFile().mkdirs();
			System.setErr(new PrintStream(logFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		File file = new File(jsonPath);
		// 得到文件内容
		String content = getFileContent(file);
		// 转换文件格式 防止乱码
		content = convertIfNeed(content);
		// 下载图片并且替换本地路径
		content = getUrlAndReplace2LocalPath(content);
		// 保存新生成的json
		saveJson(file.getName(), content);
		// 退出
		System.exit(0);
	}

	private String getFileContent(File file) {
		if (file.isDirectory()) {
			LOG_E("请拖拽上json文件！");
			return null;
		}
		currentOperateFileName = file.getName();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String string = null;

			StringBuilder sb = new StringBuilder();

			while ((string = reader.readLine()) != null) {
				sb.append(string).append("\n");
			}

			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			LOG_E(e.getMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
					LOG_E(e.getMessage());
				}
			}
		}

		return null;
	}

	private String convertIfNeed(String utfString) {
		if (utfString == null)
			return null;
		StringBuilder sb = new StringBuilder();
		int i = -1;
		int pos = 0;

		while ((i = utfString.indexOf("\\u", pos)) != -1) {
			sb.append(utfString.substring(pos, i));
			if (i + 5 < utfString.length()) {
				pos = i + 6;
				try {
					// sb.append(new String(utfString.substring(i + 2, i +
					// 6).getBytes("ISO8859-1"), "UTF-8"));
					sb.append((char) Integer.parseInt(utfString.substring(i + 2, i + 6), 16));
				} catch (Exception e) {
					e.printStackTrace();
					LOG_E(e.getMessage());
				}
			}
		}

		sb.append(utfString.substring(pos));

		return sb.toString().trim().replaceAll("\\/", "/");
	}

	private synchronized String saveImages(String url) {

		System.out.println("-> " + url);

		try {

			// get file name

			int skipLen = url.contains(".jpeg") ? 5 : 4;
			String fileName = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".") + skipLen);

			String savePath = Utils.getSaveImagePath(outputPath) + fileName;

			BufferedImage image = ImageIO.read(HttpRequest.get(url + "!bgmediam2").stream());
			if (image == null) {
				LOG_E("不支持bgmediam2属性，下载原链接图片！  ");
				image = ImageIO.read(HttpRequest.get(url).stream());
			}

			if (image == null) {
				LOG_E("该连接不是有效的图片！ " + url);
				return "";
			}

			String saveFormat = "jpg";
			int width = image.getWidth();
			int height = image.getHeight();

			if (fileName.endsWith(".png")) {
				boolean hasAlpha = false;
				for (int i = 0; i < width; i++) {
					for (int j = 0; j < height; j++) {
						// if ((image.getRGB(i, j) >> 24) == 0) {
						// hasAlpha = true;
						// }
						if (new Color(image.getRGB(i, j), true).getAlpha() != 255) {
							hasAlpha = true;
							break;
						}
					}
				}

				if (hasAlpha) {
					saveFormat = "png";
				} else {
					savePath = savePath.replace(".png", ".jpg");
					url = url.replace(".png", ".jpg");
					LOG_E(String.format("[%s]\tOops.. PNG图片没有Alpha像素。    转换为JPG。。。 From -->[%s]", fileName,
							currentOperateFileName));
				}
			}
			ImageIO.write(image, saveFormat, new File(savePath));

			String sizeMSG = String.format("ImageSize --> %d X %d  [%s]", width, height, fileName);
			if (width >= 1000 || height >= 1000) {
				sizeMSG = String.format("%s\t%s\t[%s]", sizeMSG, "此图分辨率有点大！！！！ From -->", currentOperateFileName);
				LOG_E(sizeMSG);
			} else {
				LOG_E(sizeMSG);
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOG_E(e.getMessage());
		}

		return url;
	}

	private synchronized String getUrlAndReplace2LocalPath(String content) {
		if (content == null)
			return null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content.getBytes())));
			String str = null;
			while ((str = reader.readLine()) != null) {
				if (str.contains("http")) {
					int start = str.indexOf("http");
					if (str.contains("180x180")) {
						str = str.substring(start, str.indexOf("!"));
					}

					if (str.contains("\",\"")) {
						str = str.substring(start, str.indexOf("\",\""));
					}

					if (str.contains("\",")) {
						str = str.substring(start, str.indexOf("\","));
					}

					if (str.contains("\"},{\"")) {
						str = str.substring(start, str.indexOf("\"},{\""));
					}
					content = content.replaceAll(str, saveImages(str));
					String needReplace = str.substring(0, str.lastIndexOf("/"));
					System.out.println("-> " + needReplace);
					content = content.replaceAll(needReplace, "asset:///images");
				}
			}
			return content;
		} catch (Exception e) {
			e.printStackTrace();
			LOG_E(e.getMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
					LOG_E(e.getMessage());
				}
			}
		}
		return null;
	}

	private synchronized void saveJson(String name, String json) {
		if (json == null)
			return;
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(new File(Utils.getSaveJsonPath(this.outputPath) + name)), "UTF-8"));
			writer.write(json);
			writer.flush();
			writer.close();

			LOG_E(name + " 写文件成功");

		} catch (Exception e) {
			e.printStackTrace();
			LOG_E(e.getMessage());
		}

	}

	private final void LOG_E(String msg) {
		if (DEBUG) {
			System.err.println(msg);
		}
	}
}
