package com.liferay.documentation.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CheckImagesTaskDevSite extends Task {

	@Override
	public void execute() throws BuildException {
		System.out.println("Start checking image sources ...");

		File docDir = new File("../" + _docdir);
		if (!docDir.exists()) {
			throw new BuildException("docdir " + docDir.getAbsolutePath() +
				" could not be found");
		}
		if (!docDir.isDirectory()) {
			throw new BuildException("docdir " + docDir.getAbsolutePath() +
				" is not a directory");
		}

		File[] docDirFiles = docDir.listFiles();
		if (docDirFiles.length == 0) {
			throw new BuildException("docdir " + docDir.getAbsolutePath() +
				" is empty");
		}
		
		// Get articles
		File articleDir = new File(docDir.getAbsolutePath() + "/articles");
		File[] articleDirFiles = articleDir.listFiles();
		List<File> articles = new ArrayList<File>();

		if ((!articleDir.exists() || !articleDir.isDirectory())) {
			throw new BuildException("Missing articles directory " +
					articleDir.getAbsolutePath());
		}
		
		Queue<File> q = new LinkedList<File>();
		for (File f : articleDirFiles) {
			q.add(f);
		}
		
		while (!q.isEmpty()) {
			File f = q.remove(); 
			
			if (f.isDirectory()) {
				File[] files = f.listFiles();
				
				for (File file : files) {
					q.add(file);
				}
			}
			else {
				if (f.getName().endsWith(".markdown")) {
					articles.add(f);
				}
			}
		}
		
		// Get a map of articles to lists of referenced images
		Map<File, List<String>> imagePathsMap = new HashMap<File, List<String>>();
		
		for (File article : articles) {
			List<String> imagePaths = getImagePaths(article);
			
			imagePathsMap.put(article, imagePaths);
		}
		
		// Get list of images
		File imgDir = new File(docDir.getAbsolutePath() + "/images");
		if (!imgDir.exists()) {
			throw new BuildException("imgdir " + imgDir.getAbsolutePath() +
				" could not be found");
		}
		if (!docDir.isDirectory()) {
			throw new BuildException("imgdir " + imgDir.getAbsolutePath() +
				" is not a directory");
		}
		
		File[] imagesArray = imgDir.listFiles();
		
		List<File> images = Arrays.asList(imagesArray);
		
		checkImages(images, imagePathsMap);
		
		System.out.println("Finished checking image sources.");
	}

	public void setDocdir(String docdir) {
		_docdir = docdir;
	}
	
	private static void checkImages(List<File> images, Map<File, List<String>> imagePathsMap) {
		List<String> imagePaths = new ArrayList<String>();
		
		Collection<List<String>> imagePathsLists = imagePathsMap.values();

		for (List<String> imagePathsList : imagePathsLists) {
			imagePaths.addAll(imagePathsList);
		}
		
		List<String> referencedImageNames = new ArrayList<String>();

		for (String imagePath : imagePaths) {
			imagePath = getFileName(imagePath);
			
			referencedImageNames.add(imagePath);
		}
		
		List<String> imageNames = new ArrayList<String>();
		
		for (File image : images) {
			String imageName = image.getName();
			
			imageNames.add(imageName);
		}
		
		List<String> errors = new ArrayList<String>();

		// Report missing images
		for (String referencedImageName : referencedImageNames) {
			if (!imageNames.contains(referencedImageName)) {
				errors.add("Missing image: " + referencedImageName);
			}
		}
		
		// Report extra images
		for (String imageName : imageNames) {
			if (!referencedImageNames.contains(imageName)) {
				errors.add("Extra image: " + imageName);
			}
		}
		
		// Report faulty image paths 
		Set<File> articles = imagePathsMap.keySet();
		
		for (File article : articles) {
			String parentPath = article.getParent();
			
			List<String> imagePathsList = imagePathsMap.get(article);
			
			for (String imagePath : imagePathsList) {
				File image = new File(parentPath + "/" + imagePath);
				
				if (!image.exists() || image.isDirectory()) {
					errors.add("Faulty image path: " + imagePath);
				}
			}
		}
		
		if (!errors.isEmpty()) {
			for (String error : errors) {
				System.err.println("ERROR - " + error);
			}
			
			throw new BuildException("Missing images, extra images, or faulty image paths");
		}
	}
	
	private static String getFileName(String path) {
		if (!path.contains("/")) {
			return path;
		}
		
		int index = path.lastIndexOf("/");
		
		String fileName = path.substring(index);

		fileName = fileName.replace("/", "");
		
		return fileName;
	}
	
	private static List<String> getImagePaths(File article) {
		List<String> imagePaths = new ArrayList<String>();
		
		if (article.isDirectory()) {
			return imagePaths;
		}
		
		if (!article.getName().endsWith(".markdown")) {
			return imagePaths;
		}
		
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get(article.getCanonicalPath()), StandardCharsets.UTF_8);
		}
		catch (IOException ioe) {
			System.err.println(ioe.getLocalizedMessage());

			return imagePaths;
		}
		
		if (lines == null) {
			return imagePaths;
		}
		
		// Match lines of the form ![...](...)
		String regex = "!\\[.*\\]\\(.*\\)";

		for (String line : lines) {
			line = line.trim();
			
			if (line.matches(regex)) {
				int index = line.lastIndexOf("(");
				
				line = line.substring(index);
				
				line = line.replace("(", "");
				line = line.replace(")", "");
				
				imagePaths.add(line);
			}
		}
		
		return imagePaths;
	}


	private String _docdir;
}
