/*******************************************************************************
 * Copyright (c) 2011 Google, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *    Wim Jongman  - 1.8 and higher compliance
 *******************************************************************************/
package org.eclipse.wb.swt;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.osgi.framework.Bundle;

import com.minres.scviewer.e4.application.Messages;

/**
 * Utility class for managing OS resources associated with SWT/JFace controls
 * such as colors, fonts, images, etc.
 * 
 * This class is created automatically when you fiddle around with images and
 * colors in WB. You might want to prevent your application from using this
 * class and provide your own more effective means of resource caching.
 * 
 * Even though this class can be used to manage these resources, if they are
 * here for the duration of the application and not used then you still have an
 * effective resource leak. 
 * 
 * Application code must explicitly invoke the <code>dispose()</code> method to
 * release the operating system resources managed by cached objects when those
 * objects and OS resources are no longer needed.
 * 
 * This class may be freely distributed as part of any application or plugin.
 * <p>
 * 
 * @author scheglov_ke
 * @author Dan Rubel
 * @author Wim Jongman
 */
public class ResourceManager extends SWTResourceManager {

	/**
	 * The map where we store our images. 
	 */
	private static Map<ImageDescriptor, Image> descriptorImageMap = new HashMap<>();

	/**
	 * Returns an {@link ImageDescriptor} stored in the file at the specified path
	 * relative to the specified class.
	 * 
	 * @param clazz the {@link Class} relative to which to find the image
	 *              descriptor.
	 * @param path  the path to the image file.
	 * @return the {@link ImageDescriptor} stored in the file at the specified path.
	 */
	public static ImageDescriptor getImageDescriptor(Class<?> clazz, String path) {
		return ImageDescriptor.createFromFile(clazz, path);
	}

	/**
	 * Returns an {@link ImageDescriptor} stored in the file at the specified path.
	 * 
	 * @param path the path to the image file.
	 * @return the {@link ImageDescriptor} stored in the file at the specified path.
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		try {
			return ImageDescriptor.createFromURL(new File(path).toURI().toURL());
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Returns an {@link Image} based on the specified {@link ImageDescriptor}.
	 * 
	 * @param descriptor the {@link ImageDescriptor} for the {@link Image}.
	 * @return the {@link Image} based on the specified {@link ImageDescriptor}.
	 */
	public static Image getImage(ImageDescriptor descriptor) {
		if (descriptor == null)
			return null;
		return descriptorImageMap.computeIfAbsent(descriptor, ImageDescriptor::createImage);
	}

	/**
	 * Maps images to decorated images.
	 */
	@SuppressWarnings("unchecked")
	private static Map<Image, Map<Image, Image>>[] decoratedImageMap = new Map[LAST_CORNER_KEY];

	/**
	 * Returns an {@link Image} composed of a base image decorated by another image.
	 * 
	 * @param baseImage the base {@link Image} that should be decorated.
	 * @param decorator the {@link Image} to decorate the base image.
	 * @return {@link Image} The resulting decorated image.
	 */
	public static Image decorateImage(Image baseImage, Image decorator) {
		return decorateImage(baseImage, decorator, BOTTOM_RIGHT);
	}

	/**
	 * Returns an {@link Image} composed of a base image decorated by another image.
	 * 
	 * @param baseImage
	 *            the base {@link Image} that should be decorated.
	 * @param decorator
	 *            the {@link Image} to decorate the base image.
	 * @param corner
	 *            the corner to place decorator image.
	 * @return the resulting decorated {@link Image}.
	 */
	public static Image decorateImage(final Image baseImage, final Image decorator, final int corner) {
		if (corner <= 0 || corner >= LAST_CORNER_KEY) {
			throw new IllegalArgumentException(Messages.ResourceManager_0);
		}
		Map<Image, Map<Image, Image>> cornerDecoratedImageMap = decoratedImageMap[corner];
		if (cornerDecoratedImageMap == null) {
			cornerDecoratedImageMap = new HashMap<>();
			decoratedImageMap[corner] = cornerDecoratedImageMap;
		}
		Map<Image, Image> decoratedMap = cornerDecoratedImageMap.computeIfAbsent(baseImage, k -> new HashMap<Image, Image>());
		return decoratedMap.computeIfAbsent(decorator, k -> createImage(baseImage, decorator, corner));
	}

