package com.example.hsi_converter;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    Bitmap mainImage;
    Bitmap displayImage;

    public void updateColor (double deltaHue, double deltaSaturation, double deltaIntensity) {
        ImageView mImageView = (ImageView) findViewById(R.id.imageViewMain);
        int[] pixels = new int[mainImage.getHeight() * mainImage.getWidth()];
        mainImage.getPixels(pixels, 0, mainImage.getWidth(), 0, 0, mainImage.getWidth(),
                mainImage.getHeight());
        HSI [] mainImagePixelsHSI = ARGBToHSI(pixels);
        for(int i = 0; i < mainImagePixelsHSI.length; i++) {
            mainImagePixelsHSI[i].hue = mainImagePixelsHSI[i].hue + deltaHue;
            mainImagePixelsHSI[i].saturation = mainImagePixelsHSI[i].saturation + deltaSaturation;
            mainImagePixelsHSI[i].intensity = mainImagePixelsHSI[i].intensity + deltaIntensity;
        }

        displayImage.setPixels(HSItoARGB(mainImagePixelsHSI), 0, mainImage.getWidth(), 0, 0, mainImage.getWidth(),
                mainImage.getHeight());
        mImageView.setImageBitmap(displayImage);
    }


    public class HSI {
        double hue;
        double saturation;
        double intensity;
    }

    private HSI[] ARGBToHSI(int[] input) {
        HSI[] result = new HSI[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = new HSI();
            int R = (input[i] & 0x00FF0000) >> 16; //Extract color
            int G = (input[i] & 0x0000FF00) >> 8;
            int B = input[i] & 0x000000FF;
            double sum = R + G + B;
            double r = 0;
            double g = 0;
            double b = 0;

            if (sum > 0) {
                r = R / sum; //Normalization
                g = G / sum;
                b = B / sum;
            }

            double max = Math.max(r, Math.max(g, b));
            double min = Math.min(r, Math.min(g, b));

            double h = 0;

            result[i].intensity = (R + G + B) / 3.0;

            if (max == min) {
                result[i].hue = result[i].saturation = 0;
            } else {
                double hueRad = 0; //Hue in radians
                double hueCommon = Math.acos((0.5 * ((r - g) + (r - b))) / Math.sqrt(
                        Math.pow((r - g), 2) + (r - b) * (g - b)));
                if (B <= G) {
                    hueRad = hueCommon;
                } else {
                    hueRad = (2 * Math.PI) - hueCommon;
                }
                result[i].hue = Math.toDegrees(hueRad);
                result[i].saturation = 1 - 3 * min;
            }
        }

        return result;
    }

    private int[] HSItoARGB(HSI[] input) {
        int[] result = new int[input.length];
        int R = 0;
        int G = 0;
        int B = 0;

        for (int i = 0; i < input.length; i++) {
            double h = input[i].hue * Math.PI / 180;
            double hn = 0; //normalized value
            if (h < 2 * Math.PI / 3) {
                hn = h;
            } else if (2 * Math.PI / 3 <= h && h < 4 * Math.PI / 3) {
                hn = h - 2 * Math.PI / 3;
            } else if (4 * Math.PI / 3 <= h && h < 2 * Math.PI) {
                hn = h - 4 * Math.PI / 3;
            }

            double j = input[i].intensity / 255;
            double x = j * (1 - input[i].saturation);
            double y = j * (1 + (input[i].saturation * Math.cos(hn)) / Math.cos(Math.PI / 3 - hn));
            double z = 3 * j - (x + y);
            double r = 0;
            double g = 0;
            double b = 0;

            if (h < 2 * Math.PI / 3) {
                r = y;
                g = z;
                b = x;
            } else if (2 * Math.PI / 3 <= h && h < 4 * Math.PI / 3) {
                r = x;
                g = y;
                b = z;
            } else if (4 * Math.PI / 3 <= h && h < 2 * Math.PI) {
                r = z;
                g = x;
                b = y;
            }
            R = (int) (r * 255);
            G = (int) (g * 255);
            B = (int) (b * 255);

            result[i] = 0xFF000000 | R << 16 | G << 8 | B;
        }
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        mainImage = BitmapFactory.decodeResource(getResources(), R.drawable.example,
                options);
        ImageView mImageView = (ImageView) findViewById(R.id.imageViewMain);
        displayImage = mainImage.copy(mainImage.getConfig(), true);

        mImageView.setImageBitmap(displayImage);

        SeekBar skHue = (SeekBar) findViewById(R.id.seekBarHue);
        SeekBar skSaturation = (SeekBar) findViewById(R.id.seekBarSaturation);
        SeekBar skIntensity = (SeekBar) findViewById(R.id.seekBarIntensity);

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int hueVal = skHue.getProgress();
                int saturationVal = skSaturation.getProgress();
                int intensityVal = skIntensity.getProgress();
                double deltaHue = 0;
                double deltaSaturation = 0;
                double deltaIntensity = 0;
                int pointsNumber = 500;
                double hueCoef = 360/1000.0;
                double saturationCoef = 0.001;
                double intensityCoef = 255/1000.0;
                deltaHue = (hueVal - pointsNumber) * hueCoef;
                deltaSaturation = (saturationVal - pointsNumber) * saturationCoef;
                deltaIntensity = (intensityVal - pointsNumber) * intensityCoef;

                updateColor(deltaHue, deltaSaturation, deltaIntensity);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        skHue.setOnSeekBarChangeListener(seekBarChangeListener);
        skSaturation.setOnSeekBarChangeListener(seekBarChangeListener);
        skIntensity.setOnSeekBarChangeListener(seekBarChangeListener);
    }
}