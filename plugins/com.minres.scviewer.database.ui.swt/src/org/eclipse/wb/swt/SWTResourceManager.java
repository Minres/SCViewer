/*******************************************************************************
 * Copyright (c) 2011 Google, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Google, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.wb.swt;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Utility class for managing OS resources associated with SWT controls such as
 * colors, fonts, images, etc.
 * <p>
 * !!! IMPORTANT !!! Application code must explicitly invoke the
 * <code>dispose()</code> method to release the operating system resources
 * managed by cached objects when those objects and OS resources are no longer
 * needed (e.g. on application shutdown)
 * <p>
 * This class may be freely distributed as part of any application or plugin.
 * <p>
 * 
 * @author scheglov_ke
 * @author Dan Rubel
 */
public class SWTResourceManager {
	////////////////////////////////////////////////////////////////////////////
	//
	// Color
	//
	////////////////////////////////////////////////////////////////////////////
	private static Map<RGB, Color> colorMap = new HashMap<>();

	private SWTResourceManager() {}
	
	/**
	 * Returns the system {@link Color} matching the specific ID.
	 * 
	 * @param systemColorID the ID value for the color
	 * @return the system {@link Color} matching the specific ID
	 */
	public static Color getColor(int systemColorID) {
		Display display = Display.getCurrent();
		return display.getSystemColor(systemColorID);
	}

	/**
	 * Returns a {@link Color} given its red, green and blue component values.
	 * 
	 * @param r the red component of the color
	 * @param g the green component of the color
	 * @param b the blue component of the color
	 * @return the {@link Color} matching the given red, green and blue component
	 *         values
	 */
	public static Color getColor(int r, int g, int b) {
		return getColor(new RGB(r, g, b));
	}

	/**
	 * Returns a {@link Color} given its RGB value.
	 * 
	 * @param rgb the {@link RGB} value of the color
	 * @return the {@link Color} matching the RGB value
	 */
	public static Color getColor(RGB rgb) {
		return colorMap.computeIfAbsent(rgb, k -> new Color(Display.getCurrent(), rgb));
	}

