package mil.nga.geopackage.tiles;

import android.graphics.Rect;
import android.graphics.RectF;

import mil.nga.geopackage.BoundingBox;

/**
 * Tile Bounding Box utility methods relying on Android libraries
 *
 * @author osbornb
 */
public class TileBoundingBoxAndroidUtils {

    /**
     * Get a rectangle using the tile width, height, bounding box, and the
     * bounding box section within the outer box to build the rectangle from
     *
     * @param width
     * @param height
     * @param boundingBox
     * @param boundingBoxSection
     * @return
     */
    public static Rect getRectangle(long width, long height,
                                    BoundingBox boundingBox, BoundingBox boundingBoxSection) {

        RectF rectF = getFloatRectangle(width, height, boundingBox,
                boundingBoxSection);

        Rect rect = new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));

        return rect;
    }

    /**
     * Get a rectangle with floating point boundaries using the tile width,
     * height, bounding box, and the bounding box section within the outer box
     * to build the rectangle from
     *
     * @param width
     * @param height
     * @param boundingBox
     * @param boundingBoxSection
     * @return
     */
    public static RectF getFloatRectangle(long width, long height,
                                          BoundingBox boundingBox, BoundingBox boundingBoxSection) {

        float left = TileBoundingBoxUtils.getXPixel(width, boundingBox,
                boundingBoxSection.getMinLongitude());
        float right = TileBoundingBoxUtils.getXPixel(width, boundingBox,
                boundingBoxSection.getMaxLongitude());
        float top = TileBoundingBoxUtils.getYPixel(height, boundingBox,
                boundingBoxSection.getMaxLatitude());
        float bottom = TileBoundingBoxUtils.getYPixel(height, boundingBox,
                boundingBoxSection.getMinLatitude());

        RectF rect = new RectF(left, top, right, bottom);

        return rect;
    }

}