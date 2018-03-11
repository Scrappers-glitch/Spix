package spix.swing.tools;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePacker {

    private BufferedImage[] images = new BufferedImage[4];
    private BufferedImage[] thumbs = new BufferedImage[4];
    protected int[] indexMap = new int[4];
    private int width = 0;
    private int height = 0;

    public ImagePacker() {
        for (int i = 0; i < indexMap.length; i++) {
            indexMap[i] = -1;
        }
    }

    public BufferedImage preview(int index, int channel) {
        if(images[index] == null){
            return null;
        }
        indexMap[index] = channel;
        BufferedImage source = thumbs[index];
        BufferedImage result = new BufferedImage(
                128,
                128,
                BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                int pixel = getChannel(source, x, y, channel);

                int alpha = 255;
                int value = getRgbaAsInt(pixel, pixel, pixel, alpha);

                result.setRGB(x, y, value);
            }
        }

        return result;
    }

    protected void setImage(BufferedImage img, int index ){
        images[index] = img;
        if(width == 0){
            width = img.getWidth();
            height = img.getHeight();
        }
    }
    protected void setThumb(BufferedImage img, int index ){
        thumbs[index] = img;
    }

    public int getRgbaAsInt(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                ((b & 0xFF));
    }


    public BufferedImage getThumb(BufferedImage srcImg) {
        return getResizedImage(srcImg, 128, 128);
    }

    public BufferedImage getResizedImage(BufferedImage srcImg, int width, int height) {
        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, width, height, null);
        g2.dispose();

        return resizedImg;
    }


    public BufferedImage pack() {

        BufferedImage result = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_INT_ARGB);

        for (int i = 1; i < images.length; i++) {
            if (images[i].getWidth() != width) {
                images[i] = getResizedImage(images[i], width, height);
            }
        }

        for (int x = 0; x < result.getWidth(); x++) {
            for (int y = 0; y < result.getHeight(); y++) {
                int r = getChannel(images[0], x, y, indexMap[0]);
                int g = getChannel(images[1], x, y, indexMap[1]);
                int b = getChannel(images[2], x, y, indexMap[2]);
                int a = getChannel(images[3], x, y, indexMap[3]);
                int pixel = getRgbaAsInt(r, g, b, a);
                result.setRGB(x, y, pixel);
            }
        }

        return result;
    }

    private int getChannel(BufferedImage source, int x, int y, int channel) {
        int pixel = 0;
        switch (channel) {
            case 0:
                pixel = (source.getRGB(x, y) >> 16) & 0x000000FF;
                break;
            case 1:
                pixel = (source.getRGB(x, y) >> 8) & 0x000000FF;
                break;
            case 2:
                pixel = (source.getRGB(x, y)) & 0x000000FF;
                break;
            case 3:
                pixel = (source.getRGB(x, y) >> 24) & 0x000000FF;
                break;

        }
        return pixel;
    }

}