	/**
	 * Dispose of all the cached {@link Color}'s.
	 */
	public static void disposeColors() {
		for (Color color : colorMap.values()) {
			color.dispose();
		}
		colorMap.clear();
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Image
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Maps image paths to images.
	 */
	private static Map<String, Image> imageMap = new HashMap<>();

	/**
	 * Returns an {@link Image} encoded by the specified {@link InputStream}.
	 * 
	 * @param stream the {@link InputStream} encoding the image data
	 * @return the {@link Image} encoded by the specified input stream
	 */
	protected static Image getImage(InputStream stream) throws IOException {
		try {
			Display display = Display.getCurrent();
			ImageData data = new ImageData(stream);
			if (data.transparentPixel > 0) {
				return new Image(display, data, data.getTransparencyMask());
			}
			return new Image(display, data);
		} finally {
			stream.close();
		}
	}

	/**
	 * Returns an {@link Image} stored in the file at the specified path.
	 * 
	 * @param path the path to the image file
	 * @return the {@link Image} stored in the file at the specified path
	 */
	public static Image getImage(String path) {
		Image image = imageMap.get(path);
		if (image == null) {
			try {
				image = getImage(new FileInputStream(path));
				imageMap.put(path, image);
			} catch (Exception e) {
				image = getMissingImage();
				imageMap.put(path, image);
			}
		}
		return image;
	}

	/**
	 * Returns an {@link Image} stored in the file at the specified path relative to
	 * the specified class.
	 * 
	 * @param clazz the {@link Class} relative to which to find the image
	 * @param path  the path to the image file, if starts with <code>'/'</code>
	 * @return the {@link Image} stored in the file at the specified path
	 */
	public static Image getImage(Class<?> clazz, String path) {
		String key = clazz.getName() + '|' + path;
		Image image = imageMap.get(key);
		if (image == null) {
			try {
				image = getImage(clazz.getResourceAsStream(path));
				imageMap.put(key, image);
			} catch (Exception e) {
				image = getMissingImage();
				imageMap.put(key, image);
			}
		}
		return image;
	}

	private static final int MISSING_IMAGE_SIZE = 10;

	/**
	 * @return the small {@link Image} that can be used as placeholder for missing
	 *         image.
	 */
	private static Image getMissingImage() {
		Image image = new Image(Display.getCurrent(), MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
		//
		GC gc = new GC(image);
		gc.setBackground(getColor(SWT.COLOR_RED));
		gc.fillRectangle(0, 0, MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
		gc.dispose();
		//
		return image;
	}

	/**
	 * Style constant for placing decorator image in top left corner of base image.
	 */
	public static final int TOP_LEFT = 1;
	/**
	 * Style constant for placing decorator image in top right corner of base image.
	 */
	public static final int TOP_RIGHT = 2;
	/**
	 * Style constant for placing decorator image in bottom left corner of base
	 * image.
	 */
	public static final int BOTTOM_LEFT = 3;
	/**
	 * Style constant for placing decorator image in bottom right corner of base
	 * image.
	 */
	public static final int BOTTOM_RIGHT = 4;
	/**
	 * Internal value.
	 */
	protected static final int LAST_CORNER_KEY = 5;
	/**
	 * Maps images to decorated images.
	 */
	@SuppressWarnings("unchecked")
	private static Map<Image, Map<Image, Image>>[] decoratedImageMap = new Map[LAST_CORNER_KEY];

	/**
	 * Returns an {@link Image} composed of a base image decorated by another image.
	 * 
	 * @param baseImage the base {@link Image} that should be decorated
	 * @param decorator the {@link Image} to decorate the base image
	 * @return {@link Image} The resulting decorated image
	 */
	public static Image decorateImage(Image baseImage, Image decorator) {
		return decorateImage(baseImage, decorator, BOTTOM_RIGHT);
	}

	/**
	 * Returns an {@link Image} composed of a base image decorated by another image.
	 * 
	 * @param baseImage the base {@link Image} that should be decorated
	 * @param decorator the {@link Image} to decorate the base image
	 * @param corner    the corner to place decorator image
	 * @return the resulting decorated {@link Image}
	 */
	public static Image decorateImage(final Image baseImage, final Image decorator, final int corner) {
		if (corner <= 0 || corner >= LAST_CORNER_KEY) {
			throw new IllegalArgumentException("Wrong decorate corner");
		}
		Map<Image, Map<Image, Image>> cornerDecoratedImageMap = decoratedImageMap[corner];
		if (cornerDecoratedImageMap == null) {
			cornerDecoratedImageMap = new HashMap<>();
			decoratedImageMap[corner] = cornerDecoratedImageMap;
		}
		Map<Image, Image> decoratedMap = cornerDecoratedImageMap.computeIfAbsent(baseImage,
				k -> new HashMap<Image, Image>());
		return decoratedMap.computeIfAbsent(decorator, k -> {
			Rectangle bib = baseImage.getBounds();
			Rectangle dib = decorator.getBounds();
			Image result = new Image(Display.getCurrent(), bib.width, bib.height);
			GC gc = new GC(result);
			gc.drawImage(baseImage, 0, 0);
			switch (corner) {
			case TOP_LEFT:
				gc.drawImage(decorator, 0, 0);
				break;
			case TOP_RIGHT:
				gc.drawImage(decorator, bib.width - dib.width, 0);
				break;
			case BOTTOM_LEFT:
				gc.drawImage(decorator, 0, bib.height - dib.height);
				break;
			case BOTTOM_RIGHT:
				gc.drawImage(decorator, bib.width - dib.width, bib.height - dib.height);
				break;
			default:
				// do nothing
			}
			gc.dispose();
			return result;
		});
	}

	/**
	 * Dispose all of the cached {@link Image}'s.
	 */
	public static void disposeImages() {
		// dispose loaded images
		for (Image image : imageMap.values()) {
			image.dispose();
		}
		imageMap.clear();
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
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Font
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Maps font names to fonts.
	 */
	private static Map<String, Font> fontMap = new HashMap<>();
	/**
	 * Maps fonts to their bold versions.
	 */
	private static Map<Font, Font> fontToBoldFontMap = new HashMap<>();

	/**
	 * Returns a {@link Font} based on its name, height and style.
	 * 
	 * @param name   the name of the font
	 * @param height the height of the font
	 * @param style  the style of the font
	 * @return {@link Font} The font matching the name, height and style
	 */
	public static Font getFont(String name, int height, int style) {
		return getFont(name, height, style, false, false);
	}

	/**
	 * Returns a {@link Font} based on its name, height and style. Windows-specific
	 * strikeout and underline flags are also supported.
	 * 
	 * @param name      the name of the font
	 * @param size      the size of the font
	 * @param style     the style of the font
	 * @param strikeout the strikeout flag (warning: Windows only)
	 * @param underline the underline flag (warning: Windows only)
	 * @return {@link Font} The font matching the name, height, style, strikeout and
	 *         underline
	 */
	public static Font getFont(String name, int size, int style, boolean strikeout, boolean underline) {
		String fontName = name + '|' + size + '|' + style + '|' + strikeout + '|' + underline;
		return fontMap.computeIfAbsent(fontName, k -> {
			FontData fontData = new FontData(name, size, style);
			if (strikeout || underline) {
				try {
					Class<?> logFontClass = Class.forName("org.eclipse.swt.internal.win32.LOGFONT"); //$NON-NLS-1$
					Object logFont = FontData.class.getField("data").get(fontData); //$NON-NLS-1$
					if (logFont != null && logFontClass != null) {
						if (strikeout) {
							logFontClass.getField("lfStrikeOut").set(logFont, Byte.valueOf((byte) 1)); //$NON-NLS-1$
						}
						if (underline) {
							logFontClass.getField("lfUnderline").set(logFont, Byte.valueOf((byte) 1)); //$NON-NLS-1$
						}
					}
				} catch (Exception e) {
				}
			}
			return new Font(Display.getCurrent(), fontData);

		});

	}

	/**
	 * Returns a bold version of the given {@link Font}.
	 * 
	 * @param baseFont the {@link Font} for which a bold version is desired
	 * @return the bold version of the given {@link Font}
	 */
	public static Font getBoldFont(Font baseFont) {
		return fontToBoldFontMap.computeIfAbsent(baseFont, k -> {
			FontData[] fontDatas = baseFont.getFontData();
			FontData data = fontDatas[0];
			return new Font(Display.getCurrent(), data.getName(), data.getHeight(), SWT.BOLD);
		});
	}

	/**
	 * Dispose all of the cached {@link Font}'s.
	 */
	public static void disposeFonts() {
		// clear fonts
		for (Font font : fontMap.values()) {
			font.dispose();
		}
		fontMap.clear();
		// clear bold fonts
		for (Font font : fontToBoldFontMap.values()) {
			font.dispose();
		}
		fontToBoldFontMap.clear();
	}

	////////////////////////////////////////////////////////////////////////////
	//
	// Cursor
	//
	////////////////////////////////////////////////////////////////////////////
	/**
	 * Maps IDs to cursors.
	 */
	private static Map<Integer, Cursor> idToCursorMap = new HashMap<>();

	/**
	 * Returns the system cursor matching the specific ID.
	 * 
	 * @param id int The ID value for the cursor
	 * @return Cursor The system cursor matching the specific ID
	 */
	public static Cursor getCursor(int id) {
		Integer key = Integer.valueOf(id);
		return idToCursorMap.computeIfAbsent(key, k -> new Cursor(Display.getDefault(), id));
	}

	/**
	 * Dispose all of the cached cursors.
	 */
	public static void disposeCursors() {
		for (Cursor cursor : idToCursorMap.values()) {
			cursor.dispose();
		}
		idToCursorMap.clear();
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
		disposeImages();
		disposeFonts();
		disposeCursors();
	}
}