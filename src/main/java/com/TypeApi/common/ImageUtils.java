package com.TypeApi.common;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Coordinate;
import net.coobird.thumbnailator.geometry.Position;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
public class ImageUtils {
    /**
     * 图片尺寸不变，压缩图片文件大小
     *
     * @param bytes     图片文件二进制流
     * @param quality   质量因子 1为最高质量
     * @return
     */
    public static byte[] compressImage(byte[] bytes, float quality) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            // 图片尺寸不变，压缩图片文件大小outputQuality实现，参数1为最高质量
            Thumbnails.of(in).scale(1f).outputFormat("webp").outputQuality(quality).toOutputStream(bout);

            byte[] compressiondata = bout.toByteArray();

            return compressiondata;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 指定宽高压缩图片
     *
     * @param bytes  图片文件二进制流
     * @param width  压缩宽度
     * @param height 压缩高度
     * @return
     */
    public static byte[] compressImageWithWH(byte[] bytes, int width, int height) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            Thumbnails.of(in).size(width, height).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 根据比列压缩图片
     *
     * @param bytes
     * @param scale
     * @return
     */
    public static byte[] compressImageWithScale(byte[] bytes, double scale) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            Thumbnails.of(in).scale(scale).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 转换图片格式
     *
     * @param bytes             源图片文件流
     * @param toformatImageType 转换后图片格式
     * @return
     */
    public static byte[] formatImage(byte[] bytes, String toformatImageType, int width, int height) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            Thumbnails.of(in).size(width, height).outputFormat(toformatImageType).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 根据坐标裁剪图片
     *
     * @param bytes           源图片文件流
     * @param x               起始x坐标
     * @param y               起始y坐标
     * @param x1              结束x坐标
     * @param y1              结束y坐标
     * @param keepAspectRatio 默认是按照比例缩放的,值为false 时不按比例缩放
     * @return
     */
    public static byte[] cutImage(byte[] bytes, int x, int y, int x1, int y1, boolean keepAspectRatio) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            int width = x1 - x;
            int height = y1 - y;
            Thumbnails.of(in).sourceRegion(x, y, x1, y1).size(width, height).keepAspectRatio(keepAspectRatio).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 添加图片水印
     *
     * @param bytes     源图片文件流
     * @param width     宽度
     * @param height    高度
     * @param position  位置 Positions.BOTTOM_RIGHT
     * @param watermark 水印图片地址
     * @param opacity   透明度 0.5f
     * @param quality   图片质量 0.8f
     * @return
     */
    public static byte[] addImageWater(byte[] bytes, int width, int height, Position position, String watermark, float opacity, float quality) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            Thumbnails.of(in)
                    .size(width, height)
                    // 加水印 参数：1.水印位置 2.水印图片 3.不透明度0.0-1.0
                    .watermark(position, ImageIO.read(new File(watermark)), opacity)
                    .outputQuality(quality)
                    .toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 添加文字水印
     *
     * @param bytes
     * @param position
     * @param waterText
     * @param rotate
     * @param opacity
     * @param quality
     * @return
     */
    public static byte[] addTextWater(byte[] bytes, Position position, String waterText, double rotate, float opacity, float quality) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            // 设置480x160的大小区域显示水印文本
            BufferedImage bi = new BufferedImage(480, 160, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bi.createGraphics();

            // 设置绘图区域透明
            bi = g.getDeviceConfiguration().createCompatibleImage(480, 160, Transparency.TRANSLUCENT);
            g.dispose();
            g = bi.createGraphics();

            // 设置字体类型、大小、加粗、颜色
            g.setFont(new Font("微软雅黑", Font.BOLD, 32));
            g.setColor(new Color(0, 0, 0));
            char[] data = waterText.toCharArray();
            // 设置文本显示坐标（0,80）
            g.drawChars(data, 0, data.length, 0, 80);
            g.dispose();

            Thumbnails.of(in).scale(1).watermark(position, bi, opacity).outputQuality(quality).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 整屏添加文字水印
     *
     * @param bytes          源图片二进制流
     * @param width          图片宽度
     * @param height         图片高度
     * @param intervalWidth  间隔宽度
     * @param intervalHeight 间隔高度
     * @param waterTextList  水印内容列表
     * @param fontSize       文字大小
     * @param opacity        透明度
     * @param quality        质量
     * @return
     */
    public static byte[] addTextWaterFullScreen(byte[] bytes, int width, int height, int intervalWidth, int intervalHeight, List<String> waterTextList, int fontSize, float opacity, float quality) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            // 设置图片大小区域显示水印文本
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            // 创建一个Graphics2D的对象
            Graphics2D g = bi.createGraphics();

            // 设置绘图区域透明，即背景透明
            bi = g.getDeviceConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            g.dispose();
            g = bi.createGraphics();

            // 设置字体类型，加粗，字体大小
            Font font = new Font("微软雅黑", Font.BOLD, fontSize);
            g.setFont(font);
            // 旋转角度（单位：弧度），以圆点（0,0）为圆心，正代表顺时针，负代表逆时针
            g.rotate(Math.toRadians(-30), 0, 0);
            // 设置字体颜色
            g.setColor(new Color(0, 0, 0));
            int distance = fontSize + 12;
            int size = waterTextList.size();
            // 设置文字字体显示坐标位置
            for (int i = 0; i < size; i++) {
                char[] data = waterTextList.get(i).toCharArray();
                g.drawChars(data, 0, data.length, 0, height / 2 + i * distance);
            }
            g.dispose();

            Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(in).scale(1);

            // 添加文字水印
            int wMod = (int) Math.ceil(width / intervalWidth);
            int hMod = (int) Math.ceil(height / intervalHeight);
            for (int i = 0; i <= wMod; i++) {
                for (int j = 0; j <= hMod; j++) {
                    int x = (i) * intervalWidth - intervalWidth / 2;
                    int y = (j) * intervalHeight - intervalHeight / 2;
                    System.out.println(x + "," + y);
                    builder.watermark(new Coordinate(x, y), bi, opacity);
                }
            }

            builder.outputQuality(quality).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 旋转 ,正数：顺时针 负数：逆时针
     *
     * @param bytes  源图片文件流
     * @param width  宽
     * @param height 高
     * @param rotate 角度
     */
    public static byte[] rotateImage(byte[] bytes, int width, int height, double rotate) {
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = new ByteArrayInputStream(bytes);
            bout = new ByteArrayOutputStream(1024);

            Thumbnails.of(in).size(width, height).rotate(rotate).toOutputStream(bout);

            return bout.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (bout != null) {
                    bout.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}