	private static Image createImage(final Image baseImage, final Image decorator, final int corner) {
		final Rectangle bib = baseImage.getBounds();
		final Rectangle dib = decorator.getBounds();
		final Point baseImageSize = new Point(bib.width, bib.height);
		CompositeImageDescriptor compositImageDesc = new CompositeImageDescriptor() {
			@Override
			protected void drawCompositeImage(int width, int height) {
				drawImage(createCachedImageDataProvider(baseImage), 0, 0);
				if (corner == TOP_LEFT) {
					drawImage(getUnzoomedImageDataProvider(decorator.getImageData()) , 0, 0);
				} else if (corner == TOP_RIGHT) {
					drawImage(getUnzoomedImageDataProvider(decorator.getImageData()), bib.width - dib.width, 0);
				} else if (corner == BOTTOM_LEFT) {
					drawImage(getUnzoomedImageDataProvider(decorator.getImageData()), 0, bib.height - dib.height);
				} else if (corner == BOTTOM_RIGHT) {
					drawImage(getUnzoomedImageDataProvider(decorator.getImageData()), bib.width - dib.width, bib.height - dib.height);
				}
			}
			@Override
			protected Point getSize() {
				return baseImageSize;
			}
		};
		return compositImageDesc.createImage();
	}

	private static ImageDataProvider getUnzoomedImageDataProvider(ImageData imageData) {
		return zoom -> zoom == 100 ? imageData : null;
	}


	/**
	 * Dispose all of the cached images.
	 */
	public static void disposeImages() {
		SWTResourceManager.disposeImages();
		// dispose ImageDescriptor images
		for (Iterator<Image> I = descriptorImageMap.values().iterator(); I.hasNext();) {
			I.next().dispose();
		}
		descriptorImageMap.clear();
		// dispose decorated images
		for (int i = 0; i < decoratedImageMap.length; i++) {
			Map<Image, Map<Image, Image>> cornerDecoratedImageMap = decoratedImageMap[i];
			if (cornerDecoratedImageMap != null) {
				for (Map<Image, Image> decoratedMap : cornerDecoratedImageMap.values()) {
					for (Image image : decoratedMap.values()) {
						image.dispose();
					}
					decoratedMap.clear();
				}
				cornerDecoratedImageMap.clear();
			}
		}
		// dispose plugin images
		for (Iterator<Image> I = urlImageMap.values().iterator(); I.hasNext();) {
			I.next().dispose();
		}
		urlImageMap.clear();
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Plugin images support
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Maps URL to images.
	 */
	private static Map<String, Image> urlImageMap = new HashMap<>();

	/**
	 * Provider for plugin resources, used by WindowBuilder at design time.
	 */
	public interface PluginResourceProvider {
		URL getEntry(String symbolicName, String path);
	}

	/**
	 * Instance of {@link PluginResourceProvider}, used by WindowBuilder at design
	 * time.
	 */
	private static PluginResourceProvider designTimePluginResourceProvider = null;

	/**
	 * Returns an {@link Image} based on a {@link Bundle} and resource entry path.
	 * 
	 * @param symbolicName the symbolic name of the {@link Bundle}.
	 * @param path         the path of the resource entry.
	 * @return the {@link Image} stored in the file at the specified path.
	 */
	public static Image getPluginImage(String symbolicName, String path) {
		try {
			URL url = getPluginImageURL(symbolicName, path);
			if (url != null) {
				return getPluginImageFromUrl(url);
			}
		} catch (Exception e) {
			// Ignore any exceptions
		}
		return null;
	}

	/**
	 * Returns an {@link Image} based on given {@link URL}.
	 */
	private static Image getPluginImageFromUrl(URL url) {
		try {
			String key = url.toExternalForm();
			Image image = urlImageMap.get(key);
			if (image == null) {
				InputStream stream = url.openStream();
				try {
					image = getImage(stream);
					urlImageMap.put(key, image);
				} finally {
					stream.close();
				}
			}
			return image;
		} catch (Exception e) {
			// Ignore any exceptions
		}
		return null;
	}

	/**
	 * Returns an {@link ImageDescriptor} based on a {@link Bundle} and resource
	 * entry path.
	 * 
	 * @param symbolicName the symbolic name of the {@link Bundle}.
	 * @param path         the path of the resource entry.
	 * @return the {@link ImageDescriptor} based on a {@link Bundle} and resource
	 *         entry path.
	 */
	public static ImageDescriptor getPluginImageDescriptor(String symbolicName, String path) {
		try {
			URL url = getPluginImageURL(symbolicName, path);
			if (url != null) {
				return ImageDescriptor.createFromURL(url);
			}
		} catch (Exception e) {
			// Ignore any exceptions
		}
		return null;
	}

	/**
	 * Returns an {@link URL} based on a {@link Bundle} and resource entry path.
	 */
	private static URL getPluginImageURL(String symbolicName, String path) {
		// try runtime plugins
		Bundle bundle = Platform.getBundle(symbolicName);
		if (bundle != null) {
			return bundle.getEntry(path);
		}
		// try design time provider
		if (designTimePluginResourceProvider != null) {
			return designTimePluginResourceProvider.getEntry(symbolicName, path);
		}
		// no such resource
		return null;
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// General
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Dispose of cached objects and their underlying OS resources. This should only
	 * be called when the cached objects are no longer needed (e.g. on application
	 * shutdown).
	 */
	public static void dispose() {
		disposeColors();
		disposeFonts();
		disposeImages();
	}
}